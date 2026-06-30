package com.hubinity.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.hubinity.catalog.config.RabbitConfig;
import com.hubinity.catalog.integration.OutboxDispatcher;

/**
 * Verifica o comportamento do dispatcher quando o broker está indisponível.
 * Usa apenas Postgres real; RabbitTemplate é mockado seletivamente:
 * envio ao EXCHANGE lança AmqpException, envio ao DLX passa normalmente.
 *
 * <p>Fluxo esperado: após {@code maxAttempts} (5) falhas consecutivas a mensagem
 * é marcada FAILED e publicada no DLX (→ DLQ) para inspeção operacional.
 */
@SpringBootTest(
        webEnvironment = WebEnvironment.NONE,
        properties = {
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
                // sem host rabbitmq → RabbitAutoConfiguration não ativa, evita conexão ao broker
                "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
                // reduz tentativas para 5 para o teste ser determinístico
                "app.outbox.dispatch.max-attempts=5"
        }
)
@ActiveProfiles("integration")
@Testcontainers
@Tag("integration")
@DisplayName("OutboxDispatcher — falha de broker após maxAttempts → FAILED + DLQ")
class OutboxBrokerFailureIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("hb_catalog_test")
                    .withUsername("hb_catalog")
                    .withPassword("hb_catalog");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @MockitoBean
    private JwtDecoder jwtDecoder;

    /**
     * Mock seletivo: send ao EXCHANGE principal lança AmqpException (broker down);
     * send ao DLX (para o DLQ) passa normalmente — verificamos que é chamado uma vez.
     */
    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Autowired private OutboxDispatcher dispatcher;
    @Autowired private OutboxMessageRepository outboxMessages;

    @BeforeEach
    void configureMockAndCleanOutbox() {
        outboxMessages.deleteAll();

        // send(EXCHANGE, routingKey, message) → simula broker indisponível
        doThrow(new AmqpException("simulação de broker indisponível"))
                .when(rabbitTemplate).send(eq(RabbitConfig.EXCHANGE), anyString(), any(Message.class));
        // send(DLX, "", message) → passa normalmente (default: doNothing em void mock)
    }

    @Test
    @DisplayName("Broker down: após 5 falhas a mensagem é FAILED e o DLQ recebe exatamente uma vez")
    void brokerDown_afterMaxAttempts_marksFailedAndSendsToDlq() {
        OutboxMessage msg = new OutboxMessage(
                UUID.randomUUID(), "Product", UUID.randomUUID().toString(),
                "ProductCreated", RabbitConfig.EXCHANGE.equals("catalog.events")
                        ? "catalog.product.created" : "catalog.product.created",
                "1.0.0", "{\"productId\":\"" + UUID.randomUUID() + "\"}");
        outboxMessages.save(msg);

        // 4 primeiras tentativas: PENDING, attempts cresce mas não atinge maxAttempts
        for (int i = 0; i < 4; i++) {
            dispatcher.dispatchPending();
        }
        OutboxMessage after4 = outboxMessages.findAll().get(0);
        assertThat(after4.getStatus()).isEqualTo(OutboxMessageStatus.PENDING);
        assertThat(after4.getAttempts()).isEqualTo(4);
        assertThat(after4.getLastError()).contains("broker indisponível");

        // 5ª tentativa: atinge maxAttempts → FAILED + DLQ
        dispatcher.dispatchPending();

        OutboxMessage failed = outboxMessages.findAll().get(0);
        assertThat(failed.getStatus()).isEqualTo(OutboxMessageStatus.FAILED);
        assertThat(failed.getAttempts()).isEqualTo(5);

        // DLX deve ter recebido a mensagem exatamente uma vez
        verify(rabbitTemplate, times(1))
                .send(eq(RabbitConfig.DLX), eq(""), any(Message.class));

        // 6ª chamada não deve processar nada (mensagem é FAILED, não PENDING)
        dispatcher.dispatchPending();
        assertThat(outboxMessages.findAll().get(0).getAttempts()).isEqualTo(5);
    }
}

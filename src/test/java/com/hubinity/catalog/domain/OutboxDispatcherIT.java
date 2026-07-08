package com.hubinity.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
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
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.hubinity.catalog.api.dto.CategoryRequest;
import com.hubinity.catalog.api.dto.ProductRequest;
import com.hubinity.catalog.api.error.DuplicateSkuException;
import com.hubinity.catalog.config.RabbitConfig;
import com.hubinity.catalog.integration.OutboxDispatcher;
import com.hubinity.catalog.service.CategoryService;
import com.hubinity.catalog.service.ProductService;

/**
 * Testes de integração do Outbox Pattern com Postgres + RabbitMQ reais.
 *
 * <p>Cenários cobertos:
 * <ul>
 *   <li>Happy path: criar produto → PENDING → dispatch → PUBLISHED + mensagem na queue</li>
 *   <li>Rollback de TX: se o negócio falha, nada no outbox</li>
 *   <li>2 dispatchers concorrentes: cada mensagem publicada exatamente uma vez (SKIP LOCKED)</li>
 * </ul>
 */
@SpringBootTest(
        webEnvironment = WebEnvironment.NONE,
        properties = {"spring.security.oauth2.resourceserver.jwt.issuer-uri="}
)
@ActiveProfiles("integration")
@Testcontainers
@Tag("integration")
@DisplayName("OutboxDispatcher — integração com Postgres + RabbitMQ")
class OutboxDispatcherIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("hb_catalog_test")
                    .withUsername("hb_catalog")
                    .withPassword("hb_catalog");

    @Container
    @SuppressWarnings("resource")
    static final RabbitMQContainer RABBIT =
            new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void containers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host",     RABBIT::getHost);
        registry.add("spring.rabbitmq.port",     RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
    }

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired private ProductService productService;
    @Autowired private CategoryService categoryService;
    @Autowired private OutboxDispatcher dispatcher;
    @Autowired private OutboxMessageRepository outboxMessages;
    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private AmqpAdmin amqpAdmin;

    private String testQueue;

    @BeforeEach
    void declareTestQueue() {
        testQueue = "test-outbox-" + UUID.randomUUID();
        amqpAdmin.declareQueue(QueueBuilder.nonDurable(testQueue).build());
        amqpAdmin.declareBinding(new Binding(
                testQueue, Binding.DestinationType.QUEUE,
                RabbitConfig.EXCHANGE, "catalog.#", null));
        // garante que o outbox está limpo antes de cada teste
        outboxMessages.deleteAll();
    }

    @Test
    @DisplayName("Happy path: criar produto → outbox PENDING → dispatch → PUBLISHED + mensagem na queue")
    void createProduct_outboxPending_dispatch_publishedAndMessageInQueue() {
        UUID productId = newProduct("outbox-happy-" + UUID.randomUUID());

        List<OutboxMessage> pending = outboxMessages.findAll();
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getStatus()).isEqualTo(OutboxMessageStatus.PENDING);
        assertThat(pending.get(0).getRoutingKey()).isEqualTo("catalog.product.created");
        assertThat(pending.get(0).getAggregateId()).isEqualTo(productId.toString());

        dispatcher.dispatchPending();

        OutboxMessage published = outboxMessages.findAll().get(0);
        assertThat(published.getStatus()).isEqualTo(OutboxMessageStatus.PUBLISHED);
        assertThat(published.getPublishedAt()).isNotNull();
        assertThat(published.getAttempts()).isZero();

        Message received = rabbitTemplate.receive(testQueue, 5_000);
        assertThat(received).isNotNull();
        assertThat(received.getMessageProperties().getHeader("eventType").toString())
                .isEqualTo("ProductCreated");
    }

    @Test
    @DisplayName("Rollback de TX: negócio falha → nada adicionado ao outbox")
    void businessTxRollback_noOutboxEntry() {
        UUID categoryId = categoryService.create(
                new CategoryRequest("rollback-cat-" + UUID.randomUUID(), "slug-" + UUID.randomUUID(),
                        null, null, null)).id();

        // primeira criação bem-sucedida → 1 entrada no outbox
        productService.create(new ProductRequest(
                "ROLLBACK-SKU", "Produto 1", null,
                new BigDecimal("9.90"), null, categoryId, null, null));
        assertThat(outboxMessages.findAll()).hasSize(1);

        // segunda criação com mesmo SKU → DuplicateSkuException → rollback
        assertThatThrownBy(() -> productService.create(new ProductRequest(
                "ROLLBACK-SKU", "Produto 2", null,
                new BigDecimal("9.90"), null, categoryId, null, null)))
                .isInstanceOf(DuplicateSkuException.class);

        // rollback garantiu que o outbox ainda tem apenas 1 entrada
        assertThat(outboxMessages.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("2 dispatchers concorrentes: SKIP LOCKED garante que cada mensagem é publicada exatamente uma vez")
    void concurrentDispatchers_eachMessagePublishedExactlyOnce() throws Exception {
        int count = 10;
        for (int i = 0; i < count; i++) {
            OutboxMessage msg = new OutboxMessage(
                    UUID.randomUUID(), "Product", UUID.randomUUID().toString(),
                    "ProductCreated", "catalog.product.created", "1.0.0",
                    "{\"seq\":" + i + "}");
            outboxMessages.save(msg);
        }
        assertThat(outboxMessages.findAll()).hasSize(count);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go    = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Callable<Void> dispatchTask = () -> {
            ready.countDown();
            go.await();
            dispatcher.dispatchPending();
            return null;
        };

        List<Future<Void>> futures = new ArrayList<>();
        futures.add(pool.submit(dispatchTask));
        futures.add(pool.submit(dispatchTask));

        ready.await();
        go.countDown();

        for (Future<Void> f : futures) {
            f.get();
        }
        pool.shutdown();

        List<OutboxMessage> all = outboxMessages.findAll();
        assertThat(all).hasSize(count);
        assertThat(all).allMatch(m -> m.getStatus() == OutboxMessageStatus.PUBLISHED);

        // drena a queue e verifica que cada mensagem chegou exatamente uma vez
        List<Message> received = new ArrayList<>();
        Message msg;
        while ((msg = rabbitTemplate.receive(testQueue, 500)) != null) {
            received.add(msg);
        }
        assertThat(received).hasSize(count);
    }

    private UUID newProduct(String prefix) {
        String slug = prefix.replaceAll("[^a-z0-9\\-]", "-").toLowerCase().substring(0, Math.min(prefix.length(), 50));
        UUID categoryId = categoryService.create(
                new CategoryRequest(prefix, slug + "-" + UUID.randomUUID().toString().substring(0, 8),
                        null, null, null)).id();
        return productService.create(new ProductRequest(
                prefix.toUpperCase().substring(0, Math.min(prefix.length(), 20)),
                prefix, null, new BigDecimal("9.90"), null, categoryId, null, null)).id();
    }
}

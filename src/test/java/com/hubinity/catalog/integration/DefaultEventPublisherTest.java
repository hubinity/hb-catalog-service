package com.hubinity.catalog.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hubinity.catalog.domain.OutboxMessage;
import com.hubinity.catalog.domain.OutboxMessageRepository;
import com.hubinity.catalog.domain.Product;
import com.hubinity.catalog.domain.StockMovementType;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Verifica o envelope dos eventos publicados: {@code eventId} do payload deve
 * ser IGUAL ao {@code messageId} do outbox (chave única de deduplicação
 * at-least-once) e {@code schemaVersion} deve refletir o contrato v1
 * (schemas em platform-shared-contracts/contracts-events/events/v1/).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultEventPublisher — envelope eventId/schemaVersion")
class DefaultEventPublisherTest {

    @Mock
    private OutboxMessageRepository outboxMessages;

    @Captor
    private ArgumentCaptor<OutboxMessage> messageCaptor;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private DefaultEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new DefaultEventPublisher(outboxMessages, jsonMapper);
    }

    @Test
    @DisplayName("publishStockChanged: payload espelha o schema StockChanged v1 e eventId == messageId")
    void publishStockChanged_payloadMatchesSchemaAndEventIdEqualsMessageId() {
        UUID productId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-07-03T12:00:00Z");

        // COMMIT de 3 unidades: available inalterado (7), reserved 5 -> 2
        publisher.publishStockChanged(productId, 7, 2, StockMovementType.COMMIT, 3, occurredAt);

        OutboxMessage msg = capturedMessage();
        JsonNode payload = jsonMapper.readTree(msg.getPayload());

        assertThat(payload.get("eventId").asString()).isEqualTo(msg.getMessageId().toString());
        assertThat(payload.get("schemaVersion").asString())
                .isEqualTo(DefaultEventPublisher.SCHEMA_VERSION);
        assertThat(payload.get("productId").asString()).isEqualTo(productId.toString());
        assertThat(payload.get("previousAvailable").asInt()).isEqualTo(7);
        assertThat(payload.get("previousReserved").asInt()).isEqualTo(5);
        assertThat(payload.get("available").asInt()).isEqualTo(7);
        assertThat(payload.get("reserved").asInt()).isEqualTo(2);
        assertThat(payload.get("changeType").asString()).isEqualTo("COMMIT");
        assertThat(payload.get("quantity").asInt()).isEqualTo(3);
        assertThat(payload.has("occurredAt")).isTrue();
        assertThat(msg.getSchemaVersion()).isEqualTo(DefaultEventPublisher.SCHEMA_VERSION);
    }

    @Test
    @DisplayName("publishProductCreated: envelope presente e eventId == messageId")
    void publishProductCreated_envelopePresentAndEventIdEqualsMessageId() {
        Product product = newProduct();

        publisher.publishProductCreated(product);

        OutboxMessage msg = capturedMessage();
        JsonNode payload = jsonMapper.readTree(msg.getPayload());

        assertThat(payload.get("eventId").asString()).isEqualTo(msg.getMessageId().toString());
        assertThat(payload.get("schemaVersion").asString())
                .isEqualTo(DefaultEventPublisher.SCHEMA_VERSION);
        assertThat(payload.get("productId").asString()).isEqualTo(product.getId().toString());
        assertThat(payload.get("sku").asString()).isEqualTo("SKU-ENV-1");
        assertThat(payload.get("active").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("publishPriceChanged: envelope presente e eventId == messageId")
    void publishPriceChanged_envelopePresentAndEventIdEqualsMessageId() {
        Product product = newProduct();

        publisher.publishPriceChanged(product, new BigDecimal("9.90"));

        OutboxMessage msg = capturedMessage();
        JsonNode payload = jsonMapper.readTree(msg.getPayload());

        assertThat(payload.get("eventId").asString()).isEqualTo(msg.getMessageId().toString());
        assertThat(payload.get("schemaVersion").asString())
                .isEqualTo(DefaultEventPublisher.SCHEMA_VERSION);
        assertThat(payload.get("previousPrice").decimalValue())
                .isEqualByComparingTo(new BigDecimal("9.90"));
        assertThat(payload.get("newPrice").decimalValue())
                .isEqualByComparingTo(new BigDecimal("14.90"));
    }

    private OutboxMessage capturedMessage() {
        verify(outboxMessages).save(messageCaptor.capture());
        OutboxMessage msg = messageCaptor.getValue();
        assertThat(msg.getMessageId()).isNotNull();
        return msg;
    }

    private Product newProduct() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setSku("SKU-ENV-1");
        product.setName("Envelope Test");
        product.setPrice(new BigDecimal("14.90"));
        product.setCategoryId(UUID.randomUUID());
        product.setActive(true);
        return product;
    }
}

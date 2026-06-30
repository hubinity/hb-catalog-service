package com.hubinity.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;

import com.hubinity.catalog.domain.CategoryRepository;
import com.hubinity.catalog.domain.IdempotencyRecordRepository;
import com.hubinity.catalog.domain.OutboxMessageRepository;
import com.hubinity.catalog.domain.PriceHistoryRepository;
import com.hubinity.catalog.domain.ProductRepository;
import com.hubinity.catalog.domain.StockItemRepository;
import com.hubinity.catalog.domain.StockMovementRepository;
import com.hubinity.catalog.domain.StockReservationRepository;

@SpringBootTest
@ActiveProfiles("test")
class HbCatalogServiceApplicationTests {

    /**
     * {@code application-test.yml} excludes {@code OAuth2ResourceServerAutoConfiguration}
     * to keep this context-load test fully offline, so no real {@link JwtDecoder}
     * bean is created. {@link com.hubinity.catalog.config.SecurityConfig} still
     * wires {@code oauth2ResourceServer().jwt(...)} and therefore requires a
     * {@code JwtDecoder} in the context — this mock satisfies that requirement
     * without contacting Keycloak.
     */
    @MockitoBean
    private JwtDecoder jwtDecoder;

    /**
     * {@code application-test.yml} also excludes {@code DataJpaRepositoriesAutoConfiguration}
     * (no DataSource here either), so no real Spring Data proxy exists for
     * {@link CategoryRepository}. {@code CategoryService} (the first real business
     * bean wired into the full context) needs one — mocked here for the same
     * "fully offline" reason as {@link #jwtDecoder}.
     */
    @MockitoBean
    private CategoryRepository categoryRepository;

    /**
     * {@code ProductService} (002-product-rest-endpoints) needs four more JPA
     * repositories that don't exist in this offline context, for the same
     * reason as {@link #categoryRepository} above.
     */
    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private PriceHistoryRepository priceHistoryRepository;

    @MockitoBean
    private StockItemRepository stockItemRepository;

    @MockitoBean
    private StockReservationRepository stockReservationRepository;

    /**
     * {@code StockService}/{@code StockController} (003-stock-movement-reservation) need two
     * more JPA repositories that don't exist in this offline context, for the same reason as
     * {@link #categoryRepository} above.
     */
    @MockitoBean
    private StockMovementRepository stockMovementRepository;

    @MockitoBean
    private IdempotencyRecordRepository idempotencyRecordRepository;

    /**
     * {@code OutboxMessageRepository} não existe neste contexto offline
     * (DataJpaRepositoriesAutoConfiguration excluído). {@code DefaultEventPublisher}
     * precisa dele — mockado aqui pelo mesmo motivo dos demais repositories acima.
     */
    @MockitoBean
    private OutboxMessageRepository outboxMessageRepository;

    /**
     * {@code RabbitConfig} é condicional em {@code spring.rabbitmq.host} (não definido
     * no perfil test). {@code OutboxDispatcher} e {@code DefaultEventPublisher} precisam
     * de {@code RabbitTemplate} — este mock satisfaz a injeção sem broker real.
     */
    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    /**
     * {@code HibernateJpaAutoConfiguration} é excluído no perfil test (sem DataSource),
     * portanto nenhum {@code PlatformTransactionManager} é auto-configurado.
     * {@code OutboxDispatcher} precisa dele para construir o {@code TransactionTemplate}.
     */
    @MockitoBean
    private PlatformTransactionManager platformTransactionManager;

    @Test
    void contextLoads() {
    }
}

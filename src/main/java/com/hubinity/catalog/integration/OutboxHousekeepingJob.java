package com.hubinity.catalog.integration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hubinity.catalog.domain.OutboxMessageRepository;
import com.hubinity.catalog.domain.OutboxMessageStatus;

/**
 * Remove mensagens publicadas com mais de 7 dias da tabela {@code outbox_messages}.
 * Executado uma vez por dia (meia-noite UTC) para controlar crescimento da tabela.
 */
@Component
public class OutboxHousekeepingJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxHousekeepingJob.class);

    private final OutboxMessageRepository outboxMessages;

    public OutboxHousekeepingJob(OutboxMessageRepository outboxMessages) {
        this.outboxMessages = outboxMessages;
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    @Transactional
    public void deleteStalePublished() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        outboxMessages.deletePublishedBefore(OutboxMessageStatus.PUBLISHED, cutoff);
        log.info("event=outbox_housekeeping cutoff={}", cutoff);
    }
}

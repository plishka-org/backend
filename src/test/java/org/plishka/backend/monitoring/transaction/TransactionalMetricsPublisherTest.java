package org.plishka.backend.monitoring.transaction;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionalMetricsPublisherTest {
    private TransactionalMetricsPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new TransactionalMetricsPublisher();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void afterCompletionOrNow_ShouldRunCommitCallbackImmediately_WhenNoTransactionIsActive() {
        AtomicInteger committed = new AtomicInteger();
        AtomicInteger rolledBack = new AtomicInteger();

        publisher.afterCompletionOrNow(committed::incrementAndGet, rolledBack::incrementAndGet);

        assertEquals(1, committed.get());
        assertEquals(0, rolledBack.get());
    }

    @Test
    void afterCompletionOrNow_ShouldDeferCommitCallbackUntilTransactionCommits() {
        AtomicInteger committed = new AtomicInteger();
        AtomicInteger rolledBack = new AtomicInteger();
        startTransactionSynchronization();

        publisher.afterCompletionOrNow(committed::incrementAndGet, rolledBack::incrementAndGet);

        assertEquals(0, committed.get());
        assertEquals(0, rolledBack.get());

        completeTransaction(TransactionSynchronization.STATUS_COMMITTED);

        assertEquals(1, committed.get());
        assertEquals(0, rolledBack.get());
    }

    @Test
    void afterCompletionOrNow_ShouldRunRollbackCallback_WhenTransactionRollsBack() {
        AtomicInteger committed = new AtomicInteger();
        AtomicInteger rolledBack = new AtomicInteger();
        startTransactionSynchronization();

        publisher.afterCompletionOrNow(committed::incrementAndGet, rolledBack::incrementAndGet);

        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        assertEquals(0, committed.get());
        assertEquals(1, rolledBack.get());
    }

    private void startTransactionSynchronization() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    private void completeTransaction(int status) {
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(status));
    }
}

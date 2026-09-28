package org.plishka.backend.monitoring.transaction;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publishes metrics only after the current Spring transaction outcome is known.
 *
 * <p>If no transaction is active, the commit callback runs immediately. Callers must not use this
 * as proof that a {@code @transactional} boundary exists; self-invocation or async thread hops can
 * bypass Spring's transaction proxy and make metrics publish immediately.
 */
@Component
public class TransactionalMetricsPublisher {

    /**
     * Runs {@code onCommit} after commit, or {@code onRollbackOrUnknown} after rollback/unknown.
     * When called outside an active transaction, runs {@code onCommit} immediately.
     */
    public void afterCompletionOrNow(Runnable onCommit, Runnable onRollbackOrUnknown) {
        if (!hasActiveTransactionSynchronization()) {
            onCommit.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    onCommit.run();
                    return;
                }

                onRollbackOrUnknown.run();
            }
        });
    }

    private boolean hasActiveTransactionSynchronization() {
        return TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive();
    }
}

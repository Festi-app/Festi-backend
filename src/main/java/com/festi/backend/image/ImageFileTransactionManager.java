package com.festi.backend.image;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class ImageFileTransactionManager {

    private final ImageStorage imageStorage;

    public void replaceAfterTransaction(String previousUrl, String replacementUrl) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            imageStorage.deleteIfManaged(replacementUrl);
            throw new IllegalStateException("Image lifecycle operations require an active transaction.");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                imageStorage.deleteIfManaged(previousUrl);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    imageStorage.deleteIfManaged(replacementUrl);
                }
            }
        });
    }

    public void deleteAfterCommit(String publicUrl) {
        assertTransactionSynchronization();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                imageStorage.deleteIfManaged(publicUrl);
            }
        });
    }

    private void assertTransactionSynchronization() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Image lifecycle operations require an active transaction.");
        }
    }
}

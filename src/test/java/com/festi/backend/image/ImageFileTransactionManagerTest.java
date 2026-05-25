package com.festi.backend.image;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class ImageFileTransactionManagerTest {

    @Mock
    private ImageStorage imageStorage;

    private ImageFileTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        transactionManager = new ImageFileTransactionManager(imageStorage);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deletesReplacementOnRollback() {
        transactionManager.replaceAfterTransaction(
                "/media/images/booths/old.png", "/media/images/booths/new.png");

        TransactionSynchronizationManager.getSynchronizations().forEach(
                synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(imageStorage).deleteIfManaged("/media/images/booths/new.png");
        verify(imageStorage, never()).deleteIfManaged("/media/images/booths/old.png");
    }

    @Test
    void deletesPreviousImageOnlyAfterCommit() {
        transactionManager.replaceAfterTransaction(
                "/media/images/booths/old.png", "/media/images/booths/new.png");

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        TransactionSynchronizationManager.getSynchronizations().forEach(
                synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));

        verify(imageStorage).deleteIfManaged("/media/images/booths/old.png");
        verify(imageStorage, never()).deleteIfManaged("/media/images/booths/new.png");
    }

    @Test
    void schedulesRemovedImageForCommitOnlyDeletion() {
        transactionManager.deleteAfterCommit("/media/images/menus/old.png");

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(imageStorage).deleteIfManaged("/media/images/menus/old.png");
    }

    @Test
    void removesStoredReplacementWhenCalledWithoutTransactionSynchronization() {
        TransactionSynchronizationManager.clearSynchronization();

        assertThatThrownBy(() -> transactionManager.replaceAfterTransaction(
                "/media/images/booths/old.png", "/media/images/booths/new.png"))
                .isInstanceOf(IllegalStateException.class);

        verify(imageStorage).deleteIfManaged("/media/images/booths/new.png");
    }
}

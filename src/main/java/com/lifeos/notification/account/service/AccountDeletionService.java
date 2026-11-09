package com.lifeos.notification.account.service;

import java.util.UUID;

/** KVKK: on {@code UserDeletionRequested}, Notification forgets everything it holds for the user. */
public interface AccountDeletionService {

    void deleteAll(UUID userId);
}

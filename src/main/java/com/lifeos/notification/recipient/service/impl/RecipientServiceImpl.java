package com.lifeos.notification.recipient.service.impl;

import com.lifeos.base.error.CommonErrorCode;
import com.lifeos.base.error.LifeOsException;
import com.lifeos.notification.common.error.NotificationErrorCode;
import com.lifeos.notification.recipient.dto.RecipientResponse;
import com.lifeos.notification.recipient.dto.UpdatePreferencesRequest;
import com.lifeos.notification.recipient.entity.Recipient;
import com.lifeos.notification.recipient.mapper.RecipientMapper;
import com.lifeos.notification.recipient.repository.RecipientRepository;
import com.lifeos.notification.recipient.service.RecipientService;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipientServiceImpl implements RecipientService {

    private final RecipientRepository repository;
    private final RecipientMapper recipientMapper;
    private final Clock clock;

    public RecipientServiceImpl(RecipientRepository repository, RecipientMapper recipientMapper, Clock clock) {
        this.repository = repository;
        this.recipientMapper = recipientMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RecipientResponse getOrCreate(UUID userId) {
        return recipientMapper.toResponse(loadOrCreate(userId));
    }

    @Override
    @Transactional
    public void profileUpdated(UUID userId, String locale, String displayName) {
        loadOrCreate(userId).updateProfile(locale, displayName, clock.instant());
    }

    @Override
    @Transactional
    public RecipientResponse updatePreferences(UUID userId, UpdatePreferencesRequest request, String verifiedEmail) {
        Recipient recipient = loadOrCreate(userId);
        if (recipient.getVersion() != request.version()) {
            throw new LifeOsException(CommonErrorCode.CONCURRENT_MODIFICATION,
                    "The preferences were changed in the meantime. Reload them and try again.",
                    Map.of("currentVersion", recipient.getVersion()));
        }
        if (request.emailEnabled() && verifiedEmail == null) {
            throw new LifeOsException(NotificationErrorCode.NOTIFICATION_EMAIL_NOT_VERIFIED,
                    "Verify your e-mail address in your account before turning e-mail on.");
        }
        recipient.changePreferences(request.pushEnabled(), request.emailEnabled(),
                request.emailEnabled() ? verifiedEmail : null, clock.instant());
        repository.saveAndFlush(recipient);
        return recipientMapper.toResponse(recipient);
    }

    private Recipient loadOrCreate(UUID userId) {
        repository.insertIfAbsent(userId, DEFAULT_LOCALE, clock.instant());
        return repository.findById(userId).orElseThrow();
    }
}

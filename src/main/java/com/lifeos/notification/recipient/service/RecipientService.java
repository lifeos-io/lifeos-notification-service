package com.lifeos.notification.recipient.service;

import com.lifeos.notification.recipient.dto.RecipientResponse;
import com.lifeos.notification.recipient.dto.UpdatePreferencesRequest;
import java.util.UUID;

/** Who receives notifications, in which language, on which channels. */
public interface RecipientService {

    /** Strategy's default language for a new profile; used until the user's profile event arrived. */
    String DEFAULT_LOCALE = "tr-TR";

    /** The user's settings, created with defaults (push on, e-mail off) on first need. */
    RecipientResponse getOrCreate(UUID userId);

    /** From Strategy's {@code UserProfileUpdated}. */
    void profileUpdated(UUID userId, String locale, String displayName);

    /**
     * @param verifiedEmail the address in the user's token if it is verified, else {@code null}; turning e-mail on
     *                      needs it (and refreshes the stored address)
     */
    RecipientResponse updatePreferences(UUID userId, UpdatePreferencesRequest request, String verifiedEmail);
}

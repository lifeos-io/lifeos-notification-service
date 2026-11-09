package com.lifeos.notification.recipient.dto;

import jakarta.validation.constraints.NotNull;

/**
 * {@code PUT /api/notifications/preferences}: which channels the user wants. Turning e-mail on takes the verified
 * address from the user's token.
 *
 * @param version the version the client last read (409 {@code CONCURRENT_MODIFICATION} when it is stale)
 */
public record UpdatePreferencesRequest(
        @NotNull Integer version,
        @NotNull Boolean pushEnabled,
        @NotNull Boolean emailEnabled) {
}

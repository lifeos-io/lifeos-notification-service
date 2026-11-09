package com.lifeos.notification.recipient.dto;

/**
 * The user's notification settings.
 *
 * @param email the address e-mail goes to, if one was given
 */
public record RecipientResponse(
        String locale,
        String displayName,
        String email,
        boolean pushEnabled,
        boolean emailEnabled,
        int version) {
}

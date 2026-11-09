package com.lifeos.notification.recipient.controller;

import com.lifeos.base.web.security.CurrentUser;
import com.lifeos.notification.recipient.dto.RecipientResponse;
import com.lifeos.notification.recipient.dto.UpdatePreferencesRequest;
import com.lifeos.notification.recipient.service.RecipientService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/notifications/preferences}: the channels the signed-in user wants. */
@RestController
@RequestMapping("/api/notifications/preferences")
public class PreferencesController {

    private final RecipientService recipientService;

    public PreferencesController(RecipientService recipientService) {
        this.recipientService = recipientService;
    }

    @GetMapping
    public RecipientResponse get(@CurrentUser UUID userId) {
        return recipientService.getOrCreate(userId);
    }

    /** E-mail goes to the address in the token, and only if Keycloak marked it verified. */
    @PutMapping
    public RecipientResponse update(@CurrentUser UUID userId, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdatePreferencesRequest request) {
        String verifiedEmail = Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))
                ? jwt.getClaimAsString("email") : null;
        return recipientService.updatePreferences(userId, request, verifiedEmail);
    }
}

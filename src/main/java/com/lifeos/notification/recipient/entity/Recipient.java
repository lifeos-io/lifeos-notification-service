package com.lifeos.notification.recipient.entity;

import com.lifeos.notification.common.persistence.AssignedIdEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Who receives notifications: their language and name (from Strategy's {@code UserProfileUpdated}), their e-mail
 * address (from their own token, when they turn e-mail on) and which channels they want. Push is on and e-mail off
 * until they say otherwise.
 */
@Entity
@Table(name = "recipient")
public class Recipient extends AssignedIdEntity {

    @Id
    private UUID userId;

    private String locale;
    private String displayName;
    private String email;
    private boolean pushEnabled;
    private boolean emailEnabled;

    @Version
    private int version;

    private Instant createdAt;
    private Instant updatedAt;

    protected Recipient() {
    }

    public static Recipient withDefaults(UUID userId, String locale, Instant now) {
        Recipient recipient = new Recipient();
        recipient.userId = Objects.requireNonNull(userId);
        recipient.locale = Objects.requireNonNull(locale);
        recipient.pushEnabled = true;
        recipient.emailEnabled = false;
        recipient.createdAt = now;
        recipient.updatedAt = now;
        return recipient;
    }

    public void updateProfile(String locale, String displayName, Instant now) {
        this.locale = Objects.requireNonNull(locale);
        this.displayName = displayName;
        this.updatedAt = now;
    }

    /** {@code email} is required while e-mail is on; it stays stored when e-mail is turned off. */
    public void changePreferences(boolean pushEnabled, boolean emailEnabled, String email, Instant now) {
        String address = email != null ? email : this.email;
        if (emailEnabled && address == null) {
            throw new IllegalArgumentException("E-mail needs an address");
        }
        this.pushEnabled = pushEnabled;
        this.emailEnabled = emailEnabled;
        this.email = address;
        this.updatedAt = now;
    }

    @Override
    public UUID getId() {
        return userId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getLocale() {
        return locale;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public boolean isPushEnabled() {
        return pushEnabled;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public int getVersion() {
        return version;
    }
}

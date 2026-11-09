package com.lifeos.notification.recipient.repository;

import com.lifeos.notification.recipient.entity.Recipient;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecipientRepository extends JpaRepository<Recipient, UUID> {

    /**
     * Creates the recipient with defaults unless it exists; safe when an event and a request of the same user race
     * (PostgreSQL {@code ON CONFLICT DO NOTHING}).
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO recipient (user_id, locale, push_enabled, email_enabled, version, created_at, updated_at)
            VALUES (:userId, :locale, true, false, 0, :now, :now)
            ON CONFLICT (user_id) DO NOTHING
            """)
    int insertIfAbsent(@Param("userId") UUID userId, @Param("locale") String locale, @Param("now") Instant now);
}

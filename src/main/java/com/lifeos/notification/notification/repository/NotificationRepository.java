package com.lifeos.notification.notification.repository;

import com.lifeos.notification.notification.entity.Notification;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    boolean existsBySourceEventId(UUID sourceEventId);

    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    /** Newest first; with {@code unreadOnly}, only those not read yet. */
    @Query("""
            SELECT n FROM Notification n
            WHERE n.userId = :userId AND (:unreadOnly = false OR n.readAt IS NULL)
            ORDER BY n.createdAt DESC, n.id DESC
            """)
    List<Notification> search(@Param("userId") UUID userId, @Param("unreadOnly") boolean unreadOnly);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.userId = :userId AND n.readAt IS NULL")
    int markAllRead(@Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.userId = :userId")
    int deleteByUserId(@Param("userId") UUID userId);
}

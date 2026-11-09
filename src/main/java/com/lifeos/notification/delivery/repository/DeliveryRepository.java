package com.lifeos.notification.delivery.repository;

import com.lifeos.notification.delivery.entity.Delivery;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    List<Delivery> findByNotificationId(UUID notificationId);

    /** Claims up to {@code limit} due deliveries for this transaction; rows another sender holds are skipped. */
    @Query(nativeQuery = true, value = """
            SELECT * FROM delivery
            WHERE status = 'PENDING' AND next_attempt_at <= :now
            ORDER BY next_attempt_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """)
    List<Delivery> claimDue(@Param("now") Instant now, @Param("limit") int limit);

    @Modifying
    @Query("DELETE FROM Delivery d WHERE d.userId = :userId")
    int deleteByUserId(@Param("userId") UUID userId);
}

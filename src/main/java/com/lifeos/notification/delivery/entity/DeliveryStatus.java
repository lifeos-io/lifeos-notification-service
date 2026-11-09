package com.lifeos.notification.delivery.entity;

/** PENDING (tried until it works) → SENT, or FAILED after the last attempt. */
public enum DeliveryStatus {
    PENDING,
    SENT,
    FAILED
}

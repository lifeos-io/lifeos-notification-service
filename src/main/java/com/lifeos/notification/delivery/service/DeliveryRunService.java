package com.lifeos.notification.delivery.service;

/** Sends the deliveries that are due, and schedules the next try of those that fail. */
public interface DeliveryRunService {

    /**
     * Claims and sends up to {@code limit} due deliveries in one transaction.
     *
     * @return how many were handled; fewer than {@code limit} means none are left for now
     */
    int runDue(int limit);
}

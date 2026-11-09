package com.lifeos.notification.inbound.event;

/** The user is the envelope's userId; nothing in the payload is needed. */
public final class UserDeletionRequestedV1 {

    public static final String TYPE = "UserDeletionRequested";

    private UserDeletionRequestedV1() {
    }
}

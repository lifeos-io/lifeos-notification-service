package com.lifeos.notification.common.error;

import com.lifeos.base.error.ErrorCode;

/** Notification's own error codes, next to the shared ones in {@code CommonErrorCode}. */
public enum NotificationErrorCode implements ErrorCode {

    /** E-mail can be turned on only with a verified address in the user's token. */
    NOTIFICATION_EMAIL_NOT_VERIFIED(422, "No verified e-mail address");

    private final int status;
    private final String title;

    NotificationErrorCode(int status, String title) {
        this.status = status;
        this.title = title;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public int status() {
        return status;
    }

    @Override
    public String title() {
        return title;
    }
}

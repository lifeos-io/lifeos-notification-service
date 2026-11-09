package com.lifeos.notification.notification.entity;

/** What a notification is about; each comes from one kind of event. */
public enum NotificationKind {
    /** Trigger's {@code DecisionReviewDue}. */
    DECISION_REVIEW_DUE,
    /** Trigger's {@code ReopenTriggerFired}. */
    REOPEN_TRIGGER_FIRED,
    /** Trigger's {@code WeeklyReviewDue}. */
    WEEKLY_REVIEW_DUE,
    /** Trigger's {@code DriftDetected}. */
    DRIFT_DETECTED,
    /** Strategy's {@code DecisionAssumptionFalsified}. */
    ASSUMPTION_FALSIFIED,
    /** Coach's {@code RecommendationProposed}. */
    PROPOSAL_RECEIVED
}

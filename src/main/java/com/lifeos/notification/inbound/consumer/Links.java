package com.lifeos.notification.inbound.consumer;

import java.util.UUID;

/** Where the mobile app opens a notification ({@code lifeos://} deep links). */
final class Links {

    private Links() {
    }

    static String decision(UUID decisionId) {
        return "lifeos://decisions/" + decisionId;
    }

    static String weeklyPlan(String isoWeek) {
        return "lifeos://weekly-plans/" + isoWeek;
    }

    static String weeklyReview(String isoWeek) {
        return "lifeos://weekly-plans/" + isoWeek + "/review";
    }

    static String proposals(UUID goalId) {
        return "lifeos://goals/" + goalId + "/proposals";
    }
}

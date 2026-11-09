package com.lifeos.notification.inbound.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WeeklyReviewDueV1(UUID weeklyPlanId, String isoWeek) {

    public static final String TYPE = "WeeklyReviewDue";
}

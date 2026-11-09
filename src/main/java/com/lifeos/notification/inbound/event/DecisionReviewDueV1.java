package com.lifeos.notification.inbound.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DecisionReviewDueV1(UUID decisionId, String title, LocalDate reviewOn) {

    public static final String TYPE = "DecisionReviewDue";
}

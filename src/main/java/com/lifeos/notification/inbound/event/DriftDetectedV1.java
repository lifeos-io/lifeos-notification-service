package com.lifeos.notification.inbound.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DriftDetectedV1(UUID weeklyPlanId, String isoWeek, int plannedMinutes, int doneMinutes) {

    public static final String TYPE = "DriftDetected";
}

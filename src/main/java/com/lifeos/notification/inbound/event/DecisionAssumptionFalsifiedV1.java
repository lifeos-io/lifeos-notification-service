package com.lifeos.notification.inbound.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DecisionAssumptionFalsifiedV1(UUID decisionId, String title, String statement) {

    public static final String TYPE = "DecisionAssumptionFalsified";
}

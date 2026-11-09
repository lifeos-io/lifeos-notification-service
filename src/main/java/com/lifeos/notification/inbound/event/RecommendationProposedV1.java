package com.lifeos.notification.inbound.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecommendationProposedV1(UUID recommendationId, UUID goalId, String rationale) {

    public static final String TYPE = "RecommendationProposed";
}

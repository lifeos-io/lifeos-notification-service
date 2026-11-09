package com.lifeos.notification.inbound.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/** Trigger does not send the decision's title; the message names the trigger instead. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReopenTriggerFiredV1(UUID decisionId, String description) {

    public static final String TYPE = "ReopenTriggerFired";
}

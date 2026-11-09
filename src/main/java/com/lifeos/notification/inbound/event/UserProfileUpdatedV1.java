package com.lifeos.notification.inbound.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserProfileUpdatedV1(String displayName, String locale) {

    public static final String TYPE = "UserProfileUpdated";
}

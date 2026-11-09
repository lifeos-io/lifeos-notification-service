package com.lifeos.notification.api;

import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lifeos.notification.support.Events;
import com.lifeos.notification.support.IntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** The inbox, preferences and devices over HTTP, each scoped to the signed-in user. */
class NotificationApiIntegrationTest extends IntegrationTest {

    private Events events;

    @BeforeEach
    void setUp() {
        events = new Events(jsonMapper);
        clock.set(Instant.parse("2026-11-15T15:00:00Z"));
    }

    @Test
    void theInboxIsReadOneByOneOrAllAtOnce() throws Exception {
        UUID user = UUID.randomUUID();
        event("trigger", user, "WeeklyReviewDue", events.weeklyReviewDue(UUID.randomUUID(), "2026-W45"));
        clock.set(clock.instant().plusSeconds(60));
        event("trigger", user, "WeeklyReviewDue", events.weeklyReviewDue(UUID.randomUUID(), "2026-W46"));

        String newest = jsonMapper.readTree(mockMvc.perform(get("/api/notifications").with(as(user)))
                        .andExpect(jsonPath("$[*].body").value(contains(
                                "2026-W46 haftası bitiyor. Kısa bir değerlendirmeyle haftanı kapat.",
                                "2026-W45 haftası bitiyor. Kısa bir değerlendirmeyle haftanı kapat.")))
                        .andExpect(jsonPath("$[0].readAt").isEmpty())
                        .andReturn().getResponse().getContentAsString())
                .get(0).get("id").asString();

        mockMvc.perform(post("/api/notifications/" + newest + "/read").with(as(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readAt").value("2026-11-15T15:01:00Z"));
        mockMvc.perform(get("/api/notifications").param("unread", "true").with(as(user)))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(post("/api/notifications/" + newest + "/read").with(as(UUID.randomUUID())))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/notifications/read-all").with(as(user))).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/notifications").param("unread", "true").with(as(user)))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/notifications").with(as(UUID.randomUUID())))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void emailNeedsAVerifiedAddressFromTheToken() throws Exception {
        UUID user = UUID.randomUUID();
        mockMvc.perform(get("/api/notifications/preferences").with(as(user)))
                .andExpect(jsonPath("$.locale").value("tr-TR"))
                .andExpect(jsonPath("$.pushEnabled").value(true))
                .andExpect(jsonPath("$.emailEnabled").value(false))
                .andExpect(jsonPath("$.email").isEmpty())
                .andExpect(jsonPath("$.version").value(0));

        String body = "{\"version\": 0, \"pushEnabled\": false, \"emailEnabled\": true}";
        preferences(user, body, jwt().jwt(token -> token.subject(user.toString())
                        .claim("email", "unverified@lifeos.test").claim("email_verified", false)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_EMAIL_NOT_VERIFIED"));
        preferences(user, body, as(user))
                .andExpect(jsonPath("$.code").value("NOTIFICATION_EMAIL_NOT_VERIFIED"));

        preferences(user, body, as(user, "onurcan@lifeos.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("onurcan@lifeos.test"))
                .andExpect(jsonPath("$.emailEnabled").value(true))
                .andExpect(jsonPath("$.pushEnabled").value(false))
                .andExpect(jsonPath("$.version").value(1));
        preferences(user, body, as(user, "onurcan@lifeos.test"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"))
                .andExpect(jsonPath("$.currentVersion").value(1));
        // Turning e-mail off keeps the address (KVKK deletion removes it).
        preferences(user, "{\"version\": 1, \"pushEnabled\": true, \"emailEnabled\": false}", as(user))
                .andExpect(jsonPath("$.emailEnabled").value(false))
                .andExpect(jsonPath("$.email").value("onurcan@lifeos.test"));
        preferences(user, "{\"version\": 2, \"pushEnabled\": true}", as(user))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("emailEnabled"));
    }

    @Test
    void aDeviceBelongsToWhoeverRegisteredItLast() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        String token = "shared-phone-" + UUID.randomUUID();

        registerDevice(first, token, "ANDROID").andExpect(status().isNoContent());
        registerDevice(first, token, "ANDROID").andExpect(status().isNoContent());
        registerDevice(second, token, "IOS").andExpect(status().isNoContent());

        assertEquals(second, owner(token));
        mockMvc.perform(delete("/api/notifications/devices/" + token).with(as(first)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/notifications/devices/" + token).with(as(second)))
                .andExpect(status().isNoContent());
        assertEquals(0L, jdbcClient.sql("SELECT count(*) FROM device WHERE token = :t").param("t", token)
                .query(Long.class).single());

        registerDevice(first, " ", "ANDROID").andExpect(status().isBadRequest());
        registerDevice(first, "token", "SYMBIAN").andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
    }

    private ResultActions preferences(UUID user, String body, RequestPostProcessor auth) throws Exception {
        return mockMvc.perform(put("/api/notifications/preferences").with(auth)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions registerDevice(UUID user, String token, String platform) throws Exception {
        return mockMvc.perform(post("/api/notifications/devices").with(as(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"" + token + "\", \"platform\": \"" + platform + "\"}"));
    }

    private UUID owner(String token) {
        return jdbcClient.sql("SELECT user_id FROM device WHERE token = :t").param("t", token)
                .query(UUID.class).single();
    }
}

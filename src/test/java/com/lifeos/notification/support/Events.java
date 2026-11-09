package com.lifeos.notification.support;

import java.time.LocalDate;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Complete, schema-valid payloads of the events Notification consumes (checked against lifeos-contracts). */
public final class Events {

    private final JsonMapper jsonMapper;

    public Events(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public JsonNode decisionReviewDue(UUID decisionId, String title, LocalDate reviewOn) {
        return json("""
                {"decisionId": "%s", "goalId": null, "title": "%s", "reviewOn": "%s",
                 "dueAt": "%sT06:00:00Z"}
                """.formatted(decisionId, title, reviewOn, reviewOn));
    }

    public JsonNode reopenTriggerFired(UUID decisionId, String description) {
        return json("""
                {"decisionId": "%s", "position": 0, "description": "%s", "firesOn": "2026-12-01",
                 "dueAt": "2026-12-01T06:00:00Z"}
                """.formatted(decisionId, description));
    }

    public JsonNode weeklyReviewDue(UUID weeklyPlanId, String isoWeek) {
        return json("""
                {"weeklyPlanId": "%s", "isoWeek": "%s", "weekStart": "2026-11-09", "dueAt": "2026-11-15T15:00:00Z"}
                """.formatted(weeklyPlanId, isoWeek));
    }

    public JsonNode driftDetected(UUID weeklyPlanId, String isoWeek, int planned, int done) {
        return json("""
                {"weeklyPlanId": "%s", "isoWeek": "%s", "kind": "BEHIND_PACE", "capacityMinutes": 300,
                 "plannedMinutes": %d, "doneMinutes": %d, "thresholdMinutes": %d, "deferredActions": 1,
                 "checkedAt": "2026-11-12T15:00:00Z"}
                """.formatted(weeklyPlanId, isoWeek, planned, done, planned / 2));
    }

    public JsonNode profile(String locale) {
        return json("""
                {"displayName": "Onurcan", "timezone": "Europe/Istanbul", "locale": "%s",
                 "defaultWeeklyMinutes": 300, "onboardingCompletedAt": null}
                """.formatted(locale));
    }

    public JsonNode assumptionFalsified(UUID decisionId, String title, String statement) {
        return json("""
                {"decisionId": "%s", "goalId": null, "title": "%s", "knowledgeItemId": "%s", "statement": "%s",
                 "falsifiedAt": "2026-11-09T10:00:00Z"}
                """.formatted(decisionId, title, UUID.randomUUID(), statement));
    }

    public JsonNode userDeletionRequested() {
        return json("{\"requestedAt\": \"2026-11-09T10:00:00Z\"}");
    }

    public JsonNode recommendationProposed(UUID goalId, String rationale) {
        return json("""
                {"recommendationId": "%s", "kind": "STRATEGY_REVISION", "goalId": "%s", "basedOnVersionNo": 1,
                 "rationale": "%s", "sources": [],
                 "revision": {"summary": "Interviews earlier", "changeReason": "Manager feedback",
                   "phases": [{"phaseKey": null, "title": "Foundation", "startsOn": "2026-09-01",
                               "endsOn": "2026-11-30", "exitCriteria": null, "status": "ACTIVE"}],
                   "effortAllocation": {"TECHNICAL": 60, "DOMAIN": 40, "OTHER": 0}}}
                """.formatted(UUID.randomUUID(), goalId, rationale));
    }

    private JsonNode json(String text) {
        return jsonMapper.readTree(text);
    }
}

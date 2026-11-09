package com.lifeos.notification.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.lifeos.base.id.UuidV7;
import com.lifeos.notification.inbound.consumer.CoachEventConsumer;
import com.lifeos.notification.inbound.consumer.StrategyEventConsumer;
import com.lifeos.notification.inbound.consumer.TriggerEventConsumer;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Base of the service's integration tests: the whole application over MockMvc, against real PostgreSQL and
 * Kafka. Requests are authenticated with {@link #as(UUID)} (a JWT whose {@code sub} is the user ID).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:0/realms/test",
        // The tests send due deliveries themselves, at the clock's time.
        "lifeos.notification.sender.initial-delay-ms=3600000"})
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
public abstract class IntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcClient jdbcClient;

    @Autowired
    protected JsonMapper jsonMapper;

    @Autowired
    protected MutableClock clock;

    protected static RequestPostProcessor as(UUID userId) {
        return jwt().jwt(token -> token.subject(userId.toString()));
    }

    @Autowired
    private StrategyEventConsumer strategyEventConsumer;

    @Autowired
    private TriggerEventConsumer triggerEventConsumer;

    @Autowired
    private CoachEventConsumer coachEventConsumer;

    /** A signed-in user whose token carries a verified e-mail address. */
    protected static RequestPostProcessor as(UUID userId, String verifiedEmail) {
        return jwt().jwt(token -> token.subject(userId.toString())
                .claim("email", verifiedEmail)
                .claim("email_verified", true));
    }

    /**
     * Hands an event to the consumer of its topic ({@code strategy}, {@code trigger} or {@code coach}), as Kafka
     * would; the payload is checked against its schema.
     *
     * @return the event's ID
     */
    protected UUID event(String topic, UUID userId, String eventType, JsonNode payload) {
        EventSchemas.assertValidPayload(topic, eventType, 1, payload);
        UUID eventId = UuidV7.next();
        ObjectNode envelope = jsonMapper.createObjectNode()
                .put("eventId", eventId.toString())
                .put("eventType", eventType)
                .put("eventVersion", 1)
                .put("occurredAt", clock.instant().toString())
                .put("userId", userId.toString());
        envelope.set("payload", payload);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("lifeos." + topic + ".events.v1", 0, 0L,
                userId.toString(), jsonMapper.writeValueAsString(envelope));
        switch (topic) {
            case "strategy" -> strategyEventConsumer.on(record);
            case "trigger" -> triggerEventConsumer.on(record);
            case "coach" -> coachEventConsumer.on(record);
            default -> throw new IllegalArgumentException(topic);
        }
        return eventId;
    }
}

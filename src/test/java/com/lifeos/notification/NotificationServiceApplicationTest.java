package com.lifeos.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lifeos.notification.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationServiceApplicationTest extends IntegrationTest {

    @Test
    void startsWithItsSchemaAndAnswersHealthChecks() throws Exception {
        List<String> tables = jdbcClient.sql("""
                        SELECT table_name FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name IN ('recipient', 'device', 'notification', 'delivery')
                        ORDER BY table_name
                        """)
                .query(String.class).list();
        assertEquals(List.of("delivery", "device", "notification", "recipient"), tables);
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}

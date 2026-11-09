package com.lifeos.notification.support;

import com.lifeos.notification.delivery.client.SmtpEmailSender;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Real PostgreSQL, Kafka and Mailpit (the same images as lifeos-infra/local), a clock the tests move, and senders
 * the tests can watch and make fail. As beans they live as long as the cached Spring context.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    static final int SMTP_PORT = 1025;
    static final int MAILPIT_API_PORT = 8025;

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17-alpine");
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafka() {
        return new KafkaContainer("apache/kafka:3.9.1");
    }

    /** Catches the e-mail the service sends; the tests read it back through Mailpit's API. */
    @Bean
    GenericContainer<?> mailpit() {
        return new GenericContainer<>("axllent/mailpit:v1.27").withExposedPorts(SMTP_PORT, MAILPIT_API_PORT);
    }

    @Bean
    DynamicPropertyRegistrar mailpitProperties(GenericContainer<?> mailpit) {
        return registry -> {
            registry.add("spring.mail.host", mailpit::getHost);
            registry.add("spring.mail.port", () -> mailpit.getMappedPort(SMTP_PORT));
            registry.add("lifeos.test.mailpit-api", () -> "http://" + mailpit.getHost() + ":"
                    + mailpit.getMappedPort(MAILPIT_API_PORT));
        };
    }

    @Bean
    @Primary
    MutableClock mutableClock() {
        return new MutableClock();
    }

    @Bean
    @Primary
    RecordingPushSender recordingPushSender() {
        return new RecordingPushSender();
    }

    @Bean
    @Primary
    FailingEmailSender failingEmailSender(SmtpEmailSender smtpEmailSender) {
        return new FailingEmailSender(smtpEmailSender);
    }
}

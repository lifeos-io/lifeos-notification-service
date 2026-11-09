package com.lifeos.notification.inbound.consumer;

import com.lifeos.base.messaging.MalformedEventException;
import com.lifeos.base.messaging.inbox.Inbox;
import com.lifeos.notification.account.service.AccountDeletionService;
import com.lifeos.notification.inbound.event.DecisionAssumptionFalsifiedV1;
import com.lifeos.notification.inbound.event.UserDeletionRequestedV1;
import com.lifeos.notification.inbound.event.UserProfileUpdatedV1;
import com.lifeos.notification.notification.entity.NotificationKind;
import com.lifeos.notification.notification.service.NotificationService;
import com.lifeos.notification.recipient.service.RecipientService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;

/** Strategy's events Notification uses: the user's language, falsified assumptions, and deletion. */
@Component
public class StrategyEventConsumer {

    private final Inbox inbox;
    private final RecipientService recipientService;
    private final NotificationService notificationService;
    private final AccountDeletionService accountDeletionService;

    public StrategyEventConsumer(Inbox inbox, RecipientService recipientService,
            NotificationService notificationService, AccountDeletionService accountDeletionService) {
        this.inbox = inbox;
        this.recipientService = recipientService;
        this.notificationService = notificationService;
        this.accountDeletionService = accountDeletionService;
    }

    // Own retry and dead-letter topics: other services consume this topic with retries of their own.
    @RetryableTopic(exclude = MalformedEventException.class, retryTopicSuffix = "-notification-retry",
            dltTopicSuffix = "-notification-dlt")
    @KafkaListener(topics = "${lifeos.notification.strategy-topic:lifeos.strategy.events.v1}",
            groupId = "notification")
    public void on(ConsumerRecord<String, String> record) {
        inbox.handle(record, event -> {
            if (event.eventVersion() != 1) {
                return;
            }
            switch (event.eventType()) {
                case UserProfileUpdatedV1.TYPE -> {
                    UserProfileUpdatedV1 profile = event.payloadAs(UserProfileUpdatedV1.class);
                    recipientService.profileUpdated(event.userId(), profile.locale(), profile.displayName());
                }
                case DecisionAssumptionFalsifiedV1.TYPE -> {
                    DecisionAssumptionFalsifiedV1 falsified = event.payloadAs(DecisionAssumptionFalsifiedV1.class);
                    notificationService.notify(event.userId(), event.envelope().eventId(),
                            NotificationKind.ASSUMPTION_FALSIFIED, falsified.decisionId(),
                            Links.decision(falsified.decisionId()), falsified.title(), falsified.statement());
                }
                case UserDeletionRequestedV1.TYPE -> accountDeletionService.deleteAll(event.userId());
                default -> {
                    // Not something Notification tells the user about.
                }
            }
        });
    }
}

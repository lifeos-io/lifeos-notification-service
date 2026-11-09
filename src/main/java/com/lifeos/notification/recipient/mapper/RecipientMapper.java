package com.lifeos.notification.recipient.mapper;

import com.lifeos.notification.recipient.dto.RecipientResponse;
import com.lifeos.notification.recipient.entity.Recipient;
import org.springframework.stereotype.Component;

@Component
public class RecipientMapper {

    public RecipientResponse toResponse(Recipient recipient) {
        return new RecipientResponse(recipient.getLocale(), recipient.getDisplayName(), recipient.getEmail(),
                recipient.isPushEnabled(), recipient.isEmailEnabled(), recipient.getVersion());
    }
}

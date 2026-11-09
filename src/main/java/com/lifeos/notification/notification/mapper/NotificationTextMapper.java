package com.lifeos.notification.notification.mapper;

import com.lifeos.notification.notification.entity.NotificationKind;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * The words of a notification in the user's language: {@code notification.<KIND>.title} and {@code .body} from
 * {@code messages*.properties}, dates written the way the language writes them. Texts are cut to the inbox's limits.
 */
@Component
public class NotificationTextMapper {

    private static final int MAX_TITLE = 200;
    private static final int MAX_BODY = 1000;

    private final MessageSource messageSource;

    public NotificationTextMapper(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public Texts texts(NotificationKind kind, String languageTag, Object... args) {
        Locale locale = Locale.forLanguageTag(languageTag);
        Object[] localized = Arrays.stream(args)
                .map(arg -> arg instanceof LocalDate date
                        ? DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(date)
                        : arg)
                .toArray();
        String prefix = "notification." + kind.name();
        return new Texts(
                cut(messageSource.getMessage(prefix + ".title", localized, locale), MAX_TITLE),
                cut(messageSource.getMessage(prefix + ".body", localized, locale), MAX_BODY));
    }

    private static String cut(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    public record Texts(String title, String body) {
    }
}

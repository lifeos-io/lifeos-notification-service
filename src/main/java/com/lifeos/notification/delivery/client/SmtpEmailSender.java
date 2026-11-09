package com.lifeos.notification.delivery.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** E-mail over SMTP ({@code spring.mail.*}; locally Mailpit, which keeps everything on the machine). */
@Component
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender javaMailSender;
    private final String from;

    public SmtpEmailSender(JavaMailSender javaMailSender,
            @Value("${lifeos.notification.mail.from}") String from) {
        this.javaMailSender = javaMailSender;
        this.from = from;
    }

    @Override
    public void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        javaMailSender.send(message);
    }
}

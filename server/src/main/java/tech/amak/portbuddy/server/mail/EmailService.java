/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.mail;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.server.config.AppProperties;

/**
 * Service for sending HTML email based on Thymeleaf templates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final AppProperties properties;

    /**
     * Sends an HTML email rendered from a Thymeleaf template.
     *
     * @param to       recipient email
     * @param subject  email subject
     * @param template template name under templates directory (e.g. "email/welcome")
     * @param model    model variables for the template
     */
    public void sendTemplate(final String to,
                             final String subject,
                             final String template,
                             final @Nullable Map<String, Object> model) {
        try {
            final var message = mailSender.createMimeMessage();
            final var helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name());

            final var fromName = properties.mail().fromName();
            final var fromAddress = properties.mail().fromAddress();
            helper.setFrom(new InternetAddress(fromAddress, fromName));
            helper.setTo(to);
            helper.setSubject(subject);

            final var context = new Context();
            if (model != null) {
                model.forEach(context::setVariable);
            }
            final var html = templateEngine.process(template, context);
            helper.setText(html, true);

            mailSender.send(message);
        } catch (final MessagingException e) {
            log.warn("Email send failed (MessagingException): {}", e.getMessage());
        } catch (final Exception e) {
            log.warn("Email send failed: {}", e.getMessage());
        }
    }
}

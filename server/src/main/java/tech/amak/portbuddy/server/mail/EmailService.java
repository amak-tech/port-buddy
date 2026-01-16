/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.amak.portbuddy.server.mail;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
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
    @Async
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

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

package tech.amak.portbuddy.server.service.user;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.entity.EmailOtpEntity;
import tech.amak.portbuddy.server.db.repo.EmailOtpRepository;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.mail.EmailService;

/**
 * Issues and verifies one-time registration codes sent to a prospective user's email address.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailOtpService {

    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final int MAX_ATTEMPTS = 5;
    private static final int OTP_BOUND = 10000;

    private final EmailOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;
    private final SecureRandom random = new SecureRandom();

    /**
     * Generates a fresh 4-digit code for the given email and sends it by email.
     * Fails if an account already exists for the email so that registration cannot target an existing account.
     *
     * @param email the prospective user's email address
     */
    @Transactional
    public void requestOtp(final String email) {
        final var normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            throw new IllegalArgumentException("Email is required");
        }

        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("An account already exists for this email");
        }

        final var code = String.format("%04d", random.nextInt(OTP_BOUND));

        otpRepository.deleteByEmail(normalizedEmail);
        // Flush the delete before inserting so the unique(email) constraint is not tripped by a stale row.
        otpRepository.flush();

        final var entity = new EmailOtpEntity();
        entity.setId(UUID.randomUUID());
        entity.setEmail(normalizedEmail);
        entity.setCodeHash(passwordEncoder.encode(code));
        entity.setAttempts(0);
        entity.setExpiryDate(OffsetDateTime.now().plus(OTP_TTL));
        otpRepository.save(entity);

        final var model = new HashMap<String, Object>();
        model.put("subject", "Your Port Buddy verification code");
        model.put("greeting", "Hello!");
        model.put("code", code);
        model.put("expiryMinutes", OTP_TTL.toMinutes());
        model.put("webAppUrl", properties.gateway().url());

        emailService.sendTemplate(normalizedEmail, "Your Port Buddy verification code", "email/otp-code", model);
        log.info("Registration OTP sent to {}", normalizedEmail);
    }

    /**
     * Verifies a submitted code against the stored OTP for the email. On success the OTP is consumed.
     *
     * @param email the email address the code was sent to
     * @param code  the code submitted by the user
     * @return true if the code is valid and not expired, false otherwise
     */
    @Transactional
    public boolean verifyOtp(final String email, final String code) {
        final var normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || code == null || code.isBlank()) {
            return false;
        }

        final var otpOpt = otpRepository.findByEmail(normalizedEmail);
        if (otpOpt.isEmpty()) {
            return false;
        }
        final var otp = otpOpt.get();

        if (otp.getExpiryDate().isBefore(OffsetDateTime.now())) {
            otpRepository.delete(otp);
            return false;
        }

        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            otpRepository.delete(otp);
            return false;
        }

        if (!passwordEncoder.matches(code.trim(), otp.getCodeHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            otpRepository.save(otp);
            return false;
        }

        otpRepository.delete(otp);
        return true;
    }

    private static String normalizeEmail(final String email) {
        if (email == null) {
            return null;
        }
        final var trimmed = email.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase();
    }
}

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

package tech.amak.portbuddy.sslservice.service;

import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.sslservice.config.AppProperties;

/**
 * Simple exponential backoff retry executor for transient errors.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RetryExecutor {

    private final AppProperties properties;

    /**
     * Executes the given {@code action} with retry/backoff for transient errors.
     *
     * @param stepName a human readable step name for logs
     * @param action   the action to execute
     * @param <T>      return type
     * @return result of action
     * @throws Exception last thrown error if all attempts fail or a non-transient error occurs
     */
    public <T> T callWithRetry(final String stepName, final Callable<T> action) throws Exception {
        final var retry = properties.acme().retry();
        final int maxAttempts = Math.max(1, retry.maxAttempts());
        long delay = Math.max(0L, retry.initialDelayMs());
        final long maxDelay = Math.max(delay, retry.maxDelayMs());
        final double multiplier = Math.max(1.0, retry.multiplier());
        final long jitter = Math.max(0L, retry.jitterMs());

        Exception last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (attempt > 1) {
                    final long sleep = Math.min(maxDelay, delay + jitterRandom(jitter));
                    log.info("Retry step='{}' attempt={} sleepingMs={}", stepName, attempt, sleep);
                    Thread.sleep(sleep);
                    delay = Math.min(maxDelay, (long) (delay * multiplier));
                }
                return action.call();
            } catch (final Exception e) {
                last = e;
                final boolean transientErr = TransientErrorClassifier.isTransient(e);
                log.warn("Step '{}' attempt {} failed (transient={})", stepName, attempt, transientErr, e);
                if (!transientErr || attempt >= maxAttempts) {
                    break;
                }
            }
        }
        if (last != null) {
            throw last;
        }
        throw new IllegalStateException("Unknown failure in retry executor for step=" + stepName);
    }

    private long jitterRandom(final long jitter) {
        if (jitter <= 0L) {
            return 0L;
        }
        // simple symmetric jitter in range [0, jitter]
        return (long) (Math.random() * jitter);
    }
}

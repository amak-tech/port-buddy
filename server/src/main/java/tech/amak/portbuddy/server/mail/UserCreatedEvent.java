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

import java.util.UUID;

/**
 * Application domain event published when a new user is created in the system.
 */
public record UserCreatedEvent(UUID userId,
                               UUID accountId,
                               String email,
                               String firstName,
                               String lastName,
                               String passwordResetLink) {
}

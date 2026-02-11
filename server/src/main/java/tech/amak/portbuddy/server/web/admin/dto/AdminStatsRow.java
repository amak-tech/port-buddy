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
 *
 */

package tech.amak.portbuddy.server.web.admin.dto;

import java.time.LocalDate;

/**
 * Daily statistics for the admin control center.
 *
 * @param date           the date of the statistics
 * @param newUsersCount  number of users created on this date
 * @param tunnelsCount   number of tunnels created on this date
 * @param paymentEvents  number of stripe events created on this date
 */
public record AdminStatsRow(
        LocalDate date,
        long newUsersCount,
        long tunnelsCount,
        long paymentEvents
) { }

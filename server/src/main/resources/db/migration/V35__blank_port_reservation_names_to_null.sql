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

-- A blank name means "no name". Stored as an empty string it counted as a value for the
-- (account_id, name) unique index, so a second unnamed reservation collided with the first.
UPDATE port_reservations
SET name = NULL
WHERE name IS NOT NULL AND btrim(name) = '';

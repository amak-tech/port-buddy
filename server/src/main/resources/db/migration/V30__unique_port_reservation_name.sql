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

-- Ensure port reservation name is unique within account
-- We use a partial index to only enforce uniqueness for non-deleted reservations and non-null names
CREATE UNIQUE INDEX idx_port_reservations_account_id_name_unique 
ON port_reservations (account_id, name) 
WHERE deleted = false AND name IS NOT NULL;

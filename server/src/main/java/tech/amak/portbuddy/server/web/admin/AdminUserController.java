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

package tech.amak.portbuddy.server.web.admin;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.web.admin.dto.AdminUserRow;

@RestController
@RequestMapping(path = "/api/admin/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;

    /**
     * Returns a list of users for the admin page using a single native SQL query.
     * The list is ordered by number of active tunnels (DESC) and creation time (DESC).
     * Accessible only for users with ADMIN role.
     *
     * @param search optional search string to filter users by email, name or ID
     * @return list of user rows for admin table
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AdminUserRow> listUsers(final @RequestParam(value = "search", required = false) String search) {
        return userRepository.findAdminUsers(search);
    }
}

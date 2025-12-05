/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.PortReservationEntity;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.service.PortReservationService;
import tech.amak.portbuddy.server.service.ProxyDiscoveryService;
import tech.amak.portbuddy.server.web.dto.PortRangeDto;
import tech.amak.portbuddy.server.web.dto.PortReservationDto;
import tech.amak.portbuddy.server.web.dto.PortReservationUpdateRequest;

@RestController
@RequestMapping(path = "/api/ports", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class PortsController {

    private final PortReservationService reservationService;
    private final UserRepository userRepository;
    private final ProxyDiscoveryService proxyDiscoveryService;
    private final AppProperties properties;

    /**
     * Retrieves a list of port reservations for the authenticated user's account.
     *
     * @param principal the authenticated user's JWT token, which holds the user information.
     * @return a list of {@code PortReservationDto} objects representing the port reservations for the user's account.
     */
    @GetMapping
    public List<PortReservationDto> list(final @AuthenticationPrincipal Jwt principal) {
        final var account = getAccount(principal);
        return reservationService.getReservations(account).stream()
            .map(PortsController::toDto)
            .toList();
    }

    /**
     * Creates a new port reservation for the authenticated user's account.
     *
     * @param principal the authenticated user's JWT token containing user details.
     * @return a {@code PortReservationDto} object representing the newly created port reservation.
     * @throws ResponseStatusException if the user cannot be found or is not authorized.
     */
    @PostMapping
    public PortReservationDto create(final @AuthenticationPrincipal Jwt principal) {
        final var userId = UUID.fromString(principal.getSubject());
        final var user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        final var account = user.getAccount();
        final var reservation = reservationService.createReservation(account, user);
        return toDto(reservation);
    }

    /**
     * Deletes a port reservation for the specified ID if it belongs to the authenticated user's account.
     * Responds with a 204 No Content status on success.
     *
     * @param principal the authenticated user's JWT token, used to determine the user's account.
     * @param id        the unique identifier of the port reservation to be deleted.
     * @throws ResponseStatusException with a 409 Conflict status if the reservation cannot be deleted
     *                                 due to an illegal state.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(final @AuthenticationPrincipal Jwt principal,
                       final @PathVariable("id") UUID id) {
        final var account = getAccount(principal);
        try {
            reservationService.deleteReservation(id, account);
        } catch (final IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    /**
     * Updates an existing port reservation. If there is only one available public host, UI may send only port.
     */
    @PutMapping("/{id}")
    public PortReservationDto update(final @AuthenticationPrincipal Jwt principal,
                                     final @PathVariable("id") UUID id,
                                     final @RequestBody PortReservationUpdateRequest body) {
        final var account = getAccount(principal);
        try {
            final var updated = reservationService.updateReservation(account, id, body.publicHost(), body.publicPort());
            return toDto(updated);
        } catch (final IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (final IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    /**
     * Lists available tcp-proxy public hosts for selection.
     */
    @GetMapping("/hosts")
    public List<String> hosts() {
        return proxyDiscoveryService.listPublicHosts();
    }

    /**
     * Returns allowed port range for a given host. Currently same for all hosts, derived from config.
     */
    @GetMapping("/hosts/{host}/range")
    public PortRangeDto hostRange(@PathVariable("host") final String host) {
        // Not validating host existence here; UI should have called /hosts first
        final var range = properties.portReservations().range();
        return new PortRangeDto(range.min(), range.max());
    }

    private AccountEntity getAccount(final Jwt jwt) {
        final var userId = UUID.fromString(jwt.getSubject());
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"))
            .getAccount();
    }

    private static PortReservationDto toDto(final PortReservationEntity e) {
        return new PortReservationDto(
            e.getId(),
            e.getPublicHost(),
            e.getPublicPort(),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
}

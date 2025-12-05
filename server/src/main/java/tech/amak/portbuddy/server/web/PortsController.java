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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.PortReservationEntity;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.service.PortReservationService;
import tech.amak.portbuddy.server.web.dto.PortReservationDto;

@RestController
@RequestMapping(path = "/api/ports", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class PortsController {

    private final PortReservationService reservationService;
    private final UserRepository userRepository;

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

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(final @AuthenticationPrincipal Jwt principal,
                       @PathVariable("id") final UUID id) {
        final var account = getAccount(principal);
        reservationService.deleteReservation(id, account);
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

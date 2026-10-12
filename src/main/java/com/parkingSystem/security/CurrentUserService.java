package com.parkingSystem.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.parkingSystem.model.User;
import com.parkingSystem.repository.UserRepository;

/**
 * Resolves the authenticated user (set by the JWT filter) and enforces ownership rules:
 * a USER may only access their own data, an ADMIN may access everything.
 */
@Component
public class CurrentUserService {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * @return the username of the authenticated caller
     * @throws AuthenticationCredentialsNotFoundException if nobody is authenticated
     */
    public String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("Not authenticated");
        }
        return authentication.getName();
    }

    /**
     * @return the persisted user for the authenticated caller
     */
    public User getCurrentUser() {
        return userRepository.findByUserName(getUsername())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Authenticated user no longer exists"));
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> ROLE_ADMIN.equals(authority.getAuthority()));
    }

    /**
     * Allows the call only if the caller is an admin or owns the data of {@code ownerUserId}.
     *
     * @throws AccessDeniedException (mapped to 403) otherwise
     */
    public void checkAccess(Long ownerUserId) {
        if (isAdmin()) {
            return;
        }
        if (ownerUserId == null || !ownerUserId.equals(getCurrentUser().getUserId())) {
            throw new AccessDeniedException("Access to another user's data is not allowed");
        }
    }

    /**
     * Returns the user id a request should act on: the caller's own id when none is given,
     * otherwise the given id after an ownership check.
     */
    public Long resolveUserId(Long requestedUserId) {
        if (requestedUserId == null) {
            return getCurrentUser().getUserId();
        }
        checkAccess(requestedUserId);
        return requestedUserId;
    }
}

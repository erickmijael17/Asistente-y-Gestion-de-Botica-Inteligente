package com.botica.inteligente.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public Optional<String> keycloakUserId() {
        return currentJwt().map(Jwt::getSubject);
    }

    public Optional<String> username() {
        return currentJwt()
                .map(jwt -> jwt.getClaimAsString("preferred_username"))
                .or(() -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                        .map(Authentication::getName));
    }

    private Optional<Jwt> currentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(jwt);
        }
        return Optional.empty();
    }
}

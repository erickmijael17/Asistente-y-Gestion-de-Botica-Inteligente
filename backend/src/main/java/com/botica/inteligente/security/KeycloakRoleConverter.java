package com.botica.inteligente.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<String> roles = new HashSet<>();
        extractRealmRoles(jwt, roles);
        extractResourceRoles(jwt, roles);
        return roles.stream()
                .filter(role -> role.equals("OWNER") || role.equals("SELLER"))
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private void extractRealmRoles(Jwt jwt, Set<String> roles) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> realmRoles) {
            realmRoles.forEach(role -> roles.add(String.valueOf(role)));
        }
    }

    @SuppressWarnings("unchecked")
    private void extractResourceRoles(Jwt jwt, Set<String> roles) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess == null) {
            return;
        }
        resourceAccess.values().forEach(value -> {
            if (value instanceof Map<?, ?> client && client.get("roles") instanceof Collection<?> clientRoles) {
                clientRoles.forEach(role -> roles.add(String.valueOf(role)));
            }
        });
    }
}

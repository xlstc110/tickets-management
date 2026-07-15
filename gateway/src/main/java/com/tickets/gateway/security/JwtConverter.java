package com.tickets.gateway.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import static org.springframework.security.core.authority.AuthorityUtils.commaSeparatedStringToAuthorityList;

@Component
public class JwtConverter implements Converter<Jwt, JwtAuthenticationToken> {
    // The claim name for user authorities is "authorities" in the jwt payload
    private static final String AUTHORITY_KEY = "authorities";

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        // Get the authorities from the jwt claims and convert them to a list of GrantedAuthority objects
        String claims = (String) jwt.getClaims().get(AUTHORITY_KEY);
        // Convert the comma-separated string of authorities to a list of GrantedAuthority objects
        var authorities = commaSeparatedStringToAuthorityList(claims);
        // Create a new JwtAuthenticationToken with the jwt, authorities, and subject (username) from the jwt
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}

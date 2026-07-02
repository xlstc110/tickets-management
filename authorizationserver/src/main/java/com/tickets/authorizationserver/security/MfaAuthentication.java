package com.tickets.authorizationserver.security;

import lombok.Getter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;

@Getter
public class MfaAuthentication extends AnonymousAuthenticationToken {
    private final Authentication primaryAuthentication;

    /**
     * Constructor.
     *
     * @param authentication the original authentication object
     * @param authority      the authority granted to the principal
     * @throws IllegalArgumentException if a <code>null</code> was passed
     */
    public MfaAuthentication(Authentication authentication, String authority) {
        // Call the superclass constructor with a unique key, principal, and authorities
        super("anonymous", "anonymous", createAuthorityList("ROLE_ANONYMOUS", authority));
        this.primaryAuthentication = authentication;
    }

    @Override
    public Object getPrincipal() {
        return this.primaryAuthentication;
    }
}

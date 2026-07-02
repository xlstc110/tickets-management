package com.tickets.authorizationserver.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;

@Component
public class ClientAuthenticationProvider implements AuthenticationProvider {
    private final RegisteredClientRepository registeredClientRepository;

    public ClientAuthenticationProvider(RegisteredClientRepository registeredClientRepository) {
        this.registeredClientRepository = registeredClientRepository;
    }

    @Override
    // This method authenticates the provided authentication object. It checks if the client authentication method is NONE,
    // retrieves the client ID, and verifies the registered client in the repository.
    // If valid, it returns a new ClientRefreshTokenAuthentication object; otherwise, it throws an OAuth2AuthenticationException.
    public Authentication authenticate(Authentication authentication) {
        var clientRefreshTokenAuthentication = (ClientRefreshTokenAuthentication) authentication;
        // Check if the client authentication method is NONE, which indicates that the client is not authenticated.
        if(!ClientAuthenticationMethod.NONE.equals(clientRefreshTokenAuthentication.getClientAuthenticationMethod())) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT, "client authentication method is not valid", null));
        }
        // Retrieve the client ID from the authentication object and use it to find the corresponding registered client in the repository.
        var clientId = clientRefreshTokenAuthentication.getPrincipal().toString();
        var registeredClientId = registeredClientRepository.findByClientId(clientId);
        // If the registered client is not found, throw an OAuth2AuthenticationException with an INVALID_CLIENT error.
        if(registeredClientId == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT, "client is not valid", null));
        }
        // Check if the registered client's authentication methods contain the client authentication method from the provided authentication object.
        if(!registeredClientId.getClientAuthenticationMethods().contains(clientRefreshTokenAuthentication.getClientAuthenticationMethod())) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT, "client authentication method is not valid", null));
        }
        return new ClientRefreshTokenAuthentication(registeredClientId);
    }



    @Override
    // This method checks if the provided authentication class is assignable from ClientRefreshTokenAuthentication.
    public boolean supports(Class<?> authentication) {
        return ClientRefreshTokenAuthentication.class.isAssignableFrom(authentication);
    }
}

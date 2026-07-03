package com.tickets.authorizationserver.event;

import com.tickets.authorizationserver.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import static com.tickets.authorizationserver.utils.UserAgentUtils.*;
import static com.tickets.authorizationserver.utils.UserUtils.getUser;

@Slf4j
@Component
public class ApiAuthenticationEventListener {
    private final UserService userService;
    private final HttpServletRequest request;

    public ApiAuthenticationEventListener(UserService userService, HttpServletRequest request) {
        this.userService = userService;
        this.request = request;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        log.info("Authentication Success - {}", event.getAuthentication());
        if(event.getAuthentication().getPrincipal() instanceof UsernamePasswordAuthenticationToken) {
            var user = getUser(event.getAuthentication());
            userService.setLastLogin(user.getUserId());
            userService.resetLoginAttempt(user.getUserUuid());
            userService.addLoginDevice(user.getUserId(), getDevice(request), getClient(request), getIdAddress(request));
        }
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        log.info("Authentication Failure - {}", event.getAuthentication());
        if(event.getException() instanceof BadCredentialsException) {
            var email = (String) event.getAuthentication().getPrincipal();
            userService.updateLoginAttempt(email);
        }
    }
}

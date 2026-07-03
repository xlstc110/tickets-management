package com.tickets.authorizationserver.security;

import com.tickets.authorizationserver.exception.ApiException;
import com.tickets.authorizationserver.model.User;
import com.tickets.authorizationserver.service.UserService;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

import static org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated;
import static org.springframework.security.core.authority.AuthorityUtils.commaSeparatedStringToAuthorityList;

@Component
public class UserAuthenticationProvider implements AuthenticationProvider {
    private final UserService userService;
    private final BCryptPasswordEncoder encoder;

    public UserAuthenticationProvider(UserService userService, BCryptPasswordEncoder encoder) {
        this.userService = userService;
        this.encoder = encoder;
    }


    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        try{
            var user = userService.getUserByEmail((String) authentication.getPrincipal());
            validateUser.accept(user);
            if(encoder.matches((String) authentication.getCredentials(), user.getPassword())) {
                return authenticated(
                        user,
                        "[PROTECTED]",
                        commaSeparatedStringToAuthorityList(user.getRole() + "," + user.getAuthorities()));
            } else throw new BadCredentialsException("Incorrect email or password");
        } catch(BadCredentialsException | ApiException | LockedException | CredentialsExpiredException |
                DisabledException e){
            throw new ApiException(e.getMessage());
        } catch(Exception e){
            throw new ApiException("Unable to authenticate user, please try again");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {

        return authentication.isAssignableFrom(UsernamePasswordAuthenticationToken.class);
    }

    private final Consumer<User> validateUser = user -> {
        // Change the 5 times to an environment variable or some manage tool
        if (!user.isAccountNonLocked() || user.getLoginAttempts() >= 5) {
            throw new LockedException(
                    String.format(user.getLoginAttempts() >= 0 ? "Your account has been locked due to %s failed login attempts. Please contact support." : "Your account is locked. Please contact support.",  user.getLoginAttempts())
            );
        }
        if (!user.isEnabled()) {
            throw new DisabledException(
                    "Your account is disabled. Please contact support."
            );
        }
        if (!user.isAccountNonExpired()) {
            throw new LockedException(
                    "Your account is expired. Please contact support."
            );
        }
    };
}

package com.tickets.discoveryservice.utils;

import com.tickets.discoveryservice.model.User;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;

public class UserUtils {

    public static boolean verifyUserQrCode(String secret, String code) {
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        return codeVerifier.isValidCode(secret, code);
    }

    public static User getUser(Authentication authentication) {
        if(authentication instanceof OAuth2AuthorizationCodeRequestAuthenticationToken) {;
            var usernamePasswordAuthenticationToken = (UsernamePasswordAuthenticationToken) authentication.getPrincipal();

            return (User) usernamePasswordAuthenticationToken.getPrincipal();
        }

        return (User) authentication.getPrincipal();
    }
}

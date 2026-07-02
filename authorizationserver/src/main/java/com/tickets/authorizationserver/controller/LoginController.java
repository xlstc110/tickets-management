package com.tickets.authorizationserver.controller;

import com.tickets.authorizationserver.model.User;
import com.tickets.authorizationserver.security.MfaAuthentication;
import com.tickets.authorizationserver.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

import static com.tickets.authorizationserver.utils.UserUtils.getUser;

// Not RestController since it is serving HTML file
@Controller
public class LoginController {
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final AuthenticationSuccessHandler authenticationSuccessHandler;
    private final AuthenticationFailureHandler authenticationFailureHandler;
    private final UserService userService;

    public LoginController() {
        this.authenticationSuccessHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        this.authenticationFailureHandler = new SimpleUrlAuthenticationFailureHandler("/mfa?error");
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/mfa")
    public String mfa(Model model,
                      @CurrentSecurityContext SecurityContext securityContext) {
        model.addAttribute("email", getAuthenticatedUser(securityContext.getAuthentication()));
        return "mfa";
    }

    @PostMapping("/mfa")
    // This method validates the QR code sent by the user. If the QR code is valid,
    // it saves the authentication in the security context and redirects to the success URL.
    // If the QR code is invalid, it redirects to the failure URL.
    public void validateQrCode(@RequestParam("code") String code,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 @CurrentSecurityContext SecurityContext securityContext) throws ServletException, IOException {
        var user = getUser(securityContext.getAuthentication());
        if(userService.verifyQrCode(user.getUserUuid(), code)) {
            this.authenticationSuccessHandler.onAuthenticationSuccess(request, response, saveAuthentication(request, response));
            return;
        }
        this.authenticationFailureHandler.onAuthenticationFailure(request, response, new BadCredentialsException("Invalid QR code"));
    }

    // This method saves the authentication in the security context and returns the MfaAuthentication object
    private Authentication saveAuthentication(HttpServletRequest request, HttpServletResponse response) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        MfaAuthentication mfaAuthentication = (MfaAuthentication) securityContext.getAuthentication();
        securityContext.setAuthentication(mfaAuthentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, request, response);
        return mfaAuthentication;
    }

    // This method returns the email of the authenticated user from the authentication object
    private Object getAuthenticatedUser(Authentication authentication) {
        return ((User) authentication.getPrincipal()).getEmail();
    }
}

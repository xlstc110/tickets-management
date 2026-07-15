package com.tickets.gateway.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.tickets.gateway.domain.Response;
import com.tickets.gateway.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import javax.naming.InsufficientResourcesException;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static java.time.LocalTime.now;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class RequestUtils {
    private static final BiConsumer<HttpServletResponse, Response> writeResponse = (servletResponse, response) -> {
        try {
            var outputStream = servletResponse.getOutputStream();
            new ObjectMapper().writeValue(outputStream, response);
            // Flush the output stream to ensure all data is written
            outputStream.flush();
        } catch (Exception e) {
            throw new ApiException(e.getMessage());
        }
    };
    // This method handles different types of exceptions and generates appropriate error responses based on the exception type and HTTP status code.
    public static void handleErrorResponse(HttpServletRequest request, HttpServletResponse response, Exception exception) {
        if(exception instanceof AccessDeniedException) {
            // Handle the case where the user does not have permission to access the resource
            var apiResponse = getErrorReponse(request, response, exception, FORBIDDEN);
            writeResponse.accept(response, apiResponse);
        } else if(exception instanceof InvalidBearerTokenException) {
            // Handle the case where the JWT token is invalid or expired
            var apiResponse = getErrorReponse(request, response, exception, UNAUTHORIZED);
            writeResponse.accept(response, apiResponse);
        } else if(exception instanceof InsufficientAuthenticationException) {
            // Handle the case where the user is not authenticated
            var apiResponse = getErrorReponse(request, response, exception, HttpStatus.UNAUTHORIZED);
            writeResponse.accept(response, apiResponse);
        } else if(exception instanceof MismatchedInputException) {
            // Handle the case where the input data does not match the expected format
            var apiResponse = getErrorReponse(request, response, exception, HttpStatus.BAD_REQUEST);
            writeResponse.accept(response, apiResponse);
        } else if(exception instanceof DisabledException
                || exception instanceof LockedException
                || exception instanceof BadCredentialsException
                || exception instanceof ApiException) {
            // Handle the case where the user account is disabled, locked, or has bad credentials
            var apiResponse = getErrorReponse(request, response, exception, HttpStatus.BAD_REQUEST);
            writeResponse.accept(response, apiResponse);
        } else {
            // Handle any other exceptions that are not specifically handled above
            var apiResponse = getErrorReponse(request, response, exception, HttpStatus.INTERNAL_SERVER_ERROR);
            writeResponse.accept(response, apiResponse);
        }
    }

    private static String getErrorReason(
            Exception exception,
            HttpStatusCode httpStatus) {
        // Handle specific HTTP status codes and exceptions to provide meaningful error messages
        if (httpStatus.isSameCodeAs(HttpStatus.FORBIDDEN)) {
            return "You don't have permission to access this resource.";
        }
        // Handle 401 Unauthorized status and check for JWT expiration
        if (httpStatus.isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
            String message = exception.getMessage();

            return message != null && message.contains("Jwt expired at")
                    ? "Your session has expired."
                    : "You are not logged in.";
        }
        // Handle specific exceptions to provide their messages
        if (exception instanceof DisabledException
                || exception instanceof LockedException
                || exception instanceof BadCredentialsException
                || exception instanceof ApiException) {
            return exception.getMessage();
        }
        //
        return httpStatus.is5xxServerError()
                ? "An internal server error occurred."
                : "An error occurred. Please try again later.";
    }

    private static Response getErrorReponse(HttpServletRequest request, HttpServletResponse response, Exception exception, HttpStatus status) {
        // Set the content type of the response to JSON
        response.setContentType(APPLICATION_JSON_VALUE);
        // Set the HTTP status code of the response to the provided status
        response.setStatus(status.value());
        // Create a Response object with the error details
        return new Response(
                now().toString(),
                status.value(),
                request.getRequestURI(),
                HttpStatus.valueOf(status.value()),
                getErrorReason(exception, status),
                getRootCauseMessage(exception),
                Collections.emptyMap()
        );
    }

}

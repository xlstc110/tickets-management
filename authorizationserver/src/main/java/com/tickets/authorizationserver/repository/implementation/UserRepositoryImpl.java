package com.tickets.authorizationserver.repository.implementation;

import com.tickets.authorizationserver.exception.ApiException;
import com.tickets.authorizationserver.model.User;
import com.tickets.authorizationserver.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import com.tickets.authorizationserver.query.UserQuery;

import java.util.Map;

@Slf4j
@Repository
public class UserRepositoryImpl implements UserRepository {
    private final JdbcClient  jdbcClient;

    public UserRepositoryImpl(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public User getUserByUuid(String userUuid) {
        try {
            return jdbcClient.sql(UserQuery.SELECT_USER_BY_USER_UUID_QUERY)
                    .param("userUuid", userUuid)
                    .query(User.class)
                    .single();
        } catch (EmptyResultDataAccessException e) {
            log.error(e.getMessage());
            throw new ApiException(String.format("No user found with UUID: %s", userUuid));
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ApiException("An error occurred, try again later.");
        }
    }

    @Override
    public User getUserByEmail(String email) {
        try {
            return jdbcClient.sql(UserQuery.SELECT_USER_BY_EMAIL_QUERY)
                    .param("email", email)
                    .query(User.class)
                    .single();
        } catch (EmptyResultDataAccessException e) {
            log.error(e.getMessage());
            throw new ApiException(String.format("No user found with email: %s", email));
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ApiException("An error occurred, try again later.");
        }
    }

    @Override
    public void resetLoginAttempt(String userUuid) {
        try {
            jdbcClient.sql(UserQuery.RESET_LOGIN_ATTEMPTS_QUERY)
                    .param("userUuid", userUuid)
                    .update();
        } catch (EmptyResultDataAccessException e) {
            log.error(e.getMessage());
            throw new ApiException(String.format("No user found with UUID: %s", userUuid));
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ApiException("An error occurred, try again later.");
        }

    }

    @Override
    public void updateLoginAttempt(String email) {
        try {
            jdbcClient.sql(UserQuery.UPDATE_LOGIN_ATTEMPTS_QUERY)
                    .param("email", email)
                    .update();
        } catch (EmptyResultDataAccessException e) {
            log.error(e.getMessage());
            throw new ApiException(String.format("No user found with email: %s", email));
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ApiException("An error occurred, try again later.");
        }
    }

    @Override
    public void setLastLogin(Long userId) {
        try {
            jdbcClient.sql(UserQuery.SET_LAST_LOGIN_QUERY)
                    .param("userId", userId)
                    .update();
        } catch (EmptyResultDataAccessException e) {
            log.error(e.getMessage());
            throw new ApiException(String.format("No user found with userId: %s", userId));
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ApiException("An error occurred, try again later.");
        }
    }

    @Override
    public void addLoginDevice(Long userId, String device, String client, String ipAddress) {
        try {
            jdbcClient.sql(UserQuery.INSERT_NEW_DEVICE_QUERY)
                    .params(Map.of("userId", userId,
                            "device", device,
                            "client", client,
                            "ipAddress", ipAddress)
                    )
                    .update();
        } catch (EmptyResultDataAccessException e) {
            log.error(e.getMessage());
            throw new ApiException(String.format("No user found with userId: %s", userId));
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ApiException("An error occurred, try again later.");
        }
    }
}

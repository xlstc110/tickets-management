package com.tickets.discoveryservice.repository.implementation;

import com.tickets.discoveryservice.exception.ApiException;
import com.tickets.discoveryservice.model.User;
import com.tickets.discoveryservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.tickets.discoveryservice.query.UserQuery;

import java.util.Map;

@Slf4j
@Repository
public class UserRepositoryImpl implements UserRepository {
    private final JdbcClient  jdbcClient;

    public UserRepositoryImpl(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public User getUserByUsername(String username) {
        try {
            return jdbcClient.sql(UserQuery.SELECT_USER_BY_USERNAME_QUERY)
                    .param("username", username)
                    .query(User.class)
                    .single();
        } catch (EmptyResultDataAccessException e) {
            log.error(e.getMessage());
            throw new ApiException(String.format("No user found with username: %s", username));
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ApiException("An error occurred, try again later.");
        }
    }
}

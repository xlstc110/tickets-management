package com.tickets.discoveryservice.repository;

import com.tickets.discoveryservice.model.User;

public interface UserRepository {
    User getUserByUsername(String username);
}

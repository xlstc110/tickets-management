package com.tickets.discoveryservice.repository;

import com.tickets.discoveryservice.model.User;

public interface UserRepository {
    User getUserByUuid(String userUuid);
    User getUserByEmail(String email);
    void resetLoginAttempt(String userUuid);
    void updateLoginAttempt(String email);
    void setLastLogin(Long userId);
    void addLoginDevice(Long userId, String deviceName, String client, String ipAddress);

    User getUserByUsername(String username);
}

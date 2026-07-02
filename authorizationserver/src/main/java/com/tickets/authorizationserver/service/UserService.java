package com.tickets.authorizationserver.service;

import com.tickets.authorizationserver.model.User;

public interface UserService {
    User getUserByEmail(String email);
    void resetLoginAttempt(String userUuid);
    void updateLoginAttempt(String email);
    void setLastLogin(Long userId);
    void addLoginDevice(Long userId, String deviceName, String client, String ipAddress);
    boolean verifyQrCode(String userUuid, String code);
}

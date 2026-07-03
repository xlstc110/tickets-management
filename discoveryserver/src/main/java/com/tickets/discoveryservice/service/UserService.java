package com.tickets.discoveryservice.service;

import com.tickets.discoveryservice.model.User;

public interface UserService {
    User getUserByEmail(String email);
    void resetLoginAttempt(String userUuid);
    void updateLoginAttempt(String email);
    void setLastLogin(Long userId);
    void addLoginDevice(Long userId, String deviceName, String client, String ipAddress);
    boolean verifyQrCode(String userUuid, String code);
}

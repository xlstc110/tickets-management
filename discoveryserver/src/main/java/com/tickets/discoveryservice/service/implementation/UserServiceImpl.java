package com.tickets.discoveryservice.service.implementation;

import com.tickets.discoveryservice.model.User;
import com.tickets.discoveryservice.repository.UserRepository;
import com.tickets.discoveryservice.service.UserService;
import org.springframework.stereotype.Service;

import static com.tickets.discoveryservice.utils.UserUtils.verifyUserQrCode;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.getUserByEmail(email);
    }

    @Override
    public void resetLoginAttempt(String userUuid) {
        userRepository.resetLoginAttempt(userUuid);
    }

    @Override
    public boolean verifyQrCode(String userUuid, String code) {
        var user = userRepository.getUserByUuid(userUuid);
        return verifyUserQrCode(user.getQrCodeSecret(), code);
    }

    @Override
    public void updateLoginAttempt(String email) {
        userRepository.updateLoginAttempt(email);
    }

    @Override
    public void setLastLogin(Long userId) {
        userRepository.setLastLogin(userId);
    }

    @Override
    public void addLoginDevice(Long userId, String deviceName, String client, String ipAddress) {
        userRepository.addLoginDevice(userId, deviceName, client, ipAddress);
    }
}

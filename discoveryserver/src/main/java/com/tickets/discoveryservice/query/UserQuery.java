package com.tickets.discoveryservice.query;

public class UserQuery {
    public static final String SELECT_USER_BY_USER_UUID_QUERY =
                    """
                    SELECT user_uuid, mfa, email, qr_code_secret
                    FROM users
                    WHERE user_uuid = :userUuid
                    """;
    public static final String SELECT_USER_BY_EMAIL_QUERY =
                    """
                    SELECT user_uuid, mfa, email, qr_code_secret
                    FROM users
                    WHERE email = :email
                    """;
    public static final String SELECT_USER_BY_USERNAME_QUERY =
                    """
                    SELECT user_uuid, mfa, email, qr_code_secret
                    FROM users
                    WHERE username = :username
                    """;
    public static final String RESET_LOGIN_ATTEMPTS_QUERY =
                    """
                    UPDATE users
                    SET login_attempts = 0
                    WHERE user_uuid = :userUuid
                    """;
    public static final String UPDATE_LOGIN_ATTEMPTS_QUERY =
                    """
                    UPDATE users
                    SET login_attempts = login_attempts + 1
                    WHERE email = :email
                    """;
    public static final String SET_LAST_LOGIN_QUERY =
                    """
                    UPDATE users
                    SET last_login = CURRENT_TIMESTAMP
                    WHERE user_uuid = :userId
                    """;
    public static final String INSERT_NEW_DEVICE_QUERY =
                    """
                    INSERT INTO user_devices (user_id, device, client, ip_address)
                    VALUES (:userId, :device, :client, :ipAddress)
                    """;
}

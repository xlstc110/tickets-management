BEGIN;

-- Authrozation Server

CREATE TABLE IF NOT EXISTS oauth2_registered_client (
    id VARCHAR(100) PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL,
    client_id_issued_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret VARCHAR(200) DEFAULT NULL,
    client_secret_expires_at TIMESTAMP(6) WITH TIME ZONE DEFAULT NULL,
    client_name VARCHAR(200) NOT NULL,
    client_authentication_methods VARCHAR(1000) NOT NULL,
    authorization_grant_types VARCHAR(1000) NOT NULL,
    redirect_uris VARCHAR(1000) DEFAULT NULL,
    post_logout_redirect_uris VARCHAR(1000) DEFAULT NULL,
    scopes VARCHAR(1000) NOT NULL,
    client_settings VARCHAR(2000) NOT NULL,
    token_settings VARCHAR(2000) NOT NULL
);

-- User Service

CREATE TABLE IF NOT EXISTS users (
    user_id BIGSERIAL PRIMARY KEY,
    user_uuid VARCHAR(40) NOT NULL,
    username VARCHAR(25) NOT NULL,
    first_name VARCHAR(25) NOT NULL,
    last_name VARCHAR(25) NOT NULL,
    email VARCHAR(40) NOT NULL,
    member_id VARCHAR(40) NOT NULL,
    phone VARCHAR(15) DEFAULT NULL,
    address VARCHAR(100) DEFAULT NULL,
    bio VARCHAR(100) DEFAULT NULL,
    qr_code_secret VARCHAR(50) DEFAULT NULL,
    qr_code_image_uri TEXT DEFAULT NULL,
    image_url VARCHAR(255) DEFAULT 'https://cdn-icons-png.flaticon.com/512/149/149071.png',
    last_login TIMESTAMP(6) WITH TIME ZONE DEFAULT NULL,
    login_attempts INTEGER DEFAULT 0,
    mfa BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    account_non_expired BOOLEAN NOT NULL DEFAULT FALSE,
    account_non_locked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_user_uuid UNIQUE (user_uuid),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_member_id UNIQUE (member_id)
);

CREATE TABLE IF NOT EXISTS roles (
    role_id BIGSERIAL PRIMARY KEY,
    role_uuid VARCHAR(40) NOT NULL,
    name VARCHAR(25) NOT NULL,
    authority TEXT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_roles_name UNIQUE (name),
    CONSTRAINT uq_roles_role_uuid UNIQUE (role_uuid)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_role_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_user_roles_role_id FOREIGN KEY (role_id) REFERENCES roles (role_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS credentials (
    credential_id BIGSERIAL PRIMARY KEY,
    credential_uuid VARCHAR(40) NOT NULL,
    user_id BIGINT NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_credentials_credential_uuid UNIQUE (credential_uuid),
    CONSTRAINT uq_credentials_user_id UNIQUE (user_id),
    CONSTRAINT fk_credentials_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS account_tokens (
    account_token_id BIGSERIAL PRIMARY KEY,
    token VARCHAR(40) NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_account_tokens_token UNIQUE (token),
    CONSTRAINT uq_account_tokens_user_id UNIQUE (user_id),
    CONSTRAINT fk_account_tokens_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS password_tokens (
    password_token_id BIGSERIAL PRIMARY KEY,
    token VARCHAR(40) NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_password_tokens_token UNIQUE (token),
    CONSTRAINT uq_password_tokens_user_id UNIQUE (user_id),
    CONSTRAINT fk_password_tokens_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS devices (
    device_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device VARCHAR(40) NOT NULL,
    client VARCHAR(40) NOT NULL,
    ip_address VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_devices_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT
);

-- Ticket Service

CREATE TABLE IF NOT EXISTS tickets (
    ticket_id BIGSERIAL PRIMARY KEY,
    ticket_uuid VARCHAR(40) NOT NULL,
    user_id BIGINT NOT NULL,
    assignee_id BIGINT DEFAULT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    progress INTEGER NOT NULL DEFAULT 0,
    due_date TIMESTAMP(6) WITH TIME ZONE DEFAULT NOW() + INTERVAL '2 week',
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tickets_ticket_uuid UNIQUE (ticket_uuid),
    CONSTRAINT ck_tickets_progress CHECK ((progress >= 0) AND (progress <= 100)),
    CONSTRAINT fk_tickets_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_tickets_assignee_id FOREIGN KEY (assignee_id) REFERENCES users (user_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS tasks (
    task_id BIGSERIAL PRIMARY KEY,
    task_uuid VARCHAR(40) NOT NULL,
    ticket_id BIGINT NOT NULL,
    assignee_id BIGINT DEFAULT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL,
    due_date TIMESTAMP(6) WITH TIME ZONE DEFAULT NOW() + INTERVAL '1 week',
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tasks_task_uuid UNIQUE (task_uuid),
    CONSTRAINT fk_tasks_ticket_id FOREIGN KEY (ticket_id) REFERENCES tickets (ticket_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_tasks_assignee_id FOREIGN KEY (assignee_id) REFERENCES users (user_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS files (
    file_id BIGSERIAL PRIMARY KEY,
    file_uuid VARCHAR(40) NOT NULL,
    ticket_id BIGINT NOT NULL,
    extension VARCHAR(10) NOT NULL,
    formatted_size VARCHAR(10) NOT NULL,
    name VARCHAR(50) NOT NULL,
    size BIGINT NOT NULL,
    uri VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_files_file_uuid UNIQUE (file_uuid),
    CONSTRAINT fk_files_ticket_id FOREIGN KEY (ticket_id) REFERENCES tickets (ticket_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS comments (
    comment_id BIGSERIAL PRIMARY KEY,
    comment_uuid VARCHAR(40) NOT NULL,
    user_id BIGINT NOT NULL,
    ticket_id BIGINT NOT NULL,
    comment TEXT NOT NULL,
    edited BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_comments_comment_uuid UNIQUE (comment_uuid),
    CONSTRAINT fk_comments_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_comments_ticket_id FOREIGN KEY (ticket_id) REFERENCES tickets (ticket_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS statuses (
    status_id BIGSERIAL PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    description VARCHAR(100) NOT NULL,
    CONSTRAINT ck_statuses_status CHECK(status IN ('NEW', 'IN PROGRESS', 'IN REVIEW', 'COMPLETED', 'IMPEDED', 'ASSIGNED', 'UNASSIGNED', 'CLOSED', 'PENDING'))
);

CREATE TABLE IF NOT EXISTS types (
    type_id BIGSERIAL PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    description VARCHAR(100) NOT NULL,
    CONSTRAINT ck_types_type CHECK(type IN ('INCIDENT', 'BUG', 'DESIGN', 'DEFECT', 'ENHANCEMENT'))
);

CREATE TABLE IF NOT EXISTS priorities (
    priority_id BIGSERIAL PRIMARY KEY,
    priority VARCHAR(10) NOT NULL,
    description VARCHAR(100) NOT NULL,
    CONSTRAINT ck_priorities_priority CHECK(priority IN ('LOW', 'MEDIUM', 'HIGH'))
);

CREATE TABLE IF NOT EXISTS ticket_statuses (
    ticket_status_id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    status_id BIGINT NOT NULL,
    CONSTRAINT fk_ticket_statuses_ticket_id FOREIGN KEY (ticket_id) REFERENCES tickets (ticket_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_ticket_statuses_status_id FOREIGN KEY (status_id) REFERENCES statuses (status_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS ticket_types (
    ticket_type_id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    type_id BIGINT NOT NULL,
    CONSTRAINT fk_ticket_types_ticket_id FOREIGN KEY (ticket_id) REFERENCES tickets (ticket_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_ticket_types_type_id FOREIGN KEY (type_id) REFERENCES types (type_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS ticket_priorities (
    ticket_priority_id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    priority_id BIGINT NOT NULL,
    CONSTRAINT fk_ticket_priorities_ticket_id FOREIGN KEY (ticket_id) REFERENCES tickets (ticket_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_ticket_priorities_priority_id FOREIGN KEY (priority_id) REFERENCES priorities (priority_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS task_statuses (
    task_status_id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    status_id BIGINT NOT NULL,
    CONSTRAINT fk_task_statuses_task_id FOREIGN KEY (task_id) REFERENCES tasks (task_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_task_statuses_status_id FOREIGN KEY (status_id) REFERENCES statuses (status_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS messages (
    message_id BIGSERIAL PRIMARY KEY,
    message_uuid VARCHAR(40) NOT NULL,
    conversation_id VARCHAR(40) NOT NULL,
    subject VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_messages_message_uuid UNIQUE (message_uuid),
    CONSTRAINT fk_messages_sender_id FOREIGN KEY (sender_id) REFERENCES users (user_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_messages_receiver_id FOREIGN KEY (receiver_id) REFERENCES users (user_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS message_statuses (
    message_status_id BIGSERIAL PRIMARY KEY,
    message_status VARCHAR(10) DEFAULT 'UNREAD',
    user_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    CONSTRAINT ck_message_statuses_message_status CHECK (message_status IN ('UNREAD', 'READ')),
    CONSTRAINT fk_message_statuses_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT k_message_statuses_message_id FOREIGN KEY (message_id) REFERENCES messages (message_id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE
);

-- Stored Procedures

CREATE OR REPLACE PROCEDURE create_user (IN p_user_uuid VARCHAR(40), IN p_first_name VARCHAR(25), IN p_last_name VARCHAR(25), IN p_email VARCHAR(40), IN p_username VARCHAR(25), IN p_password VARCHAR(255), IN p_credential_uuid VARCHAR(40), IN p_token VARCHAR(40), IN p_member_id VARCHAR(40))
    LANGUAGE PLPGSQL
    AS $$
    DECLARE
        v_user_id BIGINT;
    BEGIN
        INSERT INTO users (user_uuid, first_name, last_name, email, username, member_id) VALUES (p_user_uuid, p_first_name, p_last_name, p_email, p_username, p_member_id) RETURNING user_id INTO v_user_id;
        INSERT INTO credentials (credential_uuid, user_id, password) VALUES (p_credential_uuid, v_user_id, p_password);
        INSERT INTO user_roles (user_id, role_id) VALUES (v_user_id, (SELECT roles.role_id FROM roles WHERE roles.name = 'USER'));
        INSERT INTO account_tokens (user_id, token) VALUES (v_user_id, p_token);
    END;
    $$

-- Functions

CREATE OR REPLACE FUNCTION enable_user_mfa (IN p_user_uuid VARCHAR(40), IN p_qr_code_secret VARCHAR(50), IN p_qr_code_image_uri TEXT)
    RETURNS TABLE(qr_code_image_uri TEXT, member_id VARCHAR, role VARCHAR, authorities TEXT, account_non_expired BOOLEAN, account_non_locked BOOLEAN, created_at TIMESTAMP WITH TIME ZONE, email VARCHAR, enabled BOOLEAN, first_name VARCHAR, user_id BIGINT, image_url VARCHAR, last_login TIMESTAMP WITH TIME ZONE, last_name VARCHAR, mfa BOOLEAN, updated_at TIMESTAMP WITH TIME ZONE, user_uuid VARCHAR, phone VARCHAR, bio VARCHAR, address VARCHAR)
    LANGUAGE PLPGSQL
    AS $$
    BEGIN
        UPDATE users SET mfa = TRUE, qr_code_secret = p_qr_code_secret, qr_code_image_uri = p_qr_code_image_uri WHERE users.user_uuid = p_user_uuid;
        RETURN QUERY SELECT u.qr_code_image_uri, u.member_id, r.name AS role, r.authority AS authorities, u.account_non_expired, u.account_non_locked, u.created_at, u.email, u.enabled, u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, u.updated_at, u.user_uuid, u.phone, u.bio, u.address FROM users u JOIN user_roles ur ON ur.user_id = u.user_id JOIN roles r ON r.role_id = ur.role_id WHERE u.user_uuid = p_user_uuid;
    END;
    $$

CREATE OR REPLACE FUNCTION disable_user_mfa (IN p_user_uuid VARCHAR(40))
    RETURNS TABLE(member_id VARCHAR, role VARCHAR, authorities TEXT, account_non_expired BOOLEAN, account_non_locked BOOLEAN, created_at TIMESTAMP WITH TIME ZONE, email VARCHAR, enabled BOOLEAN, first_name VARCHAR, user_id BIGINT, image_url VARCHAR, last_login TIMESTAMP WITH TIME ZONE, last_name VARCHAR, mfa BOOLEAN, updated_at TIMESTAMP WITH TIME ZONE, user_uuid VARCHAR, phone VARCHAR, bio VARCHAR, address VARCHAR)
    LANGUAGE PLPGSQL
    AS $$
    BEGIN
        UPDATE users SET mfa = FALSE, qr_code_secret = NULL, qr_code_image_uri = NULL WHERE users.user_uuid = p_user_uuid;
        RETURN QUERY SELECT u.member_id, r.name AS role, r.authority AS authorities, u.account_non_expired, u.account_non_locked, u.created_at, u.email, u.enabled, u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, u.updated_at, u.user_uuid, u.phone, u.bio, u.address FROM users u JOIN user_roles ur ON ur.user_id = u.user_id JOIN roles r ON r.role_id = ur.role_id WHERE u.user_uuid = p_user_uuid;
    END;
    $$

CREATE OR REPLACE FUNCTION toggle_account_expired (IN p_user_uuid VARCHAR(40))
    RETURNS TABLE(qr_code_image_uri TEXT, member_id VARCHAR, role VARCHAR, authorities TEXT, account_non_expired BOOLEAN, account_non_locked BOOLEAN, created_at TIMESTAMP WITH TIME ZONE, email VARCHAR, enabled BOOLEAN, first_name VARCHAR, user_id BIGINT, image_url VARCHAR, last_login TIMESTAMP WITH TIME ZONE, last_name VARCHAR, mfa BOOLEAN, updated_at TIMESTAMP WITH TIME ZONE, user_uuid VARCHAR, phone VARCHAR, bio VARCHAR, address VARCHAR)
    LANGUAGE PLPGSQL
    AS $$
    BEGIN
        UPDATE users SET account_non_expired = NOT users.account_non_expired WHERE users.user_uuid = p_user_uuid;
        RETURN QUERY SELECT u.qr_code_image_uri, u.member_id, r.name AS role, r.authority AS authorities, u.account_non_expired, u.account_non_locked, u.created_at, u.email, u.enabled, u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, u.updated_at, u.user_uuid, u.phone, u.bio, u.address FROM users u JOIN user_roles ur ON ur.user_id = u.user_id JOIN roles r ON r.role_id = ur.role_id WHERE u.user_uuid = p_user_uuid;
    END;
    $$

CREATE OR REPLACE FUNCTION toggle_account_locked (IN p_user_uuid VARCHAR(40))
    RETURNS TABLE(qr_code_image_uri TEXT, member_id VARCHAR, role VARCHAR, authorities TEXT, account_non_expired BOOLEAN, account_non_locked BOOLEAN, created_at TIMESTAMP WITH TIME ZONE, email VARCHAR, enabled BOOLEAN, first_name VARCHAR, user_id BIGINT, image_url VARCHAR, last_login TIMESTAMP WITH TIME ZONE, last_name VARCHAR, mfa BOOLEAN, updated_at TIMESTAMP WITH TIME ZONE, user_uuid VARCHAR, phone VARCHAR, bio VARCHAR, address VARCHAR)
    LANGUAGE PLPGSQL
    AS $$
    BEGIN
        UPDATE users SET account_non_locked = NOT users.account_non_locked WHERE users.user_uuid = p_user_uuid;
        RETURN QUERY SELECT u.qr_code_image_uri, u.member_id, r.name AS role, r.authority AS authorities, u.account_non_expired, u.account_non_locked, u.created_at, u.email, u.enabled, u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, u.updated_at, u.user_uuid, u.phone, u.bio, u.address FROM users u JOIN user_roles ur ON ur.user_id = u.user_id JOIN roles r ON r.role_id = ur.role_id WHERE u.user_uuid = p_user_uuid;
    END;
    $$

CREATE OR REPLACE FUNCTION toggle_account_enabled (IN p_user_uuid VARCHAR(40))
    RETURNS TABLE(qr_code_image_uri TEXT, member_id VARCHAR, role VARCHAR, authorities TEXT, account_non_expired BOOLEAN, account_non_locked BOOLEAN, created_at TIMESTAMP WITH TIME ZONE, email VARCHAR, enabled BOOLEAN, first_name VARCHAR, user_id BIGINT, image_url VARCHAR, last_login TIMESTAMP WITH TIME ZONE, last_name VARCHAR, mfa BOOLEAN, updated_at TIMESTAMP WITH TIME ZONE, user_uuid VARCHAR, phone VARCHAR, bio VARCHAR, address VARCHAR)
    LANGUAGE PLPGSQL
    AS $$
    BEGIN
        UPDATE users SET enabled = NOT users.enabled WHERE users.user_uuid = p_user_uuid;
        RETURN QUERY SELECT u.qr_code_image_uri, u.member_id, r.name AS role, r.authority AS authorities, u.account_non_expired, u.account_non_locked, u.created_at, u.email, u.enabled, u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, u.updated_at, u.user_uuid, u.phone, u.bio, u.address FROM users u JOIN user_roles ur ON ur.user_id = u.user_id JOIN roles r ON r.role_id = ur.role_id WHERE u.user_uuid = p_user_uuid;
    END;
    $$

CREATE OR REPLACE FUNCTION update_user_role (IN p_user_uuid VARCHAR(40), IN p_role VARCHAR(25))
    RETURNS TABLE(qr_code_image_uri TEXT, member_id VARCHAR, role VARCHAR, authorities TEXT, account_non_expired BOOLEAN, account_non_locked BOOLEAN, created_at TIMESTAMP WITH TIME ZONE, email VARCHAR, enabled BOOLEAN, first_name VARCHAR, user_id BIGINT, image_url VARCHAR, last_login TIMESTAMP WITH TIME ZONE, last_name VARCHAR, mfa BOOLEAN, updated_at TIMESTAMP WITH TIME ZONE, user_uuid VARCHAR, phone VARCHAR, bio VARCHAR, address VARCHAR)
    LANGUAGE PLPGSQL
    AS $$
    BEGIN
        UPDATE user_roles SET role_id = (SELECT r.role_id FROM roles r WHERE r.name = p_role) WHERE user_roles.user_id = (SELECT users.user_id FROM users WHERE users.user_uuid = p_user_uuid);
        RETURN QUERY SELECT u.qr_code_image_uri, u.member_id, r.name AS role, r.authority AS authorities, u.account_non_expired, u.account_non_locked, u.created_at, u.email, u.enabled, u.first_name, u.user_id, u.image_url, u.last_login, u.last_name, u.mfa, u.updated_at, u.user_uuid, u.phone, u.bio, u.address FROM users u JOIN user_roles ur ON ur.user_id = u.user_id JOIN roles r ON r.role_id = ur.role_id WHERE u.user_uuid = p_user_uuid;
    END;
    $$

CREATE OR REPLACE FUNCTION create_ticket (IN p_ticket_uuid VARCHAR(40), IN p_user_uuid VARCHAR(40), IN p_title VARCHAR(100), IN p_description TEXT, IN p_type VARCHAR(20), IN p_priority VARCHAR(10))
    RETURNS TABLE (comment_count BIGINT, file_count BIGINT, ticket_id BIGINT, ticket_uuid VARCHAR, title VARCHAR, description TEXT, progress INT, due_date TIMESTAMP WITH TIME ZONE, created_at TIMESTAMP WITH TIME ZONE, updated_at TIMESTAMP WITH TIME ZONE, status VARCHAR, type VARCHAR, priority VARCHAR)
    LANGUAGE PLPGSQL
    AS $$
    DECLARE v_ticket_id BIGINT;
    DECLARE v_user_id BIGINT;
    BEGIN
        SELECT user_id FROM users INTO v_user_id WHERE user_uuid = p_user_uuid;
        INSERT INTO tickets (ticket_uuid, user_id, title, description) VALUES (p_ticket_uuid, v_user_id, p_title, p_description) RETURNING tickets.ticket_id INTO v_ticket_id;
        INSERT INTO ticket_statuses (ticket_id, status_id) VALUES (v_ticket_id, (SELECT statuses.status_id FROM statuses WHERE statuses.status = 'NEW'));
        INSERT INTO ticket_types (ticket_id, type_id) VALUES (v_ticket_id, (SELECT types.type_id FROM types WHERE types.type = p_type));
        INSERT INTO ticket_priorities (ticket_id, priority_id) VALUES (v_ticket_id, (SELECT priorities.priority_id FROM priorities WHERE priorities.priority = p_priority));
        RETURN QUERY SELECT COUNT(DISTINCT(c.comment_id)) AS comment_count, COUNT(DISTINCT(f.file_id)) AS file_count, t.ticket_id, t.ticket_uuid, t.title, t.description, t.progress, t.due_date, t.created_at, t.updated_at, s.status, typ.type, pr.priority FROM tickets t JOIN users u ON t.user_id = u.user_id JOIN ticket_statuses ts ON t.ticket_id = ts.ticket_id JOIN ticket_types tt ON t.ticket_id = tt.ticket_id JOIN ticket_priorities tp ON t.ticket_id = tp.ticket_id JOIN statuses s ON s.status_id = ts.status_id JOIN types typ ON typ.type_id = tt.type_id JOIN priorities pr ON pr.priority_id = tp.priority_id LEFT JOIN files f ON t.ticket_id = f.ticket_id LEFT JOIN comments c ON t.ticket_id = c.ticket_id WHERE u.user_uuid = p_user_uuid AND t.ticket_id = v_ticket_id GROUP BY u.user_id, t.ticket_id, s.status, typ.type_id, pr.priority_id;
    END;
    $$

CREATE OR REPLACE FUNCTION create_comment (IN p_comment_uuid VARCHAR(40), IN p_user_uuid VARCHAR(40), IN p_ticket_uuid VARCHAR(40), IN p_comment TEXT)
    RETURNS TABLE (comment_id BIGINT, comment_uuid VARCHAR, comment TEXT, created_at TIMESTAMP WITH TIME ZONE, updated_at TIMESTAMP WITH TIME ZONE, user_uuid VARCHAR, first_name VARCHAR, last_name VARCHAR, image_url VARCHAR)
    LANGUAGE PLPGSQL
    AS $$
    DECLARE v_user_id BIGINT;
    DECLARE v_ticket_id BIGINT;
    DECLARE v_comment_id BIGINT;
    BEGIN
        SELECT user_id FROM users INTO v_user_id WHERE users.user_uuid = p_user_uuid;
        SELECT ticket_id FROM tickets INTO v_ticket_id WHERE ticket_uuid = p_ticket_uuid;
        INSERT INTO comments (comment_uuid, user_id, ticket_id, comment) VALUES (p_comment_uuid, v_user_id, v_ticket_id, p_comment) RETURNING comments.comment_id INTO v_comment_id;
        RETURN QUERY SELECT c.comment_id, c.comment_uuid, c.comment, c.created_at, c.updated_at, u.user_uuid, u.first_name, u.last_name, u.image_url FROM comments c JOIN tickets t ON t.ticket_id = c.ticket_id JOIN users u ON u.user_id = c.user_id WHERE c.comment_id = v_comment_id;
    END;
    $$

CREATE OR REPLACE FUNCTION update_comment (IN p_comment_uuid VARCHAR(40), IN p_comment TEXT)
    RETURNS TABLE (comment_id BIGINT, comment_uuid VARCHAR, comment TEXT, created_at TIMESTAMP WITH TIME ZONE, updated_at TIMESTAMP WITH TIME ZONE, user_uuid VARCHAR, first_name VARCHAR, last_name VARCHAR, image_url VARCHAR)
    LANGUAGE PLPGSQL
    AS $$
    BEGIN
        UPDATE comments SET comment = p_comment, edited = TRUE, update_at = NOW() WHERE comments.comment_uuid = p_comment_uuid;
        RETURN QUERY SELECT c.comment_id, c.comment_uuid, c.comment, c.created_at, c.updated_at, u.user_uuid, u.first_name, u.last_name, u.image_url FROM comments c JOIN users u ON u.user_id = c.user_id WHERE c.comment_uuid = p_comment_uuid;
    END;
    $$

CREATE OR REPLACE FUNCTION update_ticket (IN p_ticket_uuid VARCHAR(40), IN p_title VARCHAR(100), IN p_description TEXT, IN p_progress INTEGER, IN p_due_date VARCHAR(40), p_status VARCHAR(20), IN p_type VARCHAR(20), IN p_priority VARCHAR(10))
    RETURNS TABLE (ticket_id BIGINT, ticket_uuid VARCHAR, title VARCHAR, description TEXT, progress INT, due_date TIMESTAMP WITH TIME ZONE, created_at TIMESTAMP WITH TIME ZONE, updated_at TIMESTAMP WITH TIME ZONE, status VARCHAR, type VARCHAR, priority VARCHAR)
    LANGUAGE PLPGSQL
    AS $$
    DECLARE v_ticket_id BIGINT;
    BEGIN
        UPDATE tickets SET title = p_title, description = p_description, progress = p_progress, due_date = TO_TIMESTAMP(p_due_date, 'YYYY-MM-DD HH24:MI:ss'), update_at = NOW() WHERE tickets.ticket_uuid = p_ticket_uuid RETURNING tickets.ticket_id INTO v_ticket_id;
        UPDATE ticket_statuses SET status_id = (SELECT statuses.status_id FROM statuses WHERE statuses.status = p_status) WHERE ticket_statuses.ticket_id = v_ticket_id;
        UPDATE ticket_types SET type_id = (SELECT types.type_id FROM types WHERE types.type = p_type) WHERE ticket_types.ticket_id = v_ticket_id;
        UPDATE ticket_priorities SET priority_id = (SELECT priorities.priority_id FROM priorities WHERE priorities.priority = p_priority) WHERE ticket_priorities.ticket_id = v_ticket_id;
        RETURN QUERY SELECT t.ticket_id, t.ticket_uuid, t.title, t.description, t.progress, t.due_date, t.created_at, t.updated_at, s.status, typ.type, pr.priority FROM tickets t JOIN ticket_statuses ts ON t.ticket_id = ts.ticket_id JOIN ticket_types tt ON t.ticket_id = tt.ticket_id JOIN ticket_priorities tp ON t.ticket_id = tp.ticket_id JOIN statuses s ON s.status_id = ts.status_id JOIN types typ ON typ.type_id = tt.type_id JOIN priorities pr ON pr.priority_id = tp.priority_id WHERE t.ticket_id = v_ticket_id;
    END;
    $$

CREATE OR REPLACE FUNCTION create_task (IN p_user_uuid VARCHAR(40), IN p_ticket_uuid VARCHAR(40), IN p_task_uuid VARCHAR(40), IN p_name VARCHAR(50), IN p_description VARCHAR(255), IN p_status VARCHAR(20))
    RETURNS TABLE (first_name VARCHAR, last_name VARCHAR, image_url VARCHAR, task_id BIGINT, task_uuid VARCHAR, name VARCHAR, description VARCHAR, status VARCHAR, due_date TIMESTAMP WITH TIME ZONE, created_at TIMESTAMP WITH TIME ZONE, updated_at TIMESTAMP WITH TIME ZONE)
    LANGUAGE PLPGSQL
    AS $$
    DECLARE v_task_id BIGINT;
    DECLARE v_ticket_id BIGINT;
    DECLARE v_user_id BIGINT;
    BEGIN
        SELECT users.user_id FROM users WHERE users.user_uuid = p_user_uuid INTO v_user_id;
        SELECT tickets.ticket_id FROM tickets WHERE ticket.ticket_uuid = p_ticket_uuid INTO v_ticket_id;
        INSERT INTO tasks (task_uuid, ticket_id, assigned_id, name, description) VALUES (p_task_uuid, v_ticket_id, v_user_id, p_name, p_description) RETURNING tasks.task_id INTO v_task_id;
        INSERT INTO task_statuses (task_id, status_id) VALUES (v_task_id, (SELECT statuses.status_id FROM statuses WHERE statuses.status = p_status));
        RETURN QUERY SELECT u.first_name, u.last_name, u.image_url, t.task_id, t.task_uuid, t.name, t.description, s.status, t.due_date, t.created_at, t.updated_at FROM tasks t JOIN users u ON t.assignee_id = u.user_id JOIN task_statuses ts ON t.task_id = ts.task_id JOIN statuses s ON ts.status_id = s.status_id WHERE t.task_id = v_task_id;
    END;
    $$

CREATE OR REPLACE FUNCTION save_ticket_file (IN p_file_uuid VARCHAR(40), IN p_ticket_id BIGINT, IN p_filename VARCHAR(50), IN p_name VARCHAR(50), IN p_size BIGINT, IN p_formatted_size VARCHAR(10), IN p_extension VARCHAR(10), IN p_uri VARCHAR(255))
    RETURNS TABLE (file_id BIGINT, file_uuid VARCHAR, extension VARCHAR, formatted_size VARCHAR, name VARCHAR, size BIGINT, uri VARCHAR, created_at TIMESTAMP WITH TIME ZONE, updated_at TIMESTAMP WITH TIME ZONE)
    LANGUAGE PLPGSQL
    AS $$
    DECLARE v_file_id BIGINT;
    BEGIN
        INSERT INTO files (file_uuid, ticket_id, name, size, formatted_size, extension, uri) VALUES (p_file_uuid, p_ticket_id, p_filename, p_size, p_formatted_size, p_extension, p_uri) RETURNING files.file_id INTO v_file_id;
        RETURN QUERY SELECT f.file_id, f.file_uuid, f.extension, f.formatted_size, f.name, f.size, f.uri, f.created_at, f.updated_at FROM files f WHERE f.file_id = v_file_id;
    END;
    $$

CREATE OR REPLACE FUNCTION create_message (IN p_message_uuid VARCHAR(40), IN p_from_user_uuid VARCHAR(40), IN p_to_email VARCHAR(40), IN p_subject VARCHAR(40), IN p_message TEXT, IN p_conversation_id VARCHAR(40))
    RETURNS TABLE (sender_uuid VARCHAR, sender_first_name VARCHAR, sender_last_name VARCHAR, sender_email VARCHAR, sender_image_url VARCHAR, receiver_uuid VARCHAR, receiver_first_name VARCHAR, receiver_last_name VARCHAR, receiver_email VARCHAR, receiver_image_url VARCHAR, message_id BIGINT, message_uuid VARCHAR, subject VARCHAR, message TEXT, conversation_id VARCHAR, status VARCHAR, created_at TIMESTAMP WITH TIME ZONE, updated_at TIMESTAMP WITH TIME ZONE)
    LANGUAGE PLPGSQL
    AS $$
    DECLARE v_from_user_id BIGINT;
    DECLARE v_to_user_id BIGINT;
    DECLARE v_message_id BIGINT;
    BEGIN
        SELECT users.user_id FROM users WHERE users.user_uuid = p_from_user_uuid INTO v_from_user_id;
        SELECT users.user_id FROM users WHERE users.email = p_to_email INTO v_to_user_id;
        INSERT INTO messages (message_uuid, subject, message, sender_id, receiver_id, conversation_id) VALUES (p_message_uuid, p_subject, p_message, v_from_user_id, v_to_user_id, p_conversation_id) RETURNING messages.message_id INTO v_message_id;
        INSERT INTO message_statuses(message_status, user_id, message_id) VALUES ('READ', v_from_user_id, v_message_id);
        INSERT INTO message_statuses(message_status, user_id, message_id) VALUES ('UNREAD', v_to_user_id, v_message_id);
        RETURN QUERY SELECT s.user_uuid AS sender_uuid, s.first_name AS sender_first_name, s.last_name AS sender_last_name, s.email AS sender_email, s.image_url AS sender_image_url, r.user_uuid AS receiver_uuid, r.first_name AS receiver_first_name, r.last_name AS receiver_last_name, r.email AS receiver_email, r.image_url AS receiver_image_url, m.message_id, m.message_uuid, m.subject, m.message, m.conversation_id, ms.message_status AS status, m.created_at, m.updated_at FROM messages m JOIN users s ON m.sender_id = s.user_id JOIN users r ON m.receiver_id = r.user_id JOIN message_statuses ms ON (ms.user_id = v_from_user_id AND ms.message_id = v_message_id) WHERE m.message_id = v_message_id;
    END;
    $$

END
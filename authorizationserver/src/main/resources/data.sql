-- Populate roles table
INSERT INTO roles (role_uuid, name, authority) VALUES ('7d1b82b1-92c7-4fae-b790-73eb1ac9d6b5', 'USER', 'app:read,user:read,user:update,ticket:create,ticket:read,ticket:update,comment:create,comment:read,comment:update,comment:delete,task:read');
INSERT INTO roles (role_uuid, name, authority) VALUES ('1a0e13de-4fdf-4db0-8a3d-08fce64cbe8c', 'TECH_SUPPORT', 'app:read,user:read,user:update,ticket:create,ticket:read,ticket:update,comment:create,comment:read,comment:update,comment:delete,task:create,task:read,task:update,task:delete');
INSERT INTO roles (role_uuid, name, authority) VALUES ('894853e1-9238-4c64-b5d8-c0a29bdf1b94', 'MANAGER', 'app:read,user:create,user:read,user:update,ticket:create,ticket:read,ticket:update,ticket:delete,comment:create,comment:read,comment:update,comment:delete,task:create,task:read,task:update,task:delete');
INSERT INTO roles (role_uuid, name, authority) VALUES ('7f907494-90b0-4165-b2fd-00e04fb18b49', 'ADMIN', 'app:read,user:create,user:read,user:update,user:delete,ticket:create,ticket:read,ticket:update,ticket:delete,comment:create,comment:read,comment:update,comment:delete,task:create,task:read,task:update,task:delete');
INSERT INTO roles (role_uuid, name, authority) VALUES ('838ca5ee-eb15-427a-b380-6cf7bfbd68b7', 'SUPER_ADMIN', 'app:create,app:read,app:update,app:delete,user:create,user:read,user:update,user:delete,ticket:create,ticket:read,ticket:update,ticket:delete,comment:create,comment:read,comment:update,comment:delete,task:create,task:read,task:update,task:delete');

-- Populate priorities table
INSERT INTO priorities (priority, description) VALUES ('LOW', 'This is low priority');
INSERT INTO priorities (priority, description) VALUES ('MEDIUM', 'This is medium priority');
INSERT INTO priorities (priority, description) VALUES ('HIGH', 'This is high priority');

-- Populate statuses table
INSERT INTO statuses (status, description) VALUES ('NEW', 'This is new');
INSERT INTO statuses (status, description) VALUES ('IN PROGRESS', 'This is in progress');
INSERT INTO statuses (status, description) VALUES ('IN REVIEW', 'This is in review');
INSERT INTO statuses (status, description) VALUES ('COMPLETED', 'This is complete');
INSERT INTO statuses (status, description) VALUES ('IMPEDED', 'This is impeded');
INSERT INTO statuses (status, description) VALUES ('ASSIGNED', 'This is assigned');
INSERT INTO statuses (status, description) VALUES ('UNASSIGNED', 'This is unassigned');
INSERT INTO statuses (status, description) VALUES ('CLOSED', 'This is closed');
INSERT INTO statuses (status, description) VALUES ('PENDING', 'This is pending');

-- Populate types table
INSERT INTO types (type, description) VALUES ('BUG', 'This is a bug fix');
INSERT INTO types (type, description) VALUES ('DEFECT', 'This is a defect');
INSERT INTO types (type, description) VALUES ('INCIDENT', 'This is an incident');
INSERT INTO types (type, description) VALUES ('ENHANCEMENT', 'This is an enhancement');
INSERT INTO types (type, description) VALUES ('DESIGN', 'This is a design');
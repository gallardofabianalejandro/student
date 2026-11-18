CREATE TABLE IF NOT EXISTS app_user (
    id                 VARCHAR(36)  PRIMARY KEY,
    username           VARCHAR(50)  NOT NULL UNIQUE,
    password_hash      VARCHAR(100) NOT NULL,
    roles              VARCHAR(100) NOT NULL,
    created_by         VARCHAR(255) NOT NULL,
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by   VARCHAR(255),
    last_modified_date TIMESTAMP WITH TIME ZONE
);

-- MySQL 8 baseline for a new, empty service database.
CREATE TABLE users (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 email VARCHAR(255) NOT NULL UNIQUE, password_hash VARCHAR(255) NOT NULL,
 full_name VARCHAR(255) NOT NULL, phone VARCHAR(255), avatar_url VARCHAR(255), date_of_birth DATE,
 gender ENUM('MALE','FEMALE','OTHER'),
 activity_level ENUM('SEDENTARY','LIGHT','MODERATE','ACTIVE','VERY_ACTIVE'),
 role ENUM('USER','ADMIN') NOT NULL, status ENUM('PENDING','ACTIVE','INACTIVE','BLOCKED') NOT NULL,
 email_verified BIT NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL
);

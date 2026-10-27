-- MySQL 8 baseline for a new, empty service database.
CREATE TABLE vegetarian_restaurants (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255) NOT NULL, address VARCHAR(255),
 latitude DOUBLE NOT NULL, longitude DOUBLE NOT NULL, phone_number VARCHAR(255), rating DOUBLE,
 is_vegan_only BIT NOT NULL, image_url VARCHAR(500)
);

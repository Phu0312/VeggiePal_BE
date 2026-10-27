-- MySQL 8 baseline for a new, empty service database.
CREATE TABLE allergens (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, code VARCHAR(255) NOT NULL UNIQUE,
 name VARCHAR(255) NOT NULL,
 category ENUM('GRAIN','LEGUME','NUT_SEED','VEGETABLE','FRUIT','MUSHROOM','SPICE','ADDITIVE') NOT NULL
);
CREATE TABLE health_records (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL,
 height_cm DECIMAL(4,1) NOT NULL, weight_kg DECIMAL(4,1) NOT NULL, bmi DECIMAL(5,1) NOT NULL,
 recorded_at DATETIME(6) NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 INDEX idx_health_records_user_recorded (user_id,recorded_at)
);
CREATE TABLE user_allergies (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, allergen_id BIGINT NOT NULL,
 created_at DATETIME(6) NOT NULL,
 CONSTRAINT uk_user_allergies_user_allergen UNIQUE(user_id,allergen_id),
 CONSTRAINT fk_user_allergies_allergen FOREIGN KEY(allergen_id) REFERENCES allergens(id)
);

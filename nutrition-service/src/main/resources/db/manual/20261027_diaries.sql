-- Run against the nutrition-service schema. This file is not auto-executed.
CREATE TABLE IF NOT EXISTS meal_diary (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    entry_date DATE NOT NULL,
    source ENUM('MANUAL','MEAL_PLAN') NOT NULL,
    meal_plan_id BIGINT NULL,
    meal_type ENUM('BREAKFAST','DINNER','LUNCH','SNACK') NOT NULL,
    recipe_id BIGINT NULL,
    title VARCHAR(200) NOT NULL,
    servings DECIMAL(6,2) NOT NULL,
    calories DECIMAL(16,4) NOT NULL,
    protein DECIMAL(16,4) NOT NULL,
    carbohydrate DECIMAL(16,4) NOT NULL,
    fat DECIMAL(16,4) NOT NULL,
    recorded_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_meal_diary_plan_slot UNIQUE (user_id,meal_plan_id,entry_date,meal_type),
    INDEX idx_meal_diary_user_date (user_id,entry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS water_diary (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    entry_date DATE NOT NULL,
    milliliters INT NOT NULL,
    INDEX idx_water_diary_user_date (user_id,entry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS activity_diary (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    entry_date DATE NOT NULL,
    activity_type VARCHAR(100) NOT NULL,
    minutes INT NOT NULL,
    INDEX idx_activity_diary_user_date (user_id,entry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

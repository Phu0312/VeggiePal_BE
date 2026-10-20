ALTER TABLE ai_meal_plans ADD COLUMN version INT NOT NULL DEFAULT 1;

CREATE TABLE ai_meal_plan_favorites (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    meal_plan_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_meal_plan_user_favorite UNIQUE (user_id, meal_plan_id),
    CONSTRAINT fk_favorite_meal_plan FOREIGN KEY (meal_plan_id)
        REFERENCES ai_meal_plans(id) ON DELETE CASCADE,
    INDEX idx_meal_plan_favorites_user_created (user_id, created_at)
);

CREATE TABLE ai_meal_plan_versions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    meal_plan_id BIGINT NOT NULL,
    version INT NOT NULL,
    name VARCHAR(160) NOT NULL,
    goal VARCHAR(500) NOT NULL,
    plan_json LONGTEXT NOT NULL,
    change_summary VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_meal_plan_version UNIQUE (meal_plan_id, version),
    CONSTRAINT fk_version_meal_plan FOREIGN KEY (meal_plan_id)
        REFERENCES ai_meal_plans(id) ON DELETE CASCADE,
    INDEX idx_meal_plan_versions_plan (meal_plan_id, version)
);

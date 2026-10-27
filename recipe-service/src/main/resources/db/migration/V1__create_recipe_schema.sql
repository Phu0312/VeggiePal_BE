-- MySQL 8 baseline for a new, empty service database.
CREATE TABLE ingredients (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, name VARCHAR(150) NOT NULL, normalized_name VARCHAR(150) NOT NULL,
 description VARCHAR(1000), calories_per_100g DECIMAL(10,2) NOT NULL, protein_per_100g DECIMAL(10,2) NOT NULL,
 carbs_per_100g DECIMAL(10,2) NOT NULL, fat_per_100g DECIMAL(10,2) NOT NULL, fiber_per_100g DECIMAL(10,2),
 vegan BIT NOT NULL, allergen BIT NOT NULL, image_url VARCHAR(512), active BIT NOT NULL,
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 CONSTRAINT uk_ingredients_normalized_name UNIQUE(normalized_name),
 INDEX idx_ingredients_name(name), INDEX idx_ingredients_vegan(vegan), INDEX idx_ingredients_allergen(allergen)
);
CREATE TABLE recipes (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, title VARCHAR(200) NOT NULL,
 description VARCHAR(2000), image_url VARCHAR(500), servings INT NOT NULL, prep_time INT, cook_time INT,
 status ENUM('DRAFT','PUBLISHED','HIDDEN','ARCHIVED') NOT NULL,
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL, published_at DATETIME(6)
);
CREATE TABLE recipe_ingredients (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, recipe_id BIGINT NOT NULL, ingredient_id BIGINT NOT NULL,
 quantity DECIMAL(10,2) NOT NULL, unit VARCHAR(30) NOT NULL, note VARCHAR(300),
 CONSTRAINT uk_recipe_ingredient UNIQUE(recipe_id,ingredient_id),
 CONSTRAINT fk_recipe_ingredients_recipe FOREIGN KEY(recipe_id) REFERENCES recipes(id),
 CONSTRAINT fk_recipe_ingredients_ingredient FOREIGN KEY(ingredient_id) REFERENCES ingredients(id)
);
CREATE TABLE recipe_steps (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, recipe_id BIGINT NOT NULL, step_number INT NOT NULL,
 instruction VARCHAR(2000) NOT NULL, CONSTRAINT uk_recipe_step_number UNIQUE(recipe_id,step_number),
 CONSTRAINT fk_recipe_steps_recipe FOREIGN KEY(recipe_id) REFERENCES recipes(id)
);

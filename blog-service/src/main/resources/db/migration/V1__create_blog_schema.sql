-- MySQL 8 baseline for a new, empty service database.
CREATE TABLE categories (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, parent_id BIGINT,
 type ENUM('FOOD_TYPE','RECIPE_TYPE') NOT NULL, name VARCHAR(100) COLLATE utf8mb4_0900_as_ci NOT NULL,
 display_order SMALLINT NOT NULL, is_active BIT NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 CONSTRAINT uk_categories_name UNIQUE(name), CONSTRAINT fk_categories_parent FOREIGN KEY(parent_id) REFERENCES categories(id),
 INDEX idx_categories_parent(parent_id,display_order), INDEX idx_categories_type(type,is_active)
);
CREATE TABLE blogs (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, author_id BIGINT NOT NULL, category_id BIGINT NOT NULL,
 title VARCHAR(200) NOT NULL, content LONGTEXT NOT NULL, thumbnail_url VARCHAR(512),
 status ENUM('DRAFT','PENDING','PUBLISHED','REJECTED','BANNED') NOT NULL,
 view_count INT NOT NULL, vote_score INT NOT NULL, published_at DATETIME(6), created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_blogs_category FOREIGN KEY(category_id) REFERENCES categories(id),
 INDEX idx_blogs_status_published(status,published_at), INDEX idx_blogs_author(author_id,created_at), INDEX idx_blogs_category(category_id,status)
);
CREATE TABLE videos (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, author_id BIGINT NOT NULL, category_id BIGINT NOT NULL,
 title VARCHAR(200) NOT NULL, description TEXT, source_type ENUM('UPLOAD','YOUTUBE') NOT NULL,
 video_url VARCHAR(512) NOT NULL, thumbnail_url VARCHAR(512),
 status ENUM('DRAFT','PENDING','PUBLISHED','REJECTED','BANNED') NOT NULL,
 view_count INT NOT NULL, published_at DATETIME(6), created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_videos_category FOREIGN KEY(category_id) REFERENCES categories(id),
 INDEX idx_videos_status_published(status,published_at), INDEX idx_videos_author(author_id,created_at), INDEX idx_videos_category(category_id,status)
);
CREATE TABLE comments (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, author_id BIGINT NOT NULL,
 target_type ENUM('BLOG','VIDEO') NOT NULL, target_id BIGINT NOT NULL, parent_comment_id BIGINT,
 content TEXT NOT NULL, status ENUM('PENDING','VISIBLE','HIDDEN','DELETED') NOT NULL,
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_comments_parent FOREIGN KEY(parent_comment_id) REFERENCES comments(id),
 INDEX idx_comments_target(target_type,target_id,created_at), INDEX idx_comments_parent(parent_comment_id,created_at), INDEX idx_comments_author(author_id,created_at)
);
CREATE TABLE content_votes (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL,
 target_type ENUM('BLOG','VIDEO') NOT NULL, target_id BIGINT NOT NULL, value INT NOT NULL,
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 CONSTRAINT uk_content_votes_user_target UNIQUE(user_id,target_type,target_id)
);

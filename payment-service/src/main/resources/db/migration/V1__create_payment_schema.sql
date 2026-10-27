-- MySQL 8 baseline for a new, empty service database.
CREATE TABLE subscription_plans (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, code ENUM('FREE','PLUS','PRO') NOT NULL,
 price BIGINT NOT NULL, duration_days INT NOT NULL, chat_per_day INT NOT NULL,
 meal_plans_per_month INT NOT NULL, videos_per_month INT NOT NULL, active BIT NOT NULL,
 CONSTRAINT uk_subscription_plan_code UNIQUE(code)
);
CREATE TABLE payment_orders (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, order_code BIGINT NOT NULL, user_id BIGINT NOT NULL,
 plan_id BIGINT NOT NULL, amount BIGINT NOT NULL, status ENUM('PENDING','PAID','CANCELLED','FAILED') NOT NULL,
 payment_link_id VARCHAR(100), checkout_url VARCHAR(1000), qr_code VARCHAR(4096), account_name VARCHAR(255),
 account_number VARCHAR(100), bin VARCHAR(20), description VARCHAR(255), expired_at DATETIME(6),
 payment_reference VARCHAR(100), created_at DATETIME(6) NOT NULL, paid_at DATETIME(6), version BIGINT NOT NULL,
 CONSTRAINT uk_payment_orders_order_code UNIQUE(order_code),
 CONSTRAINT fk_payment_orders_plan FOREIGN KEY(plan_id) REFERENCES subscription_plans(id),
 INDEX idx_payment_orders_user_created(user_id,created_at),
 INDEX idx_payment_orders_reusable(user_id,plan_id,status,created_at), INDEX idx_payment_orders_status_paid(status,paid_at)
);
CREATE TABLE user_subscriptions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, plan_id BIGINT NOT NULL,
 started_at DATETIME(6) NOT NULL, expires_at DATETIME(6), updated_at DATETIME(6) NOT NULL, version BIGINT NOT NULL,
 CONSTRAINT uk_user_subscriptions_user UNIQUE(user_id),
 CONSTRAINT fk_user_subscriptions_plan FOREIGN KEY(plan_id) REFERENCES subscription_plans(id)
);

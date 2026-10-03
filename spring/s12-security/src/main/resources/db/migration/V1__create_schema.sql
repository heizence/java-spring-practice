-- The bookstore schema (MySQL 9.7, InnoDB, utf8mb4).
-- Conventions: plural snake_case table names, BIGINT AUTO_INCREMENT ids, money in won as BIGINT (KRW has no
-- minor unit), timestamps as DATETIME(6) in UTC, enums stored as VARCHAR names.

CREATE TABLE members (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    name          VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_members_email (email)
);

CREATE TABLE categories (
    id   BIGINT      NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_name (name)
);

CREATE TABLE books (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    isbn           CHAR(13)     NOT NULL,
    title          VARCHAR(200) NOT NULL,
    author         VARCHAR(100) NOT NULL,
    category_id    BIGINT       NOT NULL,
    price          BIGINT       NOT NULL,
    published_date DATE         NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_books_isbn (isbn),
    KEY idx_books_category (category_id),
    CONSTRAINT fk_books_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

-- Stock lives in its own table, so that locking stock doesn't lock the whole book row.
CREATE TABLE inventories (
    book_id  BIGINT NOT NULL,
    quantity INT    NOT NULL,
    version  BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (book_id),
    CONSTRAINT fk_inventories_book FOREIGN KEY (book_id) REFERENCES books (id)
);

CREATE TABLE orders (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    member_id    BIGINT      NOT NULL,
    status       VARCHAR(20) NOT NULL,
    total_amount BIGINT      NOT NULL,
    ordered_at   DATETIME(6) NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_orders_member (member_id),
    KEY idx_orders_status_ordered_at (status, ordered_at),
    CONSTRAINT fk_orders_member FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE TABLE order_lines (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    order_id   BIGINT NOT NULL,
    book_id    BIGINT NOT NULL,
    quantity   INT    NOT NULL,
    unit_price BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_order_lines_order (order_id),
    KEY idx_order_lines_book (book_id),
    CONSTRAINT fk_order_lines_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_lines_book FOREIGN KEY (book_id) REFERENCES books (id)
);

CREATE TABLE wallets (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    member_id  BIGINT      NOT NULL,
    balance    BIGINT      NOT NULL,
    version    BIGINT      NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallets_member (member_id),
    CONSTRAINT fk_wallets_member FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE TABLE wallet_transactions (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    wallet_id  BIGINT      NOT NULL,
    type       VARCHAR(20) NOT NULL,
    amount     BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_wallet_transactions_wallet (wallet_id),
    CONSTRAINT fk_wallet_transactions_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id)
);

CREATE TABLE coupons (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    code            VARCHAR(30) NOT NULL,
    discount_amount BIGINT      NOT NULL,
    total_quantity  INT         NOT NULL,
    issued_quantity INT         NOT NULL DEFAULT 0,
    valid_from      DATETIME(6) NOT NULL,
    valid_until     DATETIME(6) NOT NULL,
    version         BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_coupons_code (code)
);

CREATE TABLE coupon_issues (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    coupon_id BIGINT      NOT NULL,
    member_id BIGINT      NOT NULL,
    issued_at DATETIME(6) NOT NULL,
    used_at   DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_coupon_issues_coupon_member (coupon_id, member_id),
    KEY idx_coupon_issues_member (member_id),
    CONSTRAINT fk_coupon_issues_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id),
    CONSTRAINT fk_coupon_issues_member FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE TABLE reviews (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    book_id    BIGINT        NOT NULL,
    member_id  BIGINT        NOT NULL,
    rating     TINYINT       NOT NULL,
    content    VARCHAR(1000) NOT NULL,
    created_at DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_reviews_book (book_id),
    KEY idx_reviews_member (member_id),
    CONSTRAINT fk_reviews_book FOREIGN KEY (book_id) REFERENCES books (id),
    CONSTRAINT fk_reviews_member FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE TABLE payments (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    order_id        BIGINT      NOT NULL,
    idempotency_key VARCHAR(64) NOT NULL,
    amount          BIGINT      NOT NULL,
    status          VARCHAR(20) NOT NULL,
    created_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payments_idempotency_key (idempotency_key),
    KEY idx_payments_order (order_id),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

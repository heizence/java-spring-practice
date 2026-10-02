-- S06: members can keep a wishlist of books they want to buy later.
-- One row per member and book. The note column is called "memo", the name the old shop's database used.

CREATE TABLE wishlist_items (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    member_id  BIGINT       NOT NULL,
    book_id    BIGINT       NOT NULL,
    priority   VARCHAR(10)  NOT NULL,
    memo       VARCHAR(200) NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wishlist_items_member_book (member_id, book_id),
    KEY idx_wishlist_items_book (book_id),
    CONSTRAINT fk_wishlist_items_member FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_wishlist_items_book FOREIGN KEY (book_id) REFERENCES books (id)
);

-- Wishlists: members save books they want to buy later. One row is one saved book.
-- Same conventions as V1: BIGINT AUTO_INCREMENT id, DATETIME(6) in UTC, named keys and constraints.

CREATE TABLE wishlist_items (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    member_id  BIGINT      NOT NULL,
    book_id    BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    -- A member saves a book at most once. The database enforces it, so two requests that arrive at the same time
    -- can't both insert the same book. member_id comes first, so this key also finds a member's whole wishlist.
    UNIQUE KEY uk_wishlist_items_member_book (member_id, book_id),
    -- For "how many members saved this book?", and for the foreign key, which needs an index on book_id. Without
    -- this line MySQL would quietly create one itself; declaring it, as V1 does, documents what it's for.
    KEY idx_wishlist_items_book (book_id),
    CONSTRAINT fk_wishlist_items_member FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_wishlist_items_book FOREIGN KEY (book_id) REFERENCES books (id)
);

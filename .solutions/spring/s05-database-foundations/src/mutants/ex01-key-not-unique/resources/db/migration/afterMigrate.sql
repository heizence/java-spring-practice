-- Verification mutant for S05-01. A mutant can't replace V3 itself: Flyway stops when it finds two version 3
-- migrations. Flyway runs afterMigrate.sql after every migration run instead, so this file turns the reference V3
-- into the mistaken one: the same key on (member_id, book_id), but not unique.
ALTER TABLE wishlist_items
    DROP INDEX uk_wishlist_items_member_book,
    ADD KEY uk_wishlist_items_member_book (member_id, book_id);

-- Verification mutant for S05-01. A mutant can't replace V3 itself: Flyway stops when it finds two version 3
-- migrations. Flyway runs afterMigrate.sql after every migration run instead, so this file turns the reference V3
-- into the mistaken one: without the foreign key to books.
ALTER TABLE wishlist_items DROP FOREIGN KEY fk_wishlist_items_book;

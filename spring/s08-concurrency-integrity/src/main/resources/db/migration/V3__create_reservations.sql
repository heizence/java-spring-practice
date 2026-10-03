-- S08: copies that members reserve online and pick up in the store. Reserved copies leave the stock right away.

CREATE TABLE reservations (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    book_id     BIGINT      NOT NULL,
    member_id   BIGINT      NOT NULL,
    quantity    INT         NOT NULL,
    reserved_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_reservations_book_member (book_id, member_id),
    CONSTRAINT fk_reservations_book FOREIGN KEY (book_id) REFERENCES books (id),
    CONSTRAINT fk_reservations_member FOREIGN KEY (member_id) REFERENCES members (id)
);

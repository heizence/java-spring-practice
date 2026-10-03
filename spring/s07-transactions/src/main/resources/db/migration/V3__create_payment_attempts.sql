-- S07-03: an audit log of failed payment attempts. Each row is written in a transaction of its own, so it survives
-- when the payment that failed is rolled back.
--
-- No foreign keys, on purpose. An audit row must be written even while the failing payment's transaction still
-- holds locks on its order: a foreign key check would make the insert wait for those locks, and the payment waits
-- for the insert, so both would hang until the lock wait timeout. Audit rows also outlive the rows they mention.
CREATE TABLE payment_attempts (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    order_id     BIGINT      NOT NULL,
    member_id    BIGINT      NOT NULL,
    amount       BIGINT      NOT NULL,
    error_code   VARCHAR(50) NOT NULL,
    attempted_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_payment_attempts_order (order_id)
);

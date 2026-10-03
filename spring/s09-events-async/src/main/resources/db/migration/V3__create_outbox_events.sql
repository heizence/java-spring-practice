-- S09-04: the transactional outbox.
-- A service inserts an event row in the same transaction as its business change, so the event exists exactly when
-- the change was committed. The relay (outbox/OutboxRelay) publishes the unpublished rows to the message broker and
-- then sets published_at.
CREATE TABLE outbox_events (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    aggregate_type VARCHAR(50)  NOT NULL,           -- what the event is about, such as 'Order'
    aggregate_id   BIGINT       NOT NULL,           -- the id of that order: consumers keep its events in order
    event_type     VARCHAR(100) NOT NULL,           -- such as 'OrderShipped'
    payload        JSON         NOT NULL,           -- the event itself
    created_at     DATETIME(6)  NOT NULL,
    published_at   DATETIME(6)  NULL,               -- NULL until the relay has published the event
    attempts       INT          NOT NULL DEFAULT 0, -- how many times publishing it has failed
    PRIMARY KEY (id),
    -- The relay's query: the unpublished events, oldest first.
    KEY idx_outbox_events_unpublished (published_at, id)
);

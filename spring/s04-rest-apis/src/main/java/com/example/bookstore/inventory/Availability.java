package com.example.bookstore.inventory;

/** Whether a book can be ordered now, by the number of copies in stock. */
public enum Availability {

    /** 5 copies or more. */
    IN_STOCK,

    /** 1 to 4 copies: the shop shows "Only a few left". */
    LOW_STOCK,

    /** No copies. */
    OUT_OF_STOCK;

    private static final int LOW_STOCK_BELOW = 5;

    static Availability of(int quantity) {
        if (quantity <= 0) {
            return OUT_OF_STOCK;
        }
        return quantity < LOW_STOCK_BELOW ? LOW_STOCK : IN_STOCK;
    }
}

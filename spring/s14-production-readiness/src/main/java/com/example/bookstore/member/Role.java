package com.example.bookstore.member;

/** What a member is allowed to do. Stored by name in {@code members.role}. */
public enum Role {

    /** A customer: browses books, places orders, writes reviews. */
    USER,

    /** Manages the store, for example the catalog and stock. */
    ADMIN
}

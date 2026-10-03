package com.example.bookstore.member;

import java.util.Optional;
import java.util.TreeMap;

import org.springframework.stereotype.Repository;

/**
 * Stores {@link Member}s in memory. It stands in for the {@code members} table, which arrives in S05.
 *
 * <p>The methods are {@code synchronized}: Tomcat serves each request on its own thread, so several threads use this
 * one bean at the same time.
 */
@Repository
public class MemberRepository {

    private final TreeMap<Long, Member> members = new TreeMap<>();
    private long lastId;

    public synchronized Optional<Member> findById(long id) {
        return Optional.ofNullable(members.get(id));
    }

    /** Stores a new member and returns it with its id. */
    public synchronized Member save(Member member) {
        if (member.id() != null) {
            throw new IllegalArgumentException("Only new members can be saved here: " + member);
        }
        Member saved = new Member(++lastId, member.email(), member.name(), member.role());
        members.put(saved.id(), saved);
        return saved;
    }

    /** Removes every member and starts the ids at 1 again, like SQL's {@code TRUNCATE}. */
    public synchronized void deleteAll() {
        members.clear();
        lastId = 0;
    }
}

package com.example.bookstore.member;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Stores {@link Member}s. Emails are unique ({@code uk_members_email}), and MySQL compares them without regard to
 * case, like {@link Member} does.
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);
}

package com.example.bookstore.common;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Base class for entities whose table has {@code created_at} and {@code updated_at} columns.
 *
 * <p>Spring Data JPA auditing ({@link JpaAuditingConfiguration}) fills both fields from the {@code Clock} bean:
 * {@code createdAt} and {@code updatedAt} when the entity is first saved, and {@code updatedAt} again whenever
 * Hibernate writes a change to it. Tables with only {@code created_at} (rows that never change, such as
 * {@code reviews}) don't extend this class; their entity declares its own {@code @CreatedDate} field.
 *
 * <h2>Entity conventions in this codebase</h2>
 * <ul>
 *   <li>A {@code protected} no-argument constructor for Hibernate, and no setters. Entities are created through a
 *       static factory or constructor that checks its arguments, and changed only through methods that keep the
 *       business rules ({@code inventory.decrease(2)}, {@code order.cancel()}).</li>
 *   <li>A broken business rule throws {@link com.example.bookstore.common.error.BusinessException}. An invalid
 *       argument that request validation should have stopped (a {@code null}, a negative quantity) is a programming
 *       error and throws {@link IllegalArgumentException}.</li>
 *   <li>{@code @ManyToOne} and {@code @OneToOne} associations are {@code LAZY}; enums are stored by name.</li>
 *   <li><b>{@code equals} and {@code hashCode} are not overridden</b>, so entities compare by identity. Inside one
 *       persistence context Hibernate returns the same instance for the same row, so {@code ==} already means "same
 *       row". Overriding them is easy to get wrong: a generated id is {@code null} until the entity is saved (its
 *       hash code would change while it sits in a {@code HashSet}), and Hibernate proxies are subclasses whose
 *       fields stay empty. To compare entities from different persistence contexts, compare their ids.</li>
 * </ul>
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}

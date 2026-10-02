package com.example.bookstore.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Template · Member")
class MemberTest {

    @Test
    @DisplayName("register() creates a customer and stores the email trimmed and in lower case")
    void registersACustomer() {
        Member member = Member.register("  Minji@Example.COM ", "Minji Kim", "{noop}secret");

        assertThat(member.getEmail()).isEqualTo("minji@example.com");
        assertThat(member.getName()).isEqualTo("Minji Kim");
        assertThat(member.getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("admin() creates an administrator")
    void createsAnAdmin() {
        assertThat(Member.admin("admin@example.com", "Admin", "{noop}secret").getRole()).isEqualTo(Role.ADMIN);
    }

    @ParameterizedTest(name = "{0}")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "minji.example.com"})
    @DisplayName("rejects a missing or malformed email")
    void rejectsInvalidEmails(String email) {
        assertThatThrownBy(() -> Member.register(email, "Minji Kim", "{noop}secret"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package practice.j03.ex03;

/** The rules a password can break, in the order {@link PasswordPolicy#check} reports them. */
public enum Violation {
    TOO_SHORT,
    TOO_LONG,
    MISSING_UPPERCASE,
    MISSING_LOWERCASE,
    MISSING_DIGIT,
    CONTAINS_WHITESPACE,
    CONTAINS_USERNAME
}

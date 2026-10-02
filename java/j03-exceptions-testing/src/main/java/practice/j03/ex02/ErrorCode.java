package practice.j03.ex02;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Every business error the order module can report. Each has a stable code, the HTTP status that a REST API
 * answers with, and a message template.
 *
 * <p>Why codes? Messages are for people, and their wording changes. Codes are for programs: a client app can show
 * its own translated text for {@code ORDER-001}, and a log search for {@code ORDER-001} finds every case, whatever
 * the wording was at the time.
 *
 * <p>This enum is given. Exercise J03-02 uses it.
 */
public enum ErrorCode {

    OUT_OF_STOCK("ORDER-001", 409, "Not enough stock for {sku}: requested {requested}, available {available}"),
    INVALID_QUANTITY("ORDER-002", 400, "Quantity must be between {min} and {max}, but was {quantity}"),
    PRODUCT_NOT_FOUND("ORDER-003", 404, "No product with SKU {sku}");

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\w+)}");

    private final String code;
    private final int httpStatus;
    private final String messageTemplate;

    ErrorCode(String code, int httpStatus, String messageTemplate) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.messageTemplate = messageTemplate;
    }

    /** The stable code, for example {@code "ORDER-001"}. */
    public String code() {
        return code;
    }

    /** The HTTP status for this error, for example 409 (Conflict) or 404 (Not Found). */
    public int httpStatus() {
        return httpStatus;
    }

    /** The message with {@code {name}} placeholders, for example {@code "No product with SKU {sku}"}. */
    public String messageTemplate() {
        return messageTemplate;
    }

    /**
     * Fills in the template: each {@code {name}} is replaced by the value stored under {@code name} in
     * {@code details}. For example, {@code PRODUCT_NOT_FOUND.format(Map.of("sku", "BOOK-1"))} returns
     * {@code "No product with SKU BOOK-1"}.
     *
     * @throws IllegalArgumentException if {@code details} has no value for a placeholder
     */
    public String format(Map<String, Object> details) {
        Objects.requireNonNull(details, "details");
        return PLACEHOLDER.matcher(messageTemplate).replaceAll(match -> {
            String name = match.group(1);
            if (!details.containsKey(name)) {
                throw new IllegalArgumentException(code + ": no value for {" + name + "} in " + details);
            }
            // quoteReplacement: a value such as "$5" must be inserted as it is, not read as a regex group reference.
            return Matcher.quoteReplacement(String.valueOf(details.get(name)));
        });
    }
}

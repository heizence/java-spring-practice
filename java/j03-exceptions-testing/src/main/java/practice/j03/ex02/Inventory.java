package practice.j03.ex02;

/**
 * Where {@link OrderValidator} looks up stock. In a real application this is a repository or a client for another
 * service. In the tests it's a small fake, written as a lambda.
 *
 * <p>This interface is given.
 */
@FunctionalInterface
public interface Inventory {

    /**
     * Returns how many units of {@code sku} are in stock: zero or more.
     *
     * <p>Like many data-access APIs (for example Spring's {@code JdbcTemplate.queryForObject}), this method throws
     * when the product doesn't exist, instead of returning "nothing".
     *
     * @throws java.util.NoSuchElementException if no product has this SKU
     */
    int stockOf(String sku);
}

package practice.j09.ex04.sample.shop;

import java.util.HashMap;
import java.util.Map;

import practice.j09.ex04.Component;
import practice.j09.ex04.Init;

/** A catalog that loads its prices in its {@code @Init} method. */
@Component
public class InMemoryProductCatalog implements ProductCatalog {

    private final Map<String, Long> prices = new HashMap<>();

    @Init
    public void load() {
        prices.put("book", 2_500L);
        prices.put("pen", 300L);
    }

    @Override
    public long priceCents(String product) {
        Long price = prices.get(product);
        if (price == null) {
            throw new IllegalArgumentException("Unknown product: " + product);
        }
        return price;
    }

    public boolean isLoaded() {
        return !prices.isEmpty();
    }
}

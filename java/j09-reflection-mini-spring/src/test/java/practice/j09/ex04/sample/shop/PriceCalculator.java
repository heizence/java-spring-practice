package practice.j09.ex04.sample.shop;

import practice.j09.ex04.Component;

/** Depends on the catalog through its interface. */
@Component
public class PriceCalculator {

    private final ProductCatalog catalog;

    public PriceCalculator(ProductCatalog catalog) {
        this.catalog = catalog;
    }

    public long totalCents(String product, int quantity) {
        return catalog.priceCents(product) * quantity;
    }

    public ProductCatalog catalog() {
        return catalog;
    }
}

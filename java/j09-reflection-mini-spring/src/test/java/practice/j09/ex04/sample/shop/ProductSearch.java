package practice.j09.ex04.sample.shop;

import practice.j09.ex04.Service;

/** Also depends on the catalog, so two beans share it. Marked {@code @Service}, which carries {@code @Component}. */
@Service
public class ProductSearch {

    private final ProductCatalog catalog;

    public ProductSearch(ProductCatalog catalog) {
        this.catalog = catalog;
    }

    public boolean exists(String product) {
        try {
            catalog.priceCents(product);
            return true;
        } catch (IllegalArgumentException unknown) {
            return false;
        }
    }

    public ProductCatalog catalog() {
        return catalog;
    }
}

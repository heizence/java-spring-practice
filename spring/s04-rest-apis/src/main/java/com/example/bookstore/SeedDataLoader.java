package com.example.bookstore;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import com.example.bookstore.book.Book;
import com.example.bookstore.book.BookRepository;
import com.example.bookstore.book.Category;
import com.example.bookstore.book.CategoryRepository;
import com.example.bookstore.inventory.Inventory;
import com.example.bookstore.inventory.InventoryRepository;
import com.example.bookstore.member.Member;
import com.example.bookstore.member.MemberRepository;
import com.example.bookstore.member.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Fills the in-memory repositories with the bookstore's seed data when the application starts: the categories,
 * books, stock and members described in {@code spring/BOOKSTORE.md}. The rows come from the tab-separated files in
 * {@code src/main/resources/seed/}, and they are the same rows as in the database from S05 on.
 *
 * <p>It runs in {@link #afterPropertiesSet()}, while Spring creates the beans, so the data is there before the web
 * server accepts the first request.
 */
@Component
@RequiredArgsConstructor
public class SeedDataLoader implements InitializingBean {

    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final InventoryRepository inventoryRepository;
    private final MemberRepository memberRepository;

    @Override
    public void afterPropertiesSet() {
        load();
    }

    /**
     * Saves every seed row into the repositories, which must be empty, so that the ids come out as in the files.
     *
     * @throws IllegalStateException if a saved row gets another id than the file says
     */
    public void load() {
        for (List<String> row : rows("categories.tsv")) {
            Category category = categoryRepository.save(Category.create(row.get(1)));
            checkId(category.id(), row.getFirst());
        }
        for (List<String> row : rows("books.tsv")) {
            Category category = categoryRepository.findById(Long.parseLong(row.get(4))).orElseThrow();
            Book book = bookRepository.save(Book.create(row.get(1), row.get(2), row.get(3), category,
                    Long.parseLong(row.get(5)), LocalDate.parse(row.get(6))));
            checkId(book.getId(), row.getFirst());
        }
        for (List<String> row : rows("inventories.tsv")) {
            inventoryRepository.save(new Inventory(Long.parseLong(row.get(0)), Integer.parseInt(row.get(1))));
        }
        for (List<String> row : rows("members.tsv")) {
            Member member = memberRepository.save(Member.create(row.get(1), row.get(2), Role.valueOf(row.get(3))));
            checkId(member.id(), row.getFirst());
        }
    }

    /** The rows of a seed file without its header line, each split at the tabs. */
    private static List<List<String>> rows(String fileName) {
        try (InputStream input = new ClassPathResource("seed/" + fileName).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .skip(1)
                    .filter(line -> !line.isBlank())
                    .map(line -> List.of(line.split("\t")))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the seed file " + fileName, e);
        }
    }

    private static void checkId(long savedId, String expectedId) {
        if (savedId != Long.parseLong(expectedId)) {
            throw new IllegalStateException("Seed row %s was saved with id %d; are the repositories empty?"
                    .formatted(expectedId, savedId));
        }
    }
}

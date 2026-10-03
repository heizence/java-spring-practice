package com.example.bookstore.book;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.bookstore.book.PriceChangeResponse.BookPriceChange;
import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Changes book prices in bulk, for the purchasing team. */
@Service
@RequiredArgsConstructor
public class PriceChangeService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final Clock clock;

    /**
     * Changes the price of every book in a category by a percentage: new price = old price × (100 + percent) / 100.
     * Every price in the store is a multiple of 1,000 won, so the new prices are whole numbers of won.
     *
     * <ul>
     *   <li>All prices change with <b>one</b> SQL {@code UPDATE} statement, however many books the category has.</li>
     *   <li>The {@code updated_at} of every changed book becomes the current time (the {@code Clock}'s), as after
     *       any other change.</li>
     *   <li>The response lists every book of the category with its price before and after the change.</li>
     *   <li>Code that runs later in the same transaction sees the new prices, even for books that were loaded before
     *       the change.</li>
     * </ul>
     *
     * @param percent from -50 to 50, checked by the request's validation
     * @throws BusinessException {@link ErrorCode#CATEGORY_NOT_FOUND} if no category has this id
     */
    @Transactional
    public PriceChangeResponse changePrices(long categoryId, int percent) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND,
                        "Category %d was not found.".formatted(categoryId)));
        Map<Long, Long> oldPrices = bookRepository.findByCategoryIdOrderById(categoryId).stream()
                .collect(Collectors.toMap(Book::getId, Book::getPrice));

        int changedBooks = bookRepository.changePrices(categoryId, percent, clock.instant());

        // The UPDATE changed the rows in the database, but not the Book objects loaded above. The repository method
        // clears the persistence context afterwards, so this query builds new Book objects from the new rows.
        // Without the clear, Hibernate would return the objects loaded above, which still hold the old prices.
        List<BookPriceChange> books = bookRepository.findByCategoryIdOrderById(categoryId).stream()
                .map(book -> new BookPriceChange(book.getId(), book.getTitle(), oldPrices.get(book.getId()),
                        book.getPrice()))
                .toList();
        return new PriceChangeResponse(category.getId(), category.getName(), percent, changedBooks, books);
    }
}

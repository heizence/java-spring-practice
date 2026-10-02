package com.example.bookstore.book;

import java.time.Clock;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    public PriceChangeResponse changePrices(long categoryId, int percent) {
        // TODO: S06-07
        throw new UnsupportedOperationException("TODO: S06-07");
    }
}

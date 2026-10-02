# Dates and money

Time and money bugs are expensive. They show up as wrong bills, missed deadlines, or reports that are off by a day for customers in another time zone. A few rules prevent almost all of them.

## Time: choose the right type

| Type | What it is | Use it for |
|---|---|---|
| `Instant` | A point on the global timeline (UTC) | Timestamps: `createdAt`, event times, storing and comparing moments |
| `LocalDate` | A date without time or zone | Birthdays, business dates, "valid until 2026-12-31" |
| `LocalTime` | A wall-clock time without date or zone | "The shop opens at 09:00" |
| `LocalDateTime` | A date and wall-clock time, **no zone** | Rarely right on its own: it is *not* a moment in time |
| `ZonedDateTime` | A date and time in a specific zone (`Asia/Seoul`) | Calculations that follow a region's calendar and DST rules |
| `OffsetDateTime` | A date and time with a fixed UTC offset (`+09:00`) | API payloads (ISO-8601) and database columns |
| `Duration` | An amount of time in seconds/nanoseconds | Timeouts, "2 hours 30 minutes" |
| `Period` | An amount of time in years/months/days | "1 month later" (calendar-aware) |

## Rules

- **Store and pass moments as `Instant`** (or `OffsetDateTime` in APIs). Convert to a zone only at the edges: when showing a time to a user, or when a business rule depends on a local calendar ("the coupon expires at midnight Seoul time").
- **Always name the zone explicitly**: `ZoneId.of("Asia/Seoul")`. Never depend on the server's default zone. Servers often run in UTC, while your laptop runs in KST.
- **Inject a `Clock` instead of calling `Instant.now()` or `LocalDate.now()` directly.** Production code receives `Clock.systemUTC()`; tests pass `Clock.fixed(...)` to control "now".
  ```java
  public final class CouponValidator {
      private final Clock clock;
      public CouponValidator(Clock clock) { this.clock = clock; }

      public boolean isValid(Coupon coupon) {
          return Instant.now(clock).isBefore(coupon.expiresAt());
      }
  }
  ```
- **Never use `java.util.Date`, `Calendar` or `SimpleDateFormat`** in new code. They're mutable, not thread-safe, and confusing. `java.time` replaced them in Java 8.
- **Mind daylight saving time.** In zones with DST, some local times don't exist (the clock jumps from 02:00 to 03:00) and some happen twice. `ZonedDateTime` resolves both cases; `LocalDateTime` arithmetic ignores them. Korea has no DST today, but your users and servers may be elsewhere.
- **Format with ISO-8601** for machines (`2026-10-02T09:30:00Z`). Use `DateTimeFormatter` with an explicit pattern and `Locale` for humans.

## Money: use BigDecimal

`double` can't represent most decimal fractions exactly (`0.1 + 0.2 != 0.3`), so it must never hold money.

- **Create `BigDecimal` from a `String` or with `valueOf`:** `new BigDecimal("19.99")`, `BigDecimal.valueOf(1999, 2)`. Never `new BigDecimal(19.99)`: that captures the double's error exactly.
- **Always state the rounding.** Division can produce endless decimals (1/3), and then `divide` without a `RoundingMode` throws `ArithmeticException`. Pick the mode your domain requires: `HALF_EVEN` ("banker's rounding") avoids bias across many operations, and `HALF_UP` is what people learn in school.
- **Compare with `compareTo`, not `equals`.** `new BigDecimal("1.0").equals(new BigDecimal("1.00"))` is `false`, because `equals` also compares scale. `compareTo` returns 0.
- **Currencies have different minor units.** USD has 2 decimals, KRW and JPY have none. `Currency.getInstance("KRW").getDefaultFractionDigits()` tells you.
- **Splitting money must not lose a cent (or a won).** 100 split three ways is 33.34 + 33.33 + 33.33, not 33.33 × 3.
- **Keep the amount and its currency together** in a `Money` value object, so nobody adds dollars to won.

An alternative some teams use: store money as a `long` count of minor units (cents). It's simple and fast, but rounding rules still need care whenever you multiply or divide.

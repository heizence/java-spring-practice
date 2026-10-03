# J07 · Everyday APIs

⏱ 4 sessions · week 5 · 4 exercises

Dates, money and files show up in almost every backend feature: an order has a timestamp and a total, and a monthly report is a CSV file. They're also where the bugs hide that appear only at midnight, at the end of a month, in another time zone, or when a cent goes missing. This module covers the `java.time` API, `BigDecimal` for money, and the NIO.2 file API, plus the traps of the CSV format. Read lesson sections 1–5 before J07-01 and J07-02, section 6 before J07-03, and sections 7–8 before J07-04.

## Lesson

### 1. Which date-time type?

`java.time` (Java 8 and later) replaces the old `java.util.Date` and `Calendar`. All its types are immutable and thread-safe. Choose the type by what you actually know:

| Type | What it is | Example | Use it for |
|---|---|---|---|
| `Instant` | A moment on the timeline, in UTC, to the nanosecond | `2026-03-08T07:30:00Z` | timestamps: "created at", "paid at", log entries |
| `LocalDate` | A date without a time or a zone | `2026-03-08` | birthdays, holidays, due dates, coupon dates |
| `LocalTime` | A time of day | `09:00` | opening hours |
| `LocalDateTime` | A date and a time without a zone: what a wall clock shows, but not where | `2026-03-08T02:30` | a time a user typed, before you attach their zone; rarely right to store |
| `ZonedDateTime` | A date and a time in a zone, with that zone's rules | `2026-03-08T03:30-04:00[America/New_York]` | calendar arithmetic, showing times to people |
| `OffsetDateTime` | A date and a time with a fixed offset from UTC | `2026-03-08T16:30+09:00` | JSON and database values: ISO 8601 text carries an offset, not a zone |

```java
Instant paidAt = Instant.now(clock);                             // a moment
LocalDate birthday = LocalDate.of(2004, 2, 29);                   // a date
ZoneId seoul = ZoneId.of("Asia/Seoul");
ZonedDateTime inSeoul = paidAt.atZone(seoul);                     // that moment, as clocks in Seoul show it
LocalDate dateInSeoul = LocalDate.ofInstant(paidAt, seoul);       // the date in Seoul at that moment
```

The key difference: an `Instant` is the same moment everywhere. A `LocalDateTime` isn't a moment at all: 09:00 on 2026-03-08 happens 13 hours earlier in Seoul than in New York. To turn it into a moment, add a zone: `localDateTime.atZone(zone)`.

Every operation returns a new object: `date.plusDays(1)` doesn't change `date`. Calling it without using the result does nothing, the same trap as `String.toUpperCase()`.

### 2. Time zones and daylight saving time

A `ZoneId` such as `Asia/Seoul` or `America/New_York` is a region with rules: its offset from UTC, and the dates when that offset changes for *daylight saving time* (DST). A `ZoneOffset` such as `+09:00` is a fixed number and knows nothing about DST. Use zone IDs.

Seoul has no DST, but New York does, and DST creates two kinds of strange local times:

- A *gap*: on 2026-03-08, clocks in New York jump from 02:00 to 03:00, so 02:30 doesn't exist that day.
- An *overlap*: on 2026-11-01, clocks go back from 02:00 to 01:00, so 01:30 happens twice: first at UTC-4 (EDT), then at UTC-5 (EST).

`java.time` resolves both without an error:

```java
var newYork = ZoneId.of("America/New_York");
LocalDateTime.parse("2026-03-08T02:30").atZone(newYork); // 2026-03-08T03:30-04:00: moved forward by the gap
LocalDateTime.parse("2026-11-01T01:30").atZone(newYork); // 2026-11-01T01:30-04:00: the earlier of the two
```

Two methods move a `ZonedDateTime` to another zone, and mixing them up is a classic bug:

- `withZoneSameInstant(zone)` keeps the moment and changes the wall-clock time. 09:00 in Seoul becomes 20:00 the evening before in New York (19:00 in winter). This is what "convert a meeting time" means.
- `withZoneSameLocal(zone)` keeps the wall-clock time and changes the moment. 09:00 in Seoul becomes 09:00 in New York, which is 13 or 14 hours later.

Don't rely on the computer's default zone (`ZoneId.systemDefault()`). It differs between your laptop, the CI server and a container. Name the zone you mean.

### 3. Duration and Period

| | `Duration` | `Period` |
|---|---|---|
| Measures | time: seconds and nanoseconds | the calendar: years, months and days |
| Example | `Duration.ofMinutes(90)` is `PT1H30M` | `Period.ofMonths(1)` is `P1M` |
| Between two values | `Duration.between(instant1, instant2)` | `Period.between(date1, date2)` |
| One day | exactly 24 hours | one calendar day: 23, 24 or 25 hours |

The difference shows when a DST change falls in between:

```java
var start = ZonedDateTime.of(2026, 3, 7, 12, 0, 0, 0, newYork);
start.plus(Period.ofDays(1));    // 2026-03-08T12:00-04:00: the same wall-clock time, 23 hours later
start.plus(Duration.ofDays(1));  // 2026-03-08T13:00-04:00: exactly 24 hours later
```

`Period.between(birthDate, today).getYears()` is an age in full years, and `ChronoUnit.DAYS.between(a, b)` counts days. On a `Duration`, `toMinutes()` returns the whole duration in minutes, while `toMinutesPart()` returns only the minutes "on the clock", from 0 to 59.

### 4. Formatting and parsing

Every `java.time` type prints and parses ISO 8601 text by default: `LocalDate.parse("2026-03-08")`, `instant.toString()`. For other formats, use a `DateTimeFormatter`:

```java
var invitation = DateTimeFormatter.ofPattern("EEE uuuu-MM-dd HH:mm '('VV')'", Locale.ENGLISH);
invitation.format(ZonedDateTime.of(2026, 3, 8, 16, 30, 0, 0, seoul)); // "Sun 2026-03-08 16:30 (Asia/Seoul)"
```

A `DateTimeFormatter` is immutable and thread-safe, so keep one in a `static final` field. (The old `SimpleDateFormat` isn't thread-safe: one shared instance corrupts dates under load.)

Pattern letters are case-sensitive, and several have a look-alike that compiles, runs, and prints something else:

| Letter | Means | Look-alike | Means |
|---|---|---|---|
| `uuuu` (or `yyyy`) | the year | `YYYY` | the *week-based* year: with English or Korean week rules, 2026-12-28 prints as 2027 |
| `MM` | the month | `mm` | the minutes |
| `dd` | the day of the month | `DD` | the day of the year |
| `HH` | the hour, 0–23 | `hh` | the hour, 1–12 (it needs `a` for AM/PM) |

Day and month names depend on the locale. Without a `Locale` argument, the formatter uses the computer's default, so a server set to Korean prints `일` instead of `Sun`. Pass a locale whenever a pattern contains text.

### 5. Store instants, convert at the edges, inject a clock

Two rules keep most time bugs out of a backend:

1. **Inside the system, a moment is an `Instant`**, and the database stores it in UTC. Convert to a zone only at the edges: when you show a time to a person, when a person enters one, and when a business rule is defined in a zone ("valid until the end of the day *in Korea*").
2. **Don't call `now()` without a clock.** `Instant.now()` and `LocalDate.now()` read the computer's clock, which a test can't control. Inject a `java.time.Clock` instead:

```java
public final class CouponValidator {

    private final Clock clock;

    public CouponValidator(Clock clock) {
        this.clock = clock;
    }

    public CouponStatus check(Coupon coupon) {
        LocalDate today = LocalDate.ofInstant(clock.instant(), SEOUL);
        // ...
    }
}

new CouponValidator(Clock.systemUTC());          // in production (in Spring: a Clock @Bean)
new CouponValidator(Clock.fixed(Instant.parse("2025-03-01T00:30:00Z"), ZoneOffset.UTC)); // in a test
```

`Clock.fixed` stops time at the moment you choose, so a test gives the same result today and in ten years, and it can check midnight, month ends and DST changes on purpose.

One more trap: `LocalDate.now(clock)` uses the clock's own zone. On a server that's usually UTC, which is 9 hours behind Seoul, so from 00:00 to 09:00 Seoul time the UTC date is still yesterday. When a rule is about Korean dates, ask for the Korean date: `LocalDate.ofInstant(clock.instant(), ZoneId.of("Asia/Seoul"))`.

### 6. BigDecimal for money

`double` stores numbers in binary and can't hold most decimal fractions exactly: `0.1 + 0.2` is `0.30000000000000004`, in Java as in TypeScript. For money, use `BigDecimal`:

```java
new BigDecimal("0.1").add(new BigDecimal("0.2"));  // 0.3, exactly
new BigDecimal(0.1);       // 0.1000000000000000055511151231257827021181583404541015625: never use this constructor
BigDecimal.valueOf(0.1);   // 0.1 (it goes through Double.toString): acceptable for a double you already have
```

A `BigDecimal` is a whole number plus a *scale*: the number of digits after the decimal point. `12.50` is 1250 with scale 2. Its *precision* is the number of significant digits: 4.

- Arithmetic uses methods, and each returns a new object: `a.add(b)`, `a.subtract(b)`, `a.multiply(b)`.
- Division can produce digits that never end: `BigDecimal.TEN.divide(new BigDecimal("3"))` throws `ArithmeticException`. Pass a scale and a rounding mode: `a.divide(b, 2, RoundingMode.HALF_EVEN)`.
- Rounding is always explicit: `amount.setScale(2, RoundingMode.HALF_EVEN)`.

| `RoundingMode` | 2.5 | 3.5 | -2.5 | Typical use |
|---|---|---|---|---|
| `HALF_UP` | 3 | 4 | -3 | the rounding taught at school |
| `HALF_EVEN` | 2 | 4 | -2 | "banker's rounding": a tie goes to the even neighbor, so rounding errors cancel out over many values |
| `DOWN` | 2 | 3 | -2 | cutting off, for example amounts under 1 won |
| `UNNECESSARY` | throws | throws | throws | "this value must not need rounding" |

Which mode applies is a business rule, not a technical detail. Ask, and then write it in the code.

**`equals` and `compareTo`.** `BigDecimal.equals` compares the value *and* the scale: `new BigDecimal("2.0").equals(new BigDecimal("2.00"))` is `false`, and both can sit in the same `HashSet`. `compareTo` compares only the value and returns 0 here. Compare amounts with `compareTo` (in AssertJ: `isEqualByComparingTo`), or give all values the same scale first, as `Money` does in J07-03.

**Currencies.** `java.util.Currency` knows how many decimals each ISO 4217 currency uses:

```java
Currency.getInstance("USD").getDefaultFractionDigits(); // 2 (cents)
Currency.getInstance("KRW").getDefaultFractionDigits(); // 0
Currency.getInstance("JPY").getDefaultFractionDigits(); // 0
```

Keep an amount together with its currency, so that no code can add won to dollars. In the database, store amounts in `DECIMAL` columns with a fixed scale, never in `FLOAT` or `DOUBLE`.

### 7. Files and paths (NIO.2)

The package `java.nio.file` ("NIO.2", Java 7 and later) is the modern file API. A `Path` is a location in the file system, as a value; the static methods of `Files` do the work.

```java
Path dir = Path.of("data");               // relative to the working directory
Path file = dir.resolve("books.csv");     // data/books.csv
file.getFileName();                       // books.csv
file.getParent();                         // data (null for a single name such as Path.of("books.csv"))
Files.exists(file);
Files.createDirectories(dir);             // with all parents; no error if it already exists
```

Reading text, from the simplest to the most scalable:

| Method | Returns | Good for |
|---|---|---|
| `Files.readString(path)` | the whole file as one `String` | small files |
| `Files.readAllLines(path)` | a `List<String>` with every line | small files that you process line by line |
| `Files.lines(path)` | a lazy `Stream<String>` that **must be closed** | large files, with stream operations |
| `Files.newBufferedReader(path)` | a `BufferedReader` that **must be closed** | large files, in a loop with `readLine()` and a line counter |

```java
try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
    int lineNumber = 0;
    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
        lineNumber++;
        // ...
    }
}

try (Stream<String> lines = Files.lines(path)) {
    long rows = lines.filter(line -> !line.isBlank()).count();
}
```

Writing:

```java
Files.writeString(path, text);   // creates the file, or replaces its content
Files.writeString(path, text, StandardOpenOption.CREATE, StandardOpenOption.APPEND); // adds to the end
Files.write(path, lines);        // one line per element, each followed by the system's line separator
```

The `Files` methods read and write UTF-8 unless you pass another charset, and since Java 18 the rest of the JDK does, too ([JEP 400](https://openjdk.org/jeps/400)). Pass `StandardCharsets.UTF_8` anyway when a file format requires it: then the code says so. When bytes aren't valid UTF-8, `newBufferedReader`, `readString` and `readAllLines` throw `MalformedInputException` instead of quietly producing broken text.

File errors are `IOException`s with specific subclasses: `NoSuchFileException`, `AccessDeniedException`, `FileAlreadyExistsException`. A stream can't throw a checked exception from inside, so `Files.lines` reports read errors during the stream as `UncheckedIOException`.

### 8. CSV pitfalls

CSV looks simple: values separated by commas. Real files aren't that simple. [RFC 4180](https://www.rfc-editor.org/rfc/rfc4180) describes the common format, and these are the traps:

- **Commas inside values.** A value with a comma is quoted: `"Effective Java, 3rd Edition"`. `line.split(",")` cuts it in two.
- **Quotes inside values.** Inside a quoted value, a quote is written twice: `"The ""Pragmatic"" Programmer"`. (J07-04 leaves this out, to keep the exercise small.)
- **Line breaks inside values.** A quoted value may even contain a line break, so one record can cover several lines. (J07-04 doesn't allow this, to keep the exercise small.)
- **Empty fields at the end.** `"a,b,,".split(",")` returns `[a, b]`: `split` drops empty strings at the end. Pass a negative limit to keep them: `split(",", -1)`.
- **The byte order mark.** Excel's "CSV UTF-8" format starts the file with the invisible character U+FEFF. The first header is then read as U+FEFF followed by `isbn`, which isn't equal to `"isbn"`.
- **Encodings.** Korean versions of Excel save a plain "CSV" file in CP949 (also called MS949), not in UTF-8. Read as UTF-8, it fails with `MalformedInputException`, or a lenient reader turns the Korean text into `�` characters.
- **Line endings.** Windows ends lines with `\r\n`. `readLine()` handles both kinds, but `text.split("\n")` leaves a `\r` at the end of every line, and then `new BigDecimal("45.99\r")` fails.
- **Numbers formatted for people.** `"15,000"` or the German `12,50` isn't what `new BigDecimal` reads.
- **Formula injection.** When you *write* CSV that people open in Excel, a value that starts with `=`, `+`, `-` or `@` can run as a formula. Escape such values, for example with a leading `'`.

In production, use a CSV library such as Apache Commons CSV or Jackson's CSV module. They handle these cases and more. You still need to know the traps: to choose the library's settings, and to write the tests.

## TypeScript → Java

| TypeScript | Java | Watch out for |
|---|---|---|
| `new Date()` | `Instant.now(clock)` | A JS `Date` is a moment with local-time getters; `java.time` has a separate type for each idea. |
| `Date.now()` | `clock.millis()` | |
| `date.toISOString()` | `instant.toString()`, for example `2026-03-08T07:30:00Z` | |
| `date.getMonth()`, 0–11 | `date.getMonthValue()`, 1–12, or `getMonth()`, `Month.MARCH` | Months start at 1. |
| `date.getDay()`, 0 (Sunday) to 6 | `date.getDayOfWeek()`, `MONDAY` to `SUNDAY`; `getValue()` is 1–7 | Sunday is 7, not 0. |
| `Intl.DateTimeFormat`, `format` from date-fns | `DateTimeFormatter.ofPattern("…", Locale.ENGLISH)` | Different pattern letters; beware of `YYYY`, `hh` and `mm`. |
| `Temporal.Instant`, `Temporal.PlainDate`, `Temporal.ZonedDateTime` (JavaScript's new Temporal API) | `Instant`, `LocalDate`, `ZonedDateTime` | The same model, with almost the same names. |
| `vi.useFakeTimers()` and `vi.setSystemTime(…)` | `Clock.fixed(…)`, passed to the constructor | No global patching: the code receives its clock. |
| `number` for money | `BigDecimal`, together with a `Currency` | Never `double`. |
| `(0.1 + 0.2).toFixed(2)` | `a.add(b).setScale(2, RoundingMode.HALF_EVEN)` | `equals` compares the scale; use `compareTo`. |
| `fs.readFileSync(p, "utf8")` | `Files.readString(path)` | It throws the checked `IOException`. |
| `readline.createInterface({ input: fs.createReadStream(p) })` | `Files.newBufferedReader(path)` and `readLine()` | Close it with try-with-resources. |
| `fs.writeFileSync(p, text)` | `Files.writeString(path, text)` | It replaces the file unless you pass `APPEND`. |
| `path.join(dir, "books.csv")` | `dir.resolve("books.csv")` | A `Path` is a value, not a string. |
| `text.split(",")` | `text.split(",", -1)` | Java's `split` takes a regular expression and drops empty strings at the end. |

## In real projects

- Inside the system and in the database, a moment is an `Instant` in UTC. Code converts it to a zone only to show it, or to apply a rule that is defined in a zone. See [Dates and money](../../docs/handbook/dates-and-money.md).
- The business time zone (`Asia/Seoul`) is a named constant or a configuration value, never `ZoneId.systemDefault()`: servers and containers usually run in UTC. See [Dates and money](../../docs/handbook/dates-and-money.md).
- Code that needs "now" receives a `Clock` (a `Clock.systemUTC()` bean in Spring), and tests pass `Clock.fixed(...)`. See [Testing](../../docs/handbook/testing.md).
- Money is a `BigDecimal` together with its currency, stored in `DECIMAL` columns, and the code states its rounding mode. See [Dates and money](../../docs/handbook/dates-and-money.md).
- Value objects such as `Money` are records that validate and normalize their values in the constructor. See [Immutability](../../docs/handbook/immutability.md).
- File code uses `Path` and `Files`, names the charset, and closes every reader, writer and stream with try-with-resources. `IOException` is handled or translated at the boundary. See [Exceptions](../../docs/handbook/exceptions.md).
- CSV in production goes through a library (Apache Commons CSV, Jackson CSV), and the tests include the nasty cases from section 8. See [Testing](../../docs/handbook/testing.md).

## Exercises

### J07-01 · Dates and times

**Build** · 30 min · [`BusinessDays.java`](src/main/java/practice/j07/ex01/BusinessDays.java), [`Ages.java`](src/main/java/practice/j07/ex01/Ages.java), [`MeetingTimes.java`](src/main/java/practice/j07/ex01/MeetingTimes.java), [`Durations.java`](src/main/java/practice/j07/ex01/Durations.java)

Four small utilities that most shipping, booking or HR systems need in some form: due dates in business days, ages, meeting times across time zones, and readable durations. Each takes a few lines once you know the right `java.time` method, and each has a trap: weekends, 29 February, daylight saving time, and the difference between `toHours()` and `toHoursPart()`.

**You'll practice:** `LocalDate` arithmetic and `DayOfWeek`, `Period.between`, `atZone` and `withZoneSameInstant`, `DateTimeFormatter` with a `Locale`, the `toXxxPart()` methods of `Duration`.

**Rules:** in the Javadoc of each class. The tests use fixed dates, so they give the same result on every day you run them.

<details>
<summary>Hint 1</summary>

`BusinessDays.add` is a loop that moves one day per step. `Integer.signum(days)` gives the direction (+1 or -1), and the loop stops when it has passed enough business days. Move *before* you check a date, so the start date never counts.
</details>

<details>
<summary>Hint 2</summary>

Once the arguments are checked, `Ages.ageOn` and `MeetingTimes.convert` are one line each: look at `Period.between`, `atZone` and `withZoneSameInstant`. (In Korea, this "full years" age is 만 나이, the standard in law since June 2023.)
</details>

<details>
<summary>Hint 3</summary>

`toDaysPart()`, `toHoursPart()`, `toMinutesPart()` and `toSecondsPart()` split a duration the way a clock does. A `new StringJoiner(" ").setEmptyValue("0s")` takes care of the spaces and of the zero case.
</details>

**Done?** Compare with the reference on the `solutions` branch, then check: Which line of your code decides what happens at 02:30 on 2026-03-08 in New York? Does your `format` print the same text on a computer set to Korean?

### J07-02 · Write tests: time-dependent code

**Write tests** · 30 min · [`CouponValidator.java`](src/main/java/practice/j07/ex02/CouponValidator.java) → tests in [`CouponValidatorTest.java`](src/test/java/practice/j07/ex02/CouponValidatorTest.java)

Code that asks "what time is it?" is hard to test, unless the time comes from an injected `Clock`. `CouponValidator` is finished and correct. Your tests pin down its rules: inclusive dates, Korean dates on a server that runs in UTC, and coupons that work only on weekends.

**How you're scored:** as in J03-03. `./score j07-02` runs your tests against the real `CouponValidator`, where every test must pass, and then against six broken versions.

**You'll practice:** `Clock.fixed`, turning a Seoul wall-clock time into an `Instant`, testing both sides of a boundary, the date in Seoul versus the date in UTC.

<details>
<summary>Hint 1</summary>

Write a helper that returns a validator whose clock is stopped at a given time in Seoul: `LocalDateTime.parse("2025-03-01T00:00").atZone(SEOUL).toInstant()` gives the instant. Give the clock itself the zone `ZoneOffset.UTC`, like a real server.
</details>

<details>
<summary>Hint 2</summary>

For each boundary, test the last moment on one side and the first moment on the other. Remember that 08:59 in Seoul is still the previous day in UTC, for the date and for the day of the week.
</details>

<details>
<summary>Hint 3</summary>

Choose coupon dates far away from today's real date. Then a validator that secretly reads the computer's clock can't pass your tests by luck.
</details>

**Done?** Compare with the reference tests on the `solutions` branch. Will your tests still pass in ten years? Would they pass on a laptop set to New York time?

### J07-03 · Money with BigDecimal

**Build** · 30 min · [`Money.java`](src/main/java/practice/j07/ex03/Money.java)

Prices, totals, refunds and split bills need exact decimal arithmetic and clear rounding rules. You build `Money`, a small immutable value: an amount with its currency, always with the right number of decimals, with safe arithmetic, `HALF_EVEN` rounding, and a way to split it into parts that never loses a cent.

**You'll practice:** the scale of a `BigDecimal` and `setScale`, `RoundingMode`, `Currency.getDefaultFractionDigits()`, a record whose compact constructor validates and normalizes, `equals` and the scale.

**Rules:** in the Javadoc of `Money`.

<details>
<summary>Hint 1</summary>

Do the checks in the compact constructor, then assign the normalized value to the parameter: `amount = amount.setScale(...)`. Java assigns the field from the parameter afterwards. Once every `Money` has its currency's scale, the `equals` and `hashCode` that the record generates are already right.
</details>

<details>
<summary>Hint 2</summary>

`amount.stripTrailingZeros().scale()` is the number of decimals that carry a value: 1 for 12.500. And for `allocate`, count in the smallest unit: `amount.unscaledValue()` is 1000 for 10.00 USD, and `BigInteger.divideAndRemainder` gives the base share and the leftover units.
</details>

<details>
<summary>Hint 3</summary>

Implement `toString()` early: AssertJ uses it to print values in its failure messages.
</details>

**Done?** Compare with the reference on the `solutions` branch, then check: Where could a cent get lost in your code, and which test would notice? Why does `Money` have no factory method that takes a `double`?

### J07-04 · Files and CSV

**Build** · 30 min · [`BookCsvImporter.java`](src/main/java/practice/j07/ex04/BookCsvImporter.java), [`ImportReport.java`](src/main/java/practice/j07/ex04/ImportReport.java)

Admin tools often import data from spreadsheets. You build a small CSV importer for books that handles quoted fields, reports bad rows by line number instead of rejecting the whole file, copes with the files Excel writes, and then writes a summary report.

**You'll practice:** `Files.newBufferedReader` with try-with-resources, reading line by line with a line counter, a small parser for quoted fields, `new BigDecimal(String)` as validation, collecting errors instead of stopping at the first one, `Files.writeString`.

**Rules:** in the Javadoc of `BookCsvImporter` and `ImportReport`: the header, blank lines, quoting, the exact error messages and the report format. `Book`, `ImportError` and `ImportResult` are given. The tests write their files to a `@TempDir`.

<details>
<summary>Hint 1</summary>

Read the header first with `readLine()`. It returns `null` for an empty file. If the line starts with a byte order mark (`header.charAt(0) == 0xFEFF`, for a line that isn't empty), remove it, and compare the rest with `HEADER`. Then loop over the remaining lines, and count every one of them.
</details>

<details>
<summary>Hint 2</summary>

Split a row with a loop over its characters and a `boolean quoted`. A quote opens a quoted field only as the first character of a field, and the next quote closes it. Don't use `split(",")`.
</details>

<details>
<summary>Hint 3</summary>

A private method that throws for a bad row (with a small exception class of your own) keeps the main loop short: `try { books.add(parseBook(line)); } catch (InvalidRowException e) { errors.add(...); }`.
</details>

**Done?** Compare with the reference on the `solutions` branch, then check: What does your importer do with a 2 GB file? Which rule would have to change to allow line breaks inside quoted fields, and which part of your code?

## Review questions

1. Which type would you use for: the moment a payment was made, a customer's birthday, a shop's opening time, and the time a user typed into a form?
2. What's the difference between `Duration.ofDays(1)` and `Period.ofDays(1)`? When do they give different results?
3. What does `LocalDateTime.parse("2026-03-08T02:30").atZone(ZoneId.of("America/New_York"))` return, and why?
4. Why should a class receive a `Clock` instead of calling `Instant.now()`? And why is `LocalDate.now(clock)` still risky on a server?
5. What do `new BigDecimal(0.1)`, `new BigDecimal("0.1")` and `BigDecimal.valueOf(0.1)` return?
6. Is `new BigDecimal("2.0").equals(new BigDecimal("2.00"))` true? And `compareTo`? What does that mean for a `HashSet<BigDecimal>`?
7. Round 2.5 and 3.5 to whole numbers, once with `HALF_UP` and once with `HALF_EVEN`. Why do banks like `HALF_EVEN`?
8. When would you use `Files.lines` instead of `Files.readAllLines`, and what must you not forget?
9. Name three ways in which a CSV file from a real user breaks `line.split(",")`.
10. What does `DateTimeFormatter.ofPattern("YYYY-MM-dd", Locale.ENGLISH)` print for 2026-12-28, and why?

<details>
<summary>Answers</summary>

1. An `Instant`; a `LocalDate`; a `LocalTime`; a `LocalDateTime`, until you attach the user's zone with `atZone` (then store the resulting `Instant`).
2. `Duration.ofDays(1)` is exactly 24 hours. `Period.ofDays(1)` is one calendar day: the same wall-clock time on the next date. They differ across a DST change: in New York, one calendar day after noon on 2026-03-07 is only 23 hours later.
3. `2026-03-08T03:30-04:00[America/New_York]`. That day, the clocks jump from 02:00 to 03:00, so 02:30 doesn't exist, and `java.time` moves a time in a gap forward by the length of the gap.
4. With a `Clock`, a test decides what "now" is (`Clock.fixed`), so it's repeatable and can check midnight, month ends and DST changes on purpose. `LocalDate.now(clock)` uses the clock's own zone, which is UTC on most servers: for Korean business rules, it returns yesterday's date from 00:00 to 09:00 Seoul time. Use `LocalDate.ofInstant(clock.instant(), SEOUL)`.
5. `0.1000000000000000055511151231257827021181583404541015625` (the exact value of the `double` nearest to 0.1), `0.1`, and `0.1` (through `Double.toString`).
6. `false`: `equals` compares the scale, too. `compareTo` returns 0. A `HashSet` can contain both values, while a `TreeSet`, which uses `compareTo`, keeps only one.
7. `HALF_UP` gives 3 and 4; `HALF_EVEN` gives 2 and 4. `HALF_UP` always rounds a tie up, so over many roundings totals drift upward. `HALF_EVEN` rounds ties up half of the time and down the other half, so the errors cancel out.
8. For large files: `Files.lines` reads lazily, while `readAllLines` keeps every line in memory. The stream keeps the file open, so close it with try-with-resources. Read errors inside the stream arrive as `UncheckedIOException`.
9. Any three of these: a quoted value with a comma; a quote written as `""`; empty fields at the end, which `split` drops; a byte order mark before the first header; `\r\n` line endings, which leave a `\r` in the last field; a quoted value that continues on the next line; a number with a thousands separator.
10. `2027-12-28`. `YYYY` is the week-based year. With the English (and the Korean) week rules, a week starts on Sunday, and the week that contains 1 January 2027 starts on Sunday 27 December 2026. With other locales the same bug appears on other days around New Year. Use `uuuu` or `yyyy`.
</details>

## Further reading

- Dev.java: [The Date Time API](https://dev.java/learn/api/date-time-regex/date-time/)
- Java API: [`java.time`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/time/package-summary.html), [`BigDecimal`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/math/BigDecimal.html), [`Files`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/Files.html)
- Martin Fowler, [Money](https://martinfowler.com/eaaCatalog/money.html): the pattern behind J07-03, including allocation
- [RFC 4180](https://www.rfc-editor.org/rfc/rfc4180), the common CSV format, and [OWASP: CSV Injection](https://owasp.org/www-community/attacks/CSV_Injection)
- [Apache Commons CSV](https://commons.apache.org/proper/commons-csv/), a CSV library for production code
- [Temporal documentation](https://tc39.es/proposal-temporal/docs/), to compare JavaScript's new date API with `java.time`

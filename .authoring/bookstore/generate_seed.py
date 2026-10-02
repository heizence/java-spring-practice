#!/usr/bin/env python3
"""Generates the bookstore seed data (V2__seed_data.sql) deterministically.

Run from the repository root:  python3 .authoring/bookstore/generate_seed.py
The output is the same on every run (fixed random seed), so tests and documentation can rely on it.
Facts that tests and spring/BOOKSTORE.md rely on are asserted at the bottom.
"""
import random
from datetime import date, datetime, timedelta
from pathlib import Path

OUT = Path(".authoring/bookstore-template/src/main/resources/db/migration/V2__seed_data.sql")
rng = random.Random(2026)

# Every seeded member's password is "bookstore123" (BCrypt, cost 10), stored with Spring Security's {bcrypt} prefix.
PASSWORD_HASH = "{bcrypt}$2a$10$2N2zzv6wssRHPggJQ8/eCeiSYdCO0pfG8B19zUkKRtfcyxACFcwti"
CATALOG_CREATED = datetime(2026, 7, 1, 0, 0, 0)

CATEGORIES = ["Programming", "Databases", "Software Architecture", "Fiction", "Science", "Self-Help"]

# (title, author, category index, original publication year)
BOOKS = [
    ("Effective Java", "Joshua Bloch", 0, 2018),
    ("Clean Code", "Robert C. Martin", 0, 2008),
    ("Refactoring", "Martin Fowler", 0, 2018),
    ("The Pragmatic Programmer", "David Thomas", 0, 2019),
    ("Java Concurrency in Practice", "Brian Goetz", 0, 2006),
    ("Modern Java in Action", "Raoul-Gabriel Urma", 0, 2018),
    ("Head First Design Patterns", "Eric Freeman", 0, 2020),
    ("Spring in Action", "Craig Walls", 0, 2022),
    ("Test-Driven Development by Example", "Kent Beck", 0, 2002),
    ("Working Effectively with Legacy Code", "Michael Feathers", 0, 2004),
    ("Code Complete", "Steve McConnell", 0, 2004),
    ("The Art of Computer Programming", "Donald Knuth", 0, 2011),
    ("Designing Data-Intensive Applications", "Martin Kleppmann", 1, 2017),
    ("Database Internals", "Alex Petrov", 1, 2019),
    ("High Performance MySQL", "Silvia Botros", 1, 2021),
    ("SQL Antipatterns", "Bill Karwin", 1, 2022),
    ("Database System Concepts", "Abraham Silberschatz", 1, 2019),
    ("Seven Databases in Seven Weeks", "Luc Perkins", 1, 2018),
    ("Learning SQL", "Alan Beaulieu", 1, 2020),
    ("SQL Performance Explained", "Markus Winand", 1, 2012),
    ("Transaction Processing", "Jim Gray", 1, 1992),
    ("MySQL Crash Course", "Rick Silva", 1, 2023),
    ("Domain-Driven Design", "Eric Evans", 2, 2003),
    ("Patterns of Enterprise Application Architecture", "Martin Fowler", 2, 2002),
    ("Building Microservices", "Sam Newman", 2, 2021),
    ("Release It!", "Michael T. Nygard", 2, 2018),
    ("Clean Architecture", "Robert C. Martin", 2, 2017),
    ("Fundamentals of Software Architecture", "Mark Richards", 2, 2020),
    ("Implementing Domain-Driven Design", "Vaughn Vernon", 2, 2013),
    ("Enterprise Integration Patterns", "Gregor Hohpe", 2, 2003),
    ("Site Reliability Engineering", "Betsy Beyer", 2, 2016),
    ("Accelerate", "Nicole Forsgren", 2, 2018),
    ("Pride and Prejudice", "Jane Austen", 3, 1813),
    ("Moby-Dick", "Herman Melville", 3, 1851),
    ("Frankenstein", "Mary Shelley", 3, 1818),
    ("The Great Gatsby", "F. Scott Fitzgerald", 3, 1925),
    ("Jane Eyre", "Charlotte Brontë", 3, 1847),
    ("Crime and Punishment", "Fyodor Dostoevsky", 3, 1866),
    ("The Picture of Dorian Gray", "Oscar Wilde", 3, 1890),
    ("The Time Machine", "H. G. Wells", 3, 1895),
    ("진달래꽃", "김소월", 3, 1925),
    ("운수 좋은 날", "현진건", 3, 1924),
    ("A Brief History of Time", "Stephen Hawking", 4, 1988),
    ("The Selfish Gene", "Richard Dawkins", 4, 1976),
    ("Cosmos", "Carl Sagan", 4, 1980),
    ("The Gene", "Siddhartha Mukherjee", 4, 2016),
    ("Sapiens", "Yuval Noah Harari", 4, 2014),
    ("On the Origin of Species", "Charles Darwin", 4, 1859),
    ("Surely You're Joking, Mr. Feynman!", "Richard P. Feynman", 4, 1985),
    ("The Elegant Universe", "Brian Greene", 4, 1999),
    ("Silent Spring", "Rachel Carson", 4, 1962),
    ("Atomic Habits", "James Clear", 5, 2018),
    ("Deep Work", "Cal Newport", 5, 2016),
    ("Thinking, Fast and Slow", "Daniel Kahneman", 5, 2011),
    ("Mindset", "Carol S. Dweck", 5, 2006),
    ("The 7 Habits of Highly Effective People", "Stephen R. Covey", 5, 1989),
    ("Getting Things Done", "David Allen", 5, 2015),
    ("Grit", "Angela Duckworth", 5, 2016),
    ("Range", "David Epstein", 5, 2019),
    ("Ultralearning", "Scott H. Young", 5, 2019),
]

MEMBERS = [
    ("Minji Kim", "minji"), ("Jun Park", "jun"), ("Seoyeon Lee", "seoyeon"), ("Hyunwoo Choi", "hyunwoo"),
    ("Jiwon Jung", "jiwon"), ("Doyun Kang", "doyun"), ("Haeun Cho", "haeun"), ("Siwoo Yoon", "siwoo"),
    ("Yuna Jang", "yuna"), ("Joon Lim", "joon"), ("Subin Han", "subin"), ("Taeyang Oh", "taeyang"),
    ("Chaewon Seo", "chaewon"), ("Minjun Shin", "minjun"), ("Dahye Kwon", "dahye"), ("Yejin Hwang", "yejin"),
    ("Sungmin Ahn", "sungmin"), ("Eunji Song", "eunji"), ("Hoon Yoo", "hoon"), ("Admin", "admin"),
]
ADMIN_ID = 20
CUSTOMER_IDS = list(range(1, 20))

REVIEW_TEXTS = [
    "Clear explanations and practical examples.",
    "I keep coming back to this one.",
    "Good, but the middle chapters drag a little.",
    "Changed how I write code at work.",
    "Too theoretical for my taste.",
    "A must-read for every backend developer.",
    "Well written and easy to follow.",
    "Some parts are outdated, but the ideas hold up.",
    "Dense, but worth the effort.",
    "Not what I expected, still enjoyed it.",
    "Excellent reference to keep on the desk.",
    "The exercises at the end of each chapter are great.",
]


def ts(value: datetime) -> str:
    return "'" + value.strftime("%Y-%m-%d %H:%M:%S.%f") + "'"


def sql_string(value: str) -> str:
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def isbn13(prefix: str) -> str:
    digits = prefix + "".join(str(rng.randrange(10)) for _ in range(12 - len(prefix)))
    total = sum(int(d) * (1 if i % 2 == 0 else 3) for i, d in enumerate(digits))
    return digits + str((10 - total % 10) % 10)


def insert(table: str, columns: list[str], rows: list[list[str]], batch: int = 50) -> str:
    parts = []
    for start in range(0, len(rows), batch):
        chunk = rows[start:start + batch]
        values = ",\n".join("    (" + ", ".join(row) + ")" for row in chunk)
        parts.append(f"INSERT INTO {table} ({', '.join(columns)}) VALUES\n{values};\n")
    return "\n".join(parts)


# ---------------------------------------------------------------- categories, books, inventories
categories = [[str(i + 1), sql_string(name)] for i, name in enumerate(CATEGORIES)]

books, prices, inventories = [], {}, []
used_isbns = set()
for book_id, (title, author, category, year) in enumerate(BOOKS, start=1):
    isbn = isbn13("97911")
    while isbn in used_isbns:
        isbn = isbn13("97911")
    used_isbns.add(isbn)
    price = 36000 if book_id == 1 else rng.randrange(12, 59) * 1000
    prices[book_id] = price
    published = date(year, rng.randrange(1, 13), rng.randrange(1, 29))
    books.append([str(book_id), sql_string(isbn), sql_string(title), sql_string(author), str(category + 1),
                  str(price), "'" + published.isoformat() + "'", ts(CATALOG_CREATED), ts(CATALOG_CREATED)])
    if book_id == 1:
        quantity = 10
    elif book_id in (59, 60):
        quantity = 0  # out of stock
    else:
        quantity = rng.randrange(1, 31)
    inventories.append([str(book_id), str(quantity), "0"])

# ---------------------------------------------------------------- members and wallets
members, wallets, wallet_transactions = [], [], []
for member_id, (name, handle) in enumerate(MEMBERS, start=1):
    role = "ADMIN" if member_id == ADMIN_ID else "USER"
    joined = CATALOG_CREATED + timedelta(days=member_id)
    members.append([str(member_id), sql_string(f"{handle}@example.com"), sql_string(name),
                    sql_string(PASSWORD_HASH), sql_string(role), ts(joined), ts(joined)])
    if member_id == 1:
        balance = 100_000
    elif member_id == ADMIN_ID:
        balance = 0
    else:
        balance = rng.randrange(20, 201) * 1000
    wallets.append([str(member_id), str(member_id), str(balance), "0", ts(joined), ts(joined)])
    if balance > 0:
        wallet_transactions.append([str(len(wallet_transactions) + 1), str(member_id), "'CHARGE'", str(balance),
                                    ts(joined + timedelta(hours=1))])

# ---------------------------------------------------------------- coupons and issues
coupons = [
    # id, code, discount, total, issued, from, until
    (1, "WELCOME5000", 5000, 1000, datetime(2026, 1, 1), datetime(2026, 12, 31, 23, 59, 59)),
    (2, "FLASH10000", 10000, 10, datetime(2026, 10, 1), datetime(2026, 10, 31, 23, 59, 59)),
    (3, "AUTUMN3000", 3000, 500, datetime(2026, 9, 1), datetime(2026, 11, 30, 23, 59, 59)),
    (4, "EXPIRED2000", 2000, 100, datetime(2026, 1, 1), datetime(2026, 3, 31, 23, 59, 59)),
]
issued_members = {1: range(1, 16), 2: range(0), 3: range(1, 11), 4: range(2, 7)}
coupon_rows, coupon_issue_rows = [], []
for coupon_id, code, discount, total, valid_from, valid_until in coupons:
    issued = list(issued_members[coupon_id])
    coupon_rows.append([str(coupon_id), sql_string(code), str(discount), str(total), str(len(issued)),
                        ts(valid_from), ts(valid_until), "0"])
    for member_id in issued:
        issued_at = max(valid_from, CATALOG_CREATED) + timedelta(days=member_id, hours=3)
        used_at = ts(issued_at + timedelta(days=2)) if (coupon_id, member_id) in {(1, 2), (1, 3), (3, 1)} else "NULL"
        coupon_issue_rows.append([str(len(coupon_issue_rows) + 1), str(coupon_id), str(member_id),
                                  ts(issued_at), used_at])

# ---------------------------------------------------------------- orders, lines, payments
order_specs = []
for _ in range(200):
    member_id = rng.choice(CUSTOMER_IDS)
    ordered_at = datetime(2026, 8, 1) + timedelta(minutes=rng.randrange(0, 61 * 24 * 60))
    order_specs.append((ordered_at, member_id))
# Minji (member 1) always has at least five orders.
for i in range(5):
    order_specs[i] = (order_specs[i][0], 1)
order_specs.sort()

statuses = ["PAID"] * 10 + ["SHIPPED"] * 5 + ["PENDING"] * 3 + ["CANCELLED"] * 2
orders, order_lines, payments = [], [], []
in_stock_books = [book_id for book_id in prices if book_id not in (59, 60)]
for order_id, (ordered_at, member_id) in enumerate(order_specs, start=1):
    status = rng.choice(statuses)
    chosen = rng.sample(in_stock_books, rng.choice([1, 1, 2, 2, 3]))
    total = 0
    for book_id in chosen:
        quantity = rng.choice([1, 1, 1, 2, 3])
        total += quantity * prices[book_id]
        order_lines.append([str(len(order_lines) + 1), str(order_id), str(book_id), str(quantity),
                            str(prices[book_id])])
    updated = ordered_at + (timedelta(days=2) if status == "SHIPPED" else timedelta(minutes=5))
    orders.append([str(order_id), str(member_id), sql_string(status), str(total), ts(ordered_at), ts(ordered_at),
                   ts(updated)])
    if status in ("PAID", "SHIPPED"):
        key = "%032x" % rng.getrandbits(128)
        payments.append([str(len(payments) + 1), str(order_id), sql_string(key), str(total), "'APPROVED'",
                         ts(ordered_at + timedelta(minutes=5))])

# ---------------------------------------------------------------- reviews
weights = [6 if BOOKS[i][2] in (0, 1, 2) else 2 for i in range(len(BOOKS))]
review_rows = []
for review_id in range(1, 301):
    book_id = 1 if review_id <= 12 else rng.choices(range(1, len(BOOKS) + 1), weights=weights)[0]
    member_id = rng.choice(CUSTOMER_IDS)
    rating = rng.choices([1, 2, 3, 4, 5], weights=[1, 1, 3, 6, 6])[0]
    created = datetime(2026, 8, 1) + timedelta(minutes=rng.randrange(0, 61 * 24 * 60))
    review_rows.append([str(review_id), str(book_id), str(member_id), str(rating),
                        sql_string(rng.choice(REVIEW_TEXTS)), ts(created)])

# ---------------------------------------------------------------- write
sql = [
    "-- Seed data for the bookstore. Generated by .authoring/bookstore/generate_seed.py: don't edit by hand.\n",
    "-- Every member's password is \"bookstore123\". Member 20 is the admin.\n\n",
    insert("categories", ["id", "name"], categories),
    insert("books", ["id", "isbn", "title", "author", "category_id", "price", "published_date", "created_at",
                     "updated_at"], books),
    insert("inventories", ["book_id", "quantity", "version"], inventories),
    insert("members", ["id", "email", "name", "password_hash", "role", "created_at", "updated_at"], members),
    insert("wallets", ["id", "member_id", "balance", "version", "created_at", "updated_at"], wallets),
    insert("wallet_transactions", ["id", "wallet_id", "type", "amount", "created_at"], wallet_transactions),
    insert("coupons", ["id", "code", "discount_amount", "total_quantity", "issued_quantity", "valid_from",
                       "valid_until", "version"], coupon_rows),
    insert("coupon_issues", ["id", "coupon_id", "member_id", "issued_at", "used_at"], coupon_issue_rows),
    insert("orders", ["id", "member_id", "status", "total_amount", "ordered_at", "created_at", "updated_at"], orders),
    insert("order_lines", ["id", "order_id", "book_id", "quantity", "unit_price"], order_lines),
    insert("payments", ["id", "order_id", "idempotency_key", "amount", "status", "created_at"], payments),
    insert("reviews", ["id", "book_id", "member_id", "rating", "content", "created_at"], review_rows),
]
OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text("\n".join(sql))

# ---------------------------------------------------------------- facts the documentation relies on
minji_orders = sum(1 for row in orders if row[1] == "1")
book1_reviews = sum(1 for row in review_rows if row[1] == "1")
assert prices[1] == 36000 and inventories[0][1] == "10"
assert minji_orders >= 5 and book1_reviews >= 12
print(f"wrote {OUT}")
print(f"categories={len(categories)} books={len(books)} members={len(members)} orders={len(orders)} "
      f"order_lines={len(order_lines)} payments={len(payments)} reviews={len(review_rows)} "
      f"coupon_issues={len(coupon_issue_rows)}")
print(f"minji orders={minji_orders} book 1 reviews={book1_reviews} minji wallet=100000")
status_counts = {}
for row in orders:
    status_counts[row[2]] = status_counts.get(row[2], 0) + 1
print("order statuses:", status_counts)
print("books 2-5 prices:", [prices[i] for i in range(2, 6)], "stock:", [inventories[i - 1][1] for i in range(2, 6)])

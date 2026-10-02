# J01 · Java for TypeScript developers

⏱ 6 sessions · week 1 · 6 exercises

<!-- AUTHOR: intro, lesson, TypeScript → Java table, "In real projects", and exercises J01-01, J01-03 … J01-06 go here.
     Keep the J01-02 section below (it is the format example for Predict exercises). -->

## Exercises

### J01-02 · Predict: types, values and references

**Predict** · 30 min · [`Puzzles.java`](src/main/java/practice/j01/ex02/Puzzles.java) → answers in [`Answers.java`](src/main/java/practice/j01/ex02/Answers.java)

Twenty tiny puzzles about the places where Java behaves differently from TypeScript: integer math, `char`, boxing, `==` and how arguments are passed. Predict first, then run. A wrong guess you understand is worth more than a right one you guessed.

**How:** read each method in `Puzzles` without running it, write your prediction in the matching constant in `Answers`, then run `PuzzlesTest`. Each test tells you what you predicted and what the code really gives.

<details>
<summary>Explanations (open after you've run the tests)</summary>

- **`7 / 2` is `3`.** Dividing two `int`s cuts off the fraction. TypeScript has only one number type, so the same code gives `3.5` there.
- **`-7 % 3` is `-1`.** `%` keeps the sign of the left side. `Math.floorMod(-7, 3)` gives `2`.
- **`Integer.MAX_VALUE + 1` is `Integer.MIN_VALUE`.** `int` arithmetic wraps around silently. `Math.addExact` throws instead.
- **`24 * 60 * 60 * 1000 * 1000` is `500654080`, not 86,400,000,000.** Every operand is an `int`, so the multiplication overflows *before* the result is stored in the `long`. Write `24L * 60 * ...` to compute in `long`.
- **`0.1 + 0.2` is `0.30000000000000004`.** Same as TypeScript: binary floating point can't store 0.1 exactly. Money uses `BigDecimal` (J07-03).
- **`(int) -3.99` is `-3`.** Casting drops the fraction toward zero.
- **`1.0 / 0` is `Infinity`, but `1 / 0` with `int`s throws `ArithmeticException`.**
- **`'a' + 1` is `98`.** A `char` is a number; adding an `int` gives an `int`. But `letter += 2` keeps the `char` type, giving `'c'`.
- **`1 + 2 + "3" + 4 + 5` is `"3345"`.** `+` runs left to right: numbers are added until the first `String` appears, then everything is concatenated.
- **`language.toUpperCase()` changes nothing.** Strings are immutable; the method returns a new `String`.
- **`Integer 127 == Integer 127` is `true`, but `128 == 128` is `false`.** `==` compares references. Small values are cached and shared, larger ones aren't. Always compare objects with `equals()`.
- **`Long.valueOf(1L).equals(1)` is `false`.** The `1` becomes an `Integer`, and a `Long` is only equal to another `Long`.
- **Unboxing `null` throws `NullPointerException`.** `int copy = count` calls `count.intValue()`.
- **Java always passes arguments by value.** For an `int`, the method gets a copy of the number. For an object, it gets a copy of the *reference*: it can change the object (`append`), but assigning a new object to its parameter doesn't affect the caller.
</details>

# J01 · Java for TypeScript developers

⏱ 6 sessions · week 1 · 6 exercises

This module takes you from TypeScript to Java in one week. You'll learn how a Java program is put together and run, how Java's static types differ from TypeScript's (primitives, integer math, `==` versus `equals`), the everyday tools for strings, control flow, arrays and methods, and how to find a bug with a stack trace and the debugger instead of guessing. Everything later in the course, Spring included, builds on these basics, and most surprises for TypeScript developers hide right here.

## Lesson

Each session, read the lesson sections for that day, then do the exercise.

| Session | Read | Then do |
|---|---|---|
| 1 | 1. Program structure · 2. Compiling and running · 3. Types | J01-01 |
| 2 | 4. Operators · 9. Pass-by-value | J01-02 |
| 3 | 5. Strings | J01-03 |
| 4 | 6. Control flow | J01-04 |
| 5 | 7. Arrays · 8. Methods | J01-05 |
| 6 | 10. Stack traces and the debugger | J01-06 |

### 1. A Java program is made of classes

In TypeScript, a file can hold functions, variables and classes side by side. In Java, all code lives inside a class:

```java
package practice.j01.ex01;                       // 1. the package this file belongs to

import java.util.List;                           // 2. classes from other packages that this file uses

public final class GreetingApp {                 // 3. the class; its name matches the file name

    public static void main(String[] args) {     // 4. where the program starts
        System.out.println("Hello, " + args[0]);
    }
}
```

1. **Package.** A package is a namespace that matches a folder: `practice.j01.ex01` lives in `src/main/java/practice/j01/ex01/`. A class's full name includes its package: `practice.j01.ex01.GreetingApp`. Classes in the same package can use each other without imports.
2. **Imports.** `import java.util.List;` lets you write `List` instead of `java.util.List`. Unlike a TypeScript import, it loads nothing; it only gives you the short name. Everything in `java.lang` (`String`, `Math`, `Integer`, `System`) is imported automatically.
3. **One public class per file, named like the file.** `public class GreetingApp` must be in `GreetingApp.java`. Otherwise the compiler stops with *class GreetingApp is public, should be declared in a file named GreetingApp.java*.
4. **`main` is the entry point.** It's `public` so the JVM can call it, `static` because it belongs to the class itself (no object needed), and `void` because it returns nothing. `args` holds the command-line arguments. Unlike Node's `process.argv`, it contains only the arguments, not the program name.

Access modifiers decide who can use a class, method or field:

| Modifier | Visible to |
|---|---|
| `public` | all code |
| none ("package-private") | classes in the same package |
| `private` | the same class only |

You'll meet `protected` in J02. A class that only holds static helper methods is usually `final` (nobody can extend it) and has a `private` constructor (nobody can create a useless object). You'll see that pattern in every exercise of this module.

### 2. Compiling and running

The compiler `javac` turns `.java` source files into *bytecode*, stored in `.class` files. The `java` command starts a *JVM* (Java Virtual Machine), which runs that bytecode. You'll run code in three ways:

- **Gradle**, the build tool of this repository. It plays the role of `npm` scripts and `tsc` together. `./score j01` runs this module's tests and shows your progress, and `./score j01-03` lists every failing test of one exercise. Gradle downloads its own JDK 25, so it works even if an older Java is installed.
- **IntelliJ IDEA.** Open the repository folder, and IntelliJ imports the Gradle project. Click the green ▶ next to a test class, a test method or a `main` method to run it. The debugger is one click away (section 10).
- **The JDK tools directly.** These need `java -version` to show 25. If it shows an older version, install a JDK 25, for example from [adoptium.net](https://adoptium.net).

```bash
cd java/j01-basics
javac -d /tmp/j01 src/main/java/practice/j01/ex01/*.java       # compile into /tmp/j01
java -cp /tmp/j01 practice.j01.ex01.GreetingApp Minji 9         # run: class path + full class name
java src/main/java/practice/j01/ex01/GreetingApp.java Minji 9   # compile in memory and run, in one step
```

The last line uses *source-file mode*: `java` compiles the file in memory and runs it. Since Java 22 it also finds the other classes the program needs (here `Greeting`) in the same source tree.

**Compact source files.** For scripts and quick experiments, Java 25 lets you leave out the ceremony:

```java
// Hello.java: no package, no class, no static, no String[] args
void main() {
    String name = IO.readln("Your name? ");
    IO.println("Hello, " + name + "!");
}
```

```bash
java Hello.java
```

The compiler wraps a compact source file in a class for you. `main` can be an instance method without parameters, `IO.println` and `IO.readln` write to and read from the console, and the classes of the `java.base` module, such as `List` and `Map`, are imported automatically. It feels like running a `.ts` file with `npx tsx`. Real projects, this course included, use normal classes in packages. To try out one line at a time, use `jshell`, Java's REPL (like running `node` without a file).

### 3. Types: primitives, references and wrappers

Every variable has a type that the compiler knows, and the type never changes:

```java
int count = 3;                   // a whole number
long views = 3_000_000_000L;     // L marks a long literal; _ groups digits for readability
double price = 4.5;              // a floating-point number
boolean done = false;
char grade = 'A';                // one character, in single quotes
String name = "Minji";           // text, in double quotes
```

Java has eight *primitive* types. A primitive variable holds the value itself:

| Type | Holds | Notes |
|---|---|---|
| `int` | whole numbers, 32 bits: about ±2.1 billion | the default for whole numbers |
| `long` | whole numbers, 64 bits: about ±9.2 × 10¹⁸ | literals end with `L` |
| `double` | floating-point numbers, 64 bits | the default for decimals; same as a TypeScript `number` |
| `boolean` | `true` or `false` | |
| `char` | one UTF-16 code unit, such as `'A'` or `'한'` | it's a number underneath: `'a' + 1` is `98` |
| `byte`, `short`, `float` | smaller versions of the above | rare in application code |

Every other type is a *reference type*: `String`, arrays, and every class. A variable of a reference type holds a *reference* to an object, the way a JavaScript variable refers to an object. A reference can be `null`, which means "no object". A primitive can't be `null`, and Java has no `undefined`: the compiler makes sure you assign a local variable before you read it.

**Wrapper types.** Each primitive has a class that holds one value of it: `Integer`, `Long`, `Double`, `Boolean`, `Character`, and so on. You need them where only objects are allowed, for example in collections: `List<Integer>`, not `List<int>`. The compiler converts between the two automatically (*autoboxing* and *unboxing*), with two traps that J01-02 shows: `==` between two `Integer` objects compares references, and unboxing `null` throws `NullPointerException`.

**`var`.** Inside a method, `var` lets the compiler infer the type from the right-hand side: `var names = new ArrayList<String>();`. The variable is still statically typed, so `names = 42;` doesn't compile. Use `var` when the type is obvious from the right-hand side, and write the type when it isn't.

**`final`** works like TypeScript's `const`: the variable can't be assigned again. Like `const`, it doesn't freeze the object: the elements of a `final` array can still change. Constants are `static final` fields with UPPER_SNAKE_CASE names: `static final int MAX_LENGTH = 64;`.

**Conversions.** Java converts automatically when nothing can be lost (`int` to `long` to `double`). In the other direction you need a *cast*, which can lose information: `(int) 3.99` is `3`, and `(int) 3_000_000_000L` is `-1294967296`.

### 4. Operators and integer math

Most operators work as in TypeScript: `+ - * / %`, `++`, `+=`, `&& || !`, `? :` and the comparisons. The differences are in the numbers:

```java
7 / 2                                 // 3: int / int cuts off the fraction
7 / 2.0                               // 3.5: one double operand makes it a double division
-7 % 3                                // -1: % keeps the sign of the left side
Math.floorMod(-7, 3)                  // 2: always between 0 and 3 - 1
Integer.MAX_VALUE + 1                 // -2147483648: int arithmetic wraps around silently
Math.addExact(Integer.MAX_VALUE, 1)   // throws ArithmeticException instead
1 / 0                                 // throws ArithmeticException
1.0 / 0                               // Infinity
```

- An `int` goes from -2,147,483,648 to 2,147,483,647. The range isn't symmetric: there is one more negative number than there are positive ones.
- In an operation with mixed types, the narrower operand is converted to the wider type: `int * long` gives a `long`. But that happens per operation, from left to right. In `long micros = 24 * 60 * 60 * 1000 * 1000;` every operand is an `int`, so the multiplication overflows before the result reaches the `long`. Write `24L * 60 * ...`.
- There's no `===`. For primitives, `==` compares the values. For objects, `==` checks whether two references point to the same object. To compare the contents of objects, use `equals()` (section 5).
- Conditions must be `boolean`. There is no truthy or falsy: `if (count)` and `if (name)` don't compile.

### 5. Strings

A `String` is an immutable object: its methods return a new `String` and never change the old one.

```java
String title = "  Clean Code ";
title.strip();              // returns "Clean Code", but title doesn't change
title = title.strip();      // keep the result
```

**Compare strings with `equals`, never with `==`:**

```java
String a = "java";
String b = new String("java");
a == b                      // false: two different objects
a.equals(b)                 // true: the same characters
"java".equals(input)        // false (not an exception) when input is null
a.equalsIgnoreCase("JAVA")  // true
```

`==` sometimes seems to work, because Java shares identical string literals. Text that your program builds or reads at runtime is a new object, so `==` fails exactly when real input arrives.

**Building strings.** `+` concatenates. To build a string in a loop, use a `StringBuilder`, which changes its contents in place instead of creating a new `String` for every `+`:

```java
var line = new StringBuilder();
for (String word : words) {
    line.append(word).append(' ');
}
String result = line.toString();
```

**Text blocks** are multi-line strings:

```java
String json = """
        {
          "title": "Clean Code",
          "price": 3000
        }
        """;
```

- A text block starts with `"""` and a line break, and ends with `"""`. Quotes inside don't need escaping.
- The indentation that all lines share is removed, and the closing `"""` counts as a line. So the text above starts with `{` at the left edge.
- With the closing `"""` on its own line, the text ends with a line break. Put it right after the last character if you don't want one.
- Line breaks in a text block are always `\n`, on every operating system.

**Formatting.** Java has no string interpolation like `` `Hi ${name}` ``. Use `+`, or `formatted` (the same as `String.format`) when you need layout:

```java
"%s has %d items".formatted("Minji", 3)   // "Minji has 3 items"
"[%5d]".formatted(42)                     // "[   42]"  right-aligned in 5 characters
"[%-5s]".formatted("ab")                  // "[ab   ]"  left-aligned
"%03d".formatted(7)                       // "007"      padded with zeros
"%.2f".formatted(3.14159)                 // "3.14"
```

`formatted` uses your computer's *locale*, the language and region settings, for things like the decimal separator of `%.2f`. For text that other programs read, such as logs, JSON or file names, use `String.format(Locale.ROOT, ...)`.

**Common methods:**

| Method | Example | Result |
|---|---|---|
| `length()` | `"Java".length()` | `4` |
| `charAt(i)` | `"Java".charAt(0)` | `'J'` |
| `substring(from, to)` | `"Java".substring(1, 3)` | `"av"` (`to` is excluded, like `slice` in JS) |
| `indexOf`, `lastIndexOf` | `"banana".indexOf("an")` | `1`, or `-1` if not found |
| `contains`, `startsWith`, `endsWith` | `"Java".startsWith("Ja")` | `true` |
| `strip()` | `" hi ".strip()` | `"hi"` |
| `isEmpty()`, `isBlank()` | `"  ".isBlank()` | `true` (only whitespace) |
| `toUpperCase(Locale.ROOT)` | `"java".toUpperCase(Locale.ROOT)` | `"JAVA"` |
| `replace(a, b)` | `"a-b-c".replace("-", "+")` | `"a+b+c"` (every match, unlike JS `replace`) |
| `split(regex)` | `"a,b,c".split(",")` | `["a", "b", "c"]` (the argument is a regular expression, so `"a.b".split(".")` gives an empty array) |
| `String.join(separator, parts)` | `String.join("-", "a", "b")` | `"a-b"` |
| `repeat(n)` | `"ab".repeat(3)` | `"ababab"` |

Two of these have older look-alikes. `strip()` removes every kind of Unicode whitespace, but the older `trim()` only removes ASCII spaces and control characters, so prefer `strip()`. And pass `Locale.ROOT` to `toUpperCase` and `toLowerCase` for codes and identifiers: with a Turkish locale, `"i".toUpperCase()` is `"İ"`.

**Characters.** A `char` is one UTF-16 code unit, and the `Character` class has the tools: `Character.isLetter(c)`, `isDigit(c)`, `isLetterOrDigit(c)`, `isWhitespace(c)`, `toUpperCase(c)`. Korean syllables are letters without letter case: `Character.isLetter('한')` is `true`, and `Character.toUpperCase('한')` is `'한'`. An emoji takes two `char`s, in the same way that `"😀".length` is `2` in JavaScript. Loop over the characters of a string like this:

```java
for (char c : text.toCharArray()) { ... }
for (int i = 0; i < text.length(); i++) { char c = text.charAt(i); ... }
```

### 6. Control flow

`if`/`else` works as in TypeScript, but the condition must be a `boolean`:

```java
if (name != null && !name.isBlank()) { ... }   // if (name) doesn't compile
```

**Switch expressions** are the modern `switch`. They produce a value:

```java
String kind = switch (day) {
    case SATURDAY, SUNDAY -> "weekend";                             // several labels in one case
    case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY -> "weekday";
};
```

- An arrow (`->`) case never falls through to the next one, so there's no `break`.
- A switch expression must have a result for every possible value. For an enum, listing every constant is enough, and the compiler checks it. That's better than `default`: if someone adds a constant later, the code stops compiling instead of quietly doing the wrong thing. For `int` and `String` you need a `default`.
- A case can throw: `default -> throw new IllegalArgumentException("unknown code: " + code);`
- A case that needs several statements uses a block, and `yield` gives the block's result. (`return` would leave the whole method.)

```java
int fee = switch (shipping) {
    case STANDARD -> 0;
    case EXPRESS -> {
        int base = 3_000;
        yield remoteArea ? base * 2 : base;
    }
};
```

- A `switch` on `null` throws `NullPointerException`, unless you add `case null`.
- You can switch on `int`, `char`, `String` and enums. Switching on types with patterns comes later in the course.

The older statement form with `case X:` labels still exists. It falls through to the next case when you forget a `break`, as in TypeScript, so prefer arrows.

**Loops:**

```java
for (int i = 0; i < 3; i++) { ... }          // the classic for loop
for (String name : names) { ... }           // for-each over an array or collection, like for...of
while (n != 1) { ... }
do { ... } while (input.isBlank());         // runs the body at least once
```

There's no `for...in`. When you need the index, use the classic `for`.

**`break`, `continue` and labels.** `break` leaves the innermost loop, and `continue` jumps to its next round. To leave an outer loop, give it a label:

```java
search:
for (int[] row : grid) {
    for (int cell : row) {
        if (cell < 0) {
            break search;        // leaves both loops; "continue search" would go to the next row
        }
    }
}
```

TypeScript has labels with the same syntax. In both languages they are rare: often, moving the loops into their own method and using `return` reads better.

### 7. Arrays

```java
int[] scores = new int[3];          // {0, 0, 0}: a fixed size, filled with default values
String[] names = {"Kim", "Lee"};    // an array literal
scores[0] = 90;
int size = scores.length;           // 3: a field, not a method
scores[3] = 1;                      // throws ArrayIndexOutOfBoundsException
```

- An array's size is fixed when you create it. Writing past the end throws an exception, where JavaScript would grow the array. For a list that grows, use `ArrayList` (J05).
- New arrays are filled with default values: `0` for numbers, `false` for `boolean`, and `null` for references.
- A 2D array is an array of arrays, so its rows can have different lengths: `int[][] grid = {{1}, {2, 3}};`.
- Arrays don't compare or print their contents: `a.equals(b)` is the same as `a == b`, and printing an `int[]` shows something like `[I@1b6d3586`. The `Arrays` class has the tools:

```java
Arrays.toString(scores)                            // "[90, 0, 0]"
Arrays.equals(a, b)                                // compares the contents
Arrays.sort(scores)                                // sorts in place
int[] copy = Arrays.copyOf(scores, scores.length)  // a copy; scores.clone() does the same
int[] part = Arrays.copyOfRange(scores, 1, 3)      // elements 1 and 2
Arrays.fill(scores, -1)                            // sets every element
```

Day to day, you'll use `List` more than arrays. Arrays still show up in `main(String[] args)`, in varargs, in `String.split`, and in code that must be fast.

### 8. Methods: static methods, overloading, varargs

```java
static int square(int x) {          // return type, name, typed parameters
    return x * x;
}
```

- A `static` method belongs to the class. Call it as `Math.max(1, 2)`, or as `square(3)` inside the same class. Methods that belong to objects come in J02.
- Java has no default values and no optional parameters. Instead, you *overload*: write several methods with the same name and different parameter lists. The compiler picks one from the argument types:

```java
static String describe(int count) { return count + " items"; }
static String describe(String name) { return "item " + name; }
static String describe(int count, String unit) { return count + " " + unit; }
```

- **Varargs.** `static int sum(int... numbers)` accepts any number of `int` arguments, including none. Inside the method, `numbers` is an `int[]`. Callers write `sum()`, `sum(1, 2, 3)`, or pass an array: `sum(values)`. Only the last parameter can be varargs. It's the same idea as a TypeScript rest parameter, `(...numbers: number[])`. If a method needs at least one value, `max(int first, int... rest)` lets the compiler enforce that.

### 9. Java passes arguments by value

When you call a method, each argument is copied into its parameter. For a primitive, the copy is the value itself. For an object, the copy is the *reference*: the method can change the object through it, but assigning a new object to the parameter doesn't change the caller's variable. JavaScript works exactly the same way; Java has no other mode.

```java
static void addBonus(int[] scores) {
    scores[0] += 10;                 // the caller sees this: it's the same array
}

static void replace(int[] scores) {
    scores = new int[] {100};        // the caller doesn't see this: only the copy of the reference changed
}
```

Because a method can change an array or object that it receives, good code makes clear whether it does. In J01-05, a method only changes its argument when its name says so (`reverseInPlace`). Otherwise it leaves the argument alone and returns something new.

### 10. Reading stack traces and using the debugger

When nobody catches an exception, Java prints a *stack trace*: the exception, its message, and the chain of method calls that led to it. Here is a real one from a small program that loads a price list:

```
Exception in thread "main" java.lang.IllegalStateException: Could not load the price list
	at shop.PriceList.load(PriceList.java:17)
	at shop.Main.main(Main.java:9)
Caused by: java.lang.NumberFormatException: For input string: "4,500"
	at java.base/java.lang.NumberFormatException.forInputString(NumberFormatException.java:67)
	at java.base/java.lang.Integer.parseInt(Integer.java:565)
	at java.base/java.lang.Integer.parseInt(Integer.java:662)
	at shop.PriceList.parseLine(PriceList.java:23)
	at shop.PriceList.load(PriceList.java:14)
	... 1 more
```

Read it like this:

1. **The first line** names the exception class and its message.
2. **The `at` lines** are the *call stack*, newest call first. Each one shows `class.method(File.java:line)`. The top line is where the exception was thrown; the lines below are the callers.
3. **`Caused by:`** means that the exception above caught this one and wrapped it, keeping it as its *cause*. Frameworks wrap exceptions all the time, so a trace can have several `Caused by:` sections. The last one is the root cause: read it first. Here, `"4,500"` isn't a number because of its comma.
4. **Find the first line from your own code** in that section, skipping library frames such as `java.base/...` or `org.junit...`. Here it's `shop.PriceList.parseLine(PriceList.java:23)`: the line to look at, or to put a breakpoint on.
5. **`... 1 more`** means that the remaining frames are the same as in the section above, so Java doesn't repeat them.

Since Java 15, a `NullPointerException` says exactly what was `null`, for example *Cannot invoke "String.strip()" because "couponCode" is null*. In IntelliJ, every `File.java:line` in a stack trace is a link to that line.

**The IntelliJ debugger** lets you stop a running program and look inside. Shortcuts are for macOS, then Windows/Linux. On a Mac, the F-keys may need the Fn key.

- **Breakpoint:** click the gutter next to a line number (⌘F8 / Ctrl+F8). A red dot appears.
- **Debug instead of Run:** use the bug icon next to the ▶, or ⌃D / Shift+F9. The program stops *before* it runs the line with the breakpoint.
- **Look around:** the *Variables* pane shows every local variable and field. Hover over a variable in the editor to see its value.
- **Step:** *Step Over* (F8) runs the current line and stops at the next one. *Step Into* (F7) goes into the method called on the current line. *Step Out* (⇧F8 / Shift+F8) finishes the current method. *Resume* (⌥⌘R / F9) runs on until the next breakpoint.
- **Evaluate Expression** (⌥F8 / Alt+F8): type any Java expression, such as `percent / 100` or `lines.length - 1`, and see its value at this moment. It's the fastest way to test an idea for a fix.
- **Conditional breakpoint:** right-click the red dot and enter a condition, such as `i == lines.length - 1`. The debugger stops only when the condition is true, which is ideal inside a loop.
- **Exception breakpoint:** open *Run → View Breakpoints* (⇧⌘F8 / Ctrl+Shift+F8), click **+**, choose *Java Exception Breakpoints*, and enter `NullPointerException`. The debugger stops at the moment the exception is thrown, while all the variables are still there.

## TypeScript → Java

| TypeScript | Java | Watch out for |
|---|---|---|
| `let count: number = 3` | `int count = 3;` or `var count = 3;` | pick `int`, `long` or `double`; there is no single number type |
| `const max = 64` | `final int max = 64;` | `final` doesn't freeze the object, same as `const` |
| `null`, `undefined` | `null` only | primitives can't be `null`; unboxing `null` throws |
| truthy and falsy values | `boolean` only | `if (name)` doesn't compile |
| `a === b` | `==` for primitives, `a.equals(b)` for objects | `==` on objects compares references |
| `` `Hi ${name}` `` | `"Hi " + name` or `"Hi %s".formatted(name)` | no string interpolation |
| multi-line template literal | text block `"""` | the closing `"""` sets the indentation |
| `7 / 2` is `3.5` | `7 / 2` is `3` | integer division; `int` overflow wraps silently |
| `number[]` | `int[]` (fixed size) or `List<Integer>` | arrays can't grow |
| `arr.length`, `str.length` | `arr.length`, `str.length()`, `list.size()` | three spellings |
| `function f(x: number): number` | `static int f(int x)` | it must live in a class |
| optional and default parameters | overloading | |
| `(...nums: number[])` | `(int... nums)` | only the last parameter |
| `switch` with `case x:` | `switch` expression with `case x ->` | no fall-through; exhaustive for enums |
| `for (const x of xs)` | `for (int x : xs)` | no `for...in` |
| `console.log(x)` | `System.out.println(x)`, or `IO.println(x)` in a compact source file | |
| `throw new Error("...")` | `throw new IllegalArgumentException("...")` | pick a specific exception class |
| `process.argv` | `String[] args` in `main` | `args` doesn't contain the program name |

## In real projects

- **Naming and layout.** Classes in `PascalCase`, methods and variables in `camelCase`, constants in `UPPER_SNAKE_CASE`, packages in lowercase (`com.example.bookstore.order`). Four spaces of indentation, and braces even around a one-line `if` body. → [Java style](../../docs/handbook/java-style.md)
- **Compare objects with `equals`.** When one side may be `null`, put the known value first (`"WELCOME10".equals(code)`) or use `Objects.equals(a, b)`. → [Null and Optional](../../docs/handbook/null-and-optional.md)
- **Check arguments at the start of a public method** and throw a specific exception with a helpful message: `"hour must be between 0 and 23, but was " + hour`. Keep personal data such as email addresses out of messages, because messages end up in logs. → [Exceptions](../../docs/handbook/exceptions.md)
- **Don't change what you receive.** A method that gets an array or a list shouldn't modify it unless that is its documented job. Return a new one instead. → [Immutability](../../docs/handbook/immutability.md)
- **Never use `double` for money.** Use whole cents in a `long`, as this module does, or `BigDecimal` (J07). → [Dates and money](../../docs/handbook/dates-and-money.md)
- **Reproduce a bug with a failing test, then fix it.** Read the stack trace before you change any code. J01-06 works this way. → [Testing](../../docs/handbook/testing.md)
- **Commit after every exercise** on your `my-work` branch, so you can always go back. → [Git workflow](../../docs/handbook/git-workflow.md)

## Exercises

### J01-01 · Hello, Java

**Build** · 30 min · [`Greeting.java`](src/main/java/practice/j01/ex01/Greeting.java) · [`GreetingApp.java`](src/main/java/practice/j01/ex01/GreetingApp.java)

Your first Java program: a method that builds a greeting, and a `main` method that turns command-line arguments into a call to it. Small as it is, it has what most backend code has: input to clean up, rules with boundaries, and invalid input to reject.

**You'll practice:** the parts of a class, `static` methods, `if`/`else`, `strip()` and `isEmpty()`, throwing `IllegalArgumentException`, `main(String[] args)`, `Integer.parseInt`, `System.out` and `System.err`.

**Rules:** in the Javadoc of `Greeting` and `GreetingApp`.

**Try it:** when the tests pass, run your program from `java/j01-basics` with `java src/main/java/practice/j01/ex01/GreetingApp.java Minji 9`. In IntelliJ, click the ▶ next to `main`, then add `Minji 9` as *Program arguments* in the run configuration.

<details>
<summary>Hint 1</summary>

Check the hour first. Then pick the greeting with an `if`/`else if` chain that starts at the smallest hours: when the checks run from top to bottom, each one only needs the upper end of its range (`hour < 5`, then `hour < 12`, …).
</details>

<details>
<summary>Hint 2</summary>

Which method removes the full-width space U+3000: `trim()` or `strip()`? Section 5 of the lesson has the answer. And `System.err.println` writes to standard error.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Does `format` reject a bad hour before it does anything else? How many comparisons does your hour check need?

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
- **`"hello" == new String("hello")` is `false`.** `new` always creates a new object, so the two references differ even though the text is the same.
- **`Long.valueOf(1L).equals(1)` is `false`.** The `1` becomes an `Integer`, and a `Long` is only equal to another `Long`.
- **Unboxing `null` throws `NullPointerException`.** `int copy = count` calls `count.intValue()`.
- **Java always passes arguments by value.** For an `int`, the method gets a copy of the number. For an object, it gets a copy of the *reference*: it can change the object (`append`), but assigning a new object to its parameter doesn't affect the caller.
</details>

### J01-03 · Strings

**Build** · 30 min · [`StringTools.java`](src/main/java/practice/j01/ex03/StringTools.java)

Five string helpers of the kind every code base has: a palindrome check, capitalizing words, masking an email address for a "we sent you a code" page, a receipt line with fixed columns, and an email text from a text block. Each one hides a small trap.

**You'll practice:** `char` and the `Character` methods, `StringBuilder`, `equals` versus `==`, `indexOf`, `lastIndexOf` and `substring`, `formatted` with column widths, text blocks, `Objects.requireNonNull`.

**Rules:** in the Javadoc of each method of `StringTools`. Every method rejects `null` with `NullPointerException`: `Objects.requireNonNull(text, "text")` does that in one line.

<details>
<summary>Hint 1</summary>

`isPalindrome`: build a cleaned-up string in a `StringBuilder` first (only letters and digits, in lowercase), then compare it with its reverse. How do you compare two `String`s?
</details>

<details>
<summary>Hint 2</summary>

`capitalizeWords`: go through the characters once, and remember in a `boolean` whether the previous character was whitespace. Splitting on `" "` loses whitespace.
</details>

<details>
<summary>Hint 3</summary>

`receiptLine`: the format `"%-20s %3d %10s"` lays out the three columns. For the money, `total / 100` gives the units and `total % 100` the cents, and `%02d` pads the cents to two digits. `Math.multiplyExact` multiplies and throws on overflow.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Does any of your methods use `+` on a `String` inside a loop? What would end up in your log files if `maskEmail` put the address into its exception message?

### J01-04 · Control flow

**Build** · 30 min · [`ControlFlow.java`](src/main/java/practice/j01/ex04/ControlFlow.java)

One method for each control-flow tool: a `switch` expression that classifies HTTP status codes (you'll return them yourself in S04), a `switch` with several labels per case for opening hours, two `while` loops with an integer trap each, and nested loops that stop with a labeled `break`.

**You'll practice:** `switch` expressions with `->`, several labels in one case, `default -> throw`, `while` loops, `/` and `%` on negative numbers, `long` for values beyond `int`, 2D arrays with rows of different lengths, labeled `break`.

**Rules:** in the Javadoc of `ControlFlow`. The enum `StatusClass` is given.

<details>
<summary>Hint 1</summary>

`statusClass`: what is `404 / 100`? And `99 / 100`, or `-150 / 100`?
</details>

<details>
<summary>Hint 2</summary>

`digitSum`: try `Math.abs(Integer.MIN_VALUE)` in `jshell`. Then try `-1234 % 10`. Can you sum the digits without making the number positive first?
</details>

<details>
<summary>Hint 3</summary>

`collatzSteps`: print the numbers of the sequence that starts at 113383. Which type can hold the largest one?
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Does your `openingHours` have a `default` case? What happens to it if someone adds a constant to the enum, with and without `default`? Where could a `return` replace your labeled `break`?

### J01-05 · Arrays and methods

**Build** · 30 min · [`ArrayTools.java`](src/main/java/practice/j01/ex05/ArrayTools.java)

Array helpers that look simple, and each has a classic bug waiting: the maximum of negative numbers, reversing by swapping, rotating by a negative number of steps, averaging without overflow or integer division, and searching for a `String` with `==`. Two `indexOf` methods share one name, which is overloading.

**You'll practice:** loops over arrays, swapping elements, `Math.floorMod`, `long` sums and `double` division, varargs, overloading, `Objects.equals`, leaving your arguments unchanged.

**Rules:** in the Javadoc of `ArrayTools`. Only `reverseInPlace` may change the array it receives.

<details>
<summary>Hint 1</summary>

`max`: 0 is a bad starting value. Which value is always a valid candidate?
</details>

<details>
<summary>Hint 2</summary>

`rotateRight`: where does the element at index `i` end up? Write it as a formula with `%`, then test the formula with `k = -1`. `Math.floorMod` helps.
</details>

<details>
<summary>Hint 3</summary>

`average`: in `(double) sum / values.length`, the cast happens before the division. What would `(double) (sum / values.length)` give for `average(1, 2)`?
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Which of your methods would break for `new int[0]`, and does a test prove that it doesn't? Why does `Objects.equals` beat `values[i].equals(target)` here?

### J01-06 · Debugging and stack traces

**Fix** · 30 min · [`InvoiceCalculator.java`](src/main/java/practice/j01/ex06/InvoiceCalculator.java)

The bookstore's checkout charges wrong amounts, and you're on call. The code compiles and looks reasonable, but it has three bugs. This time, how you find them matters as much as the fix: read the stack trace, and let the debugger show you what the code does instead of guessing.

> **BOOK-142 · Wrong totals at checkout**
>
> Customer service reports three problems since Monday's release:
>
> 1. "I bought *Clean Code* and *Refactoring*, but I was only charged for *Clean Code*." Orders with a single book show a total of 0.00.
> 2. "The WELCOME10 coupon was accepted, but I paid the full price."
> 3. Customers who don't enter a coupon can't check out at all. The developer on call reproduced it with a test and got this stack trace:
>
> ```
> java.lang.IllegalStateException: Checkout failed for order A-1001
> 	at practice.j01.ex06.Checkout.confirm(Checkout.java:27)
> 	at practice.j01.ex06.InvoiceCalculatorTest$Basic.checkoutWithoutCoupon(InvoiceCalculatorTest.java:51)
> Caused by: java.lang.NullPointerException: Cannot invoke "String.strip()" because "couponCode" is null
> 	at practice.j01.ex06.InvoiceCalculator.discountPercent(InvoiceCalculator.java:39)
> 	at practice.j01.ex06.InvoiceCalculator.totalCents(InvoiceCalculator.java:55)
> 	at practice.j01.ex06.Checkout.confirm(Checkout.java:24)
> 	... 1 more
> ```

**You'll practice:** reading a stack trace with `Caused by:`, breakpoints, stepping, Evaluate Expression, conditional and exception breakpoints, and fixing an off-by-one loop, an integer division and a missing `null` check.

**Rules:** the Javadoc of `InvoiceCalculator` describes the intended behavior. `InvoiceLine` (a *record*, a short way to write a data class; J02 covers it) and `Checkout` are correct, so don't change them.

**How to work:** don't fix anything by reading alone. Take one problem at a time:

1. Run `./score j01-06`, or the test class in IntelliJ, and pick one failing test.
2. If it has a stack trace, find the root cause and the first line from our code, and open that line.
3. Let the debugger show you the bug:
   - **Problem 1:** put a breakpoint inside the loop of `subtotalCents` and debug *charges every line of the invoice*. Step over (F8) and watch `i` in the Variables pane. Then make it a conditional breakpoint with `i == lines.length - 1`: does the debugger ever stop?
   - **Problem 2:** put a breakpoint in `discountCents` and debug *takes 10 % off for WELCOME10*. Use Evaluate Expression on `percent / 100`, then on `subtotalCents * percent / 100`.
   - **Problem 3:** add an exception breakpoint for `NullPointerException` and debug *lets a customer without a coupon check out*. When the debugger stops, what's the value of `couponCode`, and which caller passed it?
4. Fix that one bug, run all the tests again, and go on to the next problem.

<details>
<summary>Hint 1</summary>

Count the rounds of the loop for an array of two lines. Which index does it never reach? A for-each loop can't make this mistake.
</details>

<details>
<summary>Hint 2</summary>

What is `10 / 100` in Java? Change the order of the operations so that nothing is lost, and check that the result is still rounded down: 10 % of 1995 cents must be 199 cents.
</details>

<details>
<summary>Hint 3</summary>

`null` means "no coupon". Handle it before you call any method on `couponCode`. Make sure that `" welcome10 "` still works after your fix.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Which test would have caught each bug before the release? Which of the three bugs would a for-each loop, multiplying first, or a `null` check at the start have prevented when the code was first written?

## Review questions

1. Why is `"java" == new String("java")` false, and what do you write instead?
2. What is `7 / 2` in Java, and how do you get `3.5`?
3. Name two differences between `int` and `Integer`.
4. Given `final int[] scores = {1, 2};`, can you write `scores[0] = 5;`? Can you write `scores = new int[3];`?
5. A method receives a `StringBuilder` and assigns a new `StringBuilder` to its parameter. Does the caller see the new object?
6. When does a `switch` expression need no `default` case, and why is that an advantage?
7. A stack trace has two `Caused by:` sections. Where do you start reading?
8. Why is `Math.abs(Integer.MIN_VALUE)` negative?
9. Why shouldn't `max(int[] values)` sort the array it receives?
10. When would you write a compact source file with `void main()`, and when a class in a package?

<details>
<summary>Answers</summary>

1. `==` compares references, and `new String(...)` always creates a new object. Compare the contents with `equals()`: `"java".equals(text)`.
2. `3`: dividing two `int`s cuts off the fraction. Make one operand a `double`: `7 / 2.0` or `(double) 7 / 2`.
3. `int` is a primitive that holds the value and can't be `null`. `Integer` is an object that can be `null`, can go into collections, and must be compared with `equals()`. Unboxing a `null` `Integer` throws `NullPointerException`.
4. Yes: `final` only stops you from assigning the variable again; the array's contents can change. No: that assigns the variable again, which doesn't compile.
5. No. Java passes a copy of the reference. Assigning to the parameter changes only the method's copy. Changing the object through the parameter, with `append`, for example, is visible to the caller.
6. When the cases cover every possible value, for example every constant of an enum. Without `default`, the compiler reports a missing case when someone adds a new constant, instead of the code quietly taking the `default` path.
7. At the last `Caused by:`, which is usually the root cause. In that section, look for the first line from your own code.
8. The `int` range goes from -2,147,483,648 to 2,147,483,647. The positive value 2,147,483,648 doesn't exist, so the negation overflows and wraps around to the same negative number.
9. The caller still owns the array and doesn't expect `max` to change it. Sorting would also be slower than one pass: O(n log n) instead of O(n).
10. A compact source file suits a script or a quick experiment that runs with `java Hello.java`. Application code, which has many classes, tests and a build, lives in classes in packages.
</details>

## Further reading

- Official docs: [Learn Java on dev.java](https://dev.java/learn/) ("Getting Started" and "Language Basics") · [Java 25 API](https://docs.oracle.com/en/java/javase/25/docs/api/) · [The Java Language Specification](https://docs.oracle.com/javase/specs/jls/se25/html/index.html)
- API pages you'll use often: [`String`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/String.html), [`Character`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Character.html), [`Arrays`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Arrays.html), [`Formatter` syntax](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Formatter.html#syntax)
- The JEPs behind this module's newer features: [JEP 512: Compact Source Files and Instance Main Methods](https://openjdk.org/jeps/512) · [JEP 458: Launch Multi-File Source-Code Programs](https://openjdk.org/jeps/458) · [JEP 361: Switch Expressions](https://openjdk.org/jeps/361) · [JEP 378: Text Blocks](https://openjdk.org/jeps/378) · [JEP 358: Helpful NullPointerExceptions](https://openjdk.org/jeps/358)
- [IntelliJ IDEA: Debug code](https://www.jetbrains.com/help/idea/debugging-code.html)

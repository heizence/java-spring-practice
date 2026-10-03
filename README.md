# Java & Spring Boot Practice

A hands-on course for becoming fluent in Java and Spring Boot. Every module has a short lesson and a set of exercises sized for 30- or 60-minute sessions. Tests score your work, and reference solutions show how experienced developers would write it.

| Part | Content | Status |
|---|---|---|
| **1 · Java** | From syntax to concurrency, reflection and a mini dependency-injection container. 10 modules, about 6 weeks. | ✅ ready |
| **2 · Spring Boot** | Core Spring on one bookstore application with MySQL: IoC/DI, AOP, REST, JPA, transactions and ACID, locking, events, caching, security, testing. 14 modules, about 7 weeks, plus an optional capstone. | ✅ ready |

## Start here

1. **Set up your machine** (once, about 15 minutes): [docs/setup.md](docs/setup.md)
2. **Open [PROGRESS.md](PROGRESS.md)** and start with J01-01.

## A practice session

**Weekdays, 30 minutes:**

1. Pick the next unchecked exercise in [PROGRESS.md](PROGRESS.md).
2. Read its section in the module's README. Skim the lesson parts it builds on.
3. Open the exercise file. Its Javadoc lists the rules, including edge cases. Write your code.
4. Run the tests: click ▶ next to the test class in IntelliJ, or run `./score j01-01`.
5. When everything passes, compare your code with the reference solution, tick the box, and commit.

**Weekends, 60 minutes:** one 60-minute exercise, or two 30-minute ones.

Falling behind a week is fine. Consistency matters more than speed.

## Four kinds of exercise

| Type | What you do |
|---|---|
| **Build** | Implement methods marked `TODO` until the tests pass. Most exercises. |
| **Predict** | Read small puzzles, write down what you think they return, then run the tests to check. |
| **Fix** | Realistic code has bugs. Find them (the debugger helps) and fix them. |
| **Write tests** | The code is correct; you write the tests. Your tests are scored by how many hidden bugs they catch. |

## Your score

```bash
./score            # every module
./score j05        # one module
./score j05-04     # one exercise, with every failing test and its message
```

Example output:

```
J05 · Collections and data structures                                 3/9 done
  ✔ J05-01  Collection traps and Big-O                                   21/21
  ◐ J05-04  Hash table                                                    9/15
      Basic 4/4 · Edge cases 2/5 · Errors 3/3 · Randomized 0/3
      ✗ Edge cases › resize keeps every entry reachable
  ○ J05-05  Binary search tree                                            0/14
```

✔ done · ◐ in progress · ○ not started · ★ optional

- **Tests are grouped** into *Basic*, *Edge cases* and *Errors* (plus *Randomized* or *Concurrency* where it fits). Edge cases are where most real bugs live, so they count as much as the basics.
- **Failing test names are hints.** Read them before opening the test code.
- **Write tests exercises** run your tests against several broken versions of the code. Each broken version your tests catch counts. Don't open `src/mutants/` until you're done: it's the answer key.
- Behind the scenes, every exercise's tests were themselves checked against deliberately broken solutions, so passing them means your code really works.

## Branches

You work on **`my-work`**. `main` holds the fresh exercises and `solutions` the reference solutions. To compare your file with the reference in IntelliJ, right-click the file → **Git → Compare with Branch…** → `solutions`. More in [docs/handbook/git-workflow.md](docs/handbook/git-workflow.md).

## What's where

| Path | Contents |
|---|---|
| [PROGRESS.md](PROGRESS.md) | your checklist, week by week |
| `java/jNN-*/README.md` | lesson and exercises for each Java module |
| `java/jNN-*/src/main/java` | the code you write or read |
| `java/jNN-*/src/test/java` | the tests |
| [docs/handbook/](docs/handbook/README.md) | conventions good teams follow, with reasons |
| [docs/setup.md](docs/setup.md) | machine setup and troubleshooting |
| [notes/](notes/README.md) | your own notes and your "looked it up" log |
| [spring/](spring/README.md) | Part 2: the Spring modules, [BOOKSTORE.md](spring/BOOKSTORE.md) and the capstone |

## Course map

| Week | Modules |
|---|---|
| 1 | [J01 Java for TypeScript developers](java/j01-basics/README.md) · start of [J02 Object-oriented Java](java/j02-oop/README.md) |
| 2 | J02 (rest) · [J03 Exceptions and unit testing](java/j03-exceptions-testing/README.md) |
| 3 | [J04 Generics](java/j04-generics/README.md) · start of [J05 Collections and data structures](java/j05-collections/README.md) |
| 4 | J05 (rest) · [J06 Functional Java](java/j06-functional/README.md) |
| 5 | [J07 Everyday APIs](java/j07-everyday-apis/README.md) · start of [J08 Concurrency](java/j08-concurrency/README.md) |
| 6 | J08 (rest) · [J09 Reflection, annotations and proxies](java/j09-reflection-mini-spring/README.md) · [J10 JVM essentials](java/j10-jvm/README.md) |
| 7 | [S01 Spring Boot fundamentals](spring/s01-boot-fundamentals/README.md) · [S02 Configuration](spring/s02-configuration/README.md) · start of [S03 AOP](spring/s03-aop/README.md) |
| 8 | S03 (rest) · [S04 REST APIs](spring/s04-rest-apis/README.md) |
| 9 | S04 (rest) · [S05 Database foundations](spring/s05-database-foundations/README.md) · start of [S06 Spring Data JPA](spring/s06-spring-data-jpa/README.md) |
| 10 | S06 (rest) · start of [S07 Transactions](spring/s07-transactions/README.md) |
| 11 | S07 (rest) · [S08 Concurrency and data integrity](spring/s08-concurrency-integrity/README.md) · start of [S09 Events and async](spring/s09-events-async/README.md) |
| 12 | S09 (rest) · [S10 Caching](spring/s10-caching/README.md) · [S11 HTTP clients](spring/s11-http-clients/README.md) · start of [S12 Security](spring/s12-security/README.md) |
| 13 | S12 (rest) · [S13 Testing strategy](spring/s13-testing-strategy/README.md) · [S14 Production readiness](spring/s14-production-readiness/README.md) |
| after 13 | [Capstone ★](spring/capstone/README.md): a small API from an empty folder |

## Habits that make this work

- **Keep AI code completion off in this project.** The practice is in your own typing and thinking. Use AI as a tutor or reviewer instead: ask it to explain a concept, or to review your finished solution against the [handbook](docs/handbook/README.md).
- **Read the rules first.** The Javadoc of each exercise class is the spec.
- **When you're stuck for more than 10 minutes**, read the hints in the README, then the test code, then the reference solution. If you needed the solution, redo the exercise from memory the next day.
- **Log what you look up** in [notes/lookup-log.md](notes/lookup-log.md). Anything that shows up twice deserves a flashcard.

## Updating the material

New modules and fixes arrive on `main`:

```bash
git checkout my-work
git merge main
```

Maintainers: see [CLAUDE.md](CLAUDE.md).

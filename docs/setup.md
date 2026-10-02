# Setup

Do this once. It takes about 15 minutes, plus download time.

## 1. Install JDK 25

```bash
brew install --cask temurin@25
```

It asks for your macOS password. Check the result:

```bash
/usr/libexec/java_home -v 25
```

(The build can download JDK 25 by itself, so `./score` works even without this step. IntelliJ is easier to set up with a normally installed JDK, though.)

## 2. Update IntelliJ IDEA

Java 25 needs a recent IntelliJ IDEA. Versions from before Java 25's release (September 2025) don't understand its newest syntax. Update through **IntelliJ IDEA → Check for Updates…**, or download the latest version from jetbrains.com.

## 3. Open the project

1. **File → Open…** and select the `java-spring-practice` folder. Choose **Trust Project**.
2. IntelliJ detects the Gradle build and imports it. The first import takes a minute or two.
3. **File → Project Structure → Project → SDK**: choose the JDK 25 you installed.
4. **Settings → Build, Execution, Deployment → Build Tools → Gradle**: set *Gradle JVM* to *Project SDK*, and keep *Build and run using* and *Run tests using* on **Gradle**. Running through Gradle records your results for `./score`.

## 4. Turn off AI code completion for this project

The practice is in typing and thinking for yourself. In **Settings**:

- **Editor → General → Inline Completion** (called *Code Completion* in some versions): turn off full-line and AI suggestions.
- **Plugins**: disable AI Assistant, GitHub Copilot, or any similar plugin for this project. Many plugins offer "Disable for this project" or a status-bar switch.

The menu names vary a little between IntelliJ versions. Search **Settings** for "completion" or "AI".

## 5. Check that everything works

In a terminal, in the project folder:

```bash
./score
```

The first run downloads Gradle and the libraries, so it takes a few minutes. You should see every module with `○` (not started).

Then, in IntelliJ, open `java/j01-basics/src/test/java/practice/j01/ex02/PuzzlesTest.java` and click the ▶ next to the class name. The tests run and fail, because you haven't written any answers yet. That's correct.

## 6. Git

You're on the `my-work` branch. Check with:

```bash
git branch --show-current
```

## Later: Docker (Part 2)

Part 2 uses MySQL in Docker, both for running the application and in tests. Docker Desktop is enough. You don't need it for Part 1.

## Troubleshooting

| Problem | Fix |
|---|---|
| `./score` says Gradle stopped with an error | Read the end of `build/score.log`. Network problems during the first download are the usual cause; run it again. |
| IntelliJ shows red code everywhere after opening | Wait for the Gradle import to finish, then **File → Invalidate Caches…** if it's still red. Check that the project SDK is 25. |
| "Unsupported class file major version" or similar | IntelliJ's Gradle JVM is older than Java 21. Set it to the Project SDK (step 3.4). |
| A module "doesn't compile" in `./score` | Run the command it prints, for example `./gradlew :java:j05-collections:compileTestJava`, to see the compiler errors. A typo in one exercise blocks only its own module. |
| Tests pass in IntelliJ but `./score` doesn't show it | IntelliJ ran the tests without Gradle. Set *Run tests using* to Gradle (step 3.4), or run `./score`. |

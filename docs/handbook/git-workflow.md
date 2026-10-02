# Git workflow

## Branches in this repository

| Branch | What's on it | You… |
|---|---|---|
| `my-work` | your answers | work here, always |
| `main` | the exercises, unsolved | merge it into `my-work` to get updates |
| `solutions` | the reference solutions | compare against it after you finish an exercise |

(There's also an `authoring` branch that the material is generated from. You can ignore it.)

### Compare your solution with the reference

- **IntelliJ:** right-click a file → **Git → Compare with Branch…** → `solutions`.
- **Terminal:**
  ```bash
  git diff solutions -- java/j05-collections/src/main/java/practice/j05/ex09
  ```
  or show the reference file on its own:
  ```bash
  git show solutions:java/j05-collections/src/main/java/practice/j05/ex09/LruCache.java
  ```

### Get updates to the material

```bash
git checkout my-work
git merge main
```

If an update touches a file you've changed, Git asks you to resolve a conflict. Keep your version of your solution code, and take the new version of tests and READMEs.

### Start an exercise over

```bash
git checkout main -- java/j05-collections/src/main/java/practice/j05/ex09
```

## Commit habits that teams expect

- **Small commits, one logical change each.** "Implement LRU cache" is one commit, not "LRU cache + fix typo in README + rename stuff".
- **Write messages in the imperative mood**, like a command: "Add LRU cache", not "Added" or "Adds". Many teams use [Conventional Commits](https://www.conventionalcommits.org/):
  ```
  feat(j05): implement LRU cache with LinkedHashMap

  Access order + removeEldestEntry keeps the bookkeeping in the JDK.
  ```
  The first line is a summary of about 50 characters. The body (optional) explains *why*.
- **Commit working code.** In a team, every commit on a shared branch should build and pass its tests.
- **Never commit secrets** (passwords, API keys, tokens), build output, or IDE settings. `.gitignore` handles the last two here.

A practical rhythm for this course: one commit per finished exercise, for example `feat(j05-09): LRU cache`. Your history becomes a learning log.

## Pushing to GitHub

1. Create an **empty private** repository on GitHub, without a README.
2. Check which identity your commits will use. Your global Git settings may hold a work email:
   ```bash
   git config user.email
   ```
   To use a different email for this repository only:
   ```bash
   git config user.email "you@example.com"
   ```
3. Connect and push your branches:
   ```bash
   git remote add origin git@github.com:<you>/java-spring-practice.git
   git push -u origin my-work main solutions
   ```

The repository includes a GitHub Actions workflow (`.github/workflows/ci.yml`) that compiles everything on every push.

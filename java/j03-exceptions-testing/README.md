# J03 · Exceptions and unit testing

⏱ 4 sessions · week 2 · 4 exercises

<!-- AUTHOR: intro, lesson, TypeScript → Java table, "In real projects", and exercises J03-01, J03-02, J03-04 go here.
     Keep the J03-03 section below (it is the format example for Write tests exercises). -->

## Exercises

### J03-03 · Write tests: PasswordPolicy

**Write tests** · 30 min · [`PasswordPolicy.java`](src/main/java/practice/j03/ex03/PasswordPolicy.java) → tests in [`PasswordPolicyTest.java`](src/test/java/practice/j03/ex03/PasswordPolicyTest.java)

This time the code is finished and correct, and the tests are your job. That's most of real-world testing: pinning down what existing code must keep doing, so nobody (including an AI assistant) can break it quietly.

**How you're scored:** `./score j03-03` runs your tests twice. First against the real `PasswordPolicy`, where every test must pass. Then against six broken versions, each with one realistic bug. A broken version is *caught* when at least one of your tests fails on it. Uncaught bugs come with a hint; caught ones reveal what the bug was.

**You'll practice:** JUnit 6 `@Test`, `@Nested` and `@DisplayName`, `@ParameterizedTest`, AssertJ's `assertThat` and `assertThatThrownBy`, choosing boundary values.

<details>
<summary>Hint 1</summary>

Start from one password that is valid, and write one test per rule that breaks *only* that rule. When a test fails, you'll know exactly which rule is wrong.
</details>

<details>
<summary>Hint 2</summary>

Bugs love boundaries. For every limit in the Javadoc, test the value just inside and just outside it.
</details>

**Done?** Compare with the reference tests on the `solutions` branch. Would someone reading only your test names understand all the rules?

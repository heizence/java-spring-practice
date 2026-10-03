# S12 · Security essentials

⏱ 6 sessions · weeks 12–13 · 6 exercises

Until now, anyone could call every endpoint of the bookstore: change prices, read other members' orders, look into their wallets. This module adds Spring Security. You decide which requests need a signed-in member and which need an administrator, store passwords safely, issue JSON Web Tokens (JWTs) when members sign in, and check those tokens on every request. You also check *ownership* in your own code, because no URL rule knows who owns an order. These are the parts of security that every backend developer writes and reviews, and their mistakes end up in incident reports. Read lesson sections 1–4 before S12-01, section 5 before S12-02, sections 6–7 before S12-03 and S12-04, sections 8–10 before S12-05, and section 11 before S12-06.

## Lesson

### 1. The filter chain

Spring Security is not part of your controllers. It is a chain of *servlet filters*: objects that see every HTTP request before Spring MVC's `DispatcherServlet` does, and that can stop the request there. NestJS has the same idea in middleware and guards.

```
request → Tomcat → FilterChainProxy ─┬─ SecurityContextHolderFilter      makes room for "who is calling"
                                     ├─ BearerTokenAuthenticationFilter  reads "Authorization: Bearer …"
                                     ├─ AnonymousAuthenticationFilter    no token? then the caller is anonymous
                                     ├─ ExceptionTranslationFilter       turns security exceptions into 401/403
                                     └─ AuthorizationFilter              checks your URL rules
                                     → DispatcherServlet → your controller
```

The real chain has more filters (headers, logout, sessions), about a dozen in this module. The ones above do the work you configure here:

- **Authentication** filters find out who is calling. `BearerTokenAuthenticationFilter` reads the token, checks it, and stores the result as an `Authentication` in the `SecurityContextHolder`. Think of the `SecurityContextHolder` as `req.user` in Express: it holds the current caller for the rest of the request. Like a `ThreadLocal`, it belongs to the thread that serves the request.
- An `Authentication` holds the *principal* (who: here, the decoded token) and the *authorities* (what they may do: strings such as `ROLE_ADMIN`).
- **Authorization** happens in `AuthorizationFilter`: it compares the request with your rules and the caller's authorities.
- Without any configuration of yours, Spring Boot secures every endpoint with a login form and a password that it prints at startup. As soon as you declare a `SecurityFilterChain` bean, your rules replace that default.

### 2. Rules: deny by default, first match wins

You configure the chain with a `SecurityFilterChain` bean. `HttpSecurity` is a builder: each method switches on or configures one part of the chain.

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
            .authorizeHttpRequests(requests -> requests
                    .requestMatchers(HttpMethod.GET, "/api/events/**").permitAll() // reading events: anyone
                    .requestMatchers(HttpMethod.POST, "/api/tickets").permitAll()  // only POST, no other method
                    .requestMatchers("/api/staff/**").hasRole("STAFF")             // a whole area, every method
                    .anyRequest().authenticated())                                  // everything else: signed in
            .build();
}
```

- **The first rule that matches decides.** Spring checks the rules from top to bottom. Put specific rules before general ones; `anyRequest()` must come last, and Spring refuses to start if it doesn't.
- **Deny by default.** End with `anyRequest().authenticated()` (or `denyAll()`). Then a new endpoint is protected until someone opens it on purpose. Do it the other way round, with a list of protected paths and `anyRequest().permitAll()`, and every path that someone forgets is open.
- **The method is part of the rule.** `requestMatchers("/api/events/**").permitAll()` opens `DELETE /api/events/7` too. Name the method when only reading is public.
- **Path patterns:** `*` matches one path segment (`/api/events/*` matches `/api/events/7`, not `/api/events/7/seats`), `**` matches any number of segments.
- **What a rule can require:** `permitAll()`, `authenticated()`, `hasRole("ADMIN")`, `hasAuthority("ROLE_ADMIN")`, `denyAll()`. `hasRole("ADMIN")` checks the authority `ROLE_ADMIN`: roles are authorities with the prefix `ROLE_`.

### 3. Authentication and authorization: 401 and 403

| Question | Name | Failure |
|---|---|---|
| Who are you? | *Authentication* | `401 Unauthorized` (the name is old; it means "unauthenticated") |
| May you do this? | *Authorization* | `403 Forbidden` |

When a rule refuses a request, `ExceptionTranslationFilter` decides what the client gets:

| Situation | Who answers | Status |
|---|---|---|
| No token, protected endpoint | the chain's `AuthenticationEntryPoint` | 401 |
| A token that is expired, forged or malformed | the resource server's own entry point (section 7) | 401 |
| A valid token without the required role (URL rule) | the chain's `AccessDeniedHandler` | 403 |
| A valid token, and `@PreAuthorize` refuses (section 8) | your code throws `AccessDeniedException` | 403 |
| A valid token, but someone else's order (section 9) | your service | 404 |

A `401` must tell the client how to authenticate, in a `WWW-Authenticate` header. For bearer tokens that's `WWW-Authenticate: Bearer`.

### 4. Problem details from the filter chain

Every error of the bookstore's API is a problem detail with a `code` (S04). Security errors must be too, but there's a catch: **the filter chain runs before Spring MVC, so `@RestControllerAdvice` never sees its errors.** Spring Security has two hooks instead: the `AuthenticationEntryPoint` (401) and the `AccessDeniedHandler` (403). Both are functional interfaces, so lambdas will do. A good way is to hand their exception to Spring MVC's `HandlerExceptionResolver`, which calls your `@ExceptionHandler` methods as if a controller had thrown it:

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http,
        @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) throws Exception {
    AuthenticationEntryPoint unauthorized = (request, response, exception) ->
            resolver.resolveException(request, response, null, exception);
    return http
            // …
            .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(unauthorized))
            .build();
}
```

`@Qualifier` picks the right bean: Spring Boot has a second `HandlerExceptionResolver` for its error page. Now `GlobalExceptionHandler` writes security errors like all others, if it has handlers for them:

```java
@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException exception) { … }
```

You need these handlers anyway, for a second reason. `@PreAuthorize` throws `AccessDeniedException` *inside* the controller call, and the template's catch-all `@ExceptionHandler(Exception.class)` would turn it into a `500`.

One more setting belongs to a token API: `sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))`. Every request carries its token, so the server keeps no HTTP session: nothing to share between instances, nothing to steal from a cookie.

### 5. Passwords: hash them with the delegating encoder

**Never store a password, only a hash of it.** A hash function is one-way: you can compute the hash from the password, but not the password from the hash. When the database leaks, and many do, the attacker has hashes, not passwords. A good password hash is also:

- **salted:** a random value is mixed in, so the same password gives a different hash for every member, and precomputed tables of common passwords don't help;
- **slow on purpose:** BCrypt with a *cost* of 10 takes tens of milliseconds. A login doesn't notice, but an attacker who tries billions of guesses does.

Spring Security's delegating encoder does all of this:

```java
PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

String hash = encoder.encode("correct-horse-7");
// {bcrypt}$2a$10$o.hMPUIrcF8nAUSfDWBRxusC1MnLwxD/bMjASLX1iCAcacZqpb7rq
//  │       │   │  └─ 22 characters of salt, then 31 characters of hash
//  │       │   └─ cost 10: 2¹⁰ rounds
//  │       └─ BCrypt version
//  └─ which encoder made it: the delegating encoder reads this prefix

encoder.matches("correct-horse-7", hash); // true
```

- Use `matches`, never `encode` and compare: the salt makes every `encode` different.
- The prefix makes the encoder *delegating*: it checks every hash with the algorithm named in its prefix. When BCrypt becomes too weak, new hashes use a stronger algorithm, and old hashes keep working; the application can hash each password again, with the new algorithm, the next time its member signs in. `new BCryptPasswordEncoder()` writes no prefix and loses that path. The seed members' hashes have the prefix, so they need the delegating encoder.
- **BCrypt uses at most 72 bytes** of a password. Spring Security 7 refuses longer passwords with an `IllegalArgumentException`, which becomes a `500` if you don't check first. Count bytes, not characters: a Korean letter takes 3 bytes in UTF-8, so 25 of them are already too long.
- Passwords and hashes never appear in a response or a log line. A record's generated `toString()` prints every component, so request records with a password override it.

### 6. JSON Web Tokens

A *JSON Web Token* (JWT) is three Base64url-encoded parts, separated by dots: a header, the *claims*, and a signature.

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJib29rc3RvcmUiLCJzdWIiOiIxIiwicm9sZSI6IlVTRVIiLCJpYXQiOjE3OTIwMjI0MDAsImV4cCI6MTc5MjAyMzMwMH0.<signature>
```

Decoded, the first two parts are plain JSON:

```
{"alg":"HS256","typ":"JWT"}
{"iss":"bookstore","sub":"1","role":"USER","iat":1792022400,"exp":1792023300}
```

- **Registered claims:** `iss` (issuer: who made the token), `sub` (subject: whom it describes), `iat` (issued at) and `exp` (expires at), in seconds since 1970: this token was issued at 2026-10-15T00:00:00Z and expires 15 minutes later. Your own claims, such as `role`, sit next to them.
- **Anyone can read the claims**: they are encoded, not encrypted. Never put secrets or personal data in them. The subject is the member's id, not their email.
- **The signature protects them.** The server signs the header and claims with a key. Change one character, and the signature no longer matches. With `HS256` (HMAC with SHA-256), the same secret signs and checks; the bookstore does both, so that is enough. When another service issues the tokens, it signs with a private key (`RS256`) and publishes the public key, so that others can check without being able to sign.
- **Short-lived:** a server can't take back a token it issued. A stolen token works until it expires, so tokens live minutes, not days. Real systems add *refresh tokens* to get new access tokens without a new password; this module leaves them out.

Spring Security issues tokens with a `JwtEncoder`:

```java
@Bean
JwtEncoder jwtEncoder(JwtProperties properties) {
    return NimbusJwtEncoder.withSecretKey(properties.secretKey()).algorithm(MacAlgorithm.HS256).build();
}

JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("bookstore")
        .subject("1")
        .claim("role", "USER")
        .issuedAt(now)
        .expiresAt(now.plus(Duration.ofMinutes(15)))
        .build();
String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
```

- **The secret is configuration** (`bookstore.security.jwt.*`, bound to `JwtProperties`), never code. `application.yml` has a key for local development only; production sets `BOOKSTORE_SECURITY_JWT_SECRET`. HS256 needs at least 256 bits (32 bytes) of random key.
- **Mind the clock.** The bookstore takes "now" from its `Clock` bean, and tests stop that clock at 2026-10-15T00:00:00Z. Spring Security's token checks read the system clock unless you hand them one. A token issued "at 2026-10-15" and checked against your laptop's clock is valid or expired depending on the day you run the tests. Use one clock for both: section 7.
- **Signing in reveals nothing.** An unknown email and a wrong password get the same answer. Otherwise anyone can find out who has an account, and then guess passwords for those accounts only.

### 7. Checking tokens: the resource server

In OAuth 2.0 terms, an API that accepts access tokens is a *resource server*. One line switches it on, and a `JwtDecoder` bean says how to check tokens:

```java
http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));

@Bean
JwtDecoder jwtDecoder(JwtProperties properties) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(properties.secretKey())
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("bookstore"));
    return decoder;
}
```

For each request with `Authorization: Bearer …`, `BearerTokenAuthenticationFilter` asks the decoder to check the token. The decoder accepts only the algorithm you named (never `alg: none`), checks the signature, and then runs the *validators*:

- `JwtValidators.createDefaultWithValidators(…)` adds the default checks to yours: the `typ` header, and the timestamps `exp` and `nbf`, with 60 seconds of leeway for clocks that differ a little. If you pass a timestamp validator of your own, it takes the place of the default one.
- `JwtTimestampValidator` reads the system clock unless you call `setClock(clock)`. It also accepts a token without `exp`, which would be valid forever, unless you call `setAllowEmptyExpiryClaim(false)`.
- `JwtIssuerValidator` checks `iss`.

A valid token becomes a `JwtAuthenticationToken` in the `SecurityContextHolder`. Its authorities come from a `JwtAuthenticationConverter`, which by default reads the OAuth `scope` claim and makes `SCOPE_read` of it. The bookstore's tokens have a `role` claim instead:

```java
var authorities = new JwtGrantedAuthoritiesConverter();
authorities.setAuthoritiesClaimName("role");  // read "role": "ADMIN"…
authorities.setAuthorityPrefix("ROLE_");      // …and make ROLE_ADMIN of it, which hasRole("ADMIN") checks
```

Declare the `JwtAuthenticationConverter` as a bean, and the resource server uses it. Spring Boot can also build it from `spring.security.oauth2.resourceserver.jwt.authorities-claim-name` and `authority-prefix`.

Two more things to know:

- **An invalid token never reaches the chain's entry point.** The resource server answers it with its own entry point: a `401` with an empty body. Pass yours in: `oauth2ResourceServer(resourceServer -> resourceServer.jwt(…).authenticationEntryPoint(unauthorized))`.
- **A controller gets the token with `@AuthenticationPrincipal`:** `public MemberResponse me(@AuthenticationPrincipal Jwt jwt)`, then `jwt.getSubject()` is the member's id. Take who is calling from the token, never from the request.

Spring Security 7 also answers `GET /.well-known/oauth-protected-resource` for every resource server: public metadata that tells clients how to send tokens. It contains no secrets.

### 8. Method security: `@PreAuthorize`

URL rules protect paths. *Method security* protects Java methods, wherever they are called from:

```java
@Configuration
@EnableMethodSecurity            // once, on a configuration class
public class SecurityConfiguration { … }

@PreAuthorize("hasRole('ADMIN')")
@Transactional
public BookResponse changePrice(long id, long price) { … }
```

- Spring wraps the bean in a proxy, like `@Transactional` (S03). The proxy evaluates the expression first and throws `AuthorizationDeniedException`, a kind of `AccessDeniedException`, if it's false. The usual proxy rules apply: calling the method from inside the same class skips the check.
- **URL rules or `@PreAuthorize`?** URL rules suit whole areas: `/api/admin/**`. `@PreAuthorize` suits single operations that live among public ones, such as changing a book's price under `/api/books`. On a service method, it protects every caller, including a new endpoint that someone adds next year. Many teams use both.
- **`hasRole('ADMIN')` is not `hasAuthority('ADMIN')`.** The authority is `ROLE_ADMIN`, so `hasAuthority('ADMIN')` refuses even administrators.

### 9. Ownership: the check only your code can make

A URL rule can say "signed-in members may read orders". It can't say "…but only their own", because it doesn't know who owns order 7. Your service does:

```java
Order order = orderRepository.findById(orderId).orElseThrow(…);
if (!order.getMember().getId().equals(memberId)) {
    throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, …); // the same answer as for a missing order
}
```

- Forgetting this check has a name: **IDOR**, insecure direct object reference. Then any member can change the id in `/api/orders/7` and read other people's orders. In the OWASP list of API risks, it is number one.
- **Answer `404`, exactly as for an order that doesn't exist.** A `403` confirms that the order exists, and anyone can count your orders by trying ids.
- **Who is asking comes from the token.** Never trust a member id from the path, a query parameter or the request body. Attackers send parameters that your API doesn't document.
- Filtering in the query works too: `findByIdAndMemberId(orderId, memberId)` can't forget the check, because the query is the check.

### 10. CSRF and CORS for a token API

**CSRF** (cross-site request forgery) tricks a browser into sending a request to your site with the browser's own credentials: another site's page submits a form to `bank.example/transfer`, and the browser attaches the bank's session cookie by itself. Spring Security protects against it by default: requests that change something must carry a CSRF token. A **stateless API that accepts only `Authorization: Bearer` tokens isn't open to CSRF**, because a browser never adds that header by itself. Such an API turns CSRF protection off: `csrf(csrf -> csrf.disable())`. Keep it on for anything that authenticates with cookies, sessions included.

**CORS** (cross-origin resource sharing) controls something else: which other web sites' JavaScript may read your API's responses. A browser lets JavaScript from `https://shop.example` read responses from `https://api.example` only if the API allows that origin. It asks first, with a *preflight* `OPTIONS` request. With Spring Security, define the rules as a bean, and the chain applies them before its own rules:

```java
@Bean
UrlBasedCorsConfigurationSource corsConfigurationSource() {
    var cors = new CorsConfiguration();
    cors.setAllowedOrigins(List.of("https://shop.example"));         // exact origins, never "*" with credentials
    cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
    cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", cors);
    return source;
}
```

CORS protects browsers' users, not your server: `curl` ignores it. Your authorization rules still do the real work.

### 11. Testing security with spring-security-test

`spring-security-test` signs a test request in without a real token:

```java
mvc.get().uri("/api/wallet").with(jwt()
        .jwt(token -> token.subject("1").claim("role", "USER"))
        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
```

- `jwt()` puts a `JwtAuthenticationToken` straight into the security context. No token is signed or decoded, so it tests your rules, not your decoder. It doesn't run your `JwtAuthenticationConverter` either: give the authorities explicitly. It also skips CSRF checks, as a request with a bearer token would.
- This module's `TestUsers` wraps it for the seed members: `.with(TestUsers.minji())`, `.with(TestUsers.admin())`.
- `@WithMockUser` on a test method signs the whole method in, as a user with the role `USER` by default, for calling a service directly: `assertThatThrownBy(() -> bookService.changePrice(1, 1000)).isInstanceOf(AccessDeniedException.class)`. `@WithMockUser(roles = "ADMIN")` signs in an administrator.
- To test the decoder itself, send real tokens: `TestTokens` signs them with the configured secret, and can also sign them wrongly on purpose.

**What to test:** every endpoint as every kind of caller, nobody (401), a member (only their own data) and an administrator. Test both directions of each rule: the administrator *can*, the member *can't*. A test that checks only one direction passes when the rule refuses everybody, or nobody. Send what an attacker would send: someone else's id, an undocumented parameter, a negative amount. Then check the database, not only the status code.

## TypeScript/NestJS → Spring

| NestJS / Express | Spring Security | Watch out for |
|---|---|---|
| Express middleware, `app.use(…)` | servlet filters in a `SecurityFilterChain` | Your bean replaces Spring Boot's default chain. |
| `@UseGuards(AuthGuard('jwt'))` on a controller | `anyRequest().authenticated()` and the resource server | Spring protects by rules, so end with a rule for everything else. |
| a guard that checks `@Roles('admin')` | `hasRole("ADMIN")` in a URL rule, or `@PreAuthorize("hasRole('ADMIN')")` | Needs `@EnableMethodSecurity`; the authority is `ROLE_ADMIN`. |
| `req.user` | `@AuthenticationPrincipal Jwt jwt`, `SecurityContextHolder` | Bound to the request's thread. |
| `jwtService.sign(payload)` (`@nestjs/jwt`) | `jwtEncoder.encode(JwtEncoderParameters.from(claims))` | Claims are readable by anyone. |
| `passport-jwt` with `ExtractJwt.fromAuthHeaderAsBearerToken()` | `oauth2ResourceServer(…jwt(…))` and a `JwtDecoder` bean | The validators decide what "valid" means: check `exp`, `iss`, the clock. |
| `bcrypt.hash(password, 10)`, `bcrypt.compare` | `passwordEncoder.encode`, `passwordEncoder.matches` | The delegating encoder adds `{bcrypt}`; 72 bytes at most. |
| `UnauthorizedException`, `ForbiddenException` | `AuthenticationException`, `AccessDeniedException` | Thrown in a filter, they never reach `@RestControllerAdvice`. |
| `csurf` | CSRF protection, on by default | Off only for stateless bearer-token APIs. |
| `app.enableCors({ origin })` | a `UrlBasedCorsConfigurationSource` bean | Exact origins, especially with credentials. |
| `supertest` with a real token | `with(jwt())`, `@WithMockUser`, or real tokens | `jwt()` skips your decoder and converter. |

## In real projects

- Configure security in one `SecurityFilterChain`, deny by default, and name the HTTP method of every public rule. See [Security essentials](../../docs/handbook/security.md).
- Write 401 and 403 as the same problem details as every other error, from one `@RestControllerAdvice`. See [REST API design](../../docs/handbook/rest-api-design.md).
- Store passwords only through the delegating `PasswordEncoder`, and never log or return them or their hashes. See [Security essentials](../../docs/handbook/security.md) and [Logging and observability](../../docs/handbook/logging-and-observability.md).
- Keep signing keys out of the code: `@ConfigurationProperties` with a development default, overridden by an environment variable or a secret manager in production. See [Configuration](../../docs/handbook/configuration.md).
- Take "who is calling" from the token, and check ownership in the service, with `404` for other people's data. See [Spring layering](../../docs/handbook/spring-layering.md) and [Security essentials](../../docs/handbook/security.md).
- Test the matrix of callers and endpoints, both directions of every rule, with `@IntegrationTest` and spring-security-test. See [Testing Spring applications](../../docs/handbook/spring-testing.md).
- If you rely on Spring Boot's error page, permit its `/error` path. Since Spring Security 6, the URL rules also apply to the error page, so an anonymous caller who runs into an error gets `401` instead of the real error. The bookstore answers every error with its own problem details, so it doesn't need that rule.

## Exercises

The tests sign requests in with `TestUsers` (spring-security-test's `jwt()`), so S12-01, S12-05 and S12-06 don't need real tokens. S12-04's tests send real ones, signed by `TestTokens`. Do the tickets in order: each one builds on the filter chain from S12-01.

### S12-01 · The security filter chain

**Build** · 30 min · [`SecurityConfiguration.java`](src/main/java/com/example/bookstore/common/security/SecurityConfiguration.java) and [`GlobalExceptionHandler.java`](src/main/java/com/example/bookstore/common/error/GlobalExceptionHandler.java)

> **Ticket:** The API has no security yet: anyone can call every endpoint, the admin endpoints included. Lock it down. Clients will authenticate with bearer tokens, so switch on the resource server now; S12-03 issues the tokens and S12-04 configures how they're checked.

**Acceptance criteria** (the Javadoc of `securityFilterChain` has the same list):

- `GET /api/books/**` is open to everyone. Any other method on books needs a token.
- `POST /api/members` (sign-up) and `POST /api/auth/token` (sign-in) are open to everyone, with no other method.
- `/api/admin/**` is for the role `ADMIN`, with any method.
- Everything else needs a signed-in member, also paths that no controller handles: `401`, not `404`.
- A `401` is a problem detail with code `UNAUTHORIZED` and the header `WWW-Authenticate: Bearer`. A `403` is a problem detail with code `FORBIDDEN`, also when `@PreAuthorize` refuses (try `GET /api/wallets/2` as Minji). The two stubs in `GlobalExceptionHandler` describe both.
- Requests carry their token, so the chain creates no HTTP session and asks for no CSRF token.

**Before you start:** lesson sections 1–4.

**You'll practice:** `SecurityFilterChain`, `authorizeHttpRequests` and the order of rules, `requestMatchers` with HTTP methods, `oauth2ResourceServer`, `AuthenticationEntryPoint` and `AccessDeniedHandler`, `@ExceptionHandler` for security exceptions, stateless sessions.

<details>
<summary>Hint 1</summary>

The handbook's [Security essentials](../../docs/handbook/security.md) page has a chain with most of the parts. Start from it, then add the rule for sign-up and sign-in, and the exception handling.
</details>

<details>
<summary>Hint 2</summary>

`AuthenticationEntryPoint` and `AccessDeniedHandler` both take `(request, response, exception)`. Write each as a lambda that calls `resolver.resolveException(request, response, null, exception)`, and pass them to `exceptionHandling(…)`.
</details>

<details>
<summary>Hint 3</summary>

`ResponseEntity.status(…).header(HttpHeaders.WWW_AUTHENTICATE, "Bearer").body(…)` builds a response with a header. `GlobalExceptionHandler` has a private `problemDetail(…)` method for the body.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: What does a request for a path that doesn't exist get, with a token and without one? Which of your rules would have opened `DELETE /api/books/1` if you had left out `HttpMethod.GET`?

### S12-02 · Password hashing and sign-up

**Build** · 30 min · [`MemberService.java`](src/main/java/com/example/bookstore/member/MemberService.java) and the `passwordEncoder` bean in [`SecurityConfiguration.java`](src/main/java/com/example/bookstore/common/security/SecurityConfiguration.java)

> **Ticket:** Implement sign-up: `POST /api/members` with `{"email": "yuri@example.com", "name": "Yuri Han", "password": "…"}`. `MemberController` is done; it answers `201 Created` with what `MemberService.signUp` returns.

**Acceptance criteria:**

- The password is stored only as a hash from the delegating encoder: `{bcrypt}$2a$10$…`. Neither the password nor its hash ever appears in the response.
- An email that is already registered, in any letter case (`Minji@Example.COM`), is `409 DUPLICATE_EMAIL`, and nothing is saved.
- A password longer than 72 bytes in UTF-8 is `400 INVALID_INPUT`, also when it has fewer than 72 characters.
- The new member is a customer (`USER`), whatever the request says, and gets an empty wallet: every member has one.

**Before you start:** lesson section 5.

**You'll practice:** `PasswordEncoder` and `PasswordEncoderFactories`, salted hashes, normalizing input before you check it, counting bytes in UTF-8.

<details>
<summary>Hint 1</summary>

`request.password().getBytes(StandardCharsets.UTF_8).length` counts bytes. Check it before you hash: BCrypt throws for longer passwords.
</details>

<details>
<summary>Hint 2</summary>

`MemberRepository.existsByEmail` finds a member by email. Normalize the email first, the way `Member` stores it. `Wallet.open(member)` creates the wallet; save it with `WalletRepository`.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Two people sign up with the same email in the same millisecond. Both pass your check; what stops the second one, and what does that client see?

### S12-03 · Issuing JWTs

**Build** · 30 min · the `jwtEncoder` bean in [`JwtConfiguration.java`](src/main/java/com/example/bookstore/common/security/JwtConfiguration.java) and [`TokenService.java`](src/main/java/com/example/bookstore/auth/TokenService.java)

> **Ticket:** Implement sign-in: `POST /api/auth/token` with `{"email": "minji@example.com", "password": "bookstore123"}` answers `{"accessToken": "eyJ…", "tokenType": "Bearer", "expiresIn": 900}`. `AuthController` is done.

**Acceptance criteria** (the Javadoc of `TokenService.issueToken` has the details):

- The token is signed with HS256 and the configured secret, and contains exactly `iss`, `sub` (the member's id), `role`, `iat` and `exp`: no email, no name.
- It is issued "now" by the `Clock` bean and expires 15 minutes later (`bookstore.security.jwt.time-to-live`).
- The email matches in any letter case.
- A wrong password and an unknown email both get `401 INVALID_CREDENTIALS`, with exactly the same body.

Mind the clock: the tests read your token's times against the stopped `TestClock`. `Instant.now()` gives today's date instead.

**Before you start:** lesson section 6. Every seed member's password is `bookstore123`.

**You'll practice:** `JwtEncoder` and `NimbusJwtEncoder`, `JwtClaimsSet`, a secret from `@ConfigurationProperties`, the `Clock` bean, error answers that reveal nothing.

<details>
<summary>Hint 1</summary>

`properties.secretKey()` turns the configured Base64 secret into the `SecretKey` that `NimbusJwtEncoder.withSecretKey(…)` needs.
</details>

<details>
<summary>Hint 2</summary>

`memberRepository.findByEmail(…)` returns an `Optional<Member>`. `passwordEncoder.matches(request.password(), member.getPasswordHash())` checks the password. One exception for both failures: `new BusinessException(ErrorCode.INVALID_CREDENTIALS)`.
</details>

<details>
<summary>Hint 3</summary>

Checking a BCrypt hash takes tens of milliseconds. If your code checks the password only when the email exists, an unknown email gets its answer faster. The reference solution checks a dummy hash for unknown emails, so that both cases take equally long.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Why is the subject the member's id and not their email? What could an attacker learn if unknown emails were answered faster?

### S12-04 · Authenticating requests with JWTs

**Build** · 30 min · the `jwtDecoder` and `jwtAuthenticationConverter` beans in [`JwtConfiguration.java`](src/main/java/com/example/bookstore/common/security/JwtConfiguration.java), [`SecurityConfiguration.java`](src/main/java/com/example/bookstore/common/security/SecurityConfiguration.java) and `me` in [`MemberController.java`](src/main/java/com/example/bookstore/member/MemberController.java)

> **Ticket:** The resource server is switched on, but it can't check a single token yet. Make it accept the bookstore's own tokens, and add `GET /api/members/me`, which returns the signed-in member.

**Acceptance criteria:**

- A token is accepted only if it is signed with HS256 and the configured secret, names the configured issuer, and has an `exp` that the application's `Clock` hasn't passed yet. Everything else is `401`: expired, signed with another key, unsigned (`alg: none`), from another issuer, without `exp`, or not a JWT at all.
- Every one of those `401`s is the same `UNAUTHORIZED` problem detail as in S12-01.
- The `role` claim becomes the request's role: an `ADMIN` token opens `/api/admin/**`, a `USER` token gets `403`.
- `GET /api/members/me` returns the member that the token's subject names, through `@AuthenticationPrincipal`.

**Before you start:** lesson section 7, and S12-01.

**You'll practice:** `JwtDecoder` and `NimbusJwtDecoder`, token validators and their clock, `JwtAuthenticationConverter`, the resource server's entry point, `@AuthenticationPrincipal`.

<details>
<summary>Hint 1</summary>

Build the validator from parts: a `JwtTimestampValidator` with `setClock(clock)` and `setAllowEmptyExpiryClaim(false)`, and a `JwtIssuerValidator`. `JwtValidators.createDefaultWithValidators(List.of(…))` adds the defaults to them.
</details>

<details>
<summary>Hint 2</summary>

Expired tokens get a `401` without a body? The resource server has its own entry point. Give it yours in `SecurityConfiguration`: `.oauth2ResourceServer(resourceServer -> resourceServer.jwt(…).authenticationEntryPoint(…))`.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: A test signs a token at 2026-10-15T00:00Z, the `TestClock`'s time. Which of your tests would fail next year if the decoder used the system clock?

### S12-05 · Authorization rules

**Build** · 30 min · [`OrderService.java`](src/main/java/com/example/bookstore/order/OrderService.java) and [`BookService.java`](src/main/java/com/example/bookstore/book/BookService.java)

> **Ticket:** A security review found two holes. First, any signed-in member can change book prices: `PUT /api/books/{id}/price` is only for administrators. Second, `GET /api/orders/{id}` is about to go live, and a member must see only their own orders.

**Acceptance criteria:**

- Only the role `ADMIN` may change a price. Anyone else who is signed in gets `403 FORBIDDEN`, and the price stays as it was. Reading books stays public. Use `@PreAuthorize` or a URL rule; the tests accept both.
- `OrderService.findOrder` returns one of the signed-in member's own orders. Another member's order is `404 ORDER_NOT_FOUND`, with exactly the same body as an order that doesn't exist (`Order 7 was not found.`).
- Administrators also see only their own orders on this endpoint: it is the customers' view.

**Before you start:** lesson sections 8 and 9.

**You'll practice:** `@PreAuthorize` and `@EnableMethodSecurity`, ownership checks, answering `404` instead of `403`, identity from the token.

<details>
<summary>Hint 1</summary>

`@EnableMethodSecurity` is already on `SecurityConfiguration`, and `GlobalExceptionHandler` turns `AccessDeniedException` into `403` since S12-01.
</details>

<details>
<summary>Hint 2</summary>

`OrderRepository.findWithLinesById` loads an order with its lines and books; `OrderResponse.from(order)` maps it. `order.getMember().getId()` reads the owner's id without loading the member.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Where did you put the price rule, and what protects the price if someone adds a second endpoint that calls `changePrice`?

### S12-06 · Write tests: security rules

**Write tests** · 30 min · [`WalletService.java`](src/main/java/com/example/bookstore/wallet/WalletService.java) and [`WalletController.java`](src/main/java/com/example/bookstore/wallet/WalletController.java) → tests in [`WalletSecurityTest.java`](src/test/java/com/example/bookstore/wallet/WalletSecurityTest.java)

> **Ticket:** The wallet API moves money between members. Before it goes live, the security team wants tests that would catch anyone who weakens its rules later.

The wallet code is finished and correct. The Javadoc of `WalletService` lists its rules: who may read which wallet, whose money a transfer moves, and which amounts are allowed. Your tests also run through your filter chain from S12-01, so finish S12-01 first.

**How you're scored:** as in J03-03. `./score s12-06` runs your tests against the real wallet code, where every test must pass, and then against five broken versions. A broken version is *caught* when at least one of your tests fails on it.

**You'll practice:** the caller matrix (nobody, member, administrator), testing both directions of a rule, sending what an attacker would send, checking the database after a refused request.

<details>
<summary>Hint 1</summary>

`.with(TestUsers.minji())` and `.with(TestUsers.admin())` sign requests in. Seed facts: Minji (member 1) has 100,000 won, the administrator (member 20) has 0.
</details>

<details>
<summary>Hint 2</summary>

Check balances in the database after a transfer: `jdbc.sql("SELECT balance FROM wallets WHERE member_id = 1")`. Call `entityManager.flush()` first, because Hibernate writes its changes only when it has to.
</details>

<details>
<summary>Hint 3</summary>

Attackers don't stick to your API's documented fields. What if a request names someone else, in a parameter or in the body?
</details>

**Done?** Compare with the reference tests on the `solutions` branch. Which of your tests would still pass if every wallet endpoint refused everybody?

## Review questions

1. What is the difference between authentication and authorization? Which status code belongs to each?
2. Why does `anyRequest().authenticated()` belong at the end of the rules, and what goes wrong with `anyRequest().permitAll()` and a list of protected paths?
3. `requestMatchers("/api/books/**").permitAll()` is meant to make the catalog public. What else does it open?
4. Why doesn't `@RestControllerAdvice` see a `401` from the filter chain, and how does this module still answer it with a problem detail?
5. Why does the bookstore store `{bcrypt}$2a$10$…` and not the password, or a SHA-256 hash of it? What is the `{bcrypt}` prefix for?
6. Anyone can read a JWT's claims. What stops a member from changing `"role": "USER"` to `"role": "ADMIN"`?
7. Why are access tokens short-lived, and why is the subject a member id rather than an email?
8. Tokens are issued with the `Clock` bean. What goes wrong when the decoder checks `exp` against the system clock?
9. A member asks for `GET /api/orders/7`, which belongs to someone else. Why `404` and not `403`?
10. Why may a stateless bearer-token API turn CSRF protection off, while an application with a login session must not?
11. A test uses `jwt()` with the claim `"role": "ADMIN"` but no `.authorities(…)`, and the admin endpoint answers `403`. Why?

<details>
<summary>Answers</summary>

1. Authentication finds out who is calling: no valid token is `401`. Authorization decides whether that caller may do this: a member without the role is `403`.
2. Spring checks the rules in order, and the first match decides, so `anyRequest()` catches whatever no earlier rule matched; after it, no rule could ever match. With `permitAll()` there, every path that someone forgets to list is open, including every new endpoint. With `authenticated()`, a forgotten path is protected.
3. Every method on those paths: `POST`, `PUT` and `DELETE` too, so anyone could change prices. Name the method: `requestMatchers(HttpMethod.GET, "/api/books/**")`.
4. The filter chain runs before Spring MVC; a request it rejects never reaches a controller or the advice. The chain's `AuthenticationEntryPoint` and `AccessDeniedHandler` hand the exception to Spring MVC's `HandlerExceptionResolver`, which runs `GlobalExceptionHandler`.
5. A leaked database must not reveal passwords. A hash is one-way; BCrypt adds a random salt (equal passwords get different hashes) and is slow on purpose (guessing billions of passwords takes too long). SHA-256 is fast, which helps the attacker. The prefix names the algorithm, so the delegating encoder can check old hashes after the bookstore moves to a stronger one.
6. The signature. It covers the header and the claims, and only the holder of the secret can compute it. A changed claim no longer matches the signature, and the decoder rejects the token.
7. A server can't take back a token it issued, so a stolen one works until it expires; short lifetimes limit the damage. Anyone can read the claims, and an email is personal data. An id also doesn't change when the member changes their email.
8. The tests issue tokens at 2026-10-15T00:00Z and expect them to be valid for 15 minutes from then. Against the system clock, they are valid only until the real time passes 2026-10-15T00:15Z: the tests pass before that day and fail after it. In production, issuing and checking with different clocks hides bugs the same way.
9. A `403` says "this order exists, but it isn't yours", which tells attackers which ids exist. `404` is the same answer as for a missing order, so it reveals nothing.
10. CSRF works because a browser attaches cookies to every request to a site, even one started by another site's page. A browser never adds an `Authorization: Bearer` header by itself, so a forged request arrives without a token. A session cookie is sent automatically, so a cookie-based application needs CSRF protection.
11. `jwt()` doesn't run the application's `JwtAuthenticationConverter`. Its default authorities come from the `scope` claim, so the request has no `ROLE_ADMIN`. Give the authorities explicitly: `.authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))`.
</details>

## Further reading

- Spring Security reference: [Architecture](https://docs.spring.io/spring-security/reference/servlet/architecture.html), the filter chain explained
- Spring Security reference: [Authorize HTTP Requests](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html) and [Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- Spring Security reference: [OAuth 2.0 Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- Spring Security reference: [Password Storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)
- Spring Security reference: [CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html) and [CORS](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html)
- Spring Security reference: [Testing with MockMvc and OAuth 2.0](https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/oauth2.html) and [Testing Method Security](https://docs.spring.io/spring-security/reference/servlet/test/method.html)
- Spring Boot reference: [Spring Security](https://docs.spring.io/spring-boot/reference/web/spring-security.html)
- [RFC 7519: JSON Web Token](https://www.rfc-editor.org/rfc/rfc7519) and [RFC 6750: Bearer Token Usage](https://www.rfc-editor.org/rfc/rfc6750)
- OWASP: [Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html), [Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html) and the [API Security Top 10](https://owasp.org/API-Security/editions/2023/en/0x11-t10/) (API1: broken object level authorization, the IDOR family)
- [jwt.io](https://jwt.io/): decode a token and see its parts (never paste a production token into a website)

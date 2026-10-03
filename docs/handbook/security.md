# Security essentials

Security mistakes are the expensive kind: leaked data, stolen accounts, legal trouble. Spring Security handles the hard cryptography for you. Your job is to configure it correctly and to check authorization in your own code.

## Authentication and authorization

- **Authentication** answers "who are you?": a password, a token. Failure → `401 Unauthorized`.
- **Authorization** answers "may you do this?": roles, ownership. Failure → `403 Forbidden` (or `404` when you don't want to reveal that the resource exists).

## The filter chain

Spring Security is a chain of servlet filters in front of your controllers. You configure it with a `SecurityFilterChain` bean:

```java
@Bean
SecurityFilterChain api(HttpSecurity http) throws Exception {
    return http
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.GET, "/api/books/**").permitAll()
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated())          // deny-by-default for everything else
            .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())                   // safe only for stateless token APIs, see below
            .build();
}
```

- **Deny by default.** End with `anyRequest().authenticated()` (or `denyAll()`), and open specific paths explicitly. The other way round, a new endpoint is public by accident.
- **Rules are checked in order; the first match wins.** Put specific matchers before general ones.

## Passwords

- **Never store passwords, only slow, salted hashes.** Use Spring Security's `PasswordEncoderFactories.createDelegatingPasswordEncoder()`. It hashes with BCrypt today and stores the algorithm with the hash (`{bcrypt}$2a$10$…`), so you can move to a stronger algorithm later.
- Never log passwords, and never return hashes in API responses.
- Compare with `passwordEncoder.matches(raw, hash)`, never by hashing and comparing strings yourself.

## Tokens (JWT)

A JSON Web Token is a signed set of claims (`sub`: who, `exp`: until when, `scope` or `roles`). The server can check the signature without a database lookup, which suits stateless APIs.

- **Keep tokens short-lived** (minutes to an hour). A stolen token works until it expires.
- **Validate everything:** signature, expiry, and issuer and audience when you use them. Spring Security's resource server does this for you.
- **Claims are readable by anyone** (they're encoded, not encrypted). Never put secrets or personal data in them.
- **The signing key is a secret:** load it from configuration and an environment variable, never from code.

## Authorization in your code

URL rules can't know who owns which order. **Check ownership in the service:**

```java
Order order = orders.findById(orderId).orElseThrow(…);
if (!order.getMemberId().equals(currentMemberId)) {
    throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, …); // 404: don't reveal that the order exists
}
```

Forgetting this check is one of the most common real vulnerabilities, called *IDOR* (insecure direct object reference): any logged-in user can read or change other users' data by changing an id in the URL.

Method security (`@EnableMethodSecurity`, then `@PreAuthorize("hasRole('ADMIN')")` on a service method) keeps role checks next to the code they protect.

## CSRF and CORS

- **CSRF** (cross-site request forgery) attacks browsers that send cookies automatically. Keep CSRF protection on for cookie- or session-based logins. A stateless API that only accepts `Authorization: Bearer` tokens isn't vulnerable, because browsers don't attach those automatically, so it may disable CSRF.
- **CORS** controls which *other* web origins may call your API from a browser. Allow your front end's exact origin, not `*` with credentials.

## Testing security

- Test that **anonymous requests get 401**, that **the wrong role gets 403**, and that **one member can't see another member's data**. These are the tests that catch real vulnerabilities.
- `spring-security-test` adds request post-processors such as `jwt()` and annotations such as `@WithMockUser` to run a test as a given user.

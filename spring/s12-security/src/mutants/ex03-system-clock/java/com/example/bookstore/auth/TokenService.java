package com.example.bookstore.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.common.security.JwtConfiguration;
import com.example.bookstore.common.security.JwtProperties;
import com.example.bookstore.member.Member;
import com.example.bookstore.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Signs members in by issuing access tokens. */
@Service
@RequiredArgsConstructor
public class TokenService {

    /**
     * The hash of a random password that belongs to nobody. For an unknown email, the password is checked against
     * this hash anyway. Checking a BCrypt hash takes a noticeable time on purpose, so without this, an unknown email
     * would get its answer faster than a wrong password, and response times would reveal which emails are
     * registered.
     */
    private static final String UNKNOWN_MEMBER_HASH =
            "{bcrypt}$2a$10$xG3zpzAkB/x9B3JwvIm27.HXS3MM9ZZN3tkxtLwjuIUQpuf1eGBAu";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;

    /**
     * Checks a member's email and password, and issues an access token: the use case behind
     * {@code POST /api/auth/token}.
     *
     * <ul>
     *   <li>The email matches without regard to letter case and surrounding spaces.</li>
     *   <li>The password is checked against the member's stored hash with the {@link PasswordEncoder} bean.</li>
     *   <li>The token is a JWT signed by the {@link JwtEncoder} bean, with exactly these claims: {@code iss}, the
     *       configured {@link JwtProperties#issuer() issuer}; {@code sub}, the member's id as text, such as
     *       {@code "1"}; {@value JwtConfiguration#ROLE_CLAIM}, the member's role, {@code USER} or {@code ADMIN};
     *       {@code iat}, now; and {@code exp}, now plus the configured {@link JwtProperties#timeToLive() time to
     *       live}. "Now" comes from the {@link Clock} bean. The token contains no email or name, because anyone
     *       who has a token can read its claims.</li>
     * </ul>
     *
     * @return the token, the type {@code Bearer}, and the token's lifetime in seconds
     * @throws BusinessException {@link ErrorCode#INVALID_CREDENTIALS} ({@code 401}) with the error code's default
     *                           message if the email is unknown or the password is wrong. The two cases get exactly
     *                           the same answer, so that nobody can find out which emails are registered.
     */
    @Transactional(readOnly = true)
    public TokenResponse issueToken(TokenRequest request) {
        Optional<Member> found = memberRepository.findByEmail(request.email().strip().toLowerCase(Locale.ROOT));
        // Always run the slow password check, also for an unknown email: see UNKNOWN_MEMBER_HASH.
        boolean passwordMatches = passwordEncoder.matches(request.password(),
                found.map(Member::getPasswordHash).orElse(UNKNOWN_MEMBER_HASH));
        Member member = found.filter(ignored -> passwordMatches)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        // The application's clock, like everywhere else. JwtConfiguration checks expiry with the same clock.
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(member.getId().toString())
                .claim(JwtConfiguration.ROLE_CLAIM, member.getRole().name())
                .issuedAt(now)
                .expiresAt(now.plus(properties.timeToLive()))
                .build();
        // Without a header of our own, the encoder writes its default one: alg HS256, typ JWT.
        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return TokenResponse.bearer(token, properties.timeToLive().toSeconds());
    }
}

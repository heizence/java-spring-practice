package com.example.bookstore.auth;

import java.time.Clock;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.common.security.JwtConfiguration;
import com.example.bookstore.common.security.JwtProperties;
import com.example.bookstore.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Signs members in by issuing access tokens. */
@Service
@RequiredArgsConstructor
public class TokenService {

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
        // TODO: S12-03
        throw new UnsupportedOperationException("TODO: S12-03");
    }
}

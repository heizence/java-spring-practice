package com.example.bookstore.member;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.wallet.Wallet;
import com.example.bookstore.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for members: signing up, and looking members up. */
@Service
@RequiredArgsConstructor
public class MemberService {

    /** BCrypt uses at most 72 bytes of a password, and Spring Security refuses longer ones with an exception. */
    static final int MAX_PASSWORD_BYTES = 72;

    private final MemberRepository memberRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Signs up a new customer (role {@code USER}): the use case behind {@code POST /api/members}.
     *
     * <ul>
     *   <li>The email is stored trimmed and in lower case ({@link Member#register} does that).</li>
     *   <li>The password is stored only as a hash from the {@link PasswordEncoder} bean, never in plain text.</li>
     *   <li>The new member gets an empty wallet, because every member has exactly one.</li>
     * </ul>
     *
     * @return the new member, without its password hash
     * @throws BusinessException {@link ErrorCode#DUPLICATE_EMAIL} ({@code 409}) if a member already has this email,
     *                           in any letter case, with the detail
     *                           {@code The email address minji@example.com is already registered.} (the email in
     *                           lower case); {@link ErrorCode#INVALID_INPUT} ({@code 400}) if the password is longer
     *                           than {@value #MAX_PASSWORD_BYTES} bytes in UTF-8. A Korean letter takes 3 bytes, so a
     *                           password can pass the request's 72-character limit and still be too long. Nothing is
     *                           saved in either case.
     */
    @Transactional
    public MemberResponse signUp(SignUpRequest request) {
        // Check before hashing: BCrypt would throw IllegalArgumentException, which becomes a 500.
        if (request.password().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "The password must be at most %d bytes long in UTF-8.".formatted(MAX_PASSWORD_BYTES));
        }
        // Normalize first, the way Member stores emails, so that the check finds every letter case.
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL,
                    "The email address %s is already registered.".formatted(email));
        }
        // The hash includes a random salt and the algorithm's id: {bcrypt}$2a$10$<salt><hash>.
        String passwordHash = passwordEncoder.encode(request.password());
        Member member = memberRepository.save(Member.register(email, request.name(), passwordHash));
        walletRepository.save(Wallet.open(member));
        return MemberResponse.from(member);
    }

    /** @throws BusinessException {@link ErrorCode#MEMBER_NOT_FOUND} if no member has this id */
    @Transactional(readOnly = true)
    public MemberResponse findMember(long id) {
        return memberRepository.findById(id)
                .map(MemberResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND,
                        "Member %d was not found.".formatted(id)));
    }
}

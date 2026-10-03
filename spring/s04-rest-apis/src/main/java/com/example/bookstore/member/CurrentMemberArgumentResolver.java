package com.example.bookstore.member;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Provides the value of controller method parameters marked {@link CurrentMember @CurrentMember}: the member whose id
 * the request sends in the {@value #HEADER} header.
 *
 * <p><b>The header is a stand-in for real authentication.</b> Anyone can send any id in it, so a real application
 * must never trust it, unless a gateway in front of the application authenticated the user and set the header
 * itself. S12 replaces it with JSON Web Tokens.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>Supports exactly the parameters that are annotated with {@code @CurrentMember} and have the type
 *       {@link Member}.</li>
 *   <li>The header must hold an id of 1 to 18 digits, and a member with that id must exist.</li>
 *   <li>Otherwise (no header, a value that isn't such a number, an unknown id) it throws a
 *       {@link BusinessException} with {@link ErrorCode#UNAUTHORIZED}: the request has no valid credentials. The
 *       message is always {@code Send the id of an existing member in the X-Member-Id header.}, so that a caller
 *       can't find out which ids exist.</li>
 * </ul>
 *
 * <p>Spring calls a resolver only after it is registered: see {@code WebConfiguration}.
 *
 * <p>TODO: S04-05: register it in {@code WebConfiguration}.
 */
@Component
@RequiredArgsConstructor
public class CurrentMemberArgumentResolver implements HandlerMethodArgumentResolver {

    /** The request header that names the member. */
    public static final String HEADER = "X-Member-Id";

    private final MemberRepository memberRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // TODO: S04-05
        throw new UnsupportedOperationException("TODO: S04-05");
    }

    @Override
    public Member resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        // TODO: S04-05
        throw new UnsupportedOperationException("TODO: S04-05");
    }
}

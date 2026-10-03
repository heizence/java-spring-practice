package com.example.bookstore.wallet;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wallets: the signed-in member's own ({@code /api/wallet}) and, for administrators, anyone's
 * ({@code /api/wallets/{memberId}}). {@link WalletService} lists the security rules.
 */
@RestController
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    /** The signed-in member's wallet: {@code GET /api/wallet}. */
    @GetMapping("/api/wallet")
    public WalletResponse myWallet(@AuthenticationPrincipal Jwt jwt) {
        return walletService.findMyWallet(memberId(jwt));
    }

    /**
     * Sends money to another member: {@code POST /api/wallet/transfers} with
     * {@code {"toMemberId": 2, "amount": 5000}}.
     */
    @PostMapping("/api/wallet/transfers")
    public WalletResponse transfer(@Valid @RequestBody TransferRequest request, @AuthenticationPrincipal Jwt jwt) {
        return walletService.transfer(memberId(jwt), request);
    }

    /** Any member's wallet, for administrators: {@code GET /api/wallets/2}. */
    @GetMapping("/api/wallets/{memberId}")
    public WalletResponse wallet(@PathVariable long memberId) {
        return walletService.findWallet(memberId);
    }

    /** The access token's subject is the member's id. */
    private static long memberId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}

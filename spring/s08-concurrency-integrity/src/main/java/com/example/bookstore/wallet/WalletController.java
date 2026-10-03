package com.example.bookstore.wallet;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A member's wallet: {@code GET /api/members/{memberId}/wallet}, charges and transfers. Charges and transfers answer
 * {@code 200 OK} with the member's wallet after the change.
 */
@RestController
@RequestMapping("/api/members/{memberId}/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final TransferService transferService;

    @GetMapping
    public WalletResponse getWallet(@PathVariable long memberId) {
        return walletService.findWallet(memberId);
    }

    /** Adds money to the wallet: {@code POST /api/members/1/wallet/charges} with {@code { "amount": 10000 }}. */
    @PostMapping("/charges")
    public WalletResponse charge(@PathVariable long memberId, @Valid @RequestBody ChargeRequest request) {
        return walletService.charge(memberId, request.amount());
    }

    /** Sends money to another member's wallet: {@code { "toMemberId": 2, "amount": 5000 }}. */
    @PostMapping("/transfers")
    public WalletResponse transfer(@PathVariable long memberId, @Valid @RequestBody TransferRequest request) {
        return transferService.transfer(memberId, request.toMemberId(), request.amount());
    }
}

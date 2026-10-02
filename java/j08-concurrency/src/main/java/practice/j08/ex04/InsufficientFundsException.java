package practice.j08.ex04;

/** Thrown when an account doesn't have enough money for a transfer. The transfer then changes nothing. */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(Account account, long balance, long amount) {
        super(account + " has " + balance + " but the transfer needs " + amount);
    }
}

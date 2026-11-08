package ke.pesascore.scoring;

/** A suggested loan the borrower can afford, or a decline with the reason. */
public record LoanOffer(
        boolean approved,
        long limitCents,
        int termMonths,
        long monthlyInstalmentCents,
        double monthlyInterestRate,
        String rationale) {

    public static LoanOffer decline(double rate, String why) {
        return new LoanOffer(false, 0, 0, 0, rate, why);
    }
}

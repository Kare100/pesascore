package ke.pesascore.parse;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import ke.pesascore.model.Category;

/**
 * Turns the free-text "Details" column of an M-Pesa statement into a {@link Category}.
 * Rules are ordered: the first match wins, so specific products (Fuliza, M-Shwari,
 * betting, digital lenders) are checked before generic ones (Pay Bill, transfers).
 */
public final class TransactionClassifier {

    private static final Pattern BETTING = Pattern.compile(
            "\\b(sportpesa|betika|odibets|mozzart\\w*|betway|sportybet|shabiki|betin|bangbet|22bet|betlion|helabet|melbet|1xbet)\\b");

    private static final Pattern LENDERS = Pattern.compile(
            "\\b(tala|branch|zash|okash|hustler fund|timiza|zenka|opesa|haraka|watu|m-kopa|mogo)\\b");

    private static final Pattern UTILITIES = Pattern.compile(
            "\\b(kplc|kenya power|nairobi water|water (and|&) sewerage|zuku|safaricom home|dstv|gotv|startimes|nhif|sha)\\b");

    private static final List<String> SAVINGS_OUT = List.of(
            "m-shwari withdraw", "kcb m-pesa withdraw", "lock savings withdraw", "ziidi withdraw");

    private static final List<String> SAVINGS_IN = List.of(
            "m-shwari deposit", "kcb m-pesa deposit", "lock savings", "m-shwari lock", "ziidi");

    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:254|0)?([71]\\d{2})\\d{4}(\\d{2})(?!\\d)");

    public Category classify(String details, boolean moneyIn) {
        String d = details.toLowerCase(Locale.ROOT);

        if (d.contains("reversal")) {
            return Category.REVERSAL;
        }
        if ((d.contains("charge") && !d.contains("recharge")) || d.contains("transaction cost")) {
            return Category.CHARGE;
        }
        if (d.contains("overdraft of credit party") || (d.contains("fuliza") && moneyIn)) {
            return Category.OVERDRAFT_DRAW;
        }
        if (d.contains("od loan repayment") || d.contains("overdraw") || d.contains("fuliza")) {
            return Category.OVERDRAFT_REPAYMENT;
        }
        if (BETTING.matcher(d).find()) {
            return Category.BETTING;
        }
        if (SAVINGS_OUT.stream().anyMatch(d::contains)) {
            return Category.SAVINGS_WITHDRAWAL;
        }
        if (SAVINGS_IN.stream().anyMatch(d::contains)) {
            return Category.SAVINGS_DEPOSIT;
        }
        if (LENDERS.matcher(d).find() || d.contains("loan")) {
            return moneyIn ? Category.LOAN_DISBURSEMENT : Category.LOAN_REPAYMENT;
        }
        if (d.contains("salary")) {
            return Category.SALARY;
        }
        if (d.contains("business payment from") || d.contains("b2c")) {
            return moneyIn ? Category.B2C_INCOME : Category.OTHER;
        }
        if (UTILITIES.matcher(d).find()) {
            return Category.UTILITY;
        }
        if (d.contains("withdrawal") && (d.contains("agent") || d.contains("atm"))) {
            return Category.CASH_WITHDRAWAL;
        }
        if (d.contains("deposit of funds") || d.contains("agent deposit")) {
            return Category.CASH_DEPOSIT;
        }
        if (d.contains("pay bill") || d.contains("paybill")) {
            return Category.BILL_PAYMENT;
        }
        if (d.contains("merchant payment") || d.contains("buy goods") || d.contains("small business")) {
            return Category.MERCHANT_PAYMENT;
        }
        if (d.contains("airtime") || d.contains("bundle")) {
            return Category.AIRTIME_DATA;
        }
        if (moneyIn && (d.contains("funds received") || d.contains("received from"))) {
            return Category.P2P_IN;
        }
        if (!moneyIn && (d.contains("customer transfer") || d.contains("send money") || d.contains("transfer to"))) {
            return Category.P2P_OUT;
        }
        return Category.OTHER;
    }

    /**
     * Who the money went to or came from, with phone numbers masked
     * (0712345678 becomes 0712****78) so reports never expose them.
     */
    public String counterparty(String details) {
        String d = details.trim();
        int dash = d.lastIndexOf(" - ");
        String cp = dash >= 0 ? d.substring(dash + 3) : d.replaceFirst("(?i)^.*?\\b(to|from)\\b\\s*", "");
        cp = cp.replaceAll("(?i)\\s+acc\\.?.*$", "");
        cp = PHONE.matcher(cp).replaceAll("0$1****$2");
        return cp.replaceAll("\\s+", " ").trim().toUpperCase(Locale.ROOT);
    }
}

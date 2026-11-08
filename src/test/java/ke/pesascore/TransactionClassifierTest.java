package ke.pesascore;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ke.pesascore.model.Category;
import ke.pesascore.parse.TransactionClassifier;

class TransactionClassifierTest {

    private final TransactionClassifier classifier = new TransactionClassifier();

    private void check(Category expected, String details, boolean moneyIn) {
        assertEquals(expected, classifier.classify(details, moneyIn), details);
    }

    @Test
    void recognisesIncome() {
        check(Category.SALARY, "Salary Payment from 521100 - BIDII ACADEMY", true);
        check(Category.B2C_INCOME, "Business Payment from 600222 - JUA KALI WORKS via API", true);
        check(Category.P2P_IN, "Funds received from 0712345678 - JOHN KAMAU", true);
        check(Category.CASH_DEPOSIT, "Deposit of Funds at Agent Till 30212 - RONGAI MPESA SHOP", true);
    }

    @Test
    void recognisesEverydaySpending() {
        check(Category.UTILITY, "Pay Bill Online to 888880 - KPLC PREPAID Acc. 54200178321", false);
        check(Category.BILL_PAYMENT, "Pay Bill Online to 4002001 - JAMII HOUSING Acc. B12", false);
        check(Category.MERCHANT_PAYMENT, "Merchant Payment Online to 5203457 - NAIVAS SUPERMARKET", false);
        check(Category.MERCHANT_PAYMENT, "Customer Payment to Small Business to 0798114420 - MAMA OLIECH", false);
        check(Category.CASH_WITHDRAWAL, "Customer Withdrawal At Agent Till 30212 - RONGAI MPESA SHOP", false);
        check(Category.AIRTIME_DATA, "Airtime Purchase", false);
        check(Category.AIRTIME_DATA, "Safaricom Data Bundles purchase", false);
        check(Category.P2P_OUT, "Customer Transfer to 0722483910 - MARY WANJIKU", false);
        check(Category.CHARGE, "Customer Transfer of Funds Charge", false);
        check(Category.CHARGE, "Withdrawal Charge", false);
    }

    @Test
    void recognisesCreditProducts() {
        check(Category.OVERDRAFT_DRAW, "OverDraft of Credit Party", true);
        check(Category.OVERDRAFT_REPAYMENT, "OD Loan Repayment to 232323 - M-PESA Overdraw", false);
        check(Category.LOAN_DISBURSEMENT, "Business Payment from 851900 - TALA via API", true);
        check(Category.LOAN_REPAYMENT, "Pay Bill Online to 851900 - TALA Acc. 0790", false);
        check(Category.LOAN_DISBURSEMENT, "M-Shwari Loan", true);
        check(Category.LOAN_REPAYMENT, "Pay Bill Online to 4093333 - WATU CREDIT Acc. KMGX412B", false);
        check(Category.SAVINGS_DEPOSIT, "M-Shwari Deposit", false);
        check(Category.SAVINGS_WITHDRAWAL, "M-Shwari Withdraw", true);
    }

    @Test
    void bettingIsCaughtInBothDirections() {
        check(Category.BETTING, "Pay Bill Online to 290290 - SportPesa Acc. 0790", false);
        check(Category.BETTING, "Business Payment from 290290 - SportPesa via API", true);
        check(Category.BETTING, "Pay Bill Online to 290080 - Betika Acc. 0790", false);
    }

    @Test
    void counterpartyIsCleanedAndPhoneNumbersAreMasked() {
        assertEquals("KPLC PREPAID", classifier.counterparty("Pay Bill Online to 888880 - KPLC PREPAID Acc. 54200178321"));
        assertEquals("MARY WANJIKU", classifier.counterparty("Customer Transfer to 0722483910 - MARY WANJIKU"));
        assertEquals("0722****10", classifier.counterparty("Customer Transfer to 0722483910"));
    }
}

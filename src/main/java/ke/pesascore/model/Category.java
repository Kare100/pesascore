package ke.pesascore.model;

/** What a single M-Pesa line represents, and which bucket of cash flow it belongs to. */
public enum Category {
    SALARY(Flow.INCOME),
    B2C_INCOME(Flow.INCOME),
    P2P_IN(Flow.INCOME),
    CASH_DEPOSIT(Flow.INCOME),

    P2P_OUT(Flow.SPEND),
    MERCHANT_PAYMENT(Flow.SPEND),
    BILL_PAYMENT(Flow.SPEND),
    UTILITY(Flow.SPEND),
    AIRTIME_DATA(Flow.SPEND),
    CASH_WITHDRAWAL(Flow.SPEND),
    CHARGE(Flow.SPEND),

    LOAN_DISBURSEMENT(Flow.CREDIT),
    LOAN_REPAYMENT(Flow.CREDIT),
    OVERDRAFT_DRAW(Flow.CREDIT),
    OVERDRAFT_REPAYMENT(Flow.CREDIT),

    SAVINGS_DEPOSIT(Flow.SAVINGS),
    SAVINGS_WITHDRAWAL(Flow.SAVINGS),

    BETTING(Flow.BETTING),
    REVERSAL(Flow.NEUTRAL),
    OTHER(Flow.NEUTRAL);

    /** Coarse grouping used by the feature extractor. */
    public enum Flow { INCOME, SPEND, CREDIT, SAVINGS, BETTING, NEUTRAL }

    private final Flow flow;

    Category(Flow flow) {
        this.flow = flow;
    }

    public Flow flow() {
        return flow;
    }
}

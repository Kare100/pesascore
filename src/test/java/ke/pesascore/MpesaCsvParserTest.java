package ke.pesascore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import ke.pesascore.model.Category;
import ke.pesascore.model.Money;
import ke.pesascore.model.Transaction;
import ke.pesascore.parse.MpesaCsvParser;

class MpesaCsvParserTest {

    private static final String STATEMENT = """
            MPESA FULL STATEMENT
            Customer Name:,Test Customer
            Receipt No.,Completion Time,Details,Transaction Status,Paid In,Withdrawn,Balance
            QX3,2026-03-02 18:05:10,"Customer Transfer of Funds Charge",Completed,,"-13.00","62,487.00"
            QX3,2026-03-02 18:05:10,"Customer Transfer to 0722483910 - MARY WANJIKU",Completed,,"-1,000.00","62,500.00"
            QX2,2026-03-01 09:00:00,"Salary Payment from 521100 - BIDII ACADEMY",Completed,"63,000.00",,"63,500.00"
            QX2,2026-03-01 09:00:00,"Salary Payment from 521100 - BIDII ACADEMY",Completed,"63,000.00",,"63,500.00"
            QX1,2026-02-28 10:00:00,"Pay Bill Online to 888880 - KPLC PREPAID Acc. 1",Failed,,"-500.00","500.00"
            QX0,not a date,"Airtime Purchase",Completed,,"-50.00","450.00"
            """;

    @Test
    void parsesRowsSortsThemAndSkipsDuplicates() throws IOException {
        MpesaCsvParser.Result r = new MpesaCsvParser().parse(new StringReader(STATEMENT));

        assertEquals(4, r.transactions().size());
        Transaction first = r.transactions().get(0);
        assertEquals("QX1", first.receipt());
        assertFalse(first.completed());

        Transaction salary = r.transactions().get(1);
        assertEquals(Category.SALARY, salary.category());
        assertEquals(Money.shillings(63_000), salary.paidInCents());
        assertEquals(Money.shillings(63_500), salary.balanceCents());

        Transaction transfer = r.transactions().stream()
                .filter(t -> t.category() == Category.P2P_OUT).findFirst().orElseThrow();
        assertEquals(Money.shillings(1_000), transfer.withdrawnCents());
        assertEquals("MARY WANJIKU", transfer.counterparty());
    }

    @Test
    void reportsBadLinesInsteadOfFailing() throws IOException {
        MpesaCsvParser.Result r = new MpesaCsvParser().parse(new StringReader(STATEMENT));
        assertEquals(1, r.warnings().size());
        assertTrue(r.warnings().get(0).contains("unreadable date"));
    }

    @Test
    void rejectsFilesThatAreNotStatements() {
        assertThrows(IllegalArgumentException.class,
                () -> new MpesaCsvParser().parse(new StringReader("name,amount\nfoo,12\n")));
    }

    @Test
    void parsesMoneyFormats() {
        assertEquals(125_000, Money.parseCents("1,250.00"));
        assertEquals(-125_000, Money.parseCents("-1,250.00"));
        assertEquals(30_000, Money.parseCents("KES 300"));
        assertEquals(0, Money.parseCents(""));
        assertEquals("KES 12,346", Money.format(1_234_550));
    }
}

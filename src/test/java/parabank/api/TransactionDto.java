package parabank.api;

import java.math.BigDecimal;

/** Account transaction as returned by the ParaBank API. */
public class TransactionDto {
    public long id;
    public long accountId;
    /** "Debit" or "Credit". */
    public String type;
    /** ISO date and time in UTC, for example "2026-11-26T00:00:00.000Z". */
    public String date;
    public BigDecimal amount;
    public String description;
}

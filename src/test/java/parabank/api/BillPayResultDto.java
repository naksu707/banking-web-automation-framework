package parabank.api;

import java.math.BigDecimal;

/** Result of a bill payment as returned by the ParaBank API. */
public class BillPayResultDto {
    public long accountId;
    public BigDecimal amount;
    public String payeeName;
}

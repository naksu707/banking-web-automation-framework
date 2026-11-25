package parabank.api;

import java.math.BigDecimal;

/** Bank account as returned by the ParaBank API. */
public class AccountDto {
    public long id;
    public long customerId;
    public String type;
    public BigDecimal balance;
}

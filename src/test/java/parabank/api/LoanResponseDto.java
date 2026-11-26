package parabank.api;

/** Result of a loan request as returned by the ParaBank API. */
public class LoanResponseDto {
    public boolean approved;
    /** Message key when the loan is denied, for example "error.insufficient.funds". */
    public String message;
    /** Id of the new LOAN account; 0 or null when the loan is denied. */
    public Long accountId;
    public String loanProviderName;
}

package parabank.data;

/**
 * Account types a customer can open.
 * The code is the value the ParaBank API expects in "newAccountType".
 */
public enum AccountType {
    CHECKING(0),
    SAVINGS(1);

    private final int code;

    AccountType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}

package parabank.data;

/** The company or person a bill is paid to. */
public record Payee(
        String name,
        String street,
        String city,
        String state,
        String zipCode,
        String phoneNumber,
        String accountNumber) {
}

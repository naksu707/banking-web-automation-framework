package parabank.data;

/** Data needed to register a ParaBank customer. */
public record Customer(
        String firstName,
        String lastName,
        String street,
        String city,
        String state,
        String zipCode,
        String phoneNumber,
        String ssn,
        String username,
        String password) {

    public String fullName() {
        return firstName + " " + lastName;
    }

    public Customer withUsername(String newUsername) {
        return new Customer(firstName, lastName, street, city, state, zipCode,
                phoneNumber, ssn, newUsername, password);
    }
}

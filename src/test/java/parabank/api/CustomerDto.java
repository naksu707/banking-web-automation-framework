package parabank.api;

/** Customer as returned by the ParaBank API. */
public class CustomerDto {
    public long id;
    public String firstName;
    public String lastName;
    public String phoneNumber;
    public String ssn;
    public AddressDto address;

    public static class AddressDto {
        public String street;
        public String city;
        public String state;
        public String zipCode;
    }
}

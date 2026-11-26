package parabank.data;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Creates unique, realistic test data.
 * ParaBank is a shared public demo, so every run registers its own customers
 * instead of relying on accounts that other people can change.
 */
public final class TestDataFactory {

    private static final List<String> FIRST_NAMES = List.of("Ana", "Lucia", "Camila", "Mateo", "Santiago", "Valentina");
    private static final List<String> LAST_NAMES = List.of("Gomez", "Rodriguez", "Martinez", "Lopez", "Garcia", "Torres");

    private static final List<String> PAYEES = List.of("City Power", "Water Utility", "Metro Internet", "Gas Company");

    private TestDataFactory() {
    }

    public static Customer newCustomer() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return new Customer(
                pick(FIRST_NAMES),
                pick(LAST_NAMES),
                random.nextInt(100, 9999) + " Main Street",
                "Springfield",
                "IL",
                String.valueOf(random.nextInt(10000, 99999)),
                "555" + random.nextInt(1000000, 9999999),
                String.valueOf(random.nextLong(100000000L, 999999999L)),
                uniqueUsername(),
                "Qa!" + random.nextInt(100000, 999999));
    }

    public static Payee newPayee() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return new Payee(
                pick(PAYEES),
                random.nextInt(100, 9999) + " Market Street",
                "Springfield",
                "IL",
                String.valueOf(random.nextInt(10000, 99999)),
                "555" + random.nextInt(1000000, 9999999),
                String.valueOf(random.nextInt(10000, 99999)));
    }

    /** Short, unique username: "qa" + base-36 timestamp + 2 random digits. */
    public static String uniqueUsername() {
        return "qa" + Long.toString(System.currentTimeMillis(), 36)
                + ThreadLocalRandom.current().nextInt(10, 99);
    }

    private static String pick(List<String> values) {
        return values.get(ThreadLocalRandom.current().nextInt(values.size()));
    }
}

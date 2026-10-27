import java.util.regex.Pattern;

public class MiniBank {
    static class Customer implements Cloneable {

        private String name;
        private String email;
        private String mobile;
        private final String customerId;
        private Address address;

        private static long customerCounter = 100;

        private static String generateCustomerId() {
            customerCounter++;
            return "Cust" + customerCounter;
        }

        Customer(String name, String email, String mobile) {
            this.name = name;
            this.email = email;
            this.mobile = mobile;
            this.customerId = generateCustomerId();
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getMobile() {
            return mobile;
        }

        public String getCustomerId() {
            return customerId;
        }

        public Address getAddress() {
            return address;
        }

        // Nested Address class
        public static class Address {

            private String line;
            private String city;
            private String pincode;

            public Address(String line, String city, String pincode) {
                this.line = line;
                this.city = city;
                this.pincode = pincode;
            }

            public String getLine() {
                return line;
            }

            public String getCity() {
                return city;
            }

            public String getPincode() {
                return pincode;
            }
        }

        // Clone method
        public Customer clone() {
            try {
                return (Customer) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class Account {

        private final String accountNumber;
        private String ownerName;
        private long balance;
        private boolean active;

        private static long accountCounter = 0;

        private static String generateAccountNumber() {
            accountCounter++;
            return String.format("AC%04d", accountCounter);
        }

        Account(String ownerName, long balance) {
            this.accountNumber = generateAccountNumber();
            this.ownerName = ownerName;
            this.balance = balance;
            this.active = true;
        }

        Account(String ownerName) {
            this(ownerName, 0);
        }

        public void deposit(long amount) {
            balance = balance + amount;
        }

        public boolean withdraw(long amount) {
            if (balance >= amount) {
                balance = balance - amount;
                return true;
            }

            return false;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public String getOwnerName() {
            return ownerName;
        }

        public long getBalance() {
            return balance;
        }

        public boolean isActive() {
            return active;
        }

        @Override
        public String toString() {
            return accountNumber + " - "
                    + ownerName + " - Balance: "
                    + balance;
        }

        @Override
        public boolean equals(Object o) {

            if (this == o) {
                return true;
            }

            if (!(o instanceof Account)) {
                return false;
            }

            Account account = (Account) o;

            return accountNumber.equals(account.accountNumber);
        }

        @Override
        public int hashCode() {
            return accountNumber.hashCode();
        }
    }

    static class Validator {

        private static final Pattern MOBILE =
                Pattern.compile("^[6-9][0-9]{9}$");

        private static final Pattern EMAIL =
                Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

        private static final Pattern PAN =
                Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");

        private static final Pattern IFSC =
                Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");


        public static boolean isValidMobile(String mobile) {
            return MOBILE.matcher(mobile).matches();
        }

        public static boolean isValidEmail(String email) {
            return EMAIL.matcher(email).matches();
        }

        public static boolean isValidPan(String pan) {
            return PAN.matcher(pan).matches();
        }

        public static boolean isValidIfsc(String ifsc) {
            return IFSC.matcher(ifsc).matches();
        }
    }

    enum TransactionType {
        DEPOSIT,
        WITHDRAW,
        TRANSFER
    }

    record Command(
            TransactionType type,
            String accountNumber,
            long amount
    ) {
    }

    static class CommandParser {

        public static Command parse(String line) {

            String[] parts = line.split(" ");

            TransactionType type =
                    TransactionType.valueOf(parts[0]);

            String accountNumber = parts[1];

            long amount = Long.parseLong(parts[2]);

            return new Command(
                    type,
                    accountNumber,
                    amount
            );
        }
    }

    static class StatementFormatter {

        public static String buildStatement(Account account) {

            StringBuilder statement = new StringBuilder();

            statement.append("----- Account Statement -----\n");

            statement.append("Account Number: ")
                    .append(account.getAccountNumber())
                    .append("\n");

            statement.append("Owner Name: ")
                    .append(account.getOwnerName())
                    .append("\n");

            statement.append("Balance: ")
                    .append(account.getBalance())
                    .append("\n");

            statement.append("-----------------------------");

            return statement.toString();
        }
    }
    public static void main(String[] args) {

        Account[] accounts = new Account[3];

        accounts[0] = new Account("Nandani", 1000);
        accounts[1] = new Account("Hitanshi");
        accounts[2] = new Account("Priya", 5000);

        accounts[0].deposit(500);
        accounts[0].withdraw(200);

        accounts[1].deposit(2000);
        accounts[1].withdraw(500);

        accounts[2].withdraw(1000);
        accounts[2].deposit(500);

        System.out.println("----- Accounts -----");

        for (Account account : accounts) {
            System.out.println(account);
        }

        System.out.println("\n----- Account Comparison -----");

        System.out.println(
                "Account 1 equals Account 2: "
                        + accounts[0].equals(accounts[1])
        );

        System.out.println("\n----- instanceof Test -----");

        if (accounts[0] instanceof Account) {
            System.out.println("accounts[0] is an Account");
        }

        System.out.println("\n----- Validator Testing -----");

        System.out.println(
                "Valid Mobile: "
                        + Validator.isValidMobile("9876543210")
        );

        System.out.println(
                "Invalid Mobile: "
                        + Validator.isValidMobile("12345")
        );

        System.out.println(
                "Valid Email: "
                        + Validator.isValidEmail("test@gmail.com")
        );

        System.out.println(
                "Invalid Email: "
                        + Validator.isValidEmail("test@gmail")
        );

        System.out.println(
                "Valid PAN: "
                        + Validator.isValidPan("ABCDE1234F")
        );

        System.out.println(
                "Invalid PAN: "
                        + Validator.isValidPan("ABC123")
        );

        System.out.println(
                "Valid IFSC: "
                        + Validator.isValidIfsc("SBIN0001234")
        );

        System.out.println(
                "Invalid IFSC: "
                        + Validator.isValidIfsc("SB123")
        );

        System.out.println("\n----- Command Testing -----");

        Command command =
                CommandParser.parse("DEPOSIT AC0001 500");

        System.out.println("Type: " + command.type());
        System.out.println(
                "Account Number: " + command.accountNumber()
        );
        System.out.println("Amount: " + command.amount());

        System.out.println("\n----- Statement -----");

        System.out.println(
                StatementFormatter.buildStatement(accounts[0])
        );

        System.out.println("\n----- Customer Testing -----");

        Customer customer =
                new Customer(
                        "Nandani",
                        "nandani@gmail.com",
                        "9876543210"
                );

        Customer.Address address =
                new Customer.Address(
                        "ABC Road",
                        "Anand",
                        "388001"
                );

        System.out.println(
                "Customer ID: "
                        + customer.getCustomerId()
        );

        System.out.println(
                "Name: "
                        + customer.getName()
        );

        System.out.println(
                "Email: "
                        + customer.getEmail()
        );

        System.out.println(
                "Mobile: "
                        + customer.getMobile()
        );

        System.out.println(
                "Address: "
                        + address.getLine()
                        + ", "
                        + address.getCity()
                        + ", "
                        + address.getPincode()
        );
        Customer copy = customer.clone();

        System.out.println(
                "Cloned Customer ID: "
                        + copy.getCustomerId()
        );
    }
}
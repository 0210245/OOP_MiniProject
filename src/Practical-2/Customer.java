public class MiniBank {

    static class Customer {
        private String name;
        private String email;
        private String mobile;
        private final String customerId;

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
    }


    // MAIN METHOD BELONGS TO MINIBANK
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

        for (Account account : accounts) {
            System.out.println(
                account.getAccountNumber() + "-" +
                account.getOwnerName() +
                "-Balance: " +
                account.getBalance()
            );
        }
    }
}
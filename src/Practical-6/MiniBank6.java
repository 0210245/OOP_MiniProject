public class MiniBank6 {
    interface Transactable {
        void deposit(long amount);
        boolean withdraw(long amount);
    }
    interface InterestBearing {
        double interestRate();
        long getBalance();

        default double yearlyInterest() {
            return getBalance() * interestRate() / 100;
        }
    }
    @FunctionalInterface
    interface WithdrawRule {
        boolean allow(Account account, long amount);
    }

    interface Premium {
    }

    static abstract class Account implements Transactable, InterestBearing {
        private final String accountNumber;
        private String ownerName;
        private long balance;

        private static long accountCounter = 0;

        private static String generateAccountNumber() {
            accountCounter++;
            return String.format("AC%04d", accountCounter);
        }

        Account(String ownerName, long balance) {
            this.accountNumber = generateAccountNumber();
            this.ownerName = ownerName;
            this.balance = balance;
        }

        @Override
        public void deposit(long amount) {
            balance = balance + amount;
        }

        @Override
        public boolean withdraw(long amount) {
            if (canWithdraw(amount)) {
                balance = balance - amount;
                return true;
            }
            return false;
        }

        @Override
        public abstract double interestRate();

        public abstract boolean canWithdraw(long amount);

        public String getAccountNumber() {
            return accountNumber;
        }

        public String getOwnerName() {
            return ownerName;
        }

        @Override
        public synchronized long getBalance() {
            return balance;
        }

        @Override
        public String toString() {
            return accountNumber + " - " + ownerName
                    + " - Balance: " + getBalance();
        }
    }

    static class SavingsAccount extends Account {
        private long minBalance;

        SavingsAccount(String ownerName, long balance, long minBalance) {
            super(ownerName, balance);
            this.minBalance = minBalance;
        }
        @Override
        public double interestRate() {
            return 4.0;
        }
        @Override
        public boolean canWithdraw(long amount) {
            return getBalance() - amount >= minBalance;
        }
    }

    static class CurrentAccount extends Account implements Premium {
        private long overdraftLimit;

        CurrentAccount(String ownerName, long balance, long overdraftLimit) {
            super(ownerName, balance);
            this.overdraftLimit = overdraftLimit;
        }
        @Override
        public double interestRate() {
            return 0.0;
        }
        @Override
        public boolean canWithdraw(long amount) {
            return getBalance() - amount >= -overdraftLimit;
        }
    }

    static class FixedDepositAccount extends Account {
        FixedDepositAccount(String ownerName, long balance) {
            super(ownerName, balance);
        }
        @Override
        public double interestRate() {
            return 7.0;
        }
        @Override
        public boolean canWithdraw(long amount) {
            return false;
        }
    }

    static class StatementFormatter {
        public static String buildStatement(Account account) {
            StringBuilder statement = new StringBuilder();

            statement.append("----- Account Statement -----\n");
            statement.append("Account Number: ").append(account.getAccountNumber()).append("\n");
            statement.append("Owner Name: ").append(account.getOwnerName()).append("\n");
            statement.append("Balance: ").append(account.getBalance()).append("\n");
            statement.append("-----------------------------");

            return statement.toString();
        }
    }

    public static void main(String[] args) {
        Account[] accounts = new Account[3];
        accounts[0] = new SavingsAccount("Nandani", 1000, 500);
        accounts[1] = new CurrentAccount("Hitanshi", 1000, 2000);
        accounts[2] = new FixedDepositAccount("Priya", 5000);

        System.out.println("----- Accounts and Interest Rates -----");

        for (Account account : accounts) {
            System.out.println(account);
            System.out.println("Interest Rate: " + account.interestRate() + "%");
            System.out.println();
        }

        System.out.println("----- Withdrawal Testing -----");

        System.out.println("Savings withdrawal: " + accounts[0].withdraw(400));
        System.out.println("Savings balance: " + accounts[0].getBalance());

        System.out.println("Current withdrawal: " + accounts[1].withdraw(2500));
        System.out.println("Current balance: " + accounts[1].getBalance());

        System.out.println("Fixed Deposit withdrawal: " + accounts[2].withdraw(1000));
        System.out.println("Fixed Deposit balance: " + accounts[2].getBalance());

        System.out.println("\n----- Withdraw Rule Testing -----");

        // Anonymous class
        WithdrawRule rule1 = new WithdrawRule() {
            @Override
            public boolean allow(Account account, long amount) {
                return account.getBalance() >= amount;
            }
        };
        System.out.println("Anonymous class: " + rule1.allow(accounts[0], 300));

        // Lambda expression
        WithdrawRule rule2 = (account, amount) -> account.getBalance() >= amount;

        System.out.println("Lambda expression: " + rule2.allow(accounts[0], 300));

        System.out.println("\n----- Yearly Interest -----");

        for (Account account : accounts) {
            if (account instanceof InterestBearing interestBearing) {
                System.out.println(account.getOwnerName()
                        + " Yearly Interest: " + interestBearing.yearlyInterest());
            }
        }

        System.out.println("\n----- Premium Account -----");

        for (Account account : accounts) {
            if (account instanceof Premium) {
                System.out.println(account.getOwnerName() + " is a Premium Account");
            }
        }

        System.out.println("\n----- Account Statement -----");
        System.out.println(StatementFormatter.buildStatement(accounts[0]));
    }
}
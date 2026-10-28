public class MiniBank8 {
    static class BankException extends Exception {
        BankException(String message) {
            super(message);
        }
    }

    static class InsufficientFundsException extends BankException {
        private long shortfall;

        InsufficientFundsException(long shortfall) {
            super("Insufficient funds, short by " + shortfall);
            this.shortfall = shortfall;
        }
        public long getShortfall() {
            return shortfall;
        }
    }

    static class InvalidAmountException extends BankException {
        InvalidAmountException(long amount) {
            super("Invalid amount: " + amount);
        }
    }

    static class BankSession implements AutoCloseable {
        public void performOperation() {
            System.out.println("Session: operation performed.");
        }
        @Override
        public void close() {
            System.out.println("Session closed automatically.");
        }
    }

    interface Transactable {
        void deposit(long amount) throws InvalidAmountException;
        boolean withdraw(long amount)
                throws InsufficientFundsException, InvalidAmountException;
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
        public synchronized void deposit(long amount) throws InvalidAmountException {

            if (amount <= 0) {
                throw new InvalidAmountException(amount);
            }

            balance = balance + amount;
        }
        @Override
        public synchronized boolean withdraw(long amount)
                throws InsufficientFundsException, InvalidAmountException {

            if (amount <= 0) {
                throw new InvalidAmountException(amount);
            }

            if (!canWithdraw(amount)) {
                long shortfall = amount - balance;
                if (shortfall <= 0) {       
                    shortfall = amount;
                }
                throw new InsufficientFundsException(shortfall);
            }

            balance = balance - amount;
            return true;
        }

        public void transfer(Account to, long amount) throws BankException {
            try {
                withdraw(amount);
                to.deposit(amount);

            } catch (BankException e) {
                System.out.println("Transfer failed: " + e.getMessage());
                throw e;                   

            } finally {
                System.out.println("Transfer request processed.");
            }
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

        System.out.println("\n----- Exception Handling -----");

        try {
            accounts[0].deposit(500);
            System.out.println("Deposit successful.");
        } catch (InvalidAmountException e) {
            System.out.println("Deposit failed: " + e.getMessage());
        } finally {
            System.out.println("Deposit operation checked.");
        }

        try {
            accounts[0].withdraw(10000);
        } catch (InsufficientFundsException e) {
            System.out.println("Withdrawal failed: " + e.getMessage());
            System.out.println("Shortfall: " + e.getShortfall());
        } catch (InvalidAmountException e) {
            System.out.println("Invalid withdrawal: " + e.getMessage());
        } finally {
            System.out.println("Withdrawal operation checked.");
        }

        try {
            accounts[0].transfer(accounts[1], 200);
            System.out.println("Transfer successful.");
        } catch (BankException e) {
            System.out.println("Banking error: " + e.getMessage());
        }

        System.out.println("\n----- Try With Resources -----");

        try (BankSession session = new BankSession()) {
            session.performOperation();
        } finally {
            System.out.println("Session example finished.");
        }

        System.out.println("\n----- Accounts and Interest Rates -----");

        for (Account account : accounts) {
            System.out.println(account);
            System.out.println("Interest Rate: " + account.interestRate() + "%");
            System.out.println();
        }

        System.out.println("----- Withdrawal Testing -----");

        try {
            System.out.println("Savings withdrawal: " + accounts[0].withdraw(400));
        } catch (InsufficientFundsException | InvalidAmountException e) {
            System.out.println("Savings withdrawal failed: " + e.getMessage());
        }
        System.out.println("Savings balance: " + accounts[0].getBalance());

        try {
            System.out.println("Current withdrawal: " + accounts[1].withdraw(2500));
        } catch (InsufficientFundsException | InvalidAmountException e) {
            System.out.println("Current withdrawal failed: " + e.getMessage());
        }
        System.out.println("Current balance: " + accounts[1].getBalance());

        try {
            System.out.println("Fixed Deposit withdrawal: " + accounts[2].withdraw(1000));
        } catch (InsufficientFundsException | InvalidAmountException e) {
            System.out.println("Fixed Deposit withdrawal failed: " + e.getMessage());
        }
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
public class MiniBank9 {
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

    static abstract class Account {

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
        public synchronized void deposit(long amount) throws InvalidAmountException {
            if (amount <= 0) {
                throw new InvalidAmountException(amount);
            }
            balance = balance + amount;
        }

        public synchronized boolean withdraw(long amount)
                throws InsufficientFundsException, InvalidAmountException {
            if (amount <= 0) {
                throw new InvalidAmountException(amount);
            }
            if (!canWithdraw(amount)) {
                throw new InsufficientFundsException(amount - balance);
            }
            balance = balance - amount;
            return true;
        }

        public abstract double interestRate();
        public abstract boolean canWithdraw(long amount);

        public String getAccountNumber() {
            return accountNumber;
        }
        public String getOwnerName() {
            return ownerName;
        }
        public long getBalance() {
            return balance;
        }
        protected void setBalance(long newBalance) {
            this.balance = newBalance;
        }
        @Override
        public String toString() {
            return accountNumber + " - " + ownerName + " - Balance: " + balance;
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

    static class CurrentAccount extends Account {
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

    static class UnsafeAccount extends SavingsAccount {

        UnsafeAccount(String ownerName) {
            super(ownerName, 0, 0);
        }

        @Override
        public void deposit(long amount) throws InvalidAmountException {
            if (amount <= 0) {
                throw new InvalidAmountException(amount);
            }

            long oldBalance = getBalance();     
            Thread.yield();                     
            setBalance(oldBalance + amount);   
        }
    }

    static class AccountWorker implements Runnable {

        private Account account;
        private int times;
        private long amount;

        AccountWorker(Account account, int times, long amount) {
            this.account = account;
            this.times = times;
            this.amount = amount;
        }

        @Override
        public void run() {
            String name = Thread.currentThread().getName();
            System.out.println(name + " started");

            for (int i = 0; i < times; i++) {
                try {
                    account.deposit(amount);
                } catch (InvalidAmountException e) {
                    System.out.println(name + ": " + e.getMessage());
                }
            }
            System.out.println(name + " finished");
        }
    }

    static void runThreads(Account account, int threadCount, int times, long amount)
            throws InterruptedException {
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            AccountWorker worker = new AccountWorker(account, times, amount);
            threads[i] = new Thread(worker, "Worker-" + (i + 1));
        }
        System.out.println("State before start: " + threads[0].getState());
        for (Thread thread : threads) {
            thread.start();
        }

        System.out.println("State after start : " + threads[0].getState());

        for (Thread thread : threads) {
            thread.join();      
        }

        System.out.println("State after join  : " + threads[0].getState());
        long expected = threadCount * times * amount;
        System.out.println("Expected balance: " + expected);
        System.out.println("Actual balance  : " + account.getBalance());
    }


    public static void main(String[] args) throws InterruptedException {

        int threadCount = 10;
        int times = 1000;
        long amount = 1;

        System.out.println("===== Without synchronized (race condition) =====");
        Account unsafeAccount = new UnsafeAccount("Unsafe User");
        runThreads(unsafeAccount, threadCount, times, amount);


        System.out.println("\n===== With synchronized (thread safe) =====");
        Account safeAccount = new SavingsAccount("Safe User", 0, 0);

        runThreads(safeAccount, threadCount, times, amount);
    }
}
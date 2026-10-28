import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class MiniBank12 {
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
    static abstract class Account implements Comparable<Account> {

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

        public synchronized long getBalance() {
            return balance;
        }

        @Override
        public int compareTo(Account other) {
            return accountNumber.compareTo(other.accountNumber);
        }

        @Override
        public String toString() {
            return accountNumber + " - " + ownerName + " - Balance: " + getBalance();
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

    static class AccountStore {
        private HashMap<String, Account> accounts = new HashMap<>();

        private ArrayList<String> history = new ArrayList<>();

        public void add(Account account) {
            accounts.put(account.getAccountNumber(), account);
            history.add("Added " + account.getAccountNumber());
        }

        public Account get(String accountNumber) {
            return accounts.get(accountNumber);
        }

        public ArrayList<Account> getAll() {
            return new ArrayList<>(accounts.values());
        }

        public ArrayList<String> getHistory() {
            return history;
        }

        // Comparator: sorting rule for this one case (highest balance first)
        public ArrayList<Account> sortedByBalance() {

            ArrayList<Account> list = getAll();

            Comparator<Account> byBalance =
                    (a, b) -> Long.compare(b.getBalance(), a.getBalance());

            list.sort(byBalance);

            return list;
        }
    }
    static class TransactionProcessor {

        private ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<>();

        public void add(Account account) {
            accounts.put(account.getAccountNumber(), account);
        }

        public Account get(String accountNumber) {
            return accounts.get(accountNumber);
        }
    }

    public static void main(String[] args) {

        AccountStore store = new AccountStore();

        Account a1 = new SavingsAccount("Riya", 5000, 0);
        Account a2 = new SavingsAccount("Amit", 2000, 0);
        Account a3 = new CurrentAccount("Neha", 9000, 1000);

        store.add(a1);
        store.add(a2);
        store.add(a3);

        System.out.println("----- Look up by account number -----");
        System.out.println("get(AC0002): " + store.get("AC0002"));

        System.out.println("\n----- Sorted by balance (Comparator) -----");

        for (Account account : store.sortedByBalance()) {
            System.out.println(account);
        }

        System.out.println("\n----- Sorted by account number (Comparable) -----");

        ArrayList<Account> list = store.getAll();
        Collections.sort(list);

        for (Account account : list) {
            System.out.println(account);
        }

        System.out.println("\n----- History (ArrayList) -----");

        for (String line : store.getHistory()) {
            System.out.println(line);
        }

        System.out.println("\n----- TransactionProcessor (ConcurrentHashMap) -----");

        TransactionProcessor processor = new TransactionProcessor();

        processor.add(a1);
        processor.add(a2);
        processor.add(a3);

        System.out.println("get(AC0003): " + processor.get("AC0003"));
    }
}
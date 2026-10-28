import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

public class MiniBank13 {
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Id {
    }
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

        @Id
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

    // ================= PRACTICAL 13 : GENERICS =================

    // T is a placeholder for any type: Repository<Account>, Repository<String> ...
    static class Repository<T> {

        // key = the value of the @Id field, value = the object
        private HashMap<String, T> items = new HashMap<>();

        public T save(T entity) {
            String id = findId(entity);
            items.put(id, entity);
            return entity;
        }

        // saves many objects at once. T... means "any number of T objects"
        @SafeVarargs
        public final void saveAll(T... entities) {
            for (T entity : entities) {
                save(entity);
            }
        }

        public T findById(String id) {
            return items.get(id);
        }

        public ArrayList<T> findAll() {
            return new ArrayList<>(items.values());
        }

        public boolean delete(String id) {
            return items.remove(id) != null;
        }

        // Uses reflection to find the field marked with @Id and read its value
        private String findId(T entity) {

            Class<?> c = entity.getClass();

            // the @Id field is in Account (parent), so move up the classes
            while (c != Object.class) {

                for (Field field : c.getDeclaredFields()) {

                    if (field.isAnnotationPresent(Id.class)) {

                        field.setAccessible(true);

                        try {
                            return (String) field.get(entity);

                        } catch (IllegalAccessException e) {
                            System.out.println("Cannot read @Id field");
                        }
                    }
                }

                c = c.getSuperclass();
            }

            return null;
        }

        // Bounded type: N must be Number or a child of Number (Integer, Double...)
        public static <N extends Number> double average(N[] values) {

            double sum = 0;

            for (N value : values) {
                sum = sum + value.doubleValue();
            }

            return sum / values.length;
        }
    }

    // ================= MAIN =================

    public static void main(String[] args) {

        Repository<Account> repository = new Repository<>();

        Account a1 = new SavingsAccount("Riya", 5000, 0);
        Account a2 = new CurrentAccount("Amit", 2000, 1000);
        Account a3 = new FixedDepositAccount("Neha", 9000);

        repository.save(a1);
        repository.saveAll(a2, a3);

        System.out.println("----- findById -----");
        System.out.println("findById(AC0002): " + repository.findById("AC0002"));

        System.out.println("\n----- findAll -----");

        for (Account account : repository.findAll()) {
            System.out.println(account);
        }

        System.out.println("\n----- delete -----");
        System.out.println("delete(AC0002): " + repository.delete("AC0002"));
        System.out.println("findById(AC0002): " + repository.findById("AC0002"));

        System.out.println("\n----- Bounded type parameter -----");

        Integer[] numbers = {10, 20, 30};
        Double[] decimals = {1.5, 2.5, 3.5};

        System.out.println("average(Integer[]): " + Repository.average(numbers));
        System.out.println("average(Double[]): " + Repository.average(decimals));
    }
}
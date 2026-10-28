import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class MiniBank7 {
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Id {
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Positive {
        String message() default "must be > 0";
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface MaxLength {
        int value();
    }

    interface Transactable {
        void deposit(long amount);

        boolean withdraw(long amount);
    }

    @FunctionalInterface
    interface WithdrawRule {
        boolean allow(Account account, long amount);
    }

    static abstract class Account implements Transactable {

        @Id
        private final String accountNumber;

        @MaxLength(20)
        private String ownerName;

        @Positive
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

        @Override
        public String toString() {
            return accountNumber + " - " + ownerName + " - Balance: " + balance;
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

    static class AnnotationValidator {

        public static String[] validate(Object obj) {

            ArrayList<String> errors = new ArrayList<>();
            Class<?> c = obj.getClass();

            while (c != Object.class) {

                Field[] fields = c.getDeclaredFields();

                for (Field field : fields) {

                    field.setAccessible(true);    

                    try {
                        if (field.isAnnotationPresent(Positive.class)) {

                            long value = field.getLong(obj);

                            if (value <= 0) {
                                errors.add(field.getAnnotation(Positive.class).message());
                            }
                        }

                        if (field.isAnnotationPresent(MaxLength.class)) {

                            String text = (String) field.get(obj);
                            int max = field.getAnnotation(MaxLength.class).value();

                            if (text.length() > max) {
                                errors.add("length must be <= " + max);
                            }
                        }

                    } catch (IllegalAccessException e) {
                        System.out.println("Cannot read field: " + field.getName());
                    }
                }

                c = c.getSuperclass();
            }

            return errors.toArray(new String[0]);
        }
    }
    public static void main(String[] args) {

        Account[] accounts = new Account[3];

        accounts[0] = new SavingsAccount("Nandani", 1000, 500);
        accounts[1] = new CurrentAccount("Hitanshi", 1000, 2000);
        accounts[2] = new FixedDepositAccount("Priya", 5000);

        System.out.println("----- Accounts -----");

        for (Account account : accounts) {
            System.out.println(account);
        }

        System.out.println("\n----- Account with negative balance -----");

        Account badAccount = new SavingsAccount("Riya", -100, 0);

        String[] errors = AnnotationValidator.validate(badAccount);

        System.out.println(badAccount);

        for (String error : errors) {
            System.out.println("Error: " + error);
        }

        System.out.println("\n----- Valid account -----");

        String[] noErrors = AnnotationValidator.validate(accounts[0]);

        System.out.println(accounts[0]);
        System.out.println("Number of errors: " + noErrors.length);

        System.out.println("\n----- Owner name too long -----");

        Account longName = new SavingsAccount("Mr Very Long Owner Name Here", 500, 0);

        String[] nameErrors = AnnotationValidator.validate(longName);

        for (String error : nameErrors) {
            System.out.println("Error: " + error);
        }
    }
}
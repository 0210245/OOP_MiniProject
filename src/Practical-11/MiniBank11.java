import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

public class MiniBank11 {
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
    static class Customer implements Serializable {

        private static final long serialVersionUID = 1L;

        private String name;
        private String email;
        private String mobile;

        Customer(String name, String email, String mobile) {
            this.name = name;
            this.email = email;
            this.mobile = mobile;
        }

        public String getName() {
            return name;
        }
    }

    static abstract class Account implements Serializable {
        private static final long serialVersionUID = 1L;

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

        public void deposit(long amount) throws InvalidAmountException {
            if (amount <= 0) {
                throw new InvalidAmountException(amount);
            }
            balance = balance + amount;
        }

        public boolean withdraw(long amount)
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
    static class StatePersister {

        public static void save(Account[] accounts, Path file) throws IOException {

            try (ObjectOutputStream out =
                         new ObjectOutputStream(Files.newOutputStream(file))) {
                out.writeObject(accounts);
            }
        }

        public static Account[] load(Path file)
                throws IOException, ClassNotFoundException {

            try (ObjectInputStream in =
                         new ObjectInputStream(Files.newInputStream(file))) {
                return (Account[]) in.readObject();
            }
        }
    }
    static class TransactionLog {

        public static void append(Path file, String line) throws IOException {

            Files.write(
                    file,
                    (line + "\n").getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        }
    }

    static class ReportGenerator {
        public static String generate(Path logsFolder, Path reportFile)
                throws IOException {

            long totalDeposits = 0;
            long totalWithdrawals = 0;

            StringBuilder report = new StringBuilder();
            report.append("----- End Of Day Report -----\n");

            try (DirectoryStream<Path> files = Files.newDirectoryStream(logsFolder)) {

                for (Path file : files) {

                    // file details: size and last modified time
                    BasicFileAttributes details =
                            Files.readAttributes(file, BasicFileAttributes.class);

                    report.append("File: ").append(file.getFileName())
                            .append(", size: ").append(details.size())
                            .append(" bytes, modified: ")
                            .append(details.lastModifiedTime()).append("\n");

                    try (BufferedReader reader = Files.newBufferedReader(file)) {

                        String line;

                        while ((line = reader.readLine()) != null) {

                            String[] parts = line.split(" ");
                            long amount = Long.parseLong(parts[2]);

                            if (parts[0].equals("DEPOSIT")) {
                                totalDeposits = totalDeposits + amount;
                            } else if (parts[0].equals("WITHDRAW")) {
                                totalWithdrawals = totalWithdrawals + amount;
                            }
                        }
                    }
                }
            }

            report.append("Total deposits: ").append(totalDeposits).append("\n");
            report.append("Total withdrawals: ").append(totalWithdrawals).append("\n");
            report.append("Net change: ")
                    .append(totalDeposits - totalWithdrawals).append("\n");

            Files.write(reportFile, report.toString().getBytes());

            return report.toString();
        }
    }

    public static void main(String[] args) {

        Account[] accounts = new Account[3];

        accounts[0] = new SavingsAccount("Riya", 5000, 0);
        accounts[1] = new CurrentAccount("Amit", 2000, 1000);
        accounts[2] = new FixedDepositAccount("Neha", 9000);

        Path folder = Path.of("data");
        Path logsFolder = Path.of("data", "logs");
        Path accountsFile = Path.of("data", "accounts.dat");
        Path reportFile = Path.of("data", "report.txt");
        Path logFile = Path.of("data", "logs", "day1.log");

        try {
            Files.createDirectories(logsFolder);
            Files.deleteIfExists(logFile);

            // ---------- save and load ----------

            System.out.println("----- Saved accounts -----");

            for (Account account : accounts) {
                System.out.println(account);
            }

            StatePersister.save(accounts, accountsFile);

            Account[] loaded = StatePersister.load(accountsFile);

            System.out.println("\n----- Accounts loaded from file -----");

            for (Account account : loaded) {
                System.out.println(account);
            }

            TransactionLog.append(logFile, "DEPOSIT AC0001 500");
            TransactionLog.append(logFile, "WITHDRAW AC0002 200");
            TransactionLog.append(logFile, "DEPOSIT AC0003 1000");
            System.out.println("\n" + ReportGenerator.generate(logsFolder, reportFile));

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
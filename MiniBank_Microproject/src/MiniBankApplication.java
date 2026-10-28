import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MiniBankApplication {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Id {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Positive {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface MaxLength { int value(); }

    static class BankException extends Exception {
        BankException(String message) { super(message); }
    }

    static class InsufficientFundsException extends BankException {
        private final long shortfall;
        InsufficientFundsException(long shortfall) {
            super("Insufficient funds. Short by " + shortfall);
            this.shortfall = shortfall;
        }
        long getShortfall() { return shortfall; }
    }

    static class InvalidAmountException extends BankException {
        InvalidAmountException(long amount) { super("Invalid amount: " + amount); }
    }

    static class Address implements Serializable {
        private String line;
        private String city;
        private String pincode;

        Address(String line, String city, String pincode) {
            this.line = line;
            this.city = city;
            this.pincode = pincode;
        }

        String getLine() { return line; }
        String getCity() { return city; }
        String getPincode() { return pincode; }

        @Override
        public String toString() { return line + ", " + city + " - " + pincode; }
    }

    static class Customer implements Cloneable, Serializable {
        private String customerId;
        @MaxLength(30)
        private String name;
        private Address address;

        Customer(String customerId, String name, Address address) {
            this.customerId = customerId;
            this.name = name;
            this.address = address;
        }

        String getCustomerId() { return customerId; }
        String getName() { return name; }
        Address getAddress() { return address; }

        @Override
        public Customer clone() {
            try {
                Customer copy = (Customer) super.clone();
                copy.address = new Address(address.getLine(), address.getCity(), address.getPincode());
                return copy;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Customer)) return false;
            Customer other = (Customer) obj;
            return Objects.equals(customerId, other.customerId);
        }

        @Override
        public int hashCode() { return Objects.hash(customerId); }

        @Override
        public String toString() {
            return customerId + " - " + name + " - " + address;
        }
    }

    interface Transactable {
        void deposit(long amount) throws InvalidAmountException;
        boolean withdraw(long amount) throws BankException;
    }

    interface InterestBearing { double interestRate(); }
    interface WithdrawRule { boolean canWithdraw(long amount); }
    interface Premium { long getOverdraftLimit(); }

    static abstract class Account implements Transactable, InterestBearing, WithdrawRule,
            Comparable<Account>, Serializable {
        @Id
        private final String accountNumber;
        @MaxLength(30)
        private String ownerName;
        @Positive
        private long balance;
        private static int accountCounter = 1000;

        Account(String ownerName, long openingBalance) {
            this.accountNumber = String.format("AC%04d", ++accountCounter);
            this.ownerName = ownerName;
            this.balance = openingBalance;
        }
        
        static void updateCounter(String accountNumber) {
            try {
                int number = Integer.parseInt(accountNumber.substring(2));
                if (number > accountCounter) accountCounter = number;
            } catch (RuntimeException ignored) { }
        }

        @Override
        public synchronized void deposit(long amount) throws InvalidAmountException {
            if (amount <= 0) throw new InvalidAmountException(amount);
            balance += amount;
        }

        @Override
        public synchronized boolean withdraw(long amount) throws BankException {
            if (amount <= 0) throw new InvalidAmountException(amount);
            if (!canWithdraw(amount)) throw new InsufficientFundsException(amount - balance);
            balance -= amount;
            return true;
        }

        public void transfer(Account to, long amount) throws BankException {
            if (to == null) throw new BankException("Destination account not found.");
            synchronized (this) {
                if (amount <= 0) throw new InvalidAmountException(amount);
                if (!canWithdraw(amount)) throw new InsufficientFundsException(amount - balance);
                balance -= amount;
            }
            try {
                to.deposit(amount);
            } catch (InvalidAmountException e) {
                synchronized (this) { balance += amount; }
                throw e;
            }
        }

        public synchronized long getBalance() { return balance; }
        String getAccountNumber() { return accountNumber; }
        String getOwnerName() { return ownerName; }
        void setOwnerName(String name) { ownerName = name; }

        @Override
        public int compareTo(Account other) {
            return this.accountNumber.compareTo(other.accountNumber);
        }

        @Override
        public String toString() {
            return accountNumber + " | " + ownerName + " | " + getClass().getSimpleName()
                    + " | Balance: " + getBalance() + " | Interest: " + interestRate() + "%";
        }
    }

    static class SavingsAccount extends Account {
        private long minimumBalance;
        SavingsAccount(String name, long balance, long minimumBalance) {
            super(name, balance); this.minimumBalance = minimumBalance;
        }
        @Override public double interestRate() { return 4.0; }
        @Override public boolean canWithdraw(long amount) {
            return getBalance() - amount >= minimumBalance;
        }
    }

    static class CurrentAccount extends Account implements Premium {
        private long overdraftLimit;
        CurrentAccount(String name, long balance, long overdraftLimit) {
            super(name, balance); this.overdraftLimit = overdraftLimit;
        }
        @Override public double interestRate() { return 0.0; }
        @Override public boolean canWithdraw(long amount) {
            return getBalance() - amount >= -overdraftLimit;
        }
        @Override public long getOverdraftLimit() { return overdraftLimit; }
    }

    static class FixedDepositAccount extends Account {
        FixedDepositAccount(String name, long balance) { super(name, balance); }
        @Override public double interestRate() { return 7.0; }
        @Override public boolean canWithdraw(long amount) { return false; }
    }

    enum TransactionType { DEPOSIT, WITHDRAW, TRANSFER, BALANCE, EXIT }

    static class Command {
        private final TransactionType type;
        private final String accountNumber;
        private final long amount;
        private final String destination;

        Command(TransactionType type, String accountNumber, long amount, String destination) {
            this.type = type; this.accountNumber = accountNumber;
            this.amount = amount; this.destination = destination;
        }
        TransactionType type() { return type; }
        String accountNumber() { return accountNumber; }
        long amount() { return amount; }
        String destination() { return destination; }
    }

    static class CommandParser {
        static Command parse(String input) {
            String[] parts = input.trim().split("\\s+");
            if (parts.length == 0) throw new IllegalArgumentException("Enter a command.");
            TransactionType type = TransactionType.valueOf(parts[0].toUpperCase(Locale.ROOT));
            if (type == TransactionType.EXIT) return new Command(type, "", 0, "");
            if (type == TransactionType.BALANCE) {
                if (parts.length < 2) throw new IllegalArgumentException("Use: BALANCE accountNumber");
                return new Command(type, parts[1], 0, "");
            }
            if (type == TransactionType.TRANSFER) {
                if (parts.length < 4) throw new IllegalArgumentException("Use: TRANSFER fromAccount toAccount amount");
                return new Command(type, parts[1], Long.parseLong(parts[3]), parts[2]);
            }
            if (parts.length < 3) throw new IllegalArgumentException("Use: DEPOSIT/WITHDRAW accountNumber amount");
            return new Command(type, parts[1], Long.parseLong(parts[2]), "");
        }
    }

    static class StatementFormatter {
        static String format(String action, Account account, long amount) {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    + " | " + action + " | " + account.getAccountNumber()
                    + " | Amount: " + amount + " | Balance: " + account.getBalance();
        }
    }

    static class AnnotationValidator {
        static List<String> validate(Object object) {
            List<String> errors = new ArrayList<>();
            Class<?> type = object.getClass();
            while (type != null && type != Object.class) {
                for (Field field : type.getDeclaredFields()) {
                    field.setAccessible(true);
                    try {
                        Object value = field.get(object);
                        if (field.isAnnotationPresent(MaxLength.class) && value instanceof String) {
                            int max = field.getAnnotation(MaxLength.class).value();
                            if (((String) value).length() > max)
                                errors.add(field.getName() + " exceeds " + max + " characters.");
                        }
                        if (field.isAnnotationPresent(Positive.class) && value instanceof Number) {
                            if (((Number) value).longValue() < 0)
                                errors.add(field.getName() + " cannot be negative.");
                        }
                    } catch (IllegalAccessException e) {
                        errors.add("Cannot validate field: " + field.getName());
                    }
                }
                type = type.getSuperclass();
            }
            return errors;
        }
    }

    static class BankSession implements AutoCloseable {
        BankSession() { System.out.println("Bank session opened."); }
        void perform() { System.out.println("Session: operation performed."); }
        @Override public void close() { System.out.println("Bank session closed automatically."); }
    }

    static class Repository<T> {
        private final HashMap<String, T> items = new HashMap<>();

        private String findId(T entity) {
            Class<?> type = entity.getClass();
            while (type != null && type != Object.class) {
                for (Field field : type.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Id.class)) {
                        try {
                            field.setAccessible(true);
                            return String.valueOf(field.get(entity));
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException("Cannot read @Id field.");
                        }
                    }
                }
                type = type.getSuperclass();
            }
            throw new IllegalArgumentException("Entity has no @Id field: " + entity.getClass().getSimpleName());
        }

        T save(T entity) { items.put(findId(entity), entity); return entity; }
        @SafeVarargs final void saveAll(T... entities) { for (T entity : entities) save(entity); }
        T findById(String id) { return items.get(id); }
        ArrayList<T> findAll() { return new ArrayList<>(items.values()); }
        boolean delete(String id) { return items.remove(id) != null; }

        static <N extends Number> double average(N[] values) {
            if (values.length == 0) return 0;
            double total = 0;
            for (N value : values) total += value.doubleValue();
            return total / values.length;
        }
    }

    static class StatePersister {
        static void save(List<Account> accounts, Path file) throws IOException {
            Files.createDirectories(file.getParent());
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(file))) {
                out.writeObject(new ArrayList<>(accounts));
            }
        }

        @SuppressWarnings("unchecked")
        static List<Account> load(Path file) throws IOException, ClassNotFoundException {
            if (!Files.exists(file)) return new ArrayList<>();
            try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
                return (List<Account>) in.readObject();
            }
        }
    }

    static class TransactionLog {
        static void append(Path file, String line) throws IOException {
            Files.createDirectories(file.getParent());
            Files.write(file, (line + System.lineSeparator()).getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    static class ReportGenerator {
        static String generate(Path logFile) throws IOException {
            if (!Files.exists(logFile)) return "No transaction log available.";
            List<String> lines = Files.readAllLines(logFile);
            return "----- Transaction Report (" + lines.size() + " entries) -----\n"
                    + String.join("\n", lines);
        }
    }

    static class AccountWorker implements Runnable {
        private final Account account;
        private final int times;
        private final long amount;

        AccountWorker(Account account, int times, long amount) {
            this.account = account; this.times = times; this.amount = amount;
        }

        @Override public void run() {
            for (int i = 0; i < times; i++) {
                try { account.deposit(amount); }
                catch (InvalidAmountException e) {
                    System.out.println(Thread.currentThread().getName() + ": " + e.getMessage());
                }
            }
        }
    }

    private static final Scanner scanner = new Scanner(System.in);
    private static final Path DATA_DIR = Paths.get("data");
    private static final Path STATE_FILE = DATA_DIR.resolve("accounts.dat");
    private static final Path LOG_FILE = DATA_DIR.resolve("transactions.log");
    private static final Repository<Account> repository = new Repository<>();
    private static final ConcurrentHashMap<String, Account> accountIndex = new ConcurrentHashMap<>();
    private static final ArrayList<String> history = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("=================================");
        System.out.println("       WELCOME TO MINIBANK");
        System.out.println("=================================");
        loadSavedAccounts();

        boolean running = true;
        while (running) {
            showMenu();
            int choice = readInt("Enter choice: ");
            try (BankSession session = new BankSession()) {
                switch (choice) {
                    case 1: openAccount(); break;
                    case 2: deposit(); break;
                    case 3: withdraw(); break;
                    case 4: transfer(); break;
                    case 5: showAccounts(); break;
                    case 6: showBalance(); break;
                    case 7: showTransactionHistory(); break;
                    case 8: saveAccounts(); break;
                    case 9: showReport(); break;
                    case 10: runThreadDemo(); break;
                    case 11: showSortedAccounts(); break;
                    case 12: customerDemo(); break;
                    case 13: quickCommand(); break;
                    case 0: saveAccounts(); running = false; break;
                    default: System.out.println("Invalid choice. Try again.");
                }
                session.perform();
            } catch (Exception e) {
                System.out.println("Operation failed: " + e.getMessage());
            }
        }
        System.out.println("Thank you for using MiniBank.");
    }

    private static void showMenu() {
        System.out.println("\n---------- MINIBANK MENU ----------");
        System.out.println("1. Open Account");
        System.out.println("2. Deposit");
        System.out.println("3. Withdraw");
        System.out.println("4. Transfer");
        System.out.println("5. Display All Accounts");
        System.out.println("6. Check Balance");
        System.out.println("7. Transaction History");
        System.out.println("8. Save Accounts");
        System.out.println("9. Generate Transaction Report");
        System.out.println("10. Run Multithreading Demo");
        System.out.println("11. Sort Accounts");
        System.out.println("12. Customer Clone/Validation Demo");
        System.out.println("13. Quick Command (DEPOSIT/WITHDRAW/TRANSFER/BALANCE)");
        System.out.println("0. Save and Exit");
    }

    private static int readInt(String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextInt()) {
            scanner.next();
            System.out.print("Enter a valid number: ");
        }
        int value = scanner.nextInt();
        scanner.nextLine();
        return value;
    }

    private static long readLong(String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextLong()) {
            scanner.next();
            System.out.print("Enter a valid amount: ");
        }
        long value = scanner.nextLong();
        scanner.nextLine();
        return value;
    }

    private static String readText(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static Account findAccount(String number) {
        return accountIndex.get(number);
    }

    private static void openAccount() throws IOException {
        String owner = readText("Customer name: ");
        if (owner.isEmpty()) { System.out.println("Name cannot be empty."); return; }
        int type = readInt("Account type (1-Savings, 2-Current, 3-Fixed Deposit): ");
        long balance = readLong("Opening balance: ");
        if (balance < 0) { System.out.println("Opening balance cannot be negative."); return; }

        Account account;
        if (type == 1) {
            long minimum = readLong("Minimum balance: ");
            if (minimum < 0 || minimum > balance) {
                System.out.println("Minimum balance must be between zero and opening balance."); return;
            }
            account = new SavingsAccount(owner, balance, minimum);
        } else if (type == 2) {
            long limit = readLong("Overdraft limit: ");
            if (limit < 0) { System.out.println("Overdraft limit cannot be negative."); return; }
            account = new CurrentAccount(owner, balance, limit);
        } else if (type == 3) {
            account = new FixedDepositAccount(owner, balance);
        } else {
            System.out.println("Invalid account type."); return;
        }

        List<String> errors = AnnotationValidator.validate(account);
        if (!errors.isEmpty()) { errors.forEach(System.out::println); return; }
        repository.save(account);
        accountIndex.put(account.getAccountNumber(), account);
        log("ACCOUNT OPENED", account, balance);
        System.out.println("Account created successfully: " + account);
    }

    private static void deposit() throws BankException, IOException {
        Account account = requireAccount(readText("Account number: "));
        long amount = readLong("Deposit amount: ");
        account.deposit(amount);
        log("DEPOSIT", account, amount);
        System.out.println("Deposit successful. Balance: " + account.getBalance());
    }

    private static void withdraw() throws BankException, IOException {
        Account account = requireAccount(readText("Account number: "));
        long amount = readLong("Withdrawal amount: ");
        account.withdraw(amount);
        log("WITHDRAWAL", account, amount);
        System.out.println("Withdrawal successful. Balance: " + account.getBalance());
    }

    private static void transfer() throws BankException, IOException {
        Account from = requireAccount(readText("From account number: "));
        Account to = requireAccount(readText("To account number: "));
        long amount = readLong("Transfer amount: ");
        from.transfer(to, amount);
        log("TRANSFER TO " + to.getAccountNumber(), from, amount);
        log("TRANSFER RECEIVED FROM " + from.getAccountNumber(), to, amount);
        System.out.println("Transfer successful.");
    }

    private static Account requireAccount(String number) throws BankException {
        Account account = findAccount(number);
        if (account == null) throw new BankException("Account not found: " + number);
        return account;
    }

    private static void showAccounts() {
        if (accountIndex.isEmpty()) { System.out.println("No accounts available."); return; }
        for (Account account : repository.findAll()) System.out.println(account);
    }

    private static void showBalance() throws BankException {
        Account account = requireAccount(readText("Account number: "));
        System.out.println("Current balance: " + account.getBalance());
    }

    private static void showTransactionHistory() {
        if (history.isEmpty()) System.out.println("History is loaded from the transaction log.");
        try {
            if (Files.exists(LOG_FILE)) {
                for (String line : Files.readAllLines(LOG_FILE)) System.out.println(line);
            } else System.out.println("No transactions recorded yet.");
        } catch (IOException e) { System.out.println("Could not read history: " + e.getMessage()); }
    }

    private static void saveAccounts() throws IOException {
        StatePersister.save(repository.findAll(), STATE_FILE);
        System.out.println("Accounts saved to " + STATE_FILE);
    }

    private static void loadSavedAccounts() {
        try {
            List<Account> saved = StatePersister.load(STATE_FILE);
            for (Account account : saved) {
                repository.save(account);
                accountIndex.put(account.getAccountNumber(), account);
                Account.updateCounter(account.getAccountNumber());
            }
            if (!saved.isEmpty()) {
                System.out.println(saved.size() + " saved account(s) loaded.");
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("No saved accounts loaded: " + e.getMessage());
        }
    }

    private static void showReport() throws IOException {
        System.out.println(ReportGenerator.generate(LOG_FILE));
    }

    private static void runThreadDemo() throws InterruptedException, IOException {
        Account account;
        if (accountIndex.isEmpty()) {
            account = new SavingsAccount("Thread Demo", 0, 0);
            repository.save(account);
            accountIndex.put(account.getAccountNumber(), account);
        } else {
            account = repository.findAll().get(0);
        }
        long before = account.getBalance();
        int threadCount = 5, times = 100, amount = 10;
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(new AccountWorker(account, times, amount), "Worker-" + (i + 1));
            threads[i].start();
        }
        for (Thread thread : threads) thread.join();
        long expected = before + (long) threadCount * times * amount;
        System.out.println("Expected balance after demo: " + expected);
        System.out.println("Actual balance after demo: " + account.getBalance());
        log("MULTITHREADING DEMO", account, expected - before);
    }

    private static void showSortedAccounts() {
        ArrayList<Account> accounts = repository.findAll();
        if (accounts.isEmpty()) { System.out.println("No accounts available."); return; }
        accounts.sort(Comparator.comparingLong(Account::getBalance));
        Long[] balances = new Long[accounts.size()];
        for (int i = 0; i < accounts.size(); i++) balances[i] = accounts.get(i).getBalance();
        System.out.println("Average balance (bounded generic method): "
                + Repository.average(balances));
        System.out.println("Sorted by balance (Comparator):");
        for (Account account : accounts) System.out.println(account);
        Collections.sort(accounts);
        System.out.println("Sorted by account number (Comparable):");
        for (Account account : accounts) System.out.println(account);
    }


    private static void quickCommand() throws BankException, IOException {
        System.out.println("Commands: DEPOSIT accountNumber amount | WITHDRAW accountNumber amount");
        System.out.println("          TRANSFER fromAccount toAccount amount | BALANCE accountNumber | EXIT");
        String input = readText("Command: ");
        try {
            Command command = CommandParser.parse(input);
            switch (command.type()) {
                case DEPOSIT: {
                    Account account = requireAccount(command.accountNumber());
                    account.deposit(command.amount());
                    log("DEPOSIT (COMMAND)", account, command.amount());
                    System.out.println("Deposit successful. Balance: " + account.getBalance());
                    break;
                }
                case WITHDRAW: {
                    Account account = requireAccount(command.accountNumber());
                    account.withdraw(command.amount());
                    log("WITHDRAWAL (COMMAND)", account, command.amount());
                    System.out.println("Withdrawal successful. Balance: " + account.getBalance());
                    break;
                }
                case TRANSFER: {
                    Account from = requireAccount(command.accountNumber());
                    Account to = requireAccount(command.destination());
                    from.transfer(to, command.amount());
                    log("TRANSFER (COMMAND) TO " + to.getAccountNumber(), from, command.amount());
                    System.out.println("Transfer successful.");
                    break;
                }
                case BALANCE: {
                    Account account = requireAccount(command.accountNumber());
                    System.out.println("Balance: " + account.getBalance());
                    break;
                }
                case EXIT:
                    System.out.println("Returning to main menu.");
                    break;
                default:
                    System.out.println("Unsupported command.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid command: " + e.getMessage());
        }
    }

    private static void customerDemo() {
        String name = readText("Customer name: ");
        String line = readText("Address line: ");
        String city = readText("City: ");
        String pin = readText("Pincode: ");
        Customer customer = new Customer("CU" + System.currentTimeMillis() % 100000, name,
                new Address(line, city, pin));
        Customer copy = customer.clone();
        System.out.println("Original customer: " + customer);
        System.out.println("Cloned customer: " + copy);
        System.out.println("Customer equals clone: " + customer.equals(copy));
        List<String> errors = AnnotationValidator.validate(customer);
        if (errors.isEmpty()) System.out.println("Customer validation passed.");
        else errors.forEach(System.out::println);
    }

    private static void log(String action, Account account, long amount) throws IOException {
        String entry = StatementFormatter.format(action, account, amount);
        history.add(entry);
        TransactionLog.append(LOG_FILE, entry);
    }
}

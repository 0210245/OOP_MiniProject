# MiniBank Java Microproject

## Overview
This is a single console-based MiniBank application assembled from the Java concepts in the 13 provided practicals. It keeps the project at a coursework level and uses only the Java standard library.

## Features
- Open Savings, Current, and Fixed Deposit accounts
- Deposit, withdraw, transfer, and check balances
- Display accounts and sort by balance or account number
- Transaction history and transaction report
- Save and load accounts using Java serialization
- Multithreading demonstration using `Runnable`, `Thread`, `join()`, and synchronized account operations
- Customer/address objects, cloning, `equals()`, and `hashCode()`
- Custom annotations and reflection-based validation
- Generic repository with `@Id` lookup
- Command parser and transaction enum
- Try-with-resources using `AutoCloseable`

## Practical-to-project mapping

| Practical | Concepts represented | Where used |
|---|---|---|
| 1 | Main method, menu, switch, console input/output | `main()` and `showMenu()` |
| 2 | Classes, objects, fields, constructors, methods | `Customer`, `Address`, `Account`, account creation |
| 3 | Nested class, cloning, `equals()`, `hashCode()` | `Customer`, `Address`, `customerDemo()` |
| 4 | Validation logic, enum, command parsing, statement formatting | `TransactionType`, `CommandParser`, `AnnotationValidator`, `StatementFormatter` |
| 5 | Abstract class, inheritance, overriding, polymorphism | `Account`, `SavingsAccount`, `CurrentAccount`, `FixedDepositAccount` |
| 6 | Interfaces and implementation | `Transactable`, `InterestBearing`, `WithdrawRule`, `Premium` |
| 7 | Custom annotations and reflection | `@Id`, `@Positive`, `@MaxLength`, `AnnotationValidator` |
| 8 | Checked exceptions, exception handling, try-with-resources | Banking exceptions and `BankSession implements AutoCloseable` |
| 9 | `Runnable`, `Thread`, thread lifecycle, synchronization | `AccountWorker`, `runThreadDemo()` |
| 10 | Banking transactions, transfer rules, synchronized operations, resource cleanup | `Account.transfer()`, `deposit()`, `withdraw()`, `BankSession` |
| 11 | Serialization, file handling, transaction logging, report generation | `StatePersister`, `TransactionLog`, `ReportGenerator` |
| 12 | Collections, sorting, `Comparable`, `Comparator`, concurrent map | `Repository`, `ArrayList`, `Account.compareTo()`, `showSortedAccounts()`, `ConcurrentHashMap` |
| 13 | Generics, bounded type parameters, varargs, reflection-based ID repository | `Repository<T>`, `saveAll()`, `findById()`, `Repository.average()` |

Some concepts from later practicals naturally overlap with earlier ones, so they are reused in shared classes rather than duplicated in separate files.

## Requirements
- JDK 8 or later.
- No third-party dependencies.

## Compile and run
Open a terminal in the `src` directory:

```bash
javac MiniBankApplication.java
java MiniBankApplication
```

The application creates a `data` folder in the current working directory when accounts are saved or transactions are logged. Account data is stored in `data/accounts.dat`; transaction history is stored in `data/transactions.log`.

## Menu
1. Open Account
2. Deposit
3. Withdraw
4. Transfer
5. Display All Accounts
6. Check Balance
7. Transaction History
8. Save Accounts
9. Generate Transaction Report
10. Run Multithreading Demo
11. Sort Accounts
12. Customer Clone/Validation Demo
13. Quick Command
0. Save and Exit

Quick command examples:
- `DEPOSIT AC1001 500`
- `WITHDRAW AC1001 100`
- `TRANSFER AC1001 AC1002 50`
- `BALANCE AC1001`
- `EXIT`

Account numbers are generated automatically. A Savings Account enforces its configured minimum balance, a Current Account uses its overdraft limit, and a Fixed Deposit Account does not allow withdrawals.

## Verification
The source was compiled with `javac` and a console smoke test was run. The test opened an account, saved it, then restarted the program and confirmed that the saved account loaded and displayed. This is a basic check, not an exhaustive test of every menu path.

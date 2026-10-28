# Object-Oriented Programming (OOP) Mini-Project: MiniBank System

This repository contains the complete practical coursework and final microproject for an Object-Oriented Programming (OOP) course implemented in **Java**. The project showcases an evolutionary approach to software design, scaling a simple console application into a concurrent, persistent, and enterprise-pattern-aligned banking management system across 13 distinct practical stages.

---

##  Project Overview & Objectives
The **MiniBank System** simulates core retail banking operations. It enables users and administrators to perform secure transactions while enforcing rigorous data constraints, input validations, thread safety under multi-threaded concurrency, and binary data persistence.

### Key Architectural Highlights:
* **Object-Oriented Fundamentals:** Strong encapsulation, component composition (Customer and nested Address classes), and polymorphism through abstract base classes (`Account`).
* **Interface-Driven Design:** Decoupled business logic using interfaces like `Transactable`, `InterestBearing`, and `WithdrawRule`.
* **Metaprogramming & Validation:** Custom Java annotations (`@Positive`, `@MaxLength`, `@Id`) coupled with a custom reflection-based `AnnotationValidator`.
* **Robust Exception Handling:** Customized hierarchy of checked and unchecked exceptions (`BankException`, `InsufficientFundsException`, `InvalidAmountException`).
* **Concurrency & Synchronization:** Thread-safe balance updates and race-condition prevention using multi-threaded workers (`AccountWorker`, `ThreadDemo`).
* **Persistence & Auditing:** Binary object serialization (`accounts.dat`), real-time transaction logging (`transactions.log`), and automated report generation.

---

## Repository Structure


OOP_MininProject/
│
├── MiniBank_Microproject/          # Final integrated production microproject
│   └── src/                        # Production source code & compiled binaries
│
├── src/                            # Practical Lab Modules (1 through 13)
│   ├── Practical-1/                # Basic menu interaction & metadata structures
│   ├── Practical-2/                # Encapsulation & access modifiers
│   ├── Practical-3/                # Component composition (Customer & Address)
│   ├── Practical-4/                # Input validation framework
│   ├── Practical-5/                # Inheritance hierarchy (Savings, Current, Fixed Deposit)
│   ├── Practical-6/                # Interface contracts & behavior decoupling
│   ├── Practical-7/                # Custom runtime annotations & reflection validation
│   ├── Practical-8/                # Custom exception handling hierarchies
│   ├── Practical-9/                # Thread concurrency & race conditions (UnsafeAccount)
│   ├── Practical-10/               # Advanced thread synchronization & orchestration
│   ├── Practical-11/               # Binary file I/O serialization & transaction logs
│   ├── Practical-12/               # Modular data storage & transaction processing
│   └── Practical-13/               # Full Repository pattern implementation & reporting
│
└── README.md                       # Project Documentation

Supported Account Types & Financial Rules
Savings Account (SavingsAccount): Tailored for regular savings, enforcing minimum balance boundaries and variable interest calculations via InterestBearing.

Current Account (CurrentAccount): Designed for high-frequency transactions supporting overdraft capabilities and strict daily withdrawal limits (WithdrawRule).

Fixed Deposit Account (FixedDepositAccount): Manages locked-term funds with maturity restrictions and compound interest metrics.

Technologies Used
Language: Java (JDK 8+)

Concepts: OOP Principles, Reflection, Annotations, File Serialization, Multithreading & Synchronization, Design Patterns (Repository, Command, Session).

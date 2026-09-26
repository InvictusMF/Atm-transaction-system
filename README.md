# Apex National Bank - ATM Transaction Simulation System

A comprehensive, production-grade ATM simulation system built natively in **Java** (Java SE 21) fulfilling both project requirements:
1. **Frontend / UI Development**: Realistic physical ATM kiosk enclosure with touchscreen navigation, metallic PIN pad, receipt printer, banknote deposit hopper, and cash dispenser.
2. **Database Design**: Relational 3NF normalized schema, SQL DDL scripts, seed datasets, ER diagram, and ACID transaction guarantees.
3. **Single Output PDF**: Automated compilation of all UI screenshots and database outputs into a unified, submission-ready PDF report (`ATM_Transaction_Simulation_System_Report.pdf`).

---

## 📁 Project Architecture & Directory Layout

```
atm-transaction-system/
│
├── ATM_Transaction_Simulation_System_Report.pdf   <-- Single Submission PDF Report (9 Pages)
├── run.bat                                        <-- Double-click to run Interactive ATM GUI
├── generate_pdf.bat                               <-- Double-click to regenerate Single PDF
│
├── database/                                      <-- Component 2: Database Design
│   ├── schema.sql                                 <-- Relational Schema DDL (Tables, Constraints, Indexes)
│   ├── seed_data.sql                              <-- Seed Records (Customers, Accounts, Cards, Inventory)
│   ├── queries.sql                                <-- Analytical & Operational Verification SQL Queries
│   └── DATABASE_DESIGN.md                         <-- Data Dictionary, ERD, 3NF Proof & ACID Analysis
│
├── screenshots/                                   <-- Exported High-Resolution Screenshots
│   ├── 01_Welcome_Screen.png
│   ├── 02_PIN_Entry_Screen.png
│   ├── 03_Main_Menu_Screen.png
│   ├── 04_Balance_Inquiry_Screen.png
│   ├── 05_Fast_Cash_Screen.png
│   ├── 06_Cash_Withdrawal_Screen.png
│   ├── 07_Cash_Deposit_Screen.png
│   ├── 08_Fund_Transfer_Screen.png
│   ├── 09_Mini_Statement_Screen.png
│   ├── 10_PIN_Change_Screen.png
│   └── 11_Thermal_Receipt_Slip.png
│
└── src/com/atm/
    ├── Main.java                                  <-- CLI & GUI Application Entry Point
    ├── model/                                     <-- Relational Domain Entities
    │   ├── Customer.java
    │   ├── Account.java
    │   ├── AtmCard.java
    │   ├── Transaction.java
    │   ├── CashInventory.java
    │   └── AuditLog.java
    ├── db/
    │   └── DatabaseManager.java                   <-- Thread-safe in-memory Relational DB & ACID Engine
    ├── service/
    │   ├── AtmService.java                        <-- Banking Operations & Security Business Logic
    │   └── AtmSession.java                        <-- Active Cardholder Session State
    ├── ui/                                        <-- Component 1: Frontend / UI Development
    │   ├── UITheme.java                           <-- Colors, Fonts, Rounded Borders & Styles
    │   ├── ScreenManager.java                     <-- Screen Navigator & Keypad Event Router
    │   ├── AtmKioskFrame.java                     <-- Physical ATM Chassis & Bezel Enclosure
    │   └── screens/
    │       ├── WelcomeScreen.java                 <-- EMV Card Insert & Demo Account Selector
    │       ├── PinEntryScreen.java                <-- 4-Digit Bullet Masking & 3-Strike Lockout
    │       ├── MainMenuScreen.java                <-- 8-Operation Touchscreen Menu
    │       ├── FastCashScreen.java                <-- Instant $20-$500 Dispense Presets
    │       ├── WithdrawScreen.java                <-- Custom Cash Withdrawal with Denomination Calculator
    │       ├── DepositScreen.java                 <-- Banknote Hopper Note Counters & Live Totaling
    │       ├── BalanceScreen.java                 <-- Real-time Cleared Balance & Daily Limit Status
    │       ├── TransferScreen.java                <-- Intra-Bank Wire Transfers
    │       ├── MiniStatementScreen.java           <-- Recent Transaction Ledger (Color Coded)
    │       ├── PinChangeScreen.java               <-- PIN Security Update Form
    │       └── ReceiptPopup.java                  <-- Simulated Thermal Paper Receipt
    └── pdf/
        └── PdfReportGenerator.java                <-- Pure Java PDF 1.4 Compiler & Screenshot Stitcher
```

---

## 🚀 How to Run

### Option 1: Launch Interactive ATM Simulator GUI
Double-click `run.bat` or execute in terminal:
```bash
javac -d bin src/com/atm/model/*.java src/com/atm/db/*.java src/com/atm/service/*.java src/com/atm/ui/*.java src/com/atm/ui/screens/*.java src/com/atm/pdf/*.java src/com/atm/Main.java
java -cp bin com.atm.Main
```

### Option 2: Regenerate Single PDF Report with Screenshots
Double-click `generate_pdf.bat` or run:
```bash
java -cp bin com.atm.Main --generate-pdf
```
Output PDF is saved directly to `ATM_Transaction_Simulation_System_Report.pdf`.

---

## 🔑 Demo Accounts for Quick Testing

| Cardholder Name | Card Number | PIN | Linked Account | Balance | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Alexander Vance** | `4532 1100 2233 4455` | `1234` | `ACC-1001-8842` (Savings) | **$5,420.50** | Active |
| **Sophia Williams** | `5412 7500 8899 1234` | `4321` | `ACC-2002-4419` (Checking) | **$3,150.00** | Active |
| **David Miller** | `4000 1234 5678 9010` | `9999` | `ACC-3003-9120` (Savings) | **$820.00** | **Blocked** (3 Fails Demo) |

---

## 🏛 Database Design Highlights
- **Normalized to 3NF**: Zero transitive dependencies; customer profiles, bank accounts, security credentials, transactions, ATM cash inventory, and audit logs are fully decoupled.
- **ACID Compliant**: Atomicity ensured via rollback on failed cash dispensing or transfer failures; Invariants (`balance >= 0`, `denomination count >= 0`) guarded by constraint rules.
- **Cryptographic Security**: PINs stored as one-way SHA-256 hashes; automatic card lockout triggered after 3 consecutive invalid authentication attempts.

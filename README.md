# Bank Management System with Loan Module (Java + Swing + JDBC + MySQL)

## Files
- schema.sql          - creates database `bankdb` and the 2 tables
- src/DBConnection    - JDBC connection (edit USER / PASSWORD here)
- src/Account         - account data + deposit / withdraw
- src/Loan            - abstract loan class (EMI, eligibility, payEmi)
- src/HomeLoan        - 8% rate
- src/PersonalLoan    - 12% rate
- src/Bank            - all database (SQL) work
- src/BankApp         - Swing window (run this)

## Setup
1. Install MySQL and the MySQL Connector/J jar (https://dev.mysql.com/downloads/connector/j/).
   Put the jar in the `lib` folder, e.g. lib/mysql-connector-j-8.4.0.jar
2. Run the SQL:   mysql -u root -p < schema.sql
3. Open src/DBConnection.java and set your MySQL password.

## Compile and run (Windows: use ; instead of : in -cp)
    mkdir out
    javac -cp "lib/*" -d out src/*.java
    java -cp "out:lib/*" BankApp

In NetBeans / Eclipse / IntelliJ: create a project, add the src files, add the
connector jar to the libraries, and run BankApp.

## Rules and Access Controls
- **Roles, Login & Sign Up**:
  - **New Customer Sign Up**: Anyone can click the **Customer Sign Up** tab in the login dialog, enter their Full Name, Initial Deposit, and choose a Password. Upon creation, their generated Account Number is shown, and they are automatically logged in!
  - **Existing Customer**: Login with Account No and Password (default: `1234` for existing accounts). Can view ONLY their own loans, apply for loans, deposit/withdraw for their account, and pay EMI.
  - **Bank Employee**: Login with staff password (`admin` or `admin123`). Can view all accounts, open new accounts, view all loans across the bank, and approve or reject pending loans.
- Eligible if loan amount <= 5 x account balance
- Home loan 8% p.a., personal loan 12% p.a.
- EMI = P*r*(1+r)^n / ((1+r)^n - 1), r = rate/12/100, n = months
- Remaining starts as EMI x months; each EMI paid reduces it; loan CLOSED at 0
- Status flow: PENDING -> APPROVED (money credited) / REJECTED; APPROVED -> CLOSED


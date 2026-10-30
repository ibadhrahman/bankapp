import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * SECTION: Bank class (business logic + database work)
 * The GUI calls these methods. Each method opens a connection, runs one or
 * two SQL statements, and returns a result or a message.
 * The "?" in SQL are placeholders filled by PreparedStatement (safe and simple).
 */
public class Bank {

    public Bank() {
        // Ensure password column exists in accounts table for existing databases
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SHOW COLUMNS FROM accounts LIKE 'password'")) {
            if (!rs.next()) {
                st.executeUpdate("ALTER TABLE accounts ADD COLUMN password VARCHAR(50) NOT NULL DEFAULT '1234'");
            }
        } catch (SQLException ignored) {
        }
    }

    // ---------- ACCOUNT MANAGEMENT MODULE ----------

    // Inserts a new account and returns its generated account number (-1 on error).
    public int openAccount(String name, double initialDeposit, String password) {
        String sql = "INSERT INTO accounts (holder_name, balance, password) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setDouble(2, initialDeposit);
            ps.setString(3, (password == null || password.trim().isEmpty()) ? "1234" : password.trim());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();   // the AUTO_INCREMENT number
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int openAccount(String name, double initialDeposit) {
        return openAccount(name, initialDeposit, "1234");
    }

    // Reads one account from the database. Returns null if it does not exist.
    public Account getAccount(int accountNo) {
        String sql = "SELECT * FROM accounts WHERE account_no = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountNo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Account(rs.getInt("account_no"),
                                   rs.getString("holder_name"),
                                   rs.getDouble("balance"),
                                   rs.getString("password"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Returns all accounts as rows for the table in the GUI.
    public List<Object[]> getAllAccounts() {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT * FROM accounts";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[] { rs.getInt("account_no"),
                                        rs.getString("holder_name"),
                                        rs.getDouble("balance"),
                                        rs.getString("password") });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rows;
    }

    // Saves an account's current balance back to the database.
    private void saveBalance(Account acc) throws SQLException {
        String sql = "UPDATE accounts SET balance = ? WHERE account_no = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, acc.getBalance());
            ps.setInt(2, acc.getAccountNo());
            ps.executeUpdate();
        }
    }

    // ---------- TRANSACTION MODULE ----------

    // Deposit money. Returns a message to show the user.
    public String deposit(int accountNo, double amount) {
        Account acc = getAccount(accountNo);
        if (acc == null) return "Account not found.";
        if (!acc.deposit(amount)) return "Invalid amount.";
        try {
            saveBalance(acc);
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
        return "Deposited " + amount + ". New balance: " + acc.getBalance();
    }

    // Withdraw money. Fails if the balance is too low.
    public String withdraw(int accountNo, double amount) {
        Account acc = getAccount(accountNo);
        if (acc == null) return "Account not found.";
        if (!acc.withdraw(amount)) return "Invalid amount or insufficient balance.";
        try {
            saveBalance(acc);
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
        return "Withdrawn " + amount + ". New balance: " + acc.getBalance();
    }

    // ---------- LOAN MANAGEMENT MODULE ----------

    // Builds the right Loan object (HomeLoan or PersonalLoan) from a type name.
    private Loan makeLoan(int id, int accNo, String type, double principal,
                          int months, double remaining, String status) {
        if (type.equals("HOME")) {
            return new HomeLoan(id, accNo, principal, months, remaining, status);
        }
        return new PersonalLoan(id, accNo, principal, months, remaining, status);
    }

    // Customer applies for a loan: check eligibility, calculate EMI, save as PENDING.
    public String applyLoan(int accountNo, String type, double principal, int months) {
        Account acc = getAccount(accountNo);
        if (acc == null) return "Account not found.";
        if (principal <= 0 || months <= 0) return "Amount and months must be positive.";

        Loan loan = makeLoan(0, accountNo, type, principal, months, 0, "PENDING");

        // 1) Eligibility check (compare loan amount with balance)
        if (!loan.isEligible(acc.getBalance())) {
            return "Not eligible. Loan amount must be at most 5 times the balance ("
                    + (acc.getBalance() * 5) + ").";
        }

        // 2) EMI calculation (polymorphism: HomeLoan or PersonalLoan formula runs)
        double emi = loan.calculateEmi();
        double total = loan.totalPayable();

        // 3) Save the loan. "remaining" starts as the total amount to repay.
        String sql = "INSERT INTO loans (account_no, loan_type, principal, months, remaining, status) "
                   + "VALUES (?, ?, ?, ?, ?, 'PENDING')";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountNo);
            ps.setString(2, type);
            ps.setDouble(3, principal);
            ps.setInt(4, months);
            ps.setDouble(5, total);
            ps.executeUpdate();
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
        return "Eligible! Loan request saved (PENDING).\nRate: " + loan.getRate()
                + "%  EMI: " + emi + "  Total to repay: " + total;
    }

    // Reads one loan row and turns it into a Loan object. Returns null if not found.
    private Loan getLoan(int loanId) {
        String sql = "SELECT * FROM loans WHERE loan_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, loanId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return makeLoan(rs.getInt("loan_id"), rs.getInt("account_no"),
                        rs.getString("loan_type"), rs.getDouble("principal"),
                        rs.getInt("months"), rs.getDouble("remaining"),
                        rs.getString("status"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Updates only the status column of a loan.
    private void setStatus(int loanId, String status) throws SQLException {
        String sql = "UPDATE loans SET status = ? WHERE loan_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, loanId);
            ps.executeUpdate();
        }
    }

    // Staff approves a PENDING loan. The loan amount is added to the customer's account.
    public String approveLoan(int loanId) {
        Loan loan = getLoan(loanId);
        if (loan == null) return "Loan not found.";
        if (!loan.getStatus().equals("PENDING")) return "Only PENDING loans can be approved.";
        Account acc = getAccount(loan.getAccountNo());
        acc.deposit(loan.getPrincipal());
        try {
            saveBalance(acc);
            setStatus(loanId, "APPROVED");
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
        return "Loan " + loanId + " approved. " + loan.getPrincipal() + " added to account "
                + acc.getAccountNo() + ".";
    }

    // Staff rejects a PENDING loan.
    public String rejectLoan(int loanId) {
        Loan loan = getLoan(loanId);
        if (loan == null) return "Loan not found.";
        if (!loan.getStatus().equals("PENDING")) return "Only PENDING loans can be rejected.";
        try {
            setStatus(loanId, "REJECTED");
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
        return "Loan " + loanId + " rejected.";
    }

    // ---------- REPAYMENT MODULE ----------

    // Customer pays one EMI from the account balance. Remaining amount goes down.
    public String payEmi(int loanId) {
        Loan loan = getLoan(loanId);
        if (loan == null) return "Loan not found.";
        if (!loan.getStatus().equals("APPROVED")) return "Loan is not active (status: " + loan.getStatus() + ").";

        Account acc = getAccount(loan.getAccountNo());
        double emi = Math.min(loan.calculateEmi(), loan.getRemaining());
        if (!acc.withdraw(emi)) return "Insufficient balance to pay EMI of " + emi + ".";

        double paid = loan.payEmi();      // reduces remaining; sets CLOSED when finished
        String sql = "UPDATE loans SET remaining = ?, status = ? WHERE loan_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            saveBalance(acc);
            ps.setDouble(1, loan.getRemaining());
            ps.setString(2, loan.getStatus());
            ps.setInt(3, loanId);
            ps.executeUpdate();
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
        return "EMI paid: " + paid + ". Remaining: " + loan.getRemaining()
                + (loan.getStatus().equals("CLOSED") ? "  (Loan fully repaid!)" : "");
    }

    // Returns all loans as rows for the table in the GUI.
    // The EMI column is calculated here using the Loan object.
    public List<Object[]> getAllLoans() {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT * FROM loans";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Loan loan = makeLoan(rs.getInt("loan_id"), rs.getInt("account_no"),
                        rs.getString("loan_type"), rs.getDouble("principal"),
                        rs.getInt("months"), rs.getDouble("remaining"),
                        rs.getString("status"));
                rows.add(new Object[] { loan.getLoanId(), loan.getAccountNo(),
                        rs.getString("loan_type"), loan.getPrincipal(), loan.getMonths(),
                        loan.getRate(), loan.calculateEmi(), loan.getRemaining(),
                        loan.getStatus() });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rows;
    }

    // Returns only the loans belonging to a specific customer account.
    public List<Object[]> getLoansByAccount(int accountNo) {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT * FROM loans WHERE account_no = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountNo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Loan loan = makeLoan(rs.getInt("loan_id"), rs.getInt("account_no"),
                            rs.getString("loan_type"), rs.getDouble("principal"),
                            rs.getInt("months"), rs.getDouble("remaining"),
                            rs.getString("status"));
                    rows.add(new Object[] { loan.getLoanId(), loan.getAccountNo(),
                            rs.getString("loan_type"), loan.getPrincipal(), loan.getMonths(),
                            loan.getRate(), loan.calculateEmi(), loan.getRemaining(),
                            loan.getStatus() });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rows;
    }

    // Checks customer credentials. Returns Account if valid, null otherwise.
    public Account authenticateCustomer(int accountNo, String password) {
        Account acc = getAccount(accountNo);
        if (acc != null && acc.checkPassword(password)) {
            return acc;
        }
        return null;
    }

    // Checks employee credentials.
    public boolean authenticateEmployee(String password) {
        return "admin".equals(password) || "admin123".equals(password);
    }
}

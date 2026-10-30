import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * SECTION: Main GUI class (Swing + AWT)
 * Role-based access:
 *   - Bank Employee: view all accounts, open accounts (with password), view all loans, approve/reject loans.
 *   - Customer (User): logs in with Account No + Password, sees only their loans, applies for loans,
 *                      deposits/withdraws for their own account, pays EMI.
 */
public class BankApp extends JFrame {

    private Bank bank = new Bank();   // all database work is done through this object

    // State for logged-in user
    private boolean isEmployee = false;
    private Account currentCustomer = null;

    // Header bar components
    private JLabel userStatusLabel = new JLabel();

    // Main tabs and panels
    private JTabbedPane tabs = new JTabbedPane();
    private JPanel accountPanel;
    private JPanel transactionPanel;
    private JPanel loanPanel;
    private JPanel repaymentPanel;

    // Components whose visibility or editability changes by role
    private JPanel staffLoanPanel;
    private JTextField loanAccField;
    private JTextField transAccField;

    // Table models
    private DefaultTableModel accountModel = new DefaultTableModel(
            new String[] { "Account No", "Holder Name", "Balance", "Password" }, 0);
    private DefaultTableModel loanModel = new DefaultTableModel(
            new String[] { "Loan ID", "Account", "Type", "Amount", "Months", "Rate %",
                           "EMI", "Remaining", "Status" }, 0);

    public BankApp() {
        setTitle("Bank Management System with Loan Module");
        setSize(850, 520);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);          // open in the centre of the screen
        setLayout(new BorderLayout());

        // Header bar: shows login info and a button to switch between Customer and Employee
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        JButton switchUserButton = new JButton("Logout / Switch User");
        switchUserButton.addActionListener(e -> promptLogin());
        headerPanel.add(userStatusLabel, BorderLayout.WEST);
        headerPanel.add(switchUserButton, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Build all 4 module panels
        accountPanel = buildAccountPanel();
        transactionPanel = buildTransactionPanel();
        loanPanel = buildLoanPanel();
        repaymentPanel = buildRepaymentPanel();

        add(tabs, BorderLayout.CENTER);

        // Prompt for login immediately on startup
        promptLogin();
    }

    // ---------- LOGIN DIALOG ----------
    private void promptLogin() {
        while (true) {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            JTabbedPane loginTabs = new JTabbedPane();

            // Tab 1: Customer Login
            JPanel custPanel = new JPanel(new GridLayout(2, 2, 5, 5));
            JTextField custAccField = new JTextField();
            JPasswordField custPassField = new JPasswordField();
            custPanel.add(new JLabel("Account No:"));
            custPanel.add(custAccField);
            custPanel.add(new JLabel("Password:"));
            custPanel.add(custPassField);
            loginTabs.add("Customer Login", custPanel);

            // Tab 2: New Customer Sign Up
            JPanel signupPanel = new JPanel(new GridLayout(3, 2, 5, 5));
            JTextField signupNameField = new JTextField();
            JTextField signupDepositField = new JTextField("0");
            JPasswordField signupPassField = new JPasswordField();
            signupPanel.add(new JLabel("Full Name:"));
            signupPanel.add(signupNameField);
            signupPanel.add(new JLabel("Initial Deposit:"));
            signupPanel.add(signupDepositField);
            signupPanel.add(new JLabel("Password:"));
            signupPanel.add(signupPassField);
            loginTabs.add("Customer Sign Up", signupPanel);

            // Tab 3: Bank Employee Login
            JPanel empPanel = new JPanel(new GridLayout(2, 2, 5, 5));
            JPasswordField empPassField = new JPasswordField();
            empPanel.add(new JLabel("Staff Password:"));
            empPanel.add(empPassField);
            empPanel.add(new JLabel("(default: admin)"));
            loginTabs.add("Bank Employee Login", empPanel);

            panel.add(loginTabs, BorderLayout.CENTER);

            int choice = JOptionPane.showConfirmDialog(
                    this, panel, "Bank Management System - Login / Sign Up",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (choice != JOptionPane.OK_OPTION) {
                // If the user cancelled and nobody is logged in yet, close application
                if (!isEmployee && currentCustomer == null) {
                    System.exit(0);
                }
                return; // cancel switch user and keep existing session
            }

            int selectedTab = loginTabs.getSelectedIndex();
            if (selectedTab == 0) { // Customer Login
                try {
                    int accNo = Integer.parseInt(custAccField.getText().trim());
                    String pass = new String(custPassField.getPassword()).trim();
                    Account acc = bank.authenticateCustomer(accNo, pass);
                    if (acc != null) {
                        isEmployee = false;
                        currentCustomer = acc;
                        configureUI();
                        break;
                    } else {
                        JOptionPane.showMessageDialog(this, "Invalid Account No or Password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Account No must be a valid number.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                }
            } else if (selectedTab == 1) { // New Customer Sign Up
                try {
                    String name = signupNameField.getText().trim();
                    double deposit = Double.parseDouble(signupDepositField.getText().trim());
                    String pass = new String(signupPassField.getPassword()).trim();
                    if (name.isEmpty() || deposit < 0 || pass.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Enter name, valid deposit, and password.", "Sign Up Failed", JOptionPane.ERROR_MESSAGE);
                        continue;
                    }
                    int newAccNo = bank.openAccount(name, deposit, pass);
                    if (newAccNo > 0) {
                        JOptionPane.showMessageDialog(this,
                                "Account created successfully!\n\nYour Account Number is: " + newAccNo
                                + "\nPlease remember your Account Number to log in.",
                                "Sign Up Successful", JOptionPane.INFORMATION_MESSAGE);
                        isEmployee = false;
                        currentCustomer = bank.getAccount(newAccNo);
                        configureUI();
                        break;
                    } else {
                        JOptionPane.showMessageDialog(this, "Database error: Could not create account.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Initial deposit must be a valid number.", "Sign Up Failed", JOptionPane.ERROR_MESSAGE);
                }
            } else { // Employee Login
                String pass = new String(empPassField.getPassword()).trim();
                if (bank.authenticateEmployee(pass)) {
                    isEmployee = true;
                    currentCustomer = null;
                    configureUI();
                    break;
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid Staff Password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    // Adjust views, tabs, and fields based on role
    private void configureUI() {
        tabs.removeAll();
        if (isEmployee) {
            setTitle("Bank Management System with Loan Module - [Employee Mode]");
            tabs.add("Accounts", accountPanel);
            tabs.add("Transactions", transactionPanel);
            tabs.add("Loans", loanPanel);
            tabs.add("Repayment", repaymentPanel);

            staffLoanPanel.setVisible(true);        // Only bank employees can approve / reject
            loanAccField.setEditable(true);
            loanAccField.setText("");
            transAccField.setEditable(true);
            transAccField.setText("");
        } else {
            setTitle("Bank Management System - Customer: " + currentCustomer.getHolderName());
            tabs.add("Loans", loanPanel);
            tabs.add("Transactions", transactionPanel);
            tabs.add("Repayment", repaymentPanel);

            staffLoanPanel.setVisible(false);       // Hidden for customers
            loanAccField.setText(String.valueOf(currentCustomer.getAccountNo()));
            loanAccField.setEditable(false);
            transAccField.setText(String.valueOf(currentCustomer.getAccountNo()));
            transAccField.setEditable(false);
        }
        refreshTables();
    }

    // ---------- TAB 1: ACCOUNT MANAGEMENT (staff opens and views accounts) ----------
    private JPanel buildAccountPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JTextField nameField = new JTextField(10);
        JTextField depositField = new JTextField(6);
        JTextField passField = new JTextField("1234", 6);
        JButton openButton = new JButton("Open Account");

        JPanel top = new JPanel(new FlowLayout());
        top.add(new JLabel("Holder Name:"));
        top.add(nameField);
        top.add(new JLabel("Initial Deposit:"));
        top.add(depositField);
        top.add(new JLabel("Password:"));
        top.add(passField);
        top.add(openButton);

        openButton.addActionListener(e -> {
            try {
                String name = nameField.getText().trim();
                double amount = Double.parseDouble(depositField.getText().trim());
                String pass = passField.getText().trim();
                if (name.isEmpty() || amount < 0 || pass.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Enter name, valid amount, and password.");
                    return;
                }
                int no = bank.openAccount(name, amount, pass);
                JOptionPane.showMessageDialog(this, "Account opened! Account No: " + no + " | Password: " + pass);
                nameField.setText("");
                depositField.setText("");
                passField.setText("1234");
                refreshTables();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Deposit must be a number.");
            }
        });

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(new JTable(accountModel)), BorderLayout.CENTER);
        return panel;
    }

    // ---------- TAB 2: TRANSACTIONS (deposit and withdraw) ----------
    private JPanel buildTransactionPanel() {
        JPanel panel = new JPanel(new FlowLayout());

        transAccField = new JTextField(8);
        JTextField amountField = new JTextField(8);
        JButton depositButton = new JButton("Deposit");
        JButton withdrawButton = new JButton("Withdraw");

        panel.add(new JLabel("Account No:"));
        panel.add(transAccField);
        panel.add(new JLabel("Amount:"));
        panel.add(amountField);
        panel.add(depositButton);
        panel.add(withdrawButton);

        depositButton.addActionListener(e -> {
            try {
                int acc = Integer.parseInt(transAccField.getText().trim());
                double amt = Double.parseDouble(amountField.getText().trim());
                JOptionPane.showMessageDialog(this, bank.deposit(acc, amt));
                refreshTables();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter valid numbers.");
            }
        });

        withdrawButton.addActionListener(e -> {
            try {
                int acc = Integer.parseInt(transAccField.getText().trim());
                double amt = Double.parseDouble(amountField.getText().trim());
                JOptionPane.showMessageDialog(this, bank.withdraw(acc, amt));
                refreshTables();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter valid numbers.");
            }
        });
        return panel;
    }

    // ---------- TAB 3: LOAN MANAGEMENT ----------
    private JPanel buildLoanPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // --- Customer part: apply for a loan ---
        loanAccField = new JTextField(6);
        JComboBox<String> typeBox = new JComboBox<>(new String[] { "HOME", "PERSONAL" });
        JTextField amountField = new JTextField(8);
        JTextField monthsField = new JTextField(4);
        JButton applyButton = new JButton("Apply for Loan");

        JPanel applyPanel = new JPanel(new FlowLayout());
        applyPanel.add(new JLabel("Account No:"));
        applyPanel.add(loanAccField);
        applyPanel.add(new JLabel("Type:"));
        applyPanel.add(typeBox);
        applyPanel.add(new JLabel("Amount:"));
        applyPanel.add(amountField);
        applyPanel.add(new JLabel("Months:"));
        applyPanel.add(monthsField);
        applyPanel.add(applyButton);

        applyButton.addActionListener(e -> {
            try {
                int acc = Integer.parseInt(loanAccField.getText().trim());
                double amt = Double.parseDouble(amountField.getText().trim());
                int months = Integer.parseInt(monthsField.getText().trim());
                String type = (String) typeBox.getSelectedItem();
                JOptionPane.showMessageDialog(this, bank.applyLoan(acc, type, amt, months));
                refreshTables();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter valid numbers.");
            }
        });

        // --- Staff part: approve or reject by loan id (EMPLOYEE ONLY) ---
        JTextField loanIdField = new JTextField(6);
        JButton approveButton = new JButton("Approve");
        JButton rejectButton = new JButton("Reject");

        staffLoanPanel = new JPanel(new FlowLayout());
        staffLoanPanel.add(new JLabel("Staff - Loan ID:"));
        staffLoanPanel.add(loanIdField);
        staffLoanPanel.add(approveButton);
        staffLoanPanel.add(rejectButton);

        approveButton.addActionListener(e -> {
            try {
                int id = Integer.parseInt(loanIdField.getText().trim());
                JOptionPane.showMessageDialog(this, bank.approveLoan(id));
                refreshTables();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter a valid loan id.");
            }
        });

        rejectButton.addActionListener(e -> {
            try {
                int id = Integer.parseInt(loanIdField.getText().trim());
                JOptionPane.showMessageDialog(this, bank.rejectLoan(id));
                refreshTables();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter a valid loan id.");
            }
        });

        JPanel top = new JPanel(new GridLayout(2, 1));
        top.add(applyPanel);
        top.add(staffLoanPanel);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(new JTable(loanModel)), BorderLayout.CENTER);
        return panel;
    }

    // ---------- TAB 4: REPAYMENT (customer pays EMI) ----------
    private JPanel buildRepaymentPanel() {
        JPanel panel = new JPanel(new FlowLayout());

        JTextField loanIdField = new JTextField(8);
        JButton payButton = new JButton("Pay EMI");

        panel.add(new JLabel("Loan ID:"));
        panel.add(loanIdField);
        panel.add(payButton);
        panel.add(new JLabel("(EMI is taken from account balance. See Loans tab for remaining amount.)"));

        payButton.addActionListener(e -> {
            try {
                int id = Integer.parseInt(loanIdField.getText().trim());
                JOptionPane.showMessageDialog(this, bank.payEmi(id));
                refreshTables();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter a valid loan id.");
            }
        });
        return panel;
    }

    // ---------- Refresh: reload tables based on current role ----------
    private void refreshTables() {
        if (isEmployee) {
            accountModel.setRowCount(0);
            List<Object[]> accounts = bank.getAllAccounts();
            for (Object[] row : accounts) accountModel.addRow(row);

            loanModel.setRowCount(0);
            List<Object[]> loans = bank.getAllLoans();
            for (Object[] row : loans) loanModel.addRow(row);

            userStatusLabel.setText(" Logged in as: Bank Employee (Staff)");
        } else if (currentCustomer != null) {
            // Update latest balance for the customer
            currentCustomer = bank.getAccount(currentCustomer.getAccountNo());

            // Users should only see their loans
            loanModel.setRowCount(0);
            List<Object[]> loans = bank.getLoansByAccount(currentCustomer.getAccountNo());
            for (Object[] row : loans) loanModel.addRow(row);

            userStatusLabel.setText(String.format(" Logged in as: %s (Account: %d | Balance: %.2f)",
                    currentCustomer.getHolderName(), currentCustomer.getAccountNo(), currentCustomer.getBalance()));
        }
    }

    // ---------- main: program starts here ----------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BankApp().setVisible(true));
    }
}

/**
 * SECTION: Account class (Encapsulation)
 * Fields are private; they can only be used through the methods below.
 * deposit() and withdraw() change the balance in memory only;
 * Bank.java saves the new balance to the database.
 */
public class Account {
    private int accountNo;
    private String holderName;
    private double balance;
    private String password;

    public Account(int accountNo, String holderName, double balance, String password) {
        this.accountNo = accountNo;
        this.holderName = holderName;
        this.balance = balance;
        this.password = password;
    }

    // Add money. Returns false if the amount is not positive.
    public boolean deposit(double amount) {
        if (amount <= 0) return false;
        balance += amount;
        return true;
    }

    // Remove money. Fails if the amount is invalid or more than the balance.
    public boolean withdraw(double amount) {
        if (amount <= 0 || amount > balance) return false;
        balance -= amount;
        return true;
    }

    public double getBalance()     { return balance; }
    public int getAccountNo()      { return accountNo; }
    public String getHolderName()  { return holderName; }
    public String getPassword()    { return password; }
    public boolean checkPassword(String pass) {
        return password != null && password.equals(pass);
    }
}

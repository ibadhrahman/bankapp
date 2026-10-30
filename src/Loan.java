/**
 * SECTION: Loan class (Abstraction)
 * An abstract class cannot be created directly. It holds what every loan has
 * in common. HomeLoan and PersonalLoan extend it and supply their own rate.
 */
public abstract class Loan {
    protected int loanId;
    protected int accountNo;
    protected double principal;   // amount borrowed
    protected int months;         // loan duration
    protected double remaining;   // amount still to be paid back
    protected String status;      // PENDING, APPROVED, REJECTED, CLOSED

    public Loan(int loanId, int accountNo, double principal, int months,
                double remaining, String status) {
        this.loanId = loanId;
        this.accountNo = accountNo;
        this.principal = principal;
        this.months = months;
        this.remaining = remaining;
        this.status = status;
    }

    // Each loan type gives its own yearly interest rate (percent).
    public abstract double getRate();

    // Each loan type must provide its EMI (Polymorphism: same method, different result).
    public abstract double calculateEmi();

    // Common EMI formula: P * r * (1+r)^n / ((1+r)^n - 1)
    // r = monthly rate, n = number of months. Subclasses call this with their rate.
    protected double computeEmi(double yearlyRate) {
        double r = yearlyRate / 12 / 100;
        double power = Math.pow(1 + r, months);
        double emi = principal * r * power / (power - 1);
        return Math.round(emi * 100) / 100.0;      // round to 2 decimals
    }

    // Eligibility rule: loan amount must not be more than 5 times the account balance.
    public boolean isEligible(double accountBalance) {
        return principal <= accountBalance * 5;
    }

    // Total money to be repaid over the whole loan = EMI x months.
    public double totalPayable() {
        return Math.round(calculateEmi() * months * 100) / 100.0;
    }

    // Pays one EMI: reduces the remaining amount and returns the amount paid.
    // The last EMI may be smaller so the remaining amount never goes below zero.
    public double payEmi() {
        double emi = calculateEmi();
        if (emi > remaining) emi = remaining;
        remaining = Math.round((remaining - emi) * 100) / 100.0;
        if (remaining <= 0) status = "CLOSED";
        return emi;
    }

    public int getLoanId()       { return loanId; }
    public int getAccountNo()    { return accountNo; }
    public double getPrincipal() { return principal; }
    public int getMonths()       { return months; }
    public double getRemaining() { return remaining; }
    public String getStatus()    { return status; }
}

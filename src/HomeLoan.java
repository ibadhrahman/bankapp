/**
 * SECTION: HomeLoan (Inheritance)
 * Extends Loan. Only the interest rate is different: 8% per year.
 */
public class HomeLoan extends Loan {
    private double rate = 8.0;

    public HomeLoan(int loanId, int accountNo, double principal, int months,
                    double remaining, String status) {
        super(loanId, accountNo, principal, months, remaining, status);
    }

    public double getRate() { return rate; }

    public double calculateEmi() { return computeEmi(rate); }
}

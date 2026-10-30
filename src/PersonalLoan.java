/**
 * SECTION: PersonalLoan (Inheritance)
 * Extends Loan. Personal loans cost more: 12% per year.
 */
public class PersonalLoan extends Loan {
    private double rate = 12.0;

    public PersonalLoan(int loanId, int accountNo, double principal, int months,
                        double remaining, String status) {
        super(loanId, accountNo, principal, months, remaining, status);
    }

    public double getRate() { return rate; }

    public double calculateEmi() { return computeEmi(rate); }
}

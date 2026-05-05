package com.finflow.application.service;

import com.finflow.application.dto.CreditScoreRequest;
import com.finflow.application.entity.CreditScore;
import com.finflow.application.entity.LoanApplication;
import com.finflow.application.exception.ResourceNotFoundException;
import com.finflow.application.repository.CreditScoreRepository;
import com.finflow.application.repository.LoanApplicationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class CreditScoreService {

    private final CreditScoreRepository creditScoreRepository;
    private final LoanApplicationRepository applicationRepository;

    public CreditScoreService(CreditScoreRepository creditScoreRepository,
                               LoanApplicationRepository applicationRepository) {
        this.creditScoreRepository = creditScoreRepository;
        this.applicationRepository = applicationRepository;
    }

    /**
     * Computes and persists a credit score for a given application.
     * Uses a rule-based multi-factor scoring model (mirrors CIBIL principles).
     */
    public CreditScore assess(Long applicationId, Long applicantId, CreditScoreRequest req) {
        LoanApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("LoanApplication", applicationId));

        // Merge request fields with application data (request overrides)
        BigDecimal income       = req.getMonthlyIncome()  != null ? req.getMonthlyIncome()  : app.getMonthlyIncome();
        BigDecimal loanAmount   = req.getLoanAmount()     != null ? req.getLoanAmount()     : app.getLoanAmount();
        Integer    tenure       = req.getTenureMonths()   != null ? req.getTenureMonths()   : app.getTenureMonths();
        String     empType      = req.getEmploymentType() != null ? req.getEmploymentType() : app.getEmploymentType();
        int        existingEmis = req.getExistingEmis()   != null ? req.getExistingEmis()   : 0;
        int        histMonths   = req.getCreditHistoryMonths() != null ? req.getCreditHistoryMonths() : 0;
        String     docStatus    = req.getDocumentStatus() != null ? req.getDocumentStatus() : "SOME_PENDING";

        // ── 1. Income Stability Score (0–100) ────────────────────────────────
        int incomeScore = computeIncomeScore(income, empType);

        // ── 2. Debt-to-Income Score (0–100) ──────────────────────────────────
        int dtiScore = computeDtiScore(income, loanAmount, tenure, existingEmis);

        // ── 3. Employment Score (0–100) ───────────────────────────────────────
        int empScore = computeEmploymentScore(empType, histMonths);

        // ── 4. Document Verification Score (0–100) ───────────────────────────
        int docScore = computeDocumentScore(docStatus);

        // ── 5. Loan Affordability Score (0–100) ──────────────────────────────
        int affordScore = computeAffordabilityScore(income, loanAmount, req.getNumberOfDependents());

        // ── Weighted composite → map to 300–850 ──────────────────────────────
        // Weights: income 25%, DTI 30%, employment 20%, docs 10%, affordability 15%
        double composite = (incomeScore * 0.25)
                         + (dtiScore    * 0.30)
                         + (empScore    * 0.20)
                         + (docScore    * 0.10)
                         + (affordScore * 0.15);

        int finalScore = (int) Math.round(300 + (composite / 100.0) * 550);
        finalScore = Math.max(300, Math.min(850, finalScore));

        // ── Grade & Recommendation ────────────────────────────────────────────
        String grade          = grade(finalScore);
        String recommendation = recommendation(finalScore);

        // ── AI narrative (rule-based, deterministic) ──────────────────────────
        String aiAnalysis = buildNarrative(finalScore, grade, recommendation,
                income, loanAmount, tenure, empType, existingEmis, histMonths,
                incomeScore, dtiScore, empScore, docScore, affordScore, docStatus,
                req.getHasExistingLoan());

        // ── Persist ───────────────────────────────────────────────────────────
        CreditScore cs = new CreditScore();
        cs.setApplicationId(applicationId);
        cs.setApplicantId(applicantId);
        cs.setScore(finalScore);
        cs.setIncomeStabilityScore(incomeScore);
        cs.setDebtToIncomeScore(dtiScore);
        cs.setEmploymentScore(empScore);
        cs.setDocumentVerificationScore(docScore);
        cs.setLoanAffordabilityScore(affordScore);
        cs.setMonthlyIncome(income);
        cs.setLoanAmount(loanAmount);
        cs.setTenureMonths(tenure);
        cs.setEmploymentType(empType);
        cs.setExistingEmis(existingEmis);
        cs.setCreditHistoryMonths(histMonths);
        cs.setAiAnalysis(aiAnalysis);
        cs.setGrade(grade);
        cs.setRecommendation(recommendation);

        return creditScoreRepository.save(cs);
    }

    public Optional<CreditScore> getLatest(Long applicationId) {
        return creditScoreRepository.findTopByApplicationIdOrderByAssessedAtDesc(applicationId);
    }

    public List<CreditScore> getHistory(Long applicationId) {
        return creditScoreRepository.findByApplicationIdOrderByAssessedAtDesc(applicationId);
    }

    // ── Scoring sub-functions ────────────────────────────────────────────────

    private int computeIncomeScore(BigDecimal income, String empType) {
        if (income == null) return 20;
        double inc = income.doubleValue();
        int base;
        if      (inc >= 200000) base = 95;
        else if (inc >= 100000) base = 85;
        else if (inc >= 50000)  base = 72;
        else if (inc >= 25000)  base = 55;
        else if (inc >= 15000)  base = 38;
        else                    base = 20;

        // Stability adjustment by employment type
        if ("SALARIED".equals(empType))       base = Math.min(100, base + 5);
        else if ("SELF_EMPLOYED".equals(empType)) base = Math.max(0, base - 5);
        else if ("FREELANCER".equals(empType))    base = Math.max(0, base - 10);
        else if ("STUDENT".equals(empType))       base = Math.max(0, base - 25);
        else if ("UNEMPLOYED".equals(empType))    base = Math.max(0, base - 35);
        return base;
    }

    private int computeDtiScore(BigDecimal income, BigDecimal loanAmount, Integer tenure, int existingEmis) {
        if (income == null || loanAmount == null || tenure == null || tenure == 0) return 40;
        // Calculate EMI at 12% p.a. as baseline
        double r   = 12.0 / 1200;
        double pow = Math.pow(1 + r, tenure);
        double emi = (loanAmount.doubleValue() * r * pow) / (pow - 1);
        double totalObligation = emi + existingEmis;
        double dtiRatio = totalObligation / income.doubleValue();

        if      (dtiRatio <= 0.20) return 95;
        else if (dtiRatio <= 0.30) return 80;
        else if (dtiRatio <= 0.40) return 65;
        else if (dtiRatio <= 0.50) return 45;
        else if (dtiRatio <= 0.60) return 25;
        else                        return 10;
    }

    private int computeEmploymentScore(String empType, int histMonths) {
        int base;
        switch (empType != null ? empType : "") {
            case "SALARIED":       base = 80; break;
            case "BUSINESS_OWNER": base = 72; break;
            case "SELF_EMPLOYED":  base = 65; break;
            case "FREELANCER":     base = 50; break;
            case "RETIRED":        base = 60; break;
            case "STUDENT":        base = 30; break;
            case "UNEMPLOYED":     base = 10; break;
            default:               base = 40;
        }
        // Credit history bonus
        if      (histMonths >= 60) base = Math.min(100, base + 15);
        else if (histMonths >= 24) base = Math.min(100, base + 8);
        else if (histMonths >= 12) base = Math.min(100, base + 4);
        return base;
    }

    private int computeDocumentScore(String docStatus) {
        if (docStatus == null) return 30;
        return switch (docStatus) {
            case "ALL_VERIFIED"   -> 100;
            case "ALL_UPLOADED"   -> 70;
            case "SOME_PENDING"   -> 45;
            case "NONE_UPLOADED"  -> 10;
            default               -> 30;
        };
    }

    private int computeAffordabilityScore(BigDecimal income, BigDecimal loanAmount, Integer dependents) {
        if (income == null || loanAmount == null) return 40;
        double lti = loanAmount.doubleValue() / (income.doubleValue() * 12); // loan-to-annual-income
        int base;
        if      (lti <= 1.0) base = 95;
        else if (lti <= 2.0) base = 80;
        else if (lti <= 3.0) base = 65;
        else if (lti <= 4.0) base = 48;
        else if (lti <= 5.0) base = 30;
        else                 base = 15;

        int dep = dependents != null ? dependents : 0;
        base = Math.max(0, base - (dep * 5));
        return base;
    }

    private String grade(int score) {
        if (score >= 750) return "EXCELLENT";
        if (score >= 650) return "GOOD";
        if (score >= 550) return "FAIR";
        return "POOR";
    }

    private String recommendation(int score) {
        if (score >= 750) return "STRONGLY_RECOMMENDED";
        if (score >= 650) return "RECOMMENDED";
        if (score >= 550) return "CONDITIONAL";
        return "NOT_RECOMMENDED";
    }

    private String buildNarrative(int score, String grade, String recommendation,
                                   BigDecimal income, BigDecimal loanAmount, Integer tenure,
                                   String empType, int existingEmis, int histMonths,
                                   int incomeScore, int dtiScore, int empScore,
                                   int docScore, int affordScore, String docStatus,
                                   Boolean hasExistingLoan) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREDIT ASSESSMENT SUMMARY\n\n");
        sb.append(String.format("Overall Score: %d/850 (%s)\n", score, grade));
        sb.append(String.format("Recommendation: %s\n\n", recommendation.replace('_', ' ')));

        sb.append("FACTOR ANALYSIS:\n");
        sb.append(String.format("• Income Stability     : %d/100 — ", incomeScore));
        if (incomeScore >= 80) sb.append("Strong income profile.");
        else if (incomeScore >= 60) sb.append("Moderate income; stable but limited headroom.");
        else sb.append("Income level is a concern relative to loan size.");
        sb.append("\n");

        sb.append(String.format("• Debt-to-Income Ratio : %d/100 — ", dtiScore));
        if (dtiScore >= 80) sb.append("Excellent — total obligations well within safe threshold.");
        else if (dtiScore >= 60) sb.append("Manageable, though existing obligations reduce flexibility.");
        else sb.append("High debt burden; repayment risk is elevated.");
        sb.append("\n");

        sb.append(String.format("• Employment Profile   : %d/100 — ", empScore));
        if (empScore >= 75) sb.append("Stable employment type with good history.");
        else if (empScore >= 50) sb.append("Employment type carries moderate risk.");
        else sb.append("Employment profile indicates higher income uncertainty.");
        sb.append("\n");

        sb.append(String.format("• Document Verification: %d/100 — ", docScore));
        if ("ALL_VERIFIED".equals(docStatus)) sb.append("All documents verified — full confidence.");
        else if ("ALL_UPLOADED".equals(docStatus)) sb.append("Documents uploaded; pending verification.");
        else if ("SOME_PENDING".equals(docStatus)) sb.append("Some documents missing; verification incomplete.");
        else sb.append("No documents uploaded — assessment confidence is low.");
        sb.append("\n");

        sb.append(String.format("• Loan Affordability   : %d/100 — ", affordScore));
        if (affordScore >= 80) sb.append("Loan size is very reasonable relative to income.");
        else if (affordScore >= 55) sb.append("Loan is sizeable but within acceptable range.");
        else sb.append("Loan amount is high relative to annual income.");
        sb.append("\n\n");

        if (histMonths > 0) {
            sb.append(String.format("Credit History: %d months of credit history detected.\n", histMonths));
        } else {
            sb.append("Credit History: No prior credit history — thin file risk applies.\n");
        }

        if (Boolean.TRUE.equals(hasExistingLoan)) {
            sb.append("Note: Existing active loan(s) detected — increases total debt exposure.\n");
        }

        sb.append("\nCONCLUSION:\n");
        switch (recommendation) {
            case "STRONGLY_RECOMMENDED" -> sb.append("Applicant demonstrates excellent financial health across all assessed dimensions. Loan approval is strongly recommended with standard terms.");
            case "RECOMMENDED"          -> sb.append("Applicant profile is generally positive. Approval recommended; minor conditions or rate adjustments may apply.");
            case "CONDITIONAL"          -> sb.append("Applicant shows mixed signals. Conditional approval may be considered — recommend requesting additional guarantor or collateral.");
            case "NOT_RECOMMENDED"      -> sb.append("Significant risk factors identified. Loan approval is not recommended under current profile. Applicant may reapply after improving income, clearing existing obligations, or providing stronger documentation.");
        }

        return sb.toString();
    }
}

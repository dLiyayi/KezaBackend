package com.keza.admin.domain.service;

import com.keza.admin.domain.model.DDCheckStatus;
import com.keza.admin.domain.model.DueDiligenceCheck;
import com.keza.admin.domain.model.DueDiligenceReport;
import com.keza.admin.domain.port.out.DueDiligenceCheckRepository;
import com.keza.admin.domain.port.out.DueDiligenceReportRepository;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DueDiligenceService {

    private final DueDiligenceCheckRepository checkRepository;
    private final DueDiligenceReportRepository reportRepository;

    /**
     * Initializes a 144-point due diligence checklist for a campaign across 5 categories:
     * LEGAL (30), FINANCIAL (35), MANAGEMENT (25), MARKET (30), OPERATIONAL (24).
     */
    @Transactional
    public List<DueDiligenceCheck> initializeChecksForCampaign(UUID campaignId) {
        List<DueDiligenceCheck> existing = checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId);
        if (!existing.isEmpty()) {
            throw new BusinessRuleException("Due diligence checks already initialized for campaign: " + campaignId);
        }

        List<DueDiligenceCheck> checks = new ArrayList<>();
        int sortOrder = 0;

        // LEGAL checks (30)
        sortOrder = addChecks(checks, campaignId, "LEGAL", sortOrder, List.of(
                new CheckDef("Business Registration Verification", "Verify company registration with Registrar of Companies", 1.5),
                new CheckDef("Articles of Association Review", "Review articles of association and memorandum for compliance", 1.2),
                new CheckDef("Board Resolutions for Fundraising", "Confirm board has authorized the equity crowdfunding raise", 1.5),
                new CheckDef("Shareholder Agreement Review", "Review existing shareholder agreements for restrictions on share issuance", 1.3),
                new CheckDef("Intellectual Property Rights", "Verify ownership and registration of IP, patents, trademarks", 1.2),
                new CheckDef("Pending Litigation Check", "Search for any pending or historical litigation involving the company", 1.5),
                new CheckDef("Regulatory Licenses and Permits", "Verify all required industry licenses and permits are current", 1.4),
                new CheckDef("Tax Compliance Certificate", "Confirm valid tax compliance certificate from KRA", 1.5),
                new CheckDef("CMA Compliance Review", "Verify compliance with Capital Markets Authority regulations", 1.5),
                new CheckDef("Anti-Money Laundering Screening", "Screen company and directors against AML databases", 1.5),
                new CheckDef("Beneficial Ownership Disclosure", "Verify full disclosure of beneficial owners (25%+ stake)", 1.3),
                new CheckDef("Director Background Checks", "Criminal and credit checks on all company directors", 1.4),
                new CheckDef("Employment Law Compliance", "Review compliance with employment and labor laws", 1.0),
                new CheckDef("Environmental Compliance", "Verify compliance with NEMA and environmental regulations", 1.0),
                new CheckDef("Data Protection Compliance", "Review compliance with Kenya Data Protection Act 2019", 1.1),
                new CheckDef("Contract Review - Key Clients", "Review material contracts with key clients for risks", 1.2),
                new CheckDef("Contract Review - Key Suppliers", "Review material contracts with key suppliers for dependencies", 1.1),
                new CheckDef("Lease and Property Agreements", "Review all property leases and real estate agreements", 1.0),
                new CheckDef("Insurance Coverage Review", "Verify adequate insurance coverage for business operations", 1.0),
                new CheckDef("Debt Instruments Review", "Review all loan agreements and debt instruments for covenants", 1.3),
                new CheckDef("Share Capital Structure", "Verify authorized and issued share capital structure", 1.4),
                new CheckDef("Previous Fundraising Compliance", "Review compliance of any previous fundraising rounds", 1.2),
                new CheckDef("Offering Document Legal Review", "Legal review of the crowdfunding offering document", 1.5),
                new CheckDef("Investor Rights Documentation", "Verify investor rights are properly documented", 1.4),
                new CheckDef("Exit Mechanism Documentation", "Verify clear exit mechanisms are documented for investors", 1.3),
                new CheckDef("Related Party Transactions Disclosure", "Review and verify all related party transactions", 1.3),
                new CheckDef("Sanctions Screening", "Screen against international sanctions lists (OFAC, UN, EU)", 1.5),
                new CheckDef("Corporate Governance Structure", "Evaluate corporate governance framework and practices", 1.2),
                new CheckDef("Statutory Filings Up-to-Date", "Verify all statutory filings with BRS are current", 1.1),
                new CheckDef("Dispute Resolution Mechanisms", "Review dispute resolution clauses in key agreements", 1.0)
        ));

        // FINANCIAL checks (35)
        sortOrder = addChecks(checks, campaignId, "FINANCIAL", sortOrder, List.of(
                new CheckDef("Audited Financial Statements (3 years)", "Review audited financial statements for past 3 years", 1.5),
                new CheckDef("Revenue Growth Analysis", "Analyze revenue trends and growth trajectory", 1.4),
                new CheckDef("Profitability Assessment", "Assess gross and net profit margins and trends", 1.4),
                new CheckDef("Cash Flow Analysis", "Analyze operating, investing, and financing cash flows", 1.5),
                new CheckDef("Working Capital Assessment", "Evaluate current ratio and working capital adequacy", 1.3),
                new CheckDef("Debt-to-Equity Ratio", "Assess leverage and debt sustainability", 1.4),
                new CheckDef("Revenue Concentration Risk", "Evaluate customer concentration and revenue diversity", 1.3),
                new CheckDef("Accounts Receivable Aging", "Review aging analysis and collectability of receivables", 1.2),
                new CheckDef("Inventory Valuation", "Verify inventory valuation methods and obsolescence risk", 1.1),
                new CheckDef("Fixed Asset Verification", "Verify existence and valuation of fixed assets", 1.2),
                new CheckDef("Tax Returns Review (3 years)", "Review tax returns for past 3 fiscal years", 1.3),
                new CheckDef("Outstanding Tax Liabilities", "Verify no outstanding tax disputes or liabilities", 1.4),
                new CheckDef("Budget vs Actual Analysis", "Compare budgeted vs actual performance for credibility", 1.2),
                new CheckDef("Financial Projections Review", "Evaluate reasonableness of 3-5 year financial projections", 1.5),
                new CheckDef("Valuation Methodology Review", "Assess the valuation methodology and assumptions used", 1.5),
                new CheckDef("Use of Funds Plan", "Review detailed plan for how raised funds will be deployed", 1.5),
                new CheckDef("Unit Economics Validation", "Validate unit economics (CAC, LTV, margins) claimed", 1.4),
                new CheckDef("Burn Rate Analysis", "Assess monthly burn rate and runway with/without raise", 1.4),
                new CheckDef("Break-Even Analysis", "Review break-even timeline and assumptions", 1.3),
                new CheckDef("Capital Expenditure Plan", "Review planned capital expenditure and necessity", 1.2),
                new CheckDef("Dividend Policy Review", "Assess stated dividend policy and feasibility", 1.1),
                new CheckDef("Related Party Financial Transactions", "Quantify and assess financial transactions with related parties", 1.3),
                new CheckDef("Off-Balance Sheet Items", "Identify any off-balance sheet liabilities or commitments", 1.3),
                new CheckDef("Contingent Liabilities Assessment", "Assess potential contingent liabilities and exposure", 1.3),
                new CheckDef("Foreign Currency Exposure", "Evaluate foreign currency risks and hedging strategies", 1.1),
                new CheckDef("Banking Relationships Review", "Review banking relationships and facility terms", 1.1),
                new CheckDef("Internal Controls Assessment", "Evaluate adequacy of internal financial controls", 1.2),
                new CheckDef("Accounting Policies Review", "Review accounting policies for appropriateness and consistency", 1.2),
                new CheckDef("Going Concern Assessment", "Assess going concern status and auditor opinions", 1.5),
                new CheckDef("Funding Gap Analysis", "Analyze if raise amount is sufficient for stated objectives", 1.4),
                new CheckDef("Comparable Company Analysis", "Benchmark financial metrics against comparable companies", 1.3),
                new CheckDef("Sensitivity Analysis Review", "Review sensitivity analysis on key financial assumptions", 1.3),
                new CheckDef("Historical Fundraising Returns", "Review returns delivered to investors in previous rounds", 1.2),
                new CheckDef("Financial Reporting Quality", "Assess quality and timeliness of financial reporting", 1.2),
                new CheckDef("Statutory Audit Quality", "Evaluate quality and reputation of statutory auditors", 1.1)
        ));

        // MANAGEMENT checks (25)
        sortOrder = addChecks(checks, campaignId, "MANAGEMENT", sortOrder, List.of(
                new CheckDef("CEO Background and Track Record", "Evaluate CEO's experience, qualifications, and track record", 1.5),
                new CheckDef("Leadership Team Experience", "Assess collective experience and complementarity of leadership team", 1.4),
                new CheckDef("Key Person Dependency Risk", "Evaluate reliance on specific individuals and succession plans", 1.4),
                new CheckDef("Board Composition Assessment", "Review board composition for independence and expertise", 1.3),
                new CheckDef("Board Meeting Frequency", "Verify regular board meetings and documented minutes", 1.1),
                new CheckDef("Management Compensation Review", "Review management compensation relative to industry norms", 1.2),
                new CheckDef("Employee Stock Option Plans", "Review ESOP structure and dilution impact on new investors", 1.3),
                new CheckDef("Organizational Structure Review", "Evaluate organizational design and reporting lines", 1.1),
                new CheckDef("Key Hires Plan", "Review plan for key hires post-fundraising", 1.2),
                new CheckDef("Employee Retention Metrics", "Assess employee turnover rates and retention strategies", 1.2),
                new CheckDef("Management References", "Contact professional references for key management", 1.3),
                new CheckDef("Director Conflict of Interest", "Screen for conflicts of interest among directors", 1.4),
                new CheckDef("Advisory Board Assessment", "Evaluate quality and engagement of advisory board", 1.0),
                new CheckDef("Technical Team Capability", "Assess technical team skills and capability for execution", 1.3),
                new CheckDef("Culture and Values Assessment", "Evaluate company culture and alignment with stated values", 1.0),
                new CheckDef("Diversity and Inclusion Review", "Review D&I policies and representation in leadership", 1.0),
                new CheckDef("Management Equity Holdings", "Verify management team equity stakes and vesting schedules", 1.3),
                new CheckDef("Previous Venture Experience", "Assess management's experience with scaling ventures", 1.3),
                new CheckDef("Industry Network and Relationships", "Evaluate management's industry connections and partnerships", 1.1),
                new CheckDef("Crisis Management Capability", "Assess management's preparedness for crisis scenarios", 1.1),
                new CheckDef("Founder-Market Fit", "Evaluate alignment between founders' background and market", 1.3),
                new CheckDef("Succession Planning", "Review succession plans for key leadership positions", 1.2),
                new CheckDef("Professional Certifications", "Verify professional certifications and qualifications claimed", 1.1),
                new CheckDef("Public Records and Media Check", "Screen management team in public records and media", 1.2),
                new CheckDef("Strategic Vision Clarity", "Assess clarity and feasibility of management's strategic vision", 1.2)
        ));

        // MARKET checks (30)
        sortOrder = addChecks(checks, campaignId, "MARKET", sortOrder, List.of(
                new CheckDef("Total Addressable Market Validation", "Validate TAM size claims with independent data sources", 1.5),
                new CheckDef("Serviceable Addressable Market Analysis", "Assess realistic SAM and market penetration potential", 1.4),
                new CheckDef("Market Growth Rate Verification", "Verify stated market growth rates with credible sources", 1.3),
                new CheckDef("Competitive Landscape Analysis", "Map key competitors and assess competitive dynamics", 1.4),
                new CheckDef("Competitive Advantage Sustainability", "Evaluate moats and sustainability of competitive advantages", 1.5),
                new CheckDef("Customer Validation", "Verify customer testimonials and reference checks", 1.4),
                new CheckDef("Customer Acquisition Cost Analysis", "Validate customer acquisition costs and channels", 1.3),
                new CheckDef("Customer Retention Rate", "Verify customer retention and churn metrics", 1.4),
                new CheckDef("Net Promoter Score", "Assess customer satisfaction through NPS or equivalent", 1.2),
                new CheckDef("Product-Market Fit Evidence", "Evaluate evidence of product-market fit (metrics, traction)", 1.5),
                new CheckDef("Pricing Strategy Assessment", "Evaluate pricing strategy and elasticity", 1.2),
                new CheckDef("Distribution Channel Analysis", "Assess distribution channels and go-to-market strategy", 1.3),
                new CheckDef("Market Entry Barriers", "Evaluate barriers to entry protecting the business", 1.3),
                new CheckDef("Regulatory Environment Assessment", "Assess regulatory environment and future regulatory risks", 1.4),
                new CheckDef("Technology Disruption Risk", "Evaluate risk of technological disruption to the business model", 1.3),
                new CheckDef("Geographic Market Analysis", "Assess geographic focus and expansion potential in East Africa", 1.2),
                new CheckDef("Macroeconomic Risk Assessment", "Evaluate exposure to macroeconomic risks (inflation, FX, rates)", 1.2),
                new CheckDef("Industry Lifecycle Stage", "Determine industry lifecycle stage and implications", 1.1),
                new CheckDef("Supply Chain Risk Assessment", "Evaluate supply chain resilience and supplier dependencies", 1.3),
                new CheckDef("Seasonal and Cyclical Patterns", "Assess impact of seasonality and business cycles", 1.1),
                new CheckDef("Partnership and Alliance Review", "Review strategic partnerships and their value contribution", 1.2),
                new CheckDef("Brand Strength Assessment", "Evaluate brand recognition and reputation in target market", 1.2),
                new CheckDef("Social Media and Digital Presence", "Assess online presence, engagement metrics, and sentiment", 1.1),
                new CheckDef("Market Share Trajectory", "Evaluate current market share and growth trajectory", 1.3),
                new CheckDef("Substitute Products Analysis", "Assess threat from substitute products or services", 1.2),
                new CheckDef("Buyer Concentration Risk", "Evaluate dependency on small number of large buyers", 1.3),
                new CheckDef("Export Market Potential", "Assess potential for export or cross-border expansion", 1.1),
                new CheckDef("Impact and ESG Assessment", "Evaluate social impact and ESG alignment", 1.1),
                new CheckDef("Market Timing Assessment", "Assess whether market timing is favorable for the raise", 1.2),
                new CheckDef("Sector-Specific Risk Factors", "Identify and evaluate sector-specific risk factors", 1.3)
        ));

        // OPERATIONAL checks (24)
        sortOrder = addChecks(checks, campaignId, "OPERATIONAL", sortOrder, List.of(
                new CheckDef("Technology Stack Assessment", "Review technology infrastructure, stack, and scalability", 1.4),
                new CheckDef("IT Security and Data Protection", "Assess cybersecurity measures and data protection", 1.4),
                new CheckDef("Business Continuity Plan", "Review business continuity and disaster recovery plans", 1.3),
                new CheckDef("Quality Management System", "Evaluate quality management processes and certifications", 1.2),
                new CheckDef("Operational Scalability Assessment", "Assess ability to scale operations with growth", 1.4),
                new CheckDef("Key Operational Metrics Review", "Review KPIs and operational performance metrics", 1.3),
                new CheckDef("Supply Chain Management", "Evaluate supply chain management and procurement processes", 1.2),
                new CheckDef("Physical Infrastructure Review", "Assess adequacy of physical facilities and equipment", 1.1),
                new CheckDef("Health and Safety Compliance", "Verify compliance with occupational health and safety standards", 1.1),
                new CheckDef("Customer Service Capability", "Assess customer service infrastructure and satisfaction", 1.2),
                new CheckDef("Process Documentation", "Evaluate documentation of key business processes and SOPs", 1.1),
                new CheckDef("Vendor Management Review", "Review vendor relationships, contracts, and dependencies", 1.2),
                new CheckDef("Inventory Management Assessment", "Evaluate inventory management processes and efficiency", 1.1),
                new CheckDef("Logistics and Distribution Review", "Assess logistics capabilities and distribution efficiency", 1.2),
                new CheckDef("R&D Pipeline and Innovation", "Review R&D activities, pipeline, and innovation capacity", 1.3),
                new CheckDef("Product Roadmap Feasibility", "Assess feasibility and alignment of product roadmap", 1.3),
                new CheckDef("Operational Risk Register", "Review identified operational risks and mitigation plans", 1.3),
                new CheckDef("Compliance Monitoring System", "Evaluate systems for ongoing regulatory compliance monitoring", 1.2),
                new CheckDef("Financial Systems and ERP", "Assess financial management systems and reporting tools", 1.2),
                new CheckDef("Human Resources Systems", "Review HR systems, policies, and people management", 1.1),
                new CheckDef("Intellectual Property Protection Ops", "Evaluate operational measures for IP protection", 1.2),
                new CheckDef("Third-Party Dependency Assessment", "Assess dependencies on third-party services and platforms", 1.3),
                new CheckDef("Capacity Utilization Analysis", "Evaluate current capacity utilization and growth room", 1.2),
                new CheckDef("Operational Efficiency Benchmarking", "Benchmark operational efficiency against industry standards", 1.2)
        ));

        log.info("Initialized {} due diligence checks for campaign {}", checks.size(), campaignId);
        return checkRepository.saveAll(checks);
    }

    @Transactional
    public DueDiligenceCheck updateCheck(UUID checkId, DDCheckStatus status, String notes, UUID reviewerId) {
        DueDiligenceCheck check = checkRepository.findById(checkId)
                .orElseThrow(() -> new ResourceNotFoundException("DueDiligenceCheck", checkId));

        check.markAs(status, notes, reviewerId);
        return checkRepository.save(check);
    }

    @Transactional
    public DueDiligenceReport generateReport(UUID campaignId, UUID adminId) {
        List<DueDiligenceCheck> checks = checkRepository.findByCampaignIdOrderBySortOrderAsc(campaignId);
        if (checks.isEmpty()) {
            throw new BusinessRuleException("No due diligence checks found for campaign: " + campaignId);
        }

        long pendingCount = checks.stream().filter(c -> c.getStatus() == DDCheckStatus.PENDING).count();
        if (pendingCount > 0) {
            throw new BusinessRuleException(
                    String.format("Cannot generate report: %d checks are still PENDING", pendingCount));
        }

        int totalChecks = checks.size();
        int passedChecks = (int) checks.stream().filter(c -> c.getStatus() == DDCheckStatus.PASSED).count();
        int failedChecks = (int) checks.stream().filter(c -> c.getStatus() == DDCheckStatus.FAILED).count();
        int naChecks = (int) checks.stream().filter(c -> c.getStatus() == DDCheckStatus.NOT_APPLICABLE).count();

        // Calculate weighted score: sum(weight * passed) / sum(weight * applicable)
        BigDecimal weightedPassedSum = checks.stream()
                .filter(c -> c.getStatus() == DDCheckStatus.PASSED)
                .map(DueDiligenceCheck::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal weightedApplicableSum = checks.stream()
                .filter(c -> c.getStatus() != DDCheckStatus.NOT_APPLICABLE)
                .map(DueDiligenceCheck::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal overallScore = BigDecimal.ZERO;
        if (weightedApplicableSum.compareTo(BigDecimal.ZERO) > 0) {
            overallScore = weightedPassedSum
                    .divide(weightedApplicableSum, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        String riskLevel = determineRiskLevel(overallScore);
        String recommendation = determineRecommendation(overallScore, failedChecks);

        String summary = buildSummary(totalChecks, passedChecks, failedChecks, naChecks, overallScore, riskLevel, recommendation, checks);

        DueDiligenceReport report = reportRepository.findByCampaignId(campaignId)
                .orElse(DueDiligenceReport.builder().campaignId(campaignId).build());

        report.setTotalChecks(totalChecks);
        report.setPassedChecks(passedChecks);
        report.setFailedChecks(failedChecks);
        report.setNaChecks(naChecks);
        report.setOverallScore(overallScore);
        report.setRiskLevel(riskLevel);
        report.setRecommendation(recommendation);
        report.setSummary(summary);
        report.setGeneratedBy(adminId);
        report.setGeneratedAt(Instant.now());

        log.info("Generated due diligence report for campaign {}: score={}, risk={}, recommendation={}",
                campaignId, overallScore, riskLevel, recommendation);

        return reportRepository.save(report);
    }

    private String determineRiskLevel(BigDecimal score) {
        if (score.compareTo(new BigDecimal("85")) >= 0) return "LOW";
        if (score.compareTo(new BigDecimal("70")) >= 0) return "MEDIUM";
        if (score.compareTo(new BigDecimal("50")) >= 0) return "HIGH";
        return "CRITICAL";
    }

    private String determineRecommendation(BigDecimal score, int failedChecks) {
        if (score.compareTo(new BigDecimal("80")) >= 0 && failedChecks <= 5) {
            return "APPROVE";
        }
        if (score.compareTo(new BigDecimal("60")) >= 0 && failedChecks <= 15) {
            return "CONDITIONAL_APPROVE";
        }
        return "REJECT";
    }

    private String buildSummary(int total, int passed, int failed, int na,
                                BigDecimal score, String riskLevel, String recommendation,
                                List<DueDiligenceCheck> checks) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Due Diligence Report Summary%n"));
        sb.append(String.format("============================%n"));
        sb.append(String.format("Total Checks: %d | Passed: %d | Failed: %d | N/A: %d%n", total, passed, failed, na));
        sb.append(String.format("Overall Weighted Score: %s%%%n", score));
        sb.append(String.format("Risk Level: %s%n", riskLevel));
        sb.append(String.format("Recommendation: %s%n%n", recommendation));

        // Category breakdown
        Map<String, int[]> categoryStats = new LinkedHashMap<>();
        for (DueDiligenceCheck check : checks) {
            categoryStats.computeIfAbsent(check.getCategory(), k -> new int[3]);
            int[] stats = categoryStats.get(check.getCategory());
            if (check.getStatus() == DDCheckStatus.PASSED) stats[0]++;
            else if (check.getStatus() == DDCheckStatus.FAILED) stats[1]++;
            else if (check.getStatus() == DDCheckStatus.NOT_APPLICABLE) stats[2]++;
        }

        sb.append("Category Breakdown:%n".formatted());
        categoryStats.forEach((category, stats) -> {
            int catTotal = stats[0] + stats[1] + stats[2];
            int applicable = stats[0] + stats[1];
            double catRate = applicable > 0 ? (stats[0] * 100.0 / applicable) : 0;
            sb.append(String.format("  %s: %d passed, %d failed, %d N/A (%.1f%% pass rate)%n",
                    category, stats[0], stats[1], stats[2], catRate));
        });

        // List failed checks
        List<DueDiligenceCheck> failedChecks = checks.stream()
                .filter(c -> c.getStatus() == DDCheckStatus.FAILED)
                .toList();
        if (!failedChecks.isEmpty()) {
            sb.append(String.format("%nFailed Checks:%n"));
            for (DueDiligenceCheck fc : failedChecks) {
                sb.append(String.format("  - [%s] %s", fc.getCategory(), fc.getCheckName()));
                if (fc.getNotes() != null && !fc.getNotes().isBlank()) {
                    sb.append(String.format(": %s", fc.getNotes()));
                }
                sb.append(String.format("%n"));
            }
        }

        return sb.toString();
    }

    private int addChecks(List<DueDiligenceCheck> checks, UUID campaignId, String category,
                          int startSortOrder, List<CheckDef> definitions) {
        int sortOrder = startSortOrder;
        for (CheckDef def : definitions) {
            checks.add(DueDiligenceCheck.builder()
                    .campaignId(campaignId)
                    .category(category)
                    .checkName(def.name)
                    .description(def.description)
                    .weight(BigDecimal.valueOf(def.weight))
                    .sortOrder(sortOrder++)
                    .build());
        }
        return sortOrder;
    }

    private record CheckDef(String name, String description, double weight) {}
}

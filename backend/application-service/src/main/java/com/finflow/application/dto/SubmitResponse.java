package com.finflow.application.dto;

import com.finflow.application.entity.LoanApplication;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Returned by POST /applications/{id}/submit.
 * In addition to the updated application, it provides explicit next-step
 * guidance so the client knows exactly where to go for document upload.
 */
@Schema(description = "Response after a loan application is successfully submitted. " +
                       "Contains the application plus guided next-step details for document upload.")
public class SubmitResponse {

    @Schema(description = "The updated loan application with status = SUBMITTED")
    private LoanApplication application;

    @Schema(description = "Human-readable instruction for the next step",
            example = "Your application has been submitted. Please upload the required documents to proceed.")
    private String nextStep;

    @Schema(description = "The API endpoint to use for uploading documents",
            example = "/documents/upload?applicationId=42")
    private String documentUploadUrl;

    @Schema(description = "List of document types required to complete the application",
            example = "[\"IDENTITY_PROOF\",\"INCOME_PROOF\",\"ADDRESS_PROOF\",\"BANK_STATEMENT\"]")
    private List<String> requiredDocuments;

    @Schema(description = "Step number in the overall workflow", example = "2")
    private int workflowStep;

    @Schema(description = "Total number of steps in the workflow", example = "3")
    private int totalWorkflowSteps;

    public SubmitResponse() {}

    public SubmitResponse(LoanApplication application,
                          String nextStep,
                          String documentUploadUrl,
                          List<String> requiredDocuments) {
        this.application       = application;
        this.nextStep          = nextStep;
        this.documentUploadUrl = documentUploadUrl;
        this.requiredDocuments = requiredDocuments;
        this.workflowStep      = 2;
        this.totalWorkflowSteps = 3;
    }

    public LoanApplication getApplication()            { return application; }
    public void setApplication(LoanApplication a)      { this.application = a; }

    public String getNextStep()                        { return nextStep; }
    public void setNextStep(String nextStep)           { this.nextStep = nextStep; }

    public String getDocumentUploadUrl()               { return documentUploadUrl; }
    public void setDocumentUploadUrl(String u)         { this.documentUploadUrl = u; }

    public List<String> getRequiredDocuments()         { return requiredDocuments; }
    public void setRequiredDocuments(List<String> d)   { this.requiredDocuments = d; }

    public int getWorkflowStep()                       { return workflowStep; }
    public void setWorkflowStep(int s)                 { this.workflowStep = s; }

    public int getTotalWorkflowSteps()                 { return totalWorkflowSteps; }
    public void setTotalWorkflowSteps(int t)           { this.totalWorkflowSteps = t; }
}

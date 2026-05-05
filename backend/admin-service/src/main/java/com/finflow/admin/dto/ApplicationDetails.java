package com.finflow.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Lightweight projection of a LoanApplication as returned by application-service.
 * Only the fields needed for notification are mapped; extra JSON fields are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationDetails {

    private Long id;
    private Long applicantId;
    private String fullName;
    private String email;
    private String status;

    public Long getId()                    { return id; }
    public void setId(Long id)             { this.id = id; }

    public Long getApplicantId()           { return applicantId; }
    public void setApplicantId(Long v)     { this.applicantId = v; }

    public String getFullName()            { return fullName; }
    public void setFullName(String v)      { this.fullName = v; }

    public String getEmail()               { return email; }
    public void setEmail(String v)         { this.email = v; }

    public String getStatus()              { return status; }
    public void setStatus(String v)        { this.status = v; }
}

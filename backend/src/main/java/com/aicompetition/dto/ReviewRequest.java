package com.aicompetition.dto;

/**
 * 人工修整审批请求：通过/驳回 + 审批意见 + 审批人角色（仅核保主管）。
 */
public class ReviewRequest {

    private Boolean pass;
    private String comment;
    private String role;

    public Boolean getPass() { return pass; }
    public void setPass(Boolean pass) { this.pass = pass; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}

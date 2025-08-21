package com.ExamPort.ExamPort.Entity;

public enum ContactMessageStatus {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    REPLIED("Replied");

    private final String displayName;

    ContactMessageStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
package org.rti.ttfinder.models;

public class ProcessedImageResult {
    boolean isSuccess;
    String logFileName;

    public ProcessedImageResult() {
    }

    public ProcessedImageResult(boolean isSuccess, String logFileName) {
        this.isSuccess = isSuccess;
        this.logFileName = logFileName;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public String getLogFileName() {
        return logFileName;
    }

    public void setLogFileName(String logFileName) {
        this.logFileName = logFileName;
    }
}

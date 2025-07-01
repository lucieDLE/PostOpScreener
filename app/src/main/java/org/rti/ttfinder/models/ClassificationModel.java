package org.rti.ttfinder.models;

public class ClassificationModel {

    private String imageName, classificationResult, startedTime, endedTime, logFileName;
    private ImageModel image;
    private boolean isBlurred;
    private boolean isSuccess = false;

    public ClassificationModel() {
    }

    public ClassificationModel(ImageModel image, boolean isBlurred, String classificationResult) {
        this.classificationResult = classificationResult;
        this.image = image;
        this.isBlurred = isBlurred;
    }

    public ClassificationModel(String imageName, String classificationResult, String startedTime, String endedTime) {
        this.imageName = imageName;
        this.classificationResult = classificationResult;
        this.startedTime = startedTime;
        this.endedTime = endedTime;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getClassificationResult() {
        return classificationResult;
    }

    public void setClassificationResult(String classificationResult) {
        this.classificationResult = classificationResult;
    }

    public String getStartedTime() {
        return startedTime;
    }

    public void setStartedTime(String startedTime) {
        this.startedTime = startedTime;
    }

    public String getEndedTime() {
        return endedTime;
    }

    public void setEndedTime(String endedTime) {
        this.endedTime = endedTime;
    }

    public String getLogFileName() {
        return logFileName;
    }

    public void setLogFileName(String logFileName) {
        this.logFileName = logFileName;
    }

    public boolean isBlurred() {
        return isBlurred;
    }

    public void setBlurred(boolean blurred) {
        isBlurred = blurred;
    }

    public ImageModel getImage() {
        return image;
    }

    public void setImage(ImageModel image) {
        this.image = image;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }
}

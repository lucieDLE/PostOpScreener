package org.rti.ttfinder.models;

import android.net.Uri;

import org.rti.ttfinder.data.entity.Assessment;

public class ClassificationQueue {
    private Assessment assessment;
    private ClassificationModel leftEyeModel;
    private ClassificationModel rightEyeModel;

    public ClassificationQueue(Assessment assessment, ClassificationModel leftEyeModel, ClassificationModel rightEyeModel) {
        this.assessment = assessment;
        this.leftEyeModel = leftEyeModel;
        this.rightEyeModel = rightEyeModel;
    }

    public Assessment getAssessment() {
        return assessment;
    }

    public void setAssessment(Assessment assessment) {
        this.assessment = assessment;
    }

    public ClassificationModel getLeftEyeModel() {
        return leftEyeModel;
    }

    public void setLeftEyeModel(ClassificationModel leftEyeModel) {
        this.leftEyeModel = leftEyeModel;
    }

    public ClassificationModel getRightEyeModel() {
        return rightEyeModel;
    }

    public void setRightEyeModel(ClassificationModel rightEyeModel) {
        this.rightEyeModel = rightEyeModel;
    }
}

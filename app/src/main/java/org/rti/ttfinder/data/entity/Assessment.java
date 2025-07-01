package org.rti.ttfinder.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(indices = {@Index(value = {"tt_tracker_id"}, unique = true)})
public class Assessment {

    @PrimaryKey(autoGenerate = true)
    @NonNull
    private int id;

    @ColumnInfo(name = "consent")
    private String consent;

    @ColumnInfo(name = "assesment_started")
    private String assesment_started;

    @ColumnInfo(name = "assessment_ended")
    private String assessment_ended;

    @ColumnInfo(name = "left_eye_classification_started")
    private String left_eye_classification_started;

    @ColumnInfo(name = "left_eye_classification_ended")
    private String left_eye_classification_ended;

    @ColumnInfo(name = "right_eye_classification_started")
    private String right_eye_classification_started;

    @ColumnInfo(name = "right_eye_classification_ended")
    private String right_eye_classification_ended;

    @ColumnInfo(name = "tt_tracker_id")
    private String tt_tracker_id;

    @ColumnInfo(name = "right_eye_result")
    private String right_eye_result;

    @ColumnInfo(name = "left_eye_result")
    private String left_eye_result;

    @ColumnInfo(name = "right_image_name")
    private String right_image_name;

    @ColumnInfo(name = "left_image_name")
    private String left_image_name;

    @ColumnInfo(name = "left_image_log_file_name")
    private String left_image_log_file_name;

    @ColumnInfo(name = "right_image_log_file_name")
    private String right_image_log_file_name;

    @ColumnInfo(name = "have_tt_on_right_eye")
    private String have_tt_on_right_eye;

    @ColumnInfo(name = "have_tt_on_left_eye")
    private String have_tt_on_left_eye;

    @ColumnInfo(name = "have_tt_on_right_eye_confirm")
    private String have_tt_on_right_eye_confirm;

    @ColumnInfo(name = "have_tt_on_left_eye_confirm")
    private String have_tt_on_left_eye_confirm;

    @ColumnInfo(name = "gps_coordinate")
    private String gps_coordinate;

    public Assessment(String consent, String assesment_started, String assessment_ended, String left_eye_classification_started, String left_eye_classification_ended, String right_eye_classification_started, String right_eye_classification_ended, String tt_tracker_id, String right_eye_result, String left_eye_result, String right_image_name, String left_image_name, String left_image_log_file_name, String right_image_log_file_name, String have_tt_on_right_eye, String have_tt_on_left_eye, String have_tt_on_right_eye_confirm, String have_tt_on_left_eye_confirm, String gps_coordinate) {
        this.consent = consent;
        this.assesment_started = assesment_started;
        this.assessment_ended = assessment_ended;
        this.left_eye_classification_started = left_eye_classification_started;
        this.left_eye_classification_ended = left_eye_classification_ended;
        this.right_eye_classification_started = right_eye_classification_started;
        this.right_eye_classification_ended = right_eye_classification_ended;
        this.tt_tracker_id = tt_tracker_id;
        this.right_eye_result = right_eye_result;
        this.left_eye_result = left_eye_result;
        this.right_image_name = right_image_name;
        this.left_image_name = left_image_name;
        this.left_image_log_file_name = left_image_log_file_name;
        this.right_image_log_file_name = right_image_log_file_name;
        this.have_tt_on_left_eye = have_tt_on_left_eye;
        this.have_tt_on_right_eye = have_tt_on_right_eye;
        this.have_tt_on_right_eye_confirm = have_tt_on_right_eye_confirm;
        this.have_tt_on_left_eye_confirm = have_tt_on_left_eye_confirm;
        this.gps_coordinate = gps_coordinate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getConsent() {
        return consent;
    }

    public void setConsent(String consent) {
        this.consent = consent;
    }

    public String getAssesment_started() {
        return assesment_started;
    }

    public void setAssesment_started(String assesment_started) {
        this.assesment_started = assesment_started;
    }

    public String getAssessment_ended() {
        return assessment_ended;
    }

    public void setAssessment_ended(String assessment_ended) {
        this.assessment_ended = assessment_ended;
    }

    public String getLeft_eye_classification_started() {
        return left_eye_classification_started;
    }

    public void setLeft_eye_classification_started(String left_eye_classification_started) {
        this.left_eye_classification_started = left_eye_classification_started;
    }

    public String getLeft_eye_classification_ended() {
        return left_eye_classification_ended;
    }

    public void setLeft_eye_classification_ended(String left_eye_classification_ended) {
        this.left_eye_classification_ended = left_eye_classification_ended;
    }

    public String getRight_eye_classification_started() {
        return right_eye_classification_started;
    }

    public void setRight_eye_classification_started(String right_eye_classification_started) {
        this.right_eye_classification_started = right_eye_classification_started;
    }

    public String getRight_eye_classification_ended() {
        return right_eye_classification_ended;
    }

    public void setRight_eye_classification_ended(String right_eye_classification_ended) {
        this.right_eye_classification_ended = right_eye_classification_ended;
    }

    public String getTt_tracker_id() {
        return tt_tracker_id;
    }

    public void setTt_tracker_id(String tt_tracker_id) {
        this.tt_tracker_id = tt_tracker_id;
    }

    public String getRight_eye_result() {
        return right_eye_result;
    }

    public void setRight_eye_result(String right_eye_result) {
        this.right_eye_result = right_eye_result;
    }

    public String getLeft_eye_result() {
        return left_eye_result;
    }

    public void setLeft_eye_result(String left_eye_result) {
        this.left_eye_result = left_eye_result;
    }

    public String getRight_image_name() {
        return right_image_name;
    }

    public void setRight_image_name(String right_image_name) {
        this.right_image_name = right_image_name;
    }

    public String getLeft_image_name() {
        return left_image_name;
    }

    public void setLeft_image_name(String left_image_name) {
        this.left_image_name = left_image_name;
    }

    public String getLeft_image_log_file_name() {
        return left_image_log_file_name;
    }

    public void setLeft_image_log_file_name(String left_image_log_file_name) {
        this.left_image_log_file_name = left_image_log_file_name;
    }

    public String getRight_image_log_file_name() {
        return right_image_log_file_name;
    }

    public void setRight_image_log_file_name(String right_image_log_file_name) {
        this.right_image_log_file_name = right_image_log_file_name;
    }

    public String getHave_tt_on_right_eye() {
        return have_tt_on_right_eye;
    }

    public void setHave_tt_on_right_eye(String have_tt_on_right_eye) {
        this.have_tt_on_right_eye = have_tt_on_right_eye;
    }

    public String getHave_tt_on_left_eye() {
        return have_tt_on_left_eye;
    }

    public void setHave_tt_on_left_eye(String have_tt_on_left_eye) {
        this.have_tt_on_left_eye = have_tt_on_left_eye;
    }

    public String getHave_tt_on_right_eye_confirm() {
        return have_tt_on_right_eye_confirm;
    }

    public void setHave_tt_on_right_eye_confirm(String have_tt_on_right_eye_confirm) {
        this.have_tt_on_right_eye_confirm = have_tt_on_right_eye_confirm;
    }

    public String getHave_tt_on_left_eye_confirm() {
        return have_tt_on_left_eye_confirm;
    }

    public void setHave_tt_on_left_eye_confirm(String have_tt_on_left_eye_confirm) {
        this.have_tt_on_left_eye_confirm = have_tt_on_left_eye_confirm;
    }

    public String getGps_coordinate() {
        return gps_coordinate;
    }

    public void setGps_coordinate(String gps_coordinate) {
        this.gps_coordinate = gps_coordinate;
    }
}

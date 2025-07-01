package org.rti.ttfinder.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Eye {

    @PrimaryKey(autoGenerate = true)
    @NonNull
    private int id;

    @ColumnInfo(name = "upper_eyelid_trichiasis")
    private String upperEyelidTrichiasis;

    @ColumnInfo(name = "evidence_epilation")
    private String evidenceEpilation;

    @ColumnInfo(name = "epilation_loc_nasal")
    private String epilation_loc_nasal;

    @ColumnInfo(name = "epilation_loc_central")
    private String epilation_loc_central;

    @ColumnInfo(name = "epilation_loc_temporal")
    private String epilation_loc_temporal;

    @ColumnInfo(name = "lasses_touching_nasally")
    private String lasses_touching_nasally;

    @ColumnInfo(name = "lasses_touching_centrally")
    private String lasses_touching_centrally;

    @ColumnInfo(name = "lasses_touching_temporally")
    private String lasses_touching_temporally;

    @ColumnInfo(name = "eye_type")
    private String eye_type;

    @ColumnInfo(name = "assessment_id")
    private String assessment_id;

    public Eye(String upperEyelidTrichiasis, String evidenceEpilation, String epilation_loc_nasal, String epilation_loc_central, String epilation_loc_temporal, String lasses_touching_nasally, String lasses_touching_centrally, String lasses_touching_temporally, String eye_type, String assessment_id) {
        this.upperEyelidTrichiasis = upperEyelidTrichiasis;
        this.evidenceEpilation = evidenceEpilation;
        this.epilation_loc_nasal = epilation_loc_nasal;
        this.epilation_loc_central = epilation_loc_central;
        this.epilation_loc_temporal = epilation_loc_temporal;
        this.lasses_touching_nasally = lasses_touching_nasally;
        this.lasses_touching_centrally = lasses_touching_centrally;
        this.lasses_touching_temporally = lasses_touching_temporally;
        this.eye_type = eye_type;
        this.assessment_id = assessment_id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUpperEyelidTrichiasis() {
        return upperEyelidTrichiasis;
    }

    public void setUpperEyelidTrichiasis(String upperEyelidTrichiasis) {
        this.upperEyelidTrichiasis = upperEyelidTrichiasis;
    }

    public String getEvidenceEpilation() {
        return evidenceEpilation;
    }

    public void setEvidenceEpilation(String evidenceEpilation) {
        this.evidenceEpilation = evidenceEpilation;
    }

    public String getEpilation_loc_nasal() {
        return epilation_loc_nasal;
    }

    public void setEpilation_loc_nasal(String epilation_loc_nasal) {
        this.epilation_loc_nasal = epilation_loc_nasal;
    }

    public String getEpilation_loc_central() {
        return epilation_loc_central;
    }

    public void setEpilation_loc_central(String epilation_loc_central) {
        this.epilation_loc_central = epilation_loc_central;
    }

    public String getEpilation_loc_temporal() {
        return epilation_loc_temporal;
    }

    public void setEpilation_loc_temporal(String epilation_loc_temporal) {
        this.epilation_loc_temporal = epilation_loc_temporal;
    }

    public String getLasses_touching_nasally() {
        return lasses_touching_nasally;
    }

    public void setLasses_touching_nasally(String lasses_touching_nasally) {
        this.lasses_touching_nasally = lasses_touching_nasally;
    }

    public String getLasses_touching_centrally() {
        return lasses_touching_centrally;
    }

    public void setLasses_touching_centrally(String lasses_touching_centrally) {
        this.lasses_touching_centrally = lasses_touching_centrally;
    }

    public String getLasses_touching_temporally() {
        return lasses_touching_temporally;
    }

    public void setLasses_touching_temporally(String lasses_touching_temporally) {
        this.lasses_touching_temporally = lasses_touching_temporally;
    }

    public String getEye_type() {
        return eye_type;
    }

    public void setEye_type(String eye_type) {
        this.eye_type = eye_type;
    }

    public String getAssessment_id() {
        return assessment_id;
    }

    public void setAssessment_id(String assessment_id) {
        this.assessment_id = assessment_id;
    }
}

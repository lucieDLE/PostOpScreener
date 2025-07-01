package org.rti.ttfinder.enums;

public enum TTAnswer {
    YES("Yes"),
    NO("No"),
    NOT_DONE("Refused/Not Done/DK");

    public final String label;

    private TTAnswer(String label) {
        this.label = label;
    }
}
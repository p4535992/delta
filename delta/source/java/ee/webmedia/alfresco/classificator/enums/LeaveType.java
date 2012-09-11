package ee.webmedia.alfresco.classificator.enums;

public enum LeaveType {
    /** põhipuhkus */
    LEAVE_ANNUAL("leaveAnnual"),
    /** tasustamata puhkus */
    LEAVE_WITHOUT_PAY("leaveWithoutPay"),
    /** lapsepuhkus */
    LEAVE_CHILD("leaveChild"),
    /** õppepuhkus */
    LEAVE_STUDY("leaveStudy");

    private String valueName;

    LeaveType(String value) {
        valueName = value;
    }

    public String getValueName() {
        return valueName;
    }

    public boolean equals(String value) {
        return valueName.equalsIgnoreCase(value);
    }
}

package ee.webmedia.alfresco.classificator.enums;

/**
 * Enum for "volumeType" classificator values
 * 
 * @author Ats Uiboupin
 */
public enum VolumeType {
    YEAR_BASED("aastapõhine"),
    OBJECT("objektipõhine");

    private String valueName;

    VolumeType(String valueName) {
        this.valueName = valueName;
    }

    public static VolumeType get(String valueName) {
        final VolumeType[] values = VolumeType.values();
        for (VolumeType value : values) {
            if (value.valueName.equals(valueName)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown valueName: '" + valueName + "'");
    }

    public String getValueName() {
        return valueName;
    }

    public boolean equals(String valueName) {
        return this.valueName.equalsIgnoreCase(valueName);
    }
}
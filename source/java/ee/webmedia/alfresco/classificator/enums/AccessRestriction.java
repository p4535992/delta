package ee.webmedia.alfresco.classificator.enums;

public enum AccessRestriction {
    OPEN("Avalik"),
    AK("AK"),
    INTERNAL("Majasisene"),
    LIMITED("Piiratud");

    private String valueName;

    AccessRestriction(String value) {
        valueName = value;
    }

    public String getValueName() {
        return valueName;
    }

    public boolean equals(String value) {
        return valueName.equalsIgnoreCase(value);
    }

    /**
     * Retrieves <code>AccessRestriction</code> by value name.
     * 
     * @param valueName A restriction value name.
     * @return <code>AccessRestriction</code> value or <code>null</code> when no such restriction exists.
     */
    public static AccessRestriction valueNameOf(String valueName) {
        AccessRestriction result = null;

        for (AccessRestriction restriction : values()) {
            if (restriction.getValueName().equals(valueName)) {
                result = restriction;
                break;
            }
        }

        return result;
    }
}

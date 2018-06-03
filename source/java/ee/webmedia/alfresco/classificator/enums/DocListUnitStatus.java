package ee.webmedia.alfresco.classificator.enums;

import java.util.ArrayList;
import java.util.List;

/**
 * Enum for "docListUnitStatus" classificator values
 */
public enum DocListUnitStatus {
    CLOSED("suletud"),
    OPEN("avatud"),
    DESTROYED("hävitatud");

    private String valueName;

    DocListUnitStatus(String valueName) {
        this.valueName = valueName;
    }

    public static DocListUnitStatus get(String valueName) {
        final DocListUnitStatus[] values = DocListUnitStatus.values();
        for (DocListUnitStatus parameter : values) {
            if (parameter.valueName.equals(valueName)) {
                return parameter;
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

    public static List<String> getStatusNames(DocListUnitStatus[] statuses) {
        List<String> statusNames = new ArrayList<>();
        for (DocListUnitStatus status : statuses) {
            statusNames.add(status.getValueName());
        }
        return statusNames;
    }
}
package ee.webmedia.alfresco.docadmin.service;

import java.io.Serializable;

public class UnmodifiableDynamicType implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String typeId;
    private final String typeName;
    private final Boolean used;

    public UnmodifiableDynamicType(String typeId, String typeName, Boolean used) {
        this.typeId = typeId;
        this.typeName = typeName;
        this.used = Boolean.TRUE.equals(used);
    }

    public String getTypeId() {
        return typeId;
    }

    public String getTypeName() {
        return typeName;
    }

    public Boolean getUsed() {
        return used;
    }

}

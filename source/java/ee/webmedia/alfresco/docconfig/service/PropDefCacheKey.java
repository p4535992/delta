package ee.webmedia.alfresco.docconfig.service;

import java.io.Serializable;

import ee.webmedia.alfresco.docadmin.service.CaseFileType;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DynamicType;

public class PropDefCacheKey implements Serializable {
    private static final long serialVersionUID = 1L;

    final private Class<? extends DynamicType> dynTypeClass;
    final private String dynamicTypeId;
    final private Integer version;

    public PropDefCacheKey(Class<? extends DynamicType> typeClass, String typeId, Integer typeVersion) {
        dynTypeClass = typeClass;
        dynamicTypeId = typeId;
        version = typeVersion;
    }

    public boolean isDocumentType() {
        return DocumentType.class.equals(dynTypeClass) || dynTypeClass == null; // If the node doesn't represent document or case file, then it is a sub node of a document.
    }

    public boolean isCaseFileType() {
        return CaseFileType.class.equals(dynTypeClass);
    }

    public Class<? extends DynamicType> getDynTypeClass() {
        return dynTypeClass;
    }

    public String getDynamicTypeId() {
        return dynamicTypeId;
    }

    public Integer getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "PropDefCacheKey [dynTypeClass=" + dynTypeClass + ", dynamicTypeId=" + dynamicTypeId + ", version=" + version + "]";
    }

    // XXX NB! hashCode() and equals() are generated automatically based on three fields!
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dynTypeClass == null) ? 0 : dynTypeClass.hashCode());
        result = prime * result + ((dynamicTypeId == null) ? 0 : dynamicTypeId.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PropDefCacheKey other = (PropDefCacheKey) obj;
        if (dynTypeClass == null) {
            if (other.dynTypeClass != null) {
                return false;
            }
        } else if (!dynTypeClass.equals(other.dynTypeClass)) {
            return false;
        }
        if (dynamicTypeId == null) {
            if (other.dynamicTypeId != null) {
                return false;
            }
        } else if (!dynamicTypeId.equals(other.dynamicTypeId)) {
            return false;
        }
        if (version == null) {
            if (other.version != null) {
                return false;
            }
        } else if (!version.equals(other.version)) {
            return false;
        }
        return true;
    }


}
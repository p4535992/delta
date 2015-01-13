package ee.webmedia.alfresco.docadmin.service;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseService;
import ee.webmedia.alfresco.base.BaseServiceImpl;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;

/**
 * Base class for {@link Field} and {@link FieldGroup}
 */
public abstract class FieldAndGroupBase extends MetadataItem {
    private static final long serialVersionUID = 1L;

    protected FieldAndGroupBase(BaseObject parent, QName type) {
        super(parent, type);
    }

    /** used only by subclass */
    protected FieldAndGroupBase(NodeRef parentRef, QName type) {
        super(parentRef, type);
    }

    /** Used by {@link BaseServiceImpl#getObject(NodeRef, Class)} through reflection */
    public FieldAndGroupBase(NodeRef parentRef, WmNode node) {
        super(parentRef, node);
    }

    /** Used by {@link BaseService#getObject(NodeRef, Class)} through reflection */
    protected FieldAndGroupBase(BaseObject parent, WmNode node) {
        super(parent, node);
    }

    public final <D extends DynamicType> boolean isMandatoryForDynType(Class<D> dynTypeClass) {
        if (dynTypeClass == DocumentType.class) {
            return isMandatoryForDoc();
        } else if (dynTypeClass == CaseFileType.class) {
            return isMandatoryForVol();
        } else {
            throw new RuntimeException("Unknown dynTypeClass " + dynTypeClass);
        }
    }

    // Properties

    public final String getName() {
        return getProp(DocumentAdminModel.Props.NAME);
    }

    public final void setName(String name) {
        setProp(DocumentAdminModel.Props.NAME, name);
    }

    public final void setSystematic(boolean systematic) {
        setProp(DocumentAdminModel.Props.SYSTEMATIC, systematic);
    }

    public final boolean isSystematic() {
        return getPropBoolean(DocumentAdminModel.Props.SYSTEMATIC);
    }

    public final String getSystematicComment() {
        return getProp(DocumentAdminModel.Props.SYSTEMATIC_COMMENT);
    }

    public final String getComment() {
        return getProp(DocumentAdminModel.Props.COMMENT);
    }

    public final void setComment(String systematicComment) {
        setProp(DocumentAdminModel.Props.COMMENT, systematicComment);
    }

    public final void setMandatoryForDoc(boolean mandatoryForDoc) {
        setProp(DocumentAdminModel.Props.MANDATORY_FOR_DOC, mandatoryForDoc);
    }

    public final boolean isMandatoryForDoc() {
        return getPropBoolean(DocumentAdminModel.Props.MANDATORY_FOR_DOC);
    }

    public final void setRemovableFromSystematicDocType(boolean removableFromSystematicDocType) {
        setProp(DocumentAdminModel.Props.REMOVABLE_FROM_SYSTEMATIC_DOC_TYPE, removableFromSystematicDocType);
    }

    public final boolean isRemovableFromSystematicDocType() {
        return getPropBoolean(DocumentAdminModel.Props.REMOVABLE_FROM_SYSTEMATIC_DOC_TYPE);
    }

    public final boolean isMandatoryForVol() {
        return getPropBoolean(DocumentAdminModel.Props.MANDATORY_FOR_VOL);
    }

    public final void setMandatoryForVol(boolean mandatoryForVol) {
        setProp(DocumentAdminModel.Props.MANDATORY_FOR_VOL, mandatoryForVol);
    }

    @Override
    public boolean isRemovableFromList() {
        // if subclass delegates method call here, then expecting that parent is DocumentTypeVersion
        DocumentTypeVersion dynTypeVersion = (DocumentTypeVersion) getParent();
        DynamicType dynamicType = dynTypeVersion.getParent();
        if (dynamicType instanceof DocumentType) {
            if (isMandatoryForDoc()) {
                return false;
            }
            DocumentType docType = (DocumentType) dynamicType;
            if (!docType.isSystematic()) {
                return true;
            }
        } else if (dynamicType instanceof CaseFileType) {
            if (isMandatoryForVol()) {
                return false;
            }
        } else {
            throw new RuntimeException("Unknown dynamic type " + dynamicType);
        }
        if (isRemovableFromSystematicDocType()) {
            return true;
        }
        return false;
    }

}

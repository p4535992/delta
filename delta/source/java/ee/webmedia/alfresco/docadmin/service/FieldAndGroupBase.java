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
 * 
 * @author Ats Uiboupin
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

    // Properties

    public final String getName() {
        return getProp(DocumentAdminModel.Props.NAME);
    }

    public final void setName(String name) {
        setProp(DocumentAdminModel.Props.NAME, name);
    }

    // FIXME DLSeadist - võiks setter'i eemaldada kui testandmeid luua vaja pole, sest seda programmaatiliselt muuta ei tohiks
    public final void setSystematic(boolean systematic) {
        setProp(DocumentAdminModel.Props.SYSTEMATIC, systematic);
    }

    public final boolean isSystematic() {
        return getPropBoolean(DocumentAdminModel.Props.SYSTEMATIC);
    }

    public final String getSystematicComment() {
        return getProp(DocumentAdminModel.Props.SYSTEMATIC_COMMENT);
    }

    public final void setSystematicComment(String systematicComment) {
        setProp(DocumentAdminModel.Props.SYSTEMATIC_COMMENT, systematicComment);
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

    public final void setRemovableFromSystemDocType(boolean removableFromSystemDocType) {
        setProp(DocumentAdminModel.Props.REMOVABLE_FROM_SYSTEM_DOC_TYPE, removableFromSystemDocType);
    }

    public final boolean isRemovableFromSystemDocType() {
        return getPropBoolean(DocumentAdminModel.Props.REMOVABLE_FROM_SYSTEM_DOC_TYPE);
    }

    public final boolean isDefaultUserLoggedIn() {
        return getPropBoolean(DocumentAdminModel.Props.DEFAULT_USER_LOGGED_IN);
    }

    public final void setDefaultUserLoggedIn(boolean defaultUserLoggedIn) {
        setProp(DocumentAdminModel.Props.DEFAULT_USER_LOGGED_IN, defaultUserLoggedIn);
    }

    @Override
    public boolean isRemovableFromList() {
        // if subclass delegates method call here, then expecting that parent is DocumentTypeVersion
        DocumentTypeVersion docTypeVersion = (DocumentTypeVersion) getParent();
        if (isRemovableFromSystemDocType()) {
            return true;
        }
        DocumentType docType = docTypeVersion.getParent();
        // return !docType.isSystematic();
        if (!docType.isSystematic()) {
            return true;
        }
        return false;
    }
}

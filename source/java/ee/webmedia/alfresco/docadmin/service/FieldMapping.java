package ee.webmedia.alfresco.docadmin.service;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseServiceImpl;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * Used to map field to another field when creating new document so that data from existing document could be copied to new document
 */
public class FieldMapping extends BaseObject {
    private static final long serialVersionUID = 1L;

    private static final Map<String, QName> FROM_FIELD_TO_ASSOC_QNAME = new HashMap<>();

    /** Used by {@link BaseServiceImpl#getObject(NodeRef, Class)} through reflection */
    public FieldMapping(BaseObject parent, WmNode node) {
        super(checkParentType(parent), node);
    }

    public FieldMapping(AssociationModel parent) {
        super(checkParentType(parent), DocumentAdminModel.Types.FIELD_MAPPING);
    }

    @Override
    public AssociationModel getParent() {
        return (AssociationModel) super.getParent();
    }

    @Override
    protected QName getAssocName() {
        return RepoUtil.getFromQNamePool(getFromField(), DocumentAdminModel.URI, FROM_FIELD_TO_ASSOC_QNAME);
    }

    private static BaseObject checkParentType(BaseObject parent) {
        Assert.isTrue(parent instanceof AssociationModel);
        return parent;
    }

    public void setFromField(String fromField) {
        setProp(DocumentAdminModel.Props.FROM_FIELD, fromField);
    }

    public String getFromField() {
        return getProp(DocumentAdminModel.Props.FROM_FIELD);
    }

    public void setToField(String toField) {
        setProp(DocumentAdminModel.Props.TO_FIELD, toField);
    }

    /**
     * fieldId's will be converted to strings in future when migration from static document types is completed
     */
    public String getToField() {
        return getProp(DocumentAdminModel.Props.TO_FIELD);
    }

    public void nextSaveToParent(AssociationModel newParentFollowupAssocModel) {
        nextSaveToParent(newParentFollowupAssocModel, FieldMapping.class);
    }

}

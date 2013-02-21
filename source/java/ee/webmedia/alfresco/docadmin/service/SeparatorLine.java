package ee.webmedia.alfresco.docadmin.service;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseServiceImpl;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;

public class SeparatorLine extends MetadataItem {
    private static final long serialVersionUID = 1L;

    public SeparatorLine(BaseObject parent) {
        super(parent, DocumentAdminModel.Types.SEPARATION_LINE);
    }

    /** Used by {@link BaseServiceImpl#getObject(NodeRef, Class)} through reflection */
    public SeparatorLine(BaseObject parent, WmNode node) {
        super(parent, node);
    }

    protected void nextSaveToParent(DocumentTypeVersion newParentFieldGroup) {
        nextSaveToParent(newParentFieldGroup, MetadataItem.class);
    }

    @Override
    public String getAdditionalInfo() {
        return ""; // no additional info
    }

    @Override
    public boolean isRemovableFromList() {
        return true;
    }
}

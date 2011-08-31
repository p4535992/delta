package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getFieldDetailsDialog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.base.BaseObject.ChildrenList;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docadmin.service.MetadataContainer;
import ee.webmedia.alfresco.docadmin.service.MetadataItem;
import ee.webmedia.alfresco.docadmin.service.SeparatorLine;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Shows list of {@link Field}, {@link FieldGroup} and {@link SeparatorLine} objects bound to the latest {@link DocumentTypeVersion} of viewable {@link DocumentType}
 * 
 * @author Ats Uiboupin
 */
// TODO DLSeadist rename FieldsListBean?
public class DocTypeFieldsListBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private MetadataContainer metadataContainer;

    void init(MetadataContainer latestDocTypeVersion) {
        metadataContainer = latestDocTypeVersion;
    }

    public List<? extends MetadataItem> getMetaFieldsList() {
        ChildrenList<? extends MetadataItem> metadata = metadataContainer.getMetadata();
        if (metadata == null) {
            return Collections.emptyList();
        }
        return new ArrayList<MetadataItem>(metadata.getList()); // TODO DLSeadist
    }

    /** JSP */
    public void removeMetaField(ActionEvent event) {
        ChildrenList<? extends MetadataItem> metadata = metadataContainer.getMetadata();
        if (metadata == null) {
            throw new RuntimeException("unimplemented deleteMetaField from unsaved");
        }
        NodeRef metaFieldRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        MetadataItem removed = metadata.remove(metaFieldRef);
        MessageUtil.addWarningMessage("docType_metadataList_remove_postponed_" + removed.getType());
    }

    /** JSP */
    public void editMetadataItem(ActionEvent event) {
        NodeRef fieldRef = ActionUtil.getParam(event, "nodeRef", NodeRef.class);
        Field field = (Field) metadataContainer.getMetadata().getChildByNodeRef(fieldRef);
        navigateToFieldDetailsDialog();
        getFieldDetailsDialog().editField(field, metadataContainer);
    }

    /** JSP */
    public void addMetadataItem(ActionEvent event) {
        String itemType = ActionUtil.getParam(event, "itemType");
        @SuppressWarnings("unchecked")
        ChildrenList<MetadataItem> metadata = (ChildrenList<MetadataItem>) metadataContainer.getMetadata();
        if ("existingField".equals(itemType)) {
            // // FIXME: tavaliselt listener, action(init dialog),
            // // aga siin action(dialog.init())+addNewFieldToDocType(docType)
            // navigateToFieldDetailsDialog();
            // getFieldDetailsDialog().addNewFieldToDocType(latestDocumentTypeVersion);
            MessageUtil.addWarningMessage("TODO: realiseerimata");
        } else if ("field".equals(itemType)) {
            // FIXME: tavaliselt listener, action(init dialog),
            // aga siin action(dialog.init())+addNewFieldToDocType(docType)
            navigateToFieldDetailsDialog();
            getFieldDetailsDialog().addNewFieldToDocType(metadataContainer);
        } else if ("separator".equals(itemType)) {
            SeparatorLine sep = metadata.add(SeparatorLine.class);
            sep.setOrder(metadata.size());
        } else {
            throw new RuntimeException("Unknown itemType='" + itemType + "'");
        }
        reorder(metadata);
    }

    private void navigateToFieldDetailsDialog() {
        FacesContext context = FacesContext.getCurrentInstance();
        context.getApplication().getNavigationHandler()
                .handleNavigation(context, null, "dialog:fieldDetailsDialog");
    }

    private void reorder(ChildrenList<MetadataItem> metadata) {
        // TODO DLSeadist reorder metadata items if needed

    }

}

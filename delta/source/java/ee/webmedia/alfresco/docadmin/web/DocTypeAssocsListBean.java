package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAssociationToDocTypeDetailsDialog;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.docadmin.web.DocAdminUtil.navigate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.docadmin.service.AssociationToDocType;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

public class DocTypeAssocsListBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private DocTypeDetailsDialog docTypeDetailsDialog;
    private Map<String, String> docTypesNamesByDocTypeId;

    public void init(DocTypeDetailsDialog docTypeDetDialog) {
        docTypeDetailsDialog = docTypeDetDialog;
    }

    void reset() {
        docTypeDetailsDialog = null;
        docTypesNamesByDocTypeId = null;
    }

    public List<AssociationToDocTypeListItem> getFollowupAssocs() {
        return getAssocsOfType(DocTypeAssocType.FOLLOWUP);
    }

    public List<AssociationToDocTypeListItem> getReplyAssocs() {
        return getAssocsOfType(DocTypeAssocType.REPLY);
    }

    private List<AssociationToDocTypeListItem> getAssocsOfType(DocTypeAssocType assocType) {
        Map<String, String> docTypeNames = getDocumentTypeNames();
        List<AssociationToDocTypeListItem> results = new ArrayList<AssociationToDocTypeListItem>();
        for (AssociationToDocType associationToDocType : docTypeDetailsDialog.getDocType().getAssociationsToDocTypes()) {
            if (assocType.equals(associationToDocType.getAssociationTypeEnum())) {
                results.add(new AssociationToDocTypeListItem(associationToDocType, docTypeNames.get(associationToDocType.getDocType())));
            }
        }
        return results;
    }

    private Map<String, String> getDocumentTypeNames() {
        if (docTypesNamesByDocTypeId == null) {
            docTypesNamesByDocTypeId = getDocumentAdminService().getDocumentTypeNames(true);
        }
        return docTypesNamesByDocTypeId;
    }

    /** JSP */
    public void addAssoc(ActionEvent event) {
        try {
            if (!docTypeDetailsDialog.validate()) {
                return;
            }
            docTypeDetailsDialog.save(true);
            DocTypeAssocType assocType = ActionUtil.getParam(event, "assocType", DocTypeAssocType.class);
            getAssociationToDocTypeDetailsDialog().addNewAssociationToDocType(docTypeDetailsDialog.getDocType(), assocType);
            navigate("dialog:associationToDocTypeDetailsDialog");
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
        }
    }

    /** JSP */
    public void editAssoc(ActionEvent event) {
        try {
            if (!docTypeDetailsDialog.validate()) {
                return;
            }
            docTypeDetailsDialog.save(true);
            NodeRef assocNodeRef = ActionUtil.getParam(event, "nodeRef", NodeRef.class);
            AssociationToDocType editableAssoc = docTypeDetailsDialog.getDocType().getAssociationsToDocTypes().getChildByNodeRef(assocNodeRef);
            getAssociationToDocTypeDetailsDialog().editAssociationToDocType(editableAssoc);
            navigate("dialog:associationToDocTypeDetailsDialog");
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
        }
    }

    public class AssociationToDocTypeListItem extends AssociationToDocType {
        private static final long serialVersionUID = 1L;
        private final String docTypeName;

        public AssociationToDocTypeListItem(AssociationToDocType wrapped, String documentTypeName) {
            super(wrapped.getParent(), wrapped.getNode());
            docTypeName = documentTypeName;
        }

        public String getDocTypeName() {
            return docTypeName;
        }
    }

}

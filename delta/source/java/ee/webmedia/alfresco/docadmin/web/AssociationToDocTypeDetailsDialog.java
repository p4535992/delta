package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.utils.ComponentUtil.addDefault;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.AssociationToDocType;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Dialog for editing objects of type {@link AssociationToDocType}
 * 
 * @author Ats Uiboupin
 */
public class AssociationToDocTypeDetailsDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "AssociationToDocTypeDetailsDialog";
    private AssociationToDocType associationToDocType;
    private DocumentType documentType;

    void addNewAssociationToDocType(DocumentType docType, DocTypeAssocType assocType) {
        AssociationToDocType assocToDocType = new AssociationToDocType(docType);
        assocToDocType.setAssociationTypeEnum(assocType);
        editAssociationToDocTypeInner(assocToDocType, docType);
    }

    void editAssociationToDocType(AssociationToDocType assocToDocType) {
        editAssociationToDocTypeInner(assocToDocType, assocToDocType.getParent());
    }

    private void editAssociationToDocTypeInner(AssociationToDocType assocToDocType, DocumentType docType) {
        resetFields();
        associationToDocType = assocToDocType;
        documentType = docType;
    }

    @Override
    public String cancel() {
        resetFields();
        return super.cancel();
    }

    private void resetFields() {
        documentType = null;
        associationToDocType = null;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (validate()) {
            associationToDocType = getDocumentAdminService().saveOrUpdateAssocToDocType(associationToDocType);
            BeanHelper.getDocTypeDetailsDialog().init(documentType.getNodeRef());
        } else {
            isFinished = false;
            return null;
        }
        return outcome;
    }

    private boolean validate() {
        // TODO DLSeadist
        return true;
    }

    @Override
    public String getContainerTitle() {
        return MessageUtil.getMessage("associationToDocType_details_title_" + associationToDocType.getAssociationType());
    }

    public AssociationToDocType getAssociationToDocType() {
        return associationToDocType;
    }

    /** used by property sheet */
    public List<SelectItem> getDocTypes(FacesContext context, @SuppressWarnings("unused") UIInput selectComponent) {
        List<DocumentType> docTypes = getDocumentAdminService().getDocumentTypes(true);
        List<SelectItem> results = new ArrayList<SelectItem>(docTypes.size() + 1);
        addDefault(results, context);
        Set<String> associatedDocTypes = new HashSet<String>();
        DocTypeAssocType associationTypeEnum = associationToDocType.getAssociationTypeEnum();
        for (AssociationToDocType assocToDocType : documentType.getAssociationsToDocTypes()) {
            if (associationTypeEnum.equals(assocToDocType.getAssociationTypeEnum())) {
                associatedDocTypes.add(assocToDocType.getDocType());
            }
        }
        for (DocumentType docType : docTypes) {
            if (associatedDocTypes.contains(docType.getDocumentTypeId())) {
                continue;
            }
            SelectItem selectItem = new SelectItem(docType.getDocumentTypeId(), docType.getNameAndId());
            results.add(selectItem);
        }
        return results;
    }

}

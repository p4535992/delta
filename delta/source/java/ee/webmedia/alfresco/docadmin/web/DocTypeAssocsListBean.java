package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAssociationModelDetailsDialog;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.utils.WebUtil.navigateTo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Repository;

import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.docadmin.service.AssociationModel;
import ee.webmedia.alfresco.docdynamic.web.DialogBlockBean;
import ee.webmedia.alfresco.utils.ActionUtil;

/**
 * Base bean for associations list
 * 
 * @author Ats Uiboupin
 */
public abstract class DocTypeAssocsListBean<T extends AssociationModel> implements DialogBlockBean<DocTypeDetailsDialog> {
    private static final long serialVersionUID = 1L;

    /** reference to container dialog, where this bean is added */
    protected DocTypeDetailsDialog docTypeDetailsDialog;
    private Map<String, String> docTypesNamesByDocTypeId;

    @Override
    public void resetOrInit(DocTypeDetailsDialog docTypeDetDialog) {
        docTypeDetailsDialog = docTypeDetDialog;
        docTypesNamesByDocTypeId = null;
    }

    public abstract List<T> getAssocs();

    protected abstract T createNewAssoc();

    protected abstract T getAssocByNodeRef(NodeRef assocNodeRef);

    protected List<T> getAssocsOfType(DocTypeAssocType assocType) {
        Map<String, String> docTypeNames = getDocumentTypeNames();
        List<AssociationToDocTypeListItem> results = new ArrayList<AssociationToDocTypeListItem>();
        try {
            for (AssociationModel associationModel : docTypeDetailsDialog.getDocType().getAssociationModels(assocType)) {
                results.add(new AssociationToDocTypeListItem(associationModel, docTypeNames.get(associationModel.getDocType())));
            }
        } catch (NullPointerException e) {
            // FIXME CL_TASK 177667 Kaarel
            return Collections.emptyList();
        }
        @SuppressWarnings("unchecked")
        List<T> tmp = (List<T>) results;
        return tmp;
    }

    private Map<String, String> getDocumentTypeNames() {
        if (docTypesNamesByDocTypeId == null) {
            docTypesNamesByDocTypeId = getDocumentAdminService().getDocumentTypeNames(true);
        }
        return docTypesNamesByDocTypeId;
    }

    /** JSP */
    public void addAssoc(@SuppressWarnings("unused") ActionEvent event) {
        createOrEditAssoc(null);
    }

    /** JSP */
    public void editAssoc(ActionEvent event) {
        createOrEditAssoc(ActionUtil.getParam(event, "nodeRef", NodeRef.class));
    }

    private void createOrEditAssoc(final NodeRef assocNodeRef) {
        FacesContext context = FacesContext.getCurrentInstance();
        RetryingTransactionHelper txnHelper = Repository.getRetryingTransactionHelper(context);
        try {
            RetryingTransactionCallback<Void> callback = new RetryingTransactionCallback<Void>() {
                @Override
                public Void execute() {
                    if (!docTypeDetailsDialog.validate()) {
                        return null;
                    }
                    if (docTypeDetailsDialog.save()) {
                        // getAssocByNodeRef() must be called after saving, as documentType is replaced
                        T assocModel = assocNodeRef != null ? getAssocByNodeRef(assocNodeRef) : createNewAssoc();
                        // init & navigate
                        getAssociationModelDetailsDialog().init(assocModel);
                        navigateTo("dialog:associationModelDetailsDialog");
                    }
                    return null;
                }
            };
            txnHelper.doInTransaction(callback, false, false);
        } catch (RuntimeException e) {
            docTypeDetailsDialog.handleException(e);
        }
    }

    public void deleteAssoc(ActionEvent event) {
        NodeRef assocRef = ActionUtil.getParam(event, "nodeRef", NodeRef.class);
        getDocumentAdminService().deleteAssocToDocType(assocRef);
        doAfterDelete();
    }

    public void doAfterDelete() {
        // replace docType in memory with fresh copy from repo
        // so that deleted assocs wouldn't be visible when navigating back
        docTypeDetailsDialog.refreshDocType();
    }

    public class AssociationToDocTypeListItem extends AssociationModel {
        private static final long serialVersionUID = 1L;
        private final AssociationModel wrapped;
        private final String docTypeName;

        public AssociationToDocTypeListItem(AssociationModel wrapped, String documentTypeName) {
            super(wrapped.getParent(), wrapped.getNode());
            docTypeName = documentTypeName;
            this.wrapped = wrapped;
        }

        public String getDocTypeName() {
            return docTypeName;
        }

        @Override
        public DocTypeAssocType getAssociationType() {
            return wrapped.getAssociationType();
        }
    }

}

package ee.webmedia.alfresco.document.associations.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.config.ActionsConfigElement.ActionDefinition;

import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.service.AssociationModel;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicBlock;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicDialog;
import ee.webmedia.alfresco.document.associations.model.DocAssocInfo;
import ee.webmedia.alfresco.document.assocsdyn.web.AddFollowUpAssocEvaluator;
import ee.webmedia.alfresco.document.assocsdyn.web.AddReplyAssocEvaluator;
import ee.webmedia.alfresco.utils.MessageUtil;
<<<<<<< HEAD
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.workflow.web.WorkflowBlockBean;

/**
 * Block that shows associations of given document/volume/caseFile/case with other documents/volumes/caseFiles/cases
 * 
 * @author Ats Uiboupin
=======

/**
 * Block that shows associations of given document with other documents and related cases
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class AssocsBlockBean implements DocumentDynamicBlock {
    private static final long serialVersionUID = 1L;

<<<<<<< HEAD
    private static org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(WorkflowBlockBean.class);

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    public static final String BEAN_NAME = "AssocsBlockBean";
    private static final String FOLLOWUPS_METHOD_BINDING_NAME = "#{" + BEAN_NAME + ".createAddFollowupsMenu}";
    private static final String REPLIES_METHOD_BINDING_NAME = "#{" + BEAN_NAME + ".createAddRepliesMenu}";
    private static final String DROPDOWN_MENU_ITEM_ICON = "/images/icons/versioned_properties.gif";
    public static final String PARAM_ASSOC_MODEL_REF = "assocModelRef";

    private Node document;
<<<<<<< HEAD
    private List<DocAssocInfo> docAssocInfos = new ArrayList<DocAssocInfo>();
=======
    private List<DocAssocInfo> docAssocInfos;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    public void init(Node node) {
        reset();
        document = node;
        restore();
    }

    public void restore() {
<<<<<<< HEAD
        if (document != null && RepoUtil.isSaved(document) && BeanHelper.getNodeService().exists(document.getNodeRef())) {
            docAssocInfos = BeanHelper.getDocumentAssociationsService().getAssocInfos(document);
            sortDocAssocInfos();
        } else {
            docAssocInfos = new ArrayList<DocAssocInfo>();
        }
    }

    public void sortDocAssocInfos() {
        if (docAssocInfos != null) {
            Collections.sort(docAssocInfos);
        }
=======
        docAssocInfos = BeanHelper.getDocumentAssociationsService().getAssocInfos(document);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    public void reset() {
        document = null;
<<<<<<< HEAD
        docAssocInfos = new ArrayList<DocAssocInfo>();
=======
        docAssocInfos = null;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    @Override
    public void resetOrInit(DialogDataProvider provider) {
        if (provider == null) {
            reset();
        } else {
            init(provider.getNode());
        }
    }

    public String getFollowupAssocsBindingName() {
<<<<<<< HEAD
        try {
            WmNode document = getDocumentFromDialog();// FIXME document should be provided by bean
            if (new AddFollowUpAssocEvaluator().evaluate(document)) {
                return FOLLOWUPS_METHOD_BINDING_NAME;
            }
            return null;
        } catch (RuntimeException e) {
            // Log error here, because JSF EL evaluator does not log detailed error cause
            LOG.error("Error getting followupAssocsBindingName", e);
            throw e;
        }
    }

    public void disableDelete() {
        for (DocAssocInfo assocInf : docAssocInfos) {
            assocInf.setAllowDelete(false);
        }
=======
        WmNode document = getDocumentFromDialog();// FIXME document should be provided by bean
        if (new AddFollowUpAssocEvaluator().evaluate(document)) {
            return FOLLOWUPS_METHOD_BINDING_NAME;
        }
        return null;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    /**
     * @return document object form DocumentDynamicDialog -
     *         FIXME DLSeadist - needed until this {@link AssocsBlockBean} is not managed by that dialog
     */
    private WmNode getDocumentFromDialog() {
        return BeanHelper.getDocumentDynamicDialog().getActionsContext();
    }

    public String getRepliesAssocsBindingName() {
<<<<<<< HEAD
        try {
            WmNode document = getDocumentFromDialog();
            if (new AddReplyAssocEvaluator().evaluate(document)) {
                return REPLIES_METHOD_BINDING_NAME;
            }
            return null;
        } catch (RuntimeException e) {
            // Log error here, because JSF EL evaluator does not log detailed error cause
            LOG.error("Error getting repliesAssocsBindingName", e);
            throw e;
        }
=======
        WmNode document = getDocumentFromDialog();
        if (new AddReplyAssocEvaluator().evaluate(document)) {
            return REPLIES_METHOD_BINDING_NAME;
        }
        return null;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    public List<ActionDefinition> createAddFollowupsMenu(@SuppressWarnings("unused") String nodeTypeId) {
        return initCreateAddAssocMenu(DocTypeAssocType.FOLLOWUP);
    }

    public List<ActionDefinition> createAddRepliesMenu(@SuppressWarnings("unused") String nodeTypeId) {
        return initCreateAddAssocMenu(DocTypeAssocType.REPLY);
    }

    private List<ActionDefinition> initCreateAddAssocMenu(DocTypeAssocType docTypeAssocType) {
        DocumentType documentType = BeanHelper.getDocumentDynamicDialog().getDocumentType();
        if (documentType == null) {
            return Collections.emptyList();
        }
        List<? extends AssociationModel> assocs = documentType.getAssociationModels(docTypeAssocType);
        List<ActionDefinition> actionDefinitions = new ArrayList<ActionDefinition>(assocs.size());
        Map<String, String> documentTypeNames = getDocumentAdminService().getDocumentTypeNames(null);
        for (AssociationModel assocModel : assocs) {
            String docTypeId = assocModel.getDocType();
            if (docTypeAssocType == DocTypeAssocType.FOLLOWUP
                    && (SystematicDocumentType.REPORT.isSameType(docTypeId) || SystematicDocumentType.ERRAND_ORDER_ABROAD.isSameType(docTypeId))) {
                continue;
            }
            ActionDefinition actionDefinition = new ActionDefinition("compoundWorkflowDefinitionAction");
            actionDefinition.Image = DROPDOWN_MENU_ITEM_ICON;
            actionDefinition.Label = documentTypeNames.get(docTypeId);
            actionDefinition.ActionListener = "#{" + DocumentDynamicDialog.BEAN_NAME + ".createAssoc}";
            actionDefinition.addParam(PARAM_ASSOC_MODEL_REF, assocModel.getNodeRef().toString());

            actionDefinitions.add(actionDefinition);
        }
        return actionDefinitions;
    }

    public String getAddFollowUpsLabel() {
        return MessageUtil.getMessage("document_addFollowUp");
    }

    public String getAddRepliesLabel() {
        return MessageUtil.getMessage("document_addReply");
    }

    // START: getters / setters

    public Node getDocument() {
        return document;
    }

    public List<DocAssocInfo> getDocAssocInfos() {
        return docAssocInfos;
    }

    // END: getters / setters

}

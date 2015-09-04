package ee.webmedia.alfresco.document.associations.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.config.ActionsConfigElement.ActionDefinition;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.common.service.RequestCacheBean;
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
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.workflow.web.WorkflowBlockBean;

/**
 * Block that shows associations of given document/volume/caseFile/case with other documents/volumes/caseFiles/cases
 */
public class AssocsBlockBean implements DocumentDynamicBlock {
    private static final long serialVersionUID = 1L;

    private static org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(WorkflowBlockBean.class);

    public static final String BEAN_NAME = "AssocsBlockBean";
    private static final String FOLLOWUPS_METHOD_BINDING_NAME = "#{" + BEAN_NAME + ".createAddFollowupsMenu}";
    private static final String REPLIES_METHOD_BINDING_NAME = "#{" + BEAN_NAME + ".createAddRepliesMenu}";
    private static final String DROPDOWN_MENU_ITEM_ICON = "/images/icons/versioned_properties.gif";
    private static final String DROPDOWN_MENU_SINGLE_ITEM_ICON = "/images/icons/arrow-right.png";
    private static final String FOLLOWUP_ASSOC_BINDING_CAHCE_KEY = "FollowupAssocsBinding";
    private static final String REPLY_ASSOC_BINDING_CAHCE_KEY = "RepliesAssocsBindingName";
    public static final String PARAM_ASSOC_MODEL_REF = "assocModelRef";

    private RequestCacheBean requestCacheBean;

    private Node document;
    private List<DocAssocInfo> docAssocInfos = new ArrayList<DocAssocInfo>();

    public void init(Node node) {
        reset();
        document = node;
        restore();
    }

    public void restore() {
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
    }

    public void reset() {
        document = null;
        docAssocInfos = new ArrayList<DocAssocInfo>();
    }

    @Override
    public void resetOrInit(DialogDataProvider provider) {
        if (provider == null) {
            reset();
        } else {
            init(provider.getNode());
        }
    }

    @Override
    public void clean() {
        document = null;
        docAssocInfos = null;
    }

    public int getAssocsCount() {
        return docAssocInfos.size();
    }

    public String getFollowupAssocsBindingName() {
        WmNode document = null;
        try {
            Boolean returnBinding = (Boolean) requestCacheBean.getResult(FOLLOWUP_ASSOC_BINDING_CAHCE_KEY);
            if (returnBinding != null) {
                return returnBinding ? FOLLOWUPS_METHOD_BINDING_NAME : null;
            }
            document = getDocumentFromDialog();// FIXME document should be provided by bean
            returnBinding = new AddFollowUpAssocEvaluator().evaluate(document);
            requestCacheBean.setResult(FOLLOWUP_ASSOC_BINDING_CAHCE_KEY, returnBinding);
            return returnBinding ? FOLLOWUPS_METHOD_BINDING_NAME : null;
        } catch (InvalidNodeRefException ne) {
            LOG.warn("Node " + document + " in invalid!");
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
    }

    /**
     * @return document object form DocumentDynamicDialog -
     *         FIXME DLSeadist - needed until this {@link AssocsBlockBean} is not managed by that dialog
     */
    private WmNode getDocumentFromDialog() {
        return BeanHelper.getDocumentDynamicDialog().getActionsContext();
    }

    public String getRepliesAssocsBindingName() {
        WmNode document = null;
        try {
            Boolean returnBinding = (Boolean) requestCacheBean.getResult(REPLY_ASSOC_BINDING_CAHCE_KEY);
            if (returnBinding != null) {
                return returnBinding ? REPLIES_METHOD_BINDING_NAME : null;
            }
            document = getDocumentFromDialog();
            returnBinding = new AddReplyAssocEvaluator().evaluate(document);
            requestCacheBean.setResult(REPLY_ASSOC_BINDING_CAHCE_KEY, returnBinding);
            return returnBinding ? REPLIES_METHOD_BINDING_NAME : null;
        } catch (InvalidNodeRefException ne) {
            LOG.warn("Node " + document + " in invalid!");
            return null;
        } catch (RuntimeException e) {
            // Log error here, because JSF EL evaluator does not log detailed error cause
            LOG.error("Error getting repliesAssocsBindingName", e);
            throw e;
        }
    }

    public List<ActionDefinition> createAddFollowupsMenu(@SuppressWarnings("unused") String nodeTypeId) {
        return initCreateAddAssocMenu(DocTypeAssocType.FOLLOWUP, "document_addFollowUp");
    }

    public List<ActionDefinition> createAddRepliesMenu(@SuppressWarnings("unused") String nodeTypeId) {
        return initCreateAddAssocMenu(DocTypeAssocType.REPLY, "document_addReply");
    }

    public int getAddFollowupsMenuSize() {
        return getCreateAddAssocMenuSize(DocTypeAssocType.FOLLOWUP);
    }

    public int getAddRepliesMenuSize() {
        return getCreateAddAssocMenuSize(DocTypeAssocType.REPLY);
    }

    private int getCreateAddAssocMenuSize(DocTypeAssocType docTypeAssocType) {
        int size = 0;
        DocumentType documentType = BeanHelper.getDocumentDynamicDialog().getDocumentType();
        if (documentType == null) {
            return size;
        }
        List<? extends AssociationModel> assocs = documentType.getAssociationModels(docTypeAssocType);
        for (AssociationModel assocModel : assocs) {
            String docTypeId = assocModel.getDocType();
            if (docTypeAssocType == DocTypeAssocType.FOLLOWUP
                    && (SystematicDocumentType.REPORT.isSameType(docTypeId) || SystematicDocumentType.ERRAND_ORDER_ABROAD.isSameType(docTypeId))) {
                continue;
            }
            size++;
        }

        return size;
    }

    private List<ActionDefinition> initCreateAddAssocMenu(DocTypeAssocType docTypeAssocType, String defaultLabelMsg) {
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

        if (actionDefinitions.size() == 1 && StringUtils.isNotBlank(defaultLabelMsg)) {
            ActionDefinition actionDefinition = actionDefinitions.get(0);
            actionDefinition.Label = null;
            actionDefinition.LabelMsg = defaultLabelMsg;
            actionDefinition.Image = DROPDOWN_MENU_SINGLE_ITEM_ICON;
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

    public void setRequestCacheBean(RequestCacheBean requestCacheBean) {
        this.requestCacheBean = requestCacheBean;
    }

    // END: getters / setters

}

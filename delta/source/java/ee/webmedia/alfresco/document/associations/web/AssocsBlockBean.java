package ee.webmedia.alfresco.document.associations.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.config.ActionsConfigElement.ActionDefinition;

import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.AssociationModel;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicBlock;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicDialog;
import ee.webmedia.alfresco.document.associations.model.DocAssocInfo;
import ee.webmedia.alfresco.document.assocsdyn.web.AddFollowUpAssocEvaluator;
import ee.webmedia.alfresco.document.assocsdyn.web.AddReplyAssocEvaluator;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Block that shows associations of given document with other documents and related cases
 * 
 * @author Ats Uiboupin
 */
public class AssocsBlockBean implements DocumentDynamicBlock {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "AssocsBlockBean";
    private static final String FOLLOWUPS_METHOD_BINDING_NAME = "#{" + BEAN_NAME + ".createAddFollowupsMenu}";
    private static final String REPLIES_METHOD_BINDING_NAME = "#{" + BEAN_NAME + ".createAddRepliesMenu}";
    private static final String DROPDOWN_MENU_ITEM_ICON = "/images/icons/versioned_properties.gif";
    public static final String PARAM_ASSOC_MODEL_REF = "assocModelRef";

    private Node document;
    private List<DocAssocInfo> docAssocInfos;

    public void init(Node node) {
        reset();
        document = node;
        restore();
    }

    public void restore() {
        docAssocInfos = BeanHelper.getDocumentAssociationsService().getAssocInfos(document);
    }

    public void reset() {
        document = null;
        docAssocInfos = null;
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
        WmNode document = getDocumentFromDialog();// FIXME document should be provided by bean
        if (new AddFollowUpAssocEvaluator().evaluate(document)) {
            return FOLLOWUPS_METHOD_BINDING_NAME;
        }
        return null;
    }

    /**
     * @return document object form DocumentDynamicDialog -
     *         FIXME DLSeadist - needed until this {@link AssocsBlockBean} is not managed by that dialog
     */
    private WmNode getDocumentFromDialog() {
        return BeanHelper.getDocumentDynamicDialog().getActionsContext();
    }

    public String getRepliesAssocsBindingName() {
        WmNode document = getDocumentFromDialog();// FIXME document should be provided by bean
        if (new AddReplyAssocEvaluator().evaluate(document)) {
            return REPLIES_METHOD_BINDING_NAME;
        }
        return null;
    }

    public List<ActionDefinition> createAddFollowupsMenu(@SuppressWarnings("unused") String nodeTypeId) {
        WmNode document = getDocumentFromDialog();
        if (document == null) {
            return Collections.emptyList();
        }
        return initCreateAddAssocMenu(document, DocTypeAssocType.FOLLOWUP);
    }

    public List<ActionDefinition> createAddRepliesMenu(@SuppressWarnings("unused") String nodeTypeId) {
        WmNode document = getDocumentFromDialog();
        if (document == null) {
            return Collections.emptyList();
        }
        return initCreateAddAssocMenu(document, DocTypeAssocType.REPLY);
    }

    private List<ActionDefinition> initCreateAddAssocMenu(WmNode document, DocTypeAssocType docTypeAssocType) {
        String documentTypeId = (String) document.getProperties().get(DocumentAdminModel.Props.OBJECT_TYPE_ID);

        QName assocType = docTypeAssocType.getAssocBetweenDocTypeAndAssocModel();
        List<AssociationModel> assocs = BeanHelper.getDocumentAssociationsService().getAssocs(documentTypeId, assocType);
        List<ActionDefinition> actionDefinitions = new ArrayList<ActionDefinition>(assocs.size());
        DocumentAdminService documentAdminService = BeanHelper.getDocumentAdminService();
        Map<String, String> documentTypeNames = documentAdminService.getDocumentTypeNames(null);
        for (AssociationModel assocModel : assocs) {
            ActionDefinition actionDefinition = new ActionDefinition("compoundWorkflowDefinitionAction");
            actionDefinition.Image = DROPDOWN_MENU_ITEM_ICON;
            String docTypeId = assocModel.getDocType();
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

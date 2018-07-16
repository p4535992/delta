package ee.webmedia.alfresco.sharepoint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.service.Task;

public class OwnerEmailUpdater extends AbstractNodeUpdater {

    private UserService userService;

    private Map<String, String> userEmails;

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(OwnerEmailUpdater.class);

    @Override
    protected void executeUpdater() throws Exception {
        userEmails = new HashMap<String, String>();
        super.executeUpdater();
        updateTaskEmailsManually();
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateStringExactQuery("test22@just.ee", DocumentCommonModel.Props.OWNER_EMAIL);
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        resultSets.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        resultSets.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return resultSets;
    }

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
    }

    // In some cases lucene search fails to find all the e-mails. In this case e-mails must be updated directly in database.
    protected void updateTaskEmailsManually() {
        List<Object> arguments = new ArrayList<Object>();
        String emailToReplace = "test22@just.ee";
        arguments.add(emailToReplace);
        List<Task> results = BeanHelper.getWorkflowDbService().searchTasksAllStores("(wfc_owner_email=? )", arguments, -1).getFirst();
        Map<QName, Serializable> changedProps = new HashMap<QName, Serializable>();
        for (Task task : results) {
            changedProps.clear();
            try {
                String ownerId = task.getOwnerId();
                String newTaskOwnerEmail = BeanHelper.getUserService().getUserEmail(ownerId);
                if (StringUtils.isBlank(newTaskOwnerEmail)) {
                    LOG.warn("The e-mail of following task was not updated because no user e-mail address was found:\n" + task);
                    continue;
                }
                changedProps.put(WorkflowCommonModel.Props.OWNER_EMAIL, newTaskOwnerEmail);
                BeanHelper.getWorkflowDbService().updateTaskEntryIgnoringParent(task, changedProps);
                LOG.info("Updated task [nodeRef=" + task.getNodeRef() + "] e-mail manually: " + emailToReplace + " -> " + newTaskOwnerEmail);
            } catch (Exception e) {
                LOG.error(e);
                continue;
            }
        }
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> updatedProps = new HashMap<QName, Serializable>();
        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);
        String ownerId = (String) origProps.get(DocumentCommonModel.Props.OWNER_ID);

        String oldOwnerEmail = (String) origProps.get(DocumentCommonModel.Props.OWNER_EMAIL);
        String newOwnerEmail = oldOwnerEmail;
        Boolean setValue = Boolean.FALSE;
        if ("test22@just.ee".equals(StringUtils.strip(oldOwnerEmail))) {
            if (StringUtils.isNotBlank(ownerId)) {
                if (userEmails.containsKey(ownerId)) {
                    newOwnerEmail = userEmails.get(ownerId);
                } else {
                    newOwnerEmail = userService.getUserEmail(ownerId);
                    userEmails.put(ownerId, newOwnerEmail);
                }
            } else {
                newOwnerEmail = null;
            }
            updatedProps.put(DocumentCommonModel.Props.OWNER_EMAIL, newOwnerEmail);
            updatedProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
            updatedProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
            setValue = Boolean.TRUE;
        }

        if (!updatedProps.isEmpty()) {
            nodeService.addProperties(nodeRef, updatedProps);
        }
        Date regDateTime = (Date) origProps.get(DocumentCommonModel.Props.REG_DATE_TIME);
        return new String[] {
                setValue.toString(),
                ownerId,
                oldOwnerEmail,
                newOwnerEmail,
                (String) origProps.get(DocumentCommonModel.Props.DOC_NAME),
                (String) origProps.get(DocumentCommonModel.Props.REG_NUMBER),
                regDateTime == null ? null : regDateTime.toString() };
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] {
                "nodeRef",
                "setOwnerEmail",
                "ownerId",
                "oldOwnerEmail",
                "newOwnerEmail",
                "docName",
                "regNumber",
                "regDateTime" };
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

}

<<<<<<< HEAD
package ee.webmedia.alfresco.workflow.bootstrap;

import static ee.webmedia.alfresco.series.bootstrap.Series25To313DynamicDocTypeUpdater.getDynamicDocTypes;
import static ee.webmedia.alfresco.series.bootstrap.Series25To313DynamicDocTypeUpdater.getStaticToDynamicTypeMapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * Update compound workflow docType property to dynamic type. Should run only when migrating from 2.5 to 3.13.
 * 
 * @author Riina Tens
 */
public class Workflow25To313DynamicDocTypeUpdater extends AbstractNodeUpdater {

    private Map<String, String> staticToDynamicDocTypeIds;
    private CompoundWorkflowOwnerPropsUpdater compoundWorkflowOwnerPropsUpdater;
    private UserService userService;
    private Map<String, List<String>> userOrgPaths;
    private boolean organizationsSynchronized;

    @Override
    protected void executeInternal() throws Throwable {
        if (isEnabled()) {
            Set<String> dynamicDocTypes = BeanHelper.getDocumentAdminService().getDocumentTypeNames(null).keySet();
            staticToDynamicDocTypeIds = getStaticToDynamicTypeMapping(dynamicDocTypes, BeanHelper.getDictionaryService().getTypes(DocumentSubtypeModel.MODEL_NAME));
            compoundWorkflowOwnerPropsUpdater.setEnabled(false);
            userOrgPaths = new HashMap<String, List<String>>();
            synchronizeOrganizations();
        }
        super.executeInternal();
    }

    public void synchronizeOrganizations() {
        try {
            log.info("Starting to synchronize organization structures");
            BeanHelper.getOrganizationStructureService().updateOrganisationStructures();
            organizationsSynchronized = true;
            log.info("Finished synchronizing organization structures");
        } catch (Exception e) {
            log.error("Failed to update organization structures", e);
        }
    }

    public boolean isOrganizationsSynchronized() {
        return organizationsSynchronized;
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW, WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION);
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> compoundWorkflowProps = nodeService.getProperties(nodeRef);
        @SuppressWarnings("unchecked")
        List<String> docTypes = (List<String>) compoundWorkflowProps.get(WorkflowCommonModel.Props.DOCUMENT_TYPES);
        Pair<List<String>, List<String>> dynamicDocTypes = getDynamicDocTypes(docTypes, staticToDynamicDocTypeIds);
        Map<QName, Serializable> propsToAdd = new HashMap<QName, Serializable>();
        propsToAdd.put(WorkflowCommonModel.Props.DOCUMENT_TYPES, (Serializable) dynamicDocTypes.getFirst());
        if (StringUtils.isBlank((String) compoundWorkflowProps.get(WorkflowCommonModel.Props.TYPE))) {
            propsToAdd.put(WorkflowCommonModel.Props.TYPE, CompoundWorkflowType.DOCUMENT_WORKFLOW.toString());
        }
        String ownerId = (String) compoundWorkflowProps.get(WorkflowCommonModel.Props.OWNER_ID);
        if (StringUtils.isNotBlank(ownerId) && (!compoundWorkflowProps.containsKey(WorkflowCommonModel.Props.OWNER_JOB_TITLE) ||
                !compoundWorkflowProps.containsKey(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME))) {
            Node user = BeanHelper.getUserService().getUser(ownerId);
            if (user != null) {
                Map<String, Object> userProps = user.getProperties();
                propsToAdd.put(WorkflowCommonModel.Props.OWNER_JOB_TITLE, (Serializable) userProps.get(ContentModel.PROP_JOBTITLE));
                if (!userOrgPaths.containsKey(ownerId)) {
                    userOrgPaths.put(ownerId, userService.getUserOrgPathOrOrgName(RepoUtil.toQNameProperties(userProps)));
                }
                propsToAdd.put(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME, (Serializable) userOrgPaths.get(ownerId));
            }
        }
        nodeService.addProperties(nodeRef, propsToAdd);
        return new String[] { propsToAdd.toString() };
    }

    public void setCompoundWorkflowOwnerPropsUpdater(CompoundWorkflowOwnerPropsUpdater compoundWorkflowOwnerPropsUpdater) {
        this.compoundWorkflowOwnerPropsUpdater = compoundWorkflowOwnerPropsUpdater;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

}
=======
package ee.webmedia.alfresco.workflow.bootstrap;

import static ee.webmedia.alfresco.series.bootstrap.Series25To313DynamicDocTypeUpdater.getDynamicDocTypes;
import static ee.webmedia.alfresco.series.bootstrap.Series25To313DynamicDocTypeUpdater.getStaticToDynamicTypeMapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * Update compound workflow docType property to dynamic type. Should run only when migrating from 2.5 to 3.13.
 */
public class Workflow25To313DynamicDocTypeUpdater extends AbstractNodeUpdater {

    private Map<String, String> staticToDynamicDocTypeIds;
    private CompoundWorkflowOwnerPropsUpdater compoundWorkflowOwnerPropsUpdater;
    private UserService userService;
    private Map<String, List<String>> userOrgPaths;
    private boolean organizationsSynchronized;

    @Override
    protected void executeInternal() throws Throwable {
        if (isEnabled()) {
            Set<String> dynamicDocTypes = BeanHelper.getDocumentAdminService().getDocumentTypeNames(null).keySet();
            staticToDynamicDocTypeIds = getStaticToDynamicTypeMapping(dynamicDocTypes, BeanHelper.getDictionaryService().getTypes(DocumentSubtypeModel.MODEL_NAME));
            compoundWorkflowOwnerPropsUpdater.setEnabled(false);
            userOrgPaths = new HashMap<String, List<String>>();
            synchronizeOrganizations();
        }
        super.executeInternal();
    }

    public void synchronizeOrganizations() {
        try {
            log.info("Starting to synchronize organization structures");
            BeanHelper.getOrganizationStructureService().updateOrganisationStructures();
            organizationsSynchronized = true;
            log.info("Finished synchronizing organization structures");
        } catch (Exception e) {
            log.error("Failed to update organization structures", e);
        }
    }

    public boolean isOrganizationsSynchronized() {
        return organizationsSynchronized;
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW, WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION);
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> compoundWorkflowProps = nodeService.getProperties(nodeRef);
        @SuppressWarnings("unchecked")
        List<String> docTypes = (List<String>) compoundWorkflowProps.get(WorkflowCommonModel.Props.DOCUMENT_TYPES);
        Pair<List<String>, List<String>> dynamicDocTypes = getDynamicDocTypes(docTypes, staticToDynamicDocTypeIds);
        Map<QName, Serializable> propsToAdd = new HashMap<QName, Serializable>();
        propsToAdd.put(WorkflowCommonModel.Props.DOCUMENT_TYPES, (Serializable) dynamicDocTypes.getFirst());
        if (StringUtils.isBlank((String) compoundWorkflowProps.get(WorkflowCommonModel.Props.TYPE))) {
            propsToAdd.put(WorkflowCommonModel.Props.TYPE, CompoundWorkflowType.DOCUMENT_WORKFLOW.toString());
        }
        String ownerId = (String) compoundWorkflowProps.get(WorkflowCommonModel.Props.OWNER_ID);
        if (StringUtils.isNotBlank(ownerId) && (!compoundWorkflowProps.containsKey(WorkflowCommonModel.Props.OWNER_JOB_TITLE) ||
                !compoundWorkflowProps.containsKey(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME))) {
            Node user = BeanHelper.getUserService().getUser(ownerId);
            if (user != null) {
                Map<String, Object> userProps = user.getProperties();
                propsToAdd.put(WorkflowCommonModel.Props.OWNER_JOB_TITLE, (Serializable) userProps.get(ContentModel.PROP_JOBTITLE));
                if (!userOrgPaths.containsKey(ownerId)) {
                    userOrgPaths.put(ownerId, userService.getUserOrgPathOrOrgName(RepoUtil.toQNameProperties(userProps)));
                }
                propsToAdd.put(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME, (Serializable) userOrgPaths.get(ownerId));
            }
        }
        nodeService.addProperties(nodeRef, propsToAdd);
        return new String[] { propsToAdd.toString() };
    }

    public void setCompoundWorkflowOwnerPropsUpdater(CompoundWorkflowOwnerPropsUpdater compoundWorkflowOwnerPropsUpdater) {
        this.compoundWorkflowOwnerPropsUpdater = compoundWorkflowOwnerPropsUpdater;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

}
>>>>>>> develop-5.1

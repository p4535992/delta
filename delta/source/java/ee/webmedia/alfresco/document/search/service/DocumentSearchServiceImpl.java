package ee.webmedia.alfresco.document.search.service;

import static ee.webmedia.alfresco.utils.SearchUtil.generateAspectQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateDatePropertyRangeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateMultiStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateNodeRefQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyBooleanQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyDateQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyNullQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyWildcardQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringNotEmptyQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringNullQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.isBlank;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.lucene.search.BooleanQuery;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.adr.model.AdrModel;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.document.search.model.FakeDocument;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.search.service.AbstractSearchServiceImpl;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.substitute.model.SubstitutionInfo;
import ee.webmedia.alfresco.substitute.web.SubstitutionBean;
import ee.webmedia.alfresco.user.model.Authority;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.search.model.TaskInfo;
import ee.webmedia.alfresco.workflow.search.model.TaskSearchModel;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.SendStatus;

/**
 * @author Alar Kvell
 * @author Erko Hansar
 */
public class DocumentSearchServiceImpl extends AbstractSearchServiceImpl implements DocumentSearchService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentSearchServiceImpl.class);

    private DocumentService documentService;
    private GeneralService generalService;
    private NodeService nodeService;
    private SearchService searchService;
    private SeriesService seriesService;
    private VolumeService volumeService;
    private WorkflowService workflowService;
    private ParametersService parametersService;
    private NamespaceService namespaceService;
    private AuthorityService authorityService;
    private UserService userService;
    private List<StoreRef> allStores = null;

    @Override
    public List<Document> searchAdrDocuments(Date regDateBegin, Date regDateEnd, QName docType, String searchString, Set<QName> documentTypes) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(5);
        if (regDateBegin != null || regDateEnd != null) {
            queryParts.add(generateDatePropertyRangeQuery(regDateBegin, regDateEnd //
                    , DocumentCommonModel.Props.REG_DATE_TIME, DocumentSpecificModel.Props.SENDER_REG_DATE));
        }
        queryParts.add(generateQuickSearchQuery(searchString));
        if (docType != null) {
            if (!documentTypes.contains(docType)) {
                return Collections.emptyList();
            }
            queryParts.add(generateTypeQuery(docType));
        }
        // If parameters generate no search query, then return nothing
        // Note: you can still get ALL documents very easily if you specify a very broad date range for example
        if (isBlank(queryParts)) {
            return Collections.emptyList();
        }

        String query = generateAdrDocumentSearchQuery(queryParts, documentTypes);
        List<Document> results = searchDocumentsImpl(query, false, /* queryName */ "adrDocuments1");
        results.addAll(searchDocumentsImpl(query, false, /* queryName */ "adrDocuments2", Arrays.asList(generalService.getArchivalsStoreRef())));
        if (log.isDebugEnabled()) {
            log.debug("ADR documents search total time " + (System.currentTimeMillis() - startTime) + " ms, results " + results.size() + ", query: " + query);
        }
        return results;
    }

    @Override
    public List<Document> searchAdrDocuments(String regNumber, Date regDate, Set<QName> documentTypes) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(4);
        queryParts.add(generatePropertyDateQuery(DocumentCommonModel.Props.REG_DATE_TIME, regDate));
        queryParts.add(generateStringExactQuery(regNumber, DocumentCommonModel.Props.REG_NUMBER));

        String query = generateAdrDocumentSearchQuery(queryParts, documentTypes);
        List<Document> results = searchDocumentsImpl(query, false, /* queryName */ "adrDocumentByReg1");
        results.addAll(searchDocumentsImpl(query, false, /* queryName */ "adrDocumentByReg2", Arrays.asList(generalService.getArchivalsStoreRef())));
        if (log.isDebugEnabled()) {
            log.debug("ADR document details search total time " + (System.currentTimeMillis() - startTime) + " ms, results " + results.size() //
                    + ", query: " + query);
        }
        return results;
    }

    @Override
    public List<Document> searchAdrDocuments(Date modifiedDateBegin, Date modifiedDateEnd, Set<QName> documentTypes) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(3);
        if (modifiedDateBegin != null && modifiedDateEnd != null) {
            queryParts.add(generateDatePropertyRangeQuery(modifiedDateBegin, modifiedDateEnd, ContentModel.PROP_MODIFIED));
        }

        String query = generateAdrDocumentSearchQuery(queryParts, documentTypes);
        List<Document> results = searchDocumentsImpl(query, false, /* queryName */ "adrDocumentByModified1");
        results.addAll(searchDocumentsImpl(query, false, /* queryName */ "adrDocumentByModified2", Arrays.asList(generalService.getArchivalsStoreRef())));
        if (log.isDebugEnabled()) {
            log.debug("ADR document details search total time " + (System.currentTimeMillis() - startTime) + " ms, results " + results.size() //
                    + ", query: " + query);
        }
        return results;
    }

    @Override
    public List<NodeRef> searchAdrDeletedDocuments(Date deletedDateBegin, Date deletedDateEnd) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(2);
        queryParts.add(generateTypeQuery(AdrModel.Types.ADR_DELETED_DOCUMENT));
        queryParts.add(generateDatePropertyRangeQuery(deletedDateBegin, deletedDateEnd, AdrModel.Props.DELETED_DATE_TIME));

        String query = joinQueryPartsAnd(queryParts);
        List<NodeRef> results = searchNodes(query, false, /* queryName */ "adrDeletedDocuments");
        if (log.isDebugEnabled()) {
            log.debug("ADR deleted documents search total time " + (System.currentTimeMillis() - startTime) + " ms, results " + results.size() //
                    + ", query: " + query);
        }
        return results;
    }

    @Override
    public List<QName> searchAdrDeletedDocumentTypes(Date deletedDateBegin, Date deletedDateEnd) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(2);
        queryParts.add(generateTypeQuery(AdrModel.Types.ADR_DELETED_DOCUMENT_TYPE));
        queryParts.add(generateDatePropertyRangeQuery(deletedDateBegin, deletedDateEnd, AdrModel.Props.DELETED_DATE_TIME));

        String query = joinQueryPartsAnd(queryParts);
        List<QName> results = searchAdrDocumentTypesImpl(query, false, /* queryName */ "adrDeletedDocumentTypes");
        if (log.isDebugEnabled()) {
            log.debug("ADR deleted document types search total time " + (System.currentTimeMillis() - startTime) + " ms, results " + results.size()
                    + ", query: " + query);
        }
        return results;
    }

    @Override
    public List<QName> searchAdrAddedDocumentTypes(Date addedDateBegin, Date addedDateEnd) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(2);
        queryParts.add(generateTypeQuery(AdrModel.Types.ADR_ADDED_DOCUMENT_TYPE));
        queryParts.add(generateDatePropertyRangeQuery(addedDateBegin, addedDateEnd, AdrModel.Props.DELETED_DATE_TIME));

        String query = joinQueryPartsAnd(queryParts);
        List<QName> results = searchAdrDocumentTypesImpl(query, false, /* queryName */ "adrAddedDocumentTypes");
        if (log.isDebugEnabled()) {
            log.debug("ADR added document types search total time " + (System.currentTimeMillis() - startTime) + " ms, results " + results.size() //
                    + ", query: " + query);
        }
        return results;
    }

    private static String generateAdrDocumentSearchQuery(List<String> queryParts, Set<QName> documentTypes) {
        if (documentTypes.size() == 0) {
            return null;
        }
        List<String> documentTypeParts = new ArrayList<String>(documentTypes.size());
        for (QName documentType : documentTypes) {
            documentTypeParts.add(generateTypeQuery(documentType));
        }
        queryParts.add(joinQueryPartsOr(documentTypeParts));

        // Only finished documents go public
        queryParts.add(generateStringExactQuery(DocumentStatus.FINISHED.getValueName(), DocumentCommonModel.Props.DOC_STATUS));

        // Possible access restrictions
        List<String> accessParts = new ArrayList<String>(2);
        accessParts.add(generateStringExactQuery(AccessRestriction.AK.getValueName(), DocumentCommonModel.Props.ACCESS_RESTRICTION));
        accessParts.add(generateStringExactQuery(AccessRestriction.OPEN.getValueName(), DocumentCommonModel.Props.ACCESS_RESTRICTION));
        queryParts.add(joinQueryPartsOr(accessParts));

        queryParts.add(generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE));
        return joinQueryPartsAnd(queryParts);
    }

    @Override
    public List<Document> searchIncomingLetterRegisteredDocuments(String senderRegNumber) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(4);
        queryParts.add(generateTypeQuery(DocumentSubtypeModel.Types.INCOMING_LETTER));
        queryParts.add(generateStringNotEmptyQuery(DocumentCommonModel.Props.REG_DATE_TIME));
        queryParts.add(generateStringExactQuery(senderRegNumber, DocumentSpecificModel.Props.SENDER_REG_NUMBER));

        String query = generateDocumentSearchQuery(queryParts);
        List<Document> results = searchDocumentsImpl(query, false, /* queryName */ "incomingLetterRegisteredDocuments");
        if (log.isDebugEnabled()) {
            log.debug("Registered incoming letter documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public List<Document> searchTodayRegisteredDocuments() {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generatePropertyDateQuery(DocumentCommonModel.Props.REG_DATE_TIME, new Date()));
        String query = generateDocumentSearchQuery(queryParts);
        List<Document> results = searchDocumentsImpl(query, false, /* queryName */ "todayRegisteredDocuments");
        if (log.isDebugEnabled()) {
            log.debug("Today registered documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public List<Document> searchUserWorkingDocuments() {
        long startTime = System.currentTimeMillis();
        String query = getWorkingDocumentsOwnerQuery(AuthenticationUtil.getRunAsUser());
        List<Document> results = searchDocumentsImpl(query, false, /* queryName */ "userWorkingDocuments");
        if (log.isDebugEnabled()) {
            log.debug("Current user's and WORKING documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public int searchUserWorkingDocumentsCount() {
        long startTime = System.currentTimeMillis();
        String query = getWorkingDocumentsOwnerQuery(AuthenticationUtil.getRunAsUser());
        int count = 0;
        List<ResultSet> resultSets = doSearches(query, false, /* queryName */ "userWorkingDocumentsCount", getAllStores());
        try {
            for(ResultSet resultSet : resultSets){
                count += resultSet.length();
            }
        } finally {
            try {
                for(ResultSet resultSet : resultSets){                
                    resultSet.close();
                }
            } catch (Exception e) {
                // Do nothing
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Current user's and WORKING documents count search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return count;
    }

    @Override
    public List<Document> searchAccessRestictionEndsAfterDate(Date restrictionEndDate) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>();
        List<String> tempQueryParts = new ArrayList<String>();

        tempQueryParts.add(generateStringExactQuery("AK", DocumentCommonModel.Props.ACCESS_RESTRICTION)); // TODO this should come from classificator!
        tempQueryParts.add(generatePropertyNullQuery(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE));
        tempQueryParts.add(generateStringNotEmptyQuery(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC));
        queryParts.add(joinQueryPartsAnd(tempQueryParts));
        tempQueryParts.clear();

        tempQueryParts.add(generateStringExactQuery("AK", DocumentCommonModel.Props.ACCESS_RESTRICTION)); // TODO this should come from classificator!
        tempQueryParts.add(generateDatePropertyRangeQuery(null, restrictionEndDate, DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE));
        queryParts.add(joinQueryPartsAnd(tempQueryParts));
        tempQueryParts.clear();

        tempQueryParts.addAll(queryParts);
        queryParts.clear();
        queryParts.add(joinQueryPartsOr(tempQueryParts));

        String query = generateDocumentSearchQuery(queryParts);
        List<Document> results = searchDocumentsImpl(query, false, /* queryName */ "accessRestictionEndsAfterDate");
        if (log.isDebugEnabled()) {
            log.debug("Search for documents with access restriction took " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public List<Document> searchRecipientFinishedDocuments() {
        long startTime = System.currentTimeMillis();
        String query = generateRecipientFinichedQuery();
        List<Document> results = searchDocumentsImpl(query, false, /* queryName */ "recipientFinishedDocuments");

        if (log.isDebugEnabled()) {
            log.debug("FINISHED documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public int searchRecipientFinishedDocumentsCount() {
        long startTime = System.currentTimeMillis();
        String query = generateRecipientFinichedQuery();
        List<NodeRef> results = searchNodesFromAllStores(query, false, /* queryName */ "recipientFinishedDocumentsCount");

        if (log.isDebugEnabled()) {
            log.debug("FINISHED documents count search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results.size();
    }

    private String generateRecipientFinichedQuery() {
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateStringNotEmptyQuery(DocumentCommonModel.Props.RECIPIENT_NAME, DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME));
        queryParts.add(generateStringExactQuery(DocumentStatus.FINISHED.getValueName(), DocumentCommonModel.Props.DOC_STATUS));
        queryParts.add(generateStringNullQuery(DocumentCommonModel.Props.SEARCHABLE_SEND_MODE));
        String query = generateDocumentSearchQuery(queryParts);
        return query;
    }

    @Override
    public List<Series> searchSeriesUnit(String unit) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateStringExactQuery(unit, SeriesModel.Props.STRUCT_UNIT));
        String query = generateSeriesSearchQuery(queryParts);
        List<Series> results = searchSeriesImpl(query, false, /* queryName */ "seriesUnit");

        if (log.isDebugEnabled()) {
            log.debug("FINISHED documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public List<Task> searchCurrentUsersTasksInProgress(QName taskType) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = getTaskQuery(taskType, AuthenticationUtil.getRunAsUser(), Status.IN_PROGRESS);
        addSubstitutionRestriction(queryParts);
        String query = generateTaskSearchQuery(queryParts);
        List<Task> results = searchTasksImpl(query, false, /* queryName */ "CurrentUsersTasksInProgress");
        if (log.isDebugEnabled()) {
            log.debug("Current user's and IN_PROGRESS tasks search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public List<Volume> searchVolumesDispositionedAfterDate(Date dispositionDate) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateDatePropertyRangeQuery(Calendar.getInstance().getTime(), dispositionDate, VolumeModel.Props.DISPOSITION_DATE));
        String query = generateVolumeSearchQuery(queryParts);
        List<Volume> results = searchVolumesImpl(query, false, /* queryName */ "volumesDispositionedAfterDate");
        if (log.isDebugEnabled()) {
            log.debug("Search for volumes that are dispositioned between now and " + dispositionDate //
                    + " total time: " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public List<Task> searchTasksDueAfterDate(Date dueDate) {
        long startTime = System.currentTimeMillis();
        Date fromDate = Calendar.getInstance().getTime(); // Set time frame from today, tasks that are due today are also due in e.g. 2 weeks
        if (dueDate == null) { // If no due date is specified search for tasks that are due
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -1);
            dueDate = cal.getTime();
            fromDate = null; // If due date isn't set then we must include result from the glory days of past
        }

        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateStringExactQuery(Status.IN_PROGRESS.getName(), WorkflowCommonModel.Props.STATUS));
        queryParts.add(generateStringNotEmptyQuery(WorkflowCommonModel.Props.OWNER_ID));
        queryParts.add(generateDatePropertyRangeQuery(fromDate, dueDate, WorkflowSpecificModel.Props.DUE_DATE));
        String query = generateTaskSearchQuery(queryParts);
        List<Task> results = searchTasksImpl(query, false, /* queryName */ "tasksDueAfterDate");
        if (log.isDebugEnabled()) {
            log.debug("Due date passed tasks search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public List<Authority> searchAuthorityGroups(String input, boolean returnAllGroups) {
        input = StringUtils.trimToEmpty(input);
        Set<String> results;
        List<Authority> authorities = new ArrayList<Authority>();
        if (input.length() == 0) {
            if (!returnAllGroups) {
                return Collections.emptyList();
            }
            results = authorityService.getAllAuthorities(AuthorityType.GROUP);
        } else {
            List<NodeRef> authorityRefs = searchAuthorityGroups(input);
            results = new HashSet<String>(authorityRefs.size());
            results.addAll(CollectionUtils.collect(authorityRefs, new Transformer(){
                @Override
                public Object transform(Object arg0) {
                    NodeRef groupRef = (NodeRef) arg0;
                    return nodeService.getProperty(groupRef, ContentModel.PROP_AUTHORITY_NAME);
                }
                
            }));
        }
        for (String result : results) {
            if (!userService.getAdministratorsGroup().equals(result) && !userService.getDocumentManagersGroup().equals(result)) {
                authorities.add(userService.getAuthority(result));
            }
        }        
        return authorities;
    }    
    
    private List<NodeRef> searchAuthorityGroups(String groupName) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(2);
        queryParts.add(generateTypeQuery(ContentModel.TYPE_AUTHORITY_CONTAINER));
        queryParts.add(generateStringWordsWildcardQuery(groupName, ContentModel.PROP_AUTHORITY_DISPLAY_NAME));

        String query = joinQueryPartsAnd(queryParts);
        List<NodeRef> results = searchNodes(query, false, /* queryName */ "authorityGroups");
        if (log.isDebugEnabled()) {
            log.debug("Authority groups search total time " + (System.currentTimeMillis() - startTime) + " ms, results " + results.size() //
                    + ", query: " + query);
        }
        return results;
    }    

    @Override
    public int getCurrentUsersTaskCount(QName taskType) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>();
        String ownerId = AuthenticationUtil.getRunAsUser();        
        queryParts.add(generateTypeQuery(taskType));
        queryParts.add(generateStringExactQuery(Status.IN_PROGRESS.getName(), WorkflowCommonModel.Props.STATUS));
        queryParts.add(generateStringExactQuery(ownerId, WorkflowCommonModel.Props.OWNER_ID));
        addSubstitutionRestriction(queryParts);
        String query = generateTaskSearchQuery(queryParts);
        int count = 0;
        List<ResultSet> resultSets = doSearches(query, false, /* queryName */ "currentUsersTaskCount", getAllStores());
        try {
            for(ResultSet resultSet : resultSets){
                count += resultSet.length();
            }
        } finally {
            try {
                for(ResultSet resultSet : resultSets){                
                    resultSet.close();
                }
            } catch (Exception e) {
                // Do nothing
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Current user's and IN_PROGRESS tasks count search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return count;
    }

    @Override
    public List<TaskInfo> searchTasks(Node filter) {
        long startTime = System.currentTimeMillis();
        String query = generateTaskSearchQuery(filter);
        List<TaskInfo> results = searchTaskInfosImpl(query, true, /* queryName */ "tasksByFilter");
        if (log.isDebugEnabled()) {
            log.debug("Tasks search total time " + (System.currentTimeMillis() - startTime) + " ms");
        }
        return results;
    }

    private void addSubstitutionRestriction(List<String> queryParts) {
        SubstitutionBean substitutionBean = (SubstitutionBean) FacesContextUtils.getRequiredWebApplicationContext( //
                FacesContext.getCurrentInstance()).getBean(SubstitutionBean.BEAN_NAME);        
        SubstitutionInfo subInfo = substitutionBean.getSubstitutionInfo();
        if (subInfo.isSubstituting()) {
            Date start = DateUtils.truncate(subInfo.getSubstitution().getSubstitutionStartDate(), Calendar.DATE);
            Long daysForSubstitutionTasksCalc = parametersService.getLongParameter(Parameters.DAYS_FOR_SUBSTITUTION_TASKS_CALC);
            Date end = DateUtils.truncate(subInfo.getSubstitution().getSubstitutionEndDate(), Calendar.DATE);
            end = DateUtils.addDays(end, daysForSubstitutionTasksCalc.intValue());
            queryParts.add(generateDatePropertyRangeQuery(start, end, WorkflowSpecificModel.Props.DUE_DATE));
        }
    }

    private static String DOCUMENTS_FOR_REGISTERING_QUERY;

    static {
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateStringExactQuery(DocumentStatus.WORKING.getValueName(), DocumentCommonModel.Props.DOC_STATUS));
        queryParts.add(generateStringNullQuery(DocumentCommonModel.Props.REG_DATE_TIME));
        DOCUMENTS_FOR_REGISTERING_QUERY = generateDocumentSearchQuery(queryParts);
    }

    @Override
    public List<Document> searchDocumentsForRegistering() {
        long startTime = System.currentTimeMillis();
        List<Document> results = searchGeneralImpl(DOCUMENTS_FOR_REGISTERING_QUERY, false, /* queryName */ "documentsForRegistering", new SearchCallback<Document>() {
            @Override
            public Document addResult(ResultSetRow row) {
                if (workflowService.hasAllFinishedCompoundWorkflows(row.getNodeRef())) {
                    return documentService.getDocumentByNodeRef(row.getNodeRef());
                }
                return null;
            }
        });
        if (log.isDebugEnabled()) {
            log.debug(String.format("Documents for registering search total time %d ms, query: %s" //
                    , (System.currentTimeMillis() - startTime), DOCUMENTS_FOR_REGISTERING_QUERY));
        }
        return results;
    }

    @Override
    public int getCountOfDocumentsForRegistering() {
        long startTime = System.currentTimeMillis();
        int count = searchGeneralImpl(DOCUMENTS_FOR_REGISTERING_QUERY, false, /* queryName */ "documentsForRegisteringCount", new SearchCallback<String>() {
            @Override
            public String addResult(ResultSetRow row) {
                return workflowService.hasAllFinishedCompoundWorkflows(row.getNodeRef())
                        ? row.getNodeRef().toString()
                        : null;
            }
        }).size();

        if (log.isDebugEnabled()) {
            log.debug(String.format("Documents for registering count search total time %d ms, query: %s" //
                    , (System.currentTimeMillis() - startTime), DOCUMENTS_FOR_REGISTERING_QUERY));
        }
        return count;
    }

    @Override
    public List<Document> searchDocuments(Node filter) {
        long startTime = System.currentTimeMillis();
        @SuppressWarnings("unchecked")
        final List<StoreRef> storeRefs = (List<StoreRef>) filter.getProperties().get(DocumentSearchModel.Props.STORE);
        String query = generateDocumentSearchQuery(filter);
        try {
            List<Document> results = searchDocumentsImpl(query, true, /* queryName */ "documentsByFilter", storeRefs);
            if (log.isDebugEnabled()) {
                log.debug("Documents search total time " + (System.currentTimeMillis() - startTime) + " ms");
            }
            return results;
        } catch (RuntimeException e) {
            Map<QName, Serializable> filterProps = RepoUtil.getNotEmptyProperties(RepoUtil.toQNameProperties(filter.getProperties()));
            filterProps.remove(DocumentSearchModel.Props.OUTPUT);
            log.error("Document search failed: "
                    + e.getMessage()
                    + "\n  searchFilter=" + WmNode.toString(filterProps, namespaceService)
                    + "\n  query=" + query, e);
            throw e;
        }
    }

    @Override
    public List<Document> searchDocumentsInOutbox() {
        String query = getDvkOutboxQuery();
        log.debug("searchDocumentsInOutbox with query '" + query + "'");
        return searchDocumentsBySendInfoImpl(query, false, /* queryName */ "documentsInOutbox");
    }

    @Override
    public int searchDocumentsInOutboxCount() {
        String query = getDvkOutboxQuery();
        log.debug("searchDocumentsInOutboxCount with query '" + query + "'");
        return searchDocumentsBySendInfoImplCount(query, false, /* queryName */ "documentsInOutboxCount");
    }

    @Override
    public Map<NodeRef /* sendInfo */, Pair<String /* dvkId */, String /* recipientRegNr */>> searchOutboxDvkIds() {
        String query = getDvkOutboxQuery();
        log.debug("searchDocumentsInOutbox with query '" + query + "'");
        return searchDhlIdsBySendInfoImpl(query, false, /* queryName */ "outboxDvkIds");
    }

    @Override
    public List<Document> searchDocumentsQuick(String searchValue) {
        return searchDocumentsQuick(searchValue, false);
    }

    @Override
    public List<Document> searchDocumentsQuick(String searchString, boolean includeCaseTitles) {
        long startTime = System.currentTimeMillis();
        String query = generateQuickSearchQuery(searchString);
        try {
            final List<Document> results;
            if (includeCaseTitles) {
                final String caseByTitleQuery = getCaseByTitleQuery(searchString);
                query = joinQueryPartsOr(Arrays.asList(query, caseByTitleQuery));
                results = searchDocumentsAndCaseTitlesImpl(query, true, /* queryName */ "documentsQuickAndCaseTitles");
            } else {
                results = searchDocumentsImpl(query, true, /* queryName */ "documentsQuick");
            }
            if (log.isDebugEnabled()) {
                log.debug("Quick search total time " + (System.currentTimeMillis() - startTime) + " ms");
            }
            return results;
        } catch (RuntimeException e) {
            log.error("Quick search failed: "
                    + e.getMessage()
                    + "\n  searchString=" + searchString
                    + "\n  includeCaseTitles=" + includeCaseTitles
                    + "\n  query=" + query, e);
            throw e;
        }
    }

    private String getCaseByTitleQuery(String searchValue) {
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateTypeQuery(CaseModel.Types.CASE));
        queryParts.add(generateStringWordsWildcardQuery(searchValue, CaseModel.Props.TITLE));
        String query = joinQueryPartsAnd(queryParts, false);
        return query;
    }

    @Override
    public List<NodeRef> searchWorkingDocumentsByOwnerId(String ownerId) {
        long startTime = System.currentTimeMillis();
        String query = getWorkingDocumentsOwnerQuery(ownerId);
        List<NodeRef> results = searchNodesFromAllStores(query, false, /* queryName */ "workingDocumentsByOwnerId");
        if (log.isDebugEnabled()) {
            log.debug("User's " + ownerId + " working documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public List<NodeRef> searchNewTasksByOwnerId(String ownerId) {
        long startTime = System.currentTimeMillis();
        String query = generateTaskSearchQuery(getTaskQuery(null, ownerId, Status.NEW));
        List<NodeRef> results = searchNodesFromAllStores(query, false, /* queryName */ "newTasksByOwnerId");
        if (log.isDebugEnabled()) {
            log.debug("User's " + ownerId + " new tasks search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    private List<String> getTaskQuery(QName taskType, String ownerId, Status status) {
        if (taskType == null) {
            taskType = WorkflowCommonModel.Types.TASK;
        }
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateTypeQuery(taskType));
        queryParts.add(generateStringExactQuery(status.getName(), WorkflowCommonModel.Props.STATUS));
        queryParts.add(generateStringExactQuery(ownerId, WorkflowCommonModel.Props.OWNER_ID));
        return queryParts;
    }

    private String getWorkingDocumentsOwnerQuery(String ownerId) {
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateStringExactQuery(ownerId, DocumentCommonModel.Props.OWNER_ID));
        queryParts.add(generateStringExactQuery(DocumentStatus.WORKING.getValueName(), DocumentCommonModel.Props.DOC_STATUS));
        return generateDocumentSearchQuery(queryParts);
    }

    private String getDvkOutboxQuery() {
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateTypeQuery(DocumentCommonModel.Types.SEND_INFO));
        queryParts.add(generateStringExactQuery(SendOutService.SEND_MODE_DVK, DocumentCommonModel.Props.SEND_INFO_SEND_MODE));
        queryParts.add(generateStringExactQuery(SendStatus.SENT.toString(), DocumentCommonModel.Props.SEND_INFO_SEND_STATUS));
        String query = joinQueryPartsAnd(queryParts, false);
        return query;
    }

    private Map<NodeRef /* sendInfo */, Pair<String /* dvkId */, String /* recipientRegNr */> > searchDhlIdsBySendInfoImpl(String query, boolean limited, String queryName) {
        final HashMap<NodeRef, Pair<String, String>> refsAndDvkIds = new HashMap<NodeRef, Pair<String, String> >();
        searchGeneralImpl(query, limited, queryName, new SearchCallback<String>() {
            @Override
            public String addResult(ResultSetRow row) {
                final NodeRef sendInfoRef = row.getNodeRef();
                Pair<String, String> dvkIdAndRecipientregNr = 
                    new Pair<String, String>((String) nodeService.getProperty(sendInfoRef, DocumentCommonModel.Props.SEND_INFO_DVK_ID), 
                            (String) nodeService.getProperty(sendInfoRef, DocumentCommonModel.Props.SEND_INFO_RECIPIENT_REG_NR));
                refsAndDvkIds.put(sendInfoRef, dvkIdAndRecipientregNr);
                return null;
            }
        });
        return refsAndDvkIds;
    }

    private List<Document> searchDocumentsBySendInfoImpl(String query, boolean limited, String queryName) {
        return searchGeneralImpl(query, limited, queryName, new SearchCallback<Document>() {
            private Set<String> nodeIds = new HashSet<String>();

            @Override
            public Document addResult(ResultSetRow row) {
                NodeRef docRef = row.getChildAssocRef().getParentRef();
                if (!nodeIds.contains(docRef.getId())) { // duplicate documents should be ignored
                    nodeIds.add(docRef.getId());
                    if (nodeService.hasAspect(docRef, DocumentCommonModel.Aspects.SEARCHABLE)) {
                        return documentService.getDocumentByNodeRef(docRef);
                    }
                }
                return null;
            }
        }, getAllStores());
    }

    private int searchDocumentsBySendInfoImplCount(String query, boolean limited, String queryName) {
        final Set<String> nodeIds = new HashSet<String>();
        searchGeneralImpl(query, limited, queryName, new SearchCallback<String>() {
            @Override
            public String addResult(ResultSetRow row) {
                final NodeRef docRef = row.getChildAssocRef().getParentRef();
                if (!nodeIds.contains(docRef.getId())) { // duplicate documents should be ignored
                    nodeIds.add(docRef.getId());
                }
                return null;
            }
        }, getAllStores());
        return nodeIds.size();
    }

    private String generateDocumentSearchQuery(Node filter) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(50);
        Map<String, Object> props = filter.getProperties();

        @SuppressWarnings("unchecked")
        List<QName> documentTypes = (List<QName>) props.get(DocumentSearchModel.Props.DOCUMENT_TYPE);
        queryParts.add(generateTypeQuery(documentTypes));
        queryParts.add(generateNodeRefQuery((NodeRef) props.get(DocumentSearchModel.Props.FUNCTION), DocumentCommonModel.Props.FUNCTION));
        queryParts.add(generateNodeRefQuery((NodeRef) props.get(DocumentSearchModel.Props.SERIES), DocumentCommonModel.Props.SERIES));
        queryParts.add(generateNodeRefQuery((NodeRef) props.get(DocumentSearchModel.Props.VOLUME), DocumentCommonModel.Props.VOLUME));
        queryParts.add(generateNodeRefQuery((NodeRef) props.get(DocumentSearchModel.Props.CASE), DocumentCommonModel.Props.CASE));
        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(DocumentSearchModel.Props.REG_DATE_TIME_BEGIN) //
                , (Date) props.get(DocumentSearchModel.Props.REG_DATE_TIME_END), DocumentCommonModel.Props.REG_DATE_TIME));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.REG_NUMBER), DocumentCommonModel.Props.REG_NUMBER));
        @SuppressWarnings("unchecked")
        List<String> status = (List<String>) props.get(DocumentSearchModel.Props.DOC_STATUS);
        queryParts.add(generateMultiStringExactQuery(status, DocumentCommonModel.Props.DOC_STATUS));
        @SuppressWarnings("unchecked")
        List<String> senderNames = (List<String>) props.get(DocumentSearchModel.Props.SENDER_NAME);
        queryParts.add(generateMultiStringWordsWildcardQuery(senderNames, DocumentSpecificModel.Props.SENDER_DETAILS_NAME));
        @SuppressWarnings("unchecked")
        List<String> recipientNames = (List<String>) props.get(DocumentSearchModel.Props.RECIPIENT_NAME);
        queryParts.add(generateMultiStringWordsWildcardQuery(recipientNames, DocumentCommonModel.Props.RECIPIENT_NAME,
                DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.DOC_NAME), DocumentCommonModel.Props.DOC_NAME));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.SENDER_REG_NUMBER),
                DocumentSpecificModel.Props.SENDER_REG_NUMBER));
        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(DocumentSearchModel.Props.SENDER_REG_DATE_BEGIN), //
                (Date) props.get(DocumentSearchModel.Props.SENDER_REG_DATE_END), DocumentSpecificModel.Props.SENDER_REG_DATE));
        queryParts.add(generateDatePropertyRangeQuery(
                (Date) props.get(DocumentSearchModel.Props.DUE_DATE_BEGIN),
                (Date) props.get(DocumentSearchModel.Props.DUE_DATE_END), DocumentSpecificModel.Props.DUE_DATE,
                DocumentSpecificModel.Props.CONTRACT_SIM_END_DATE,
                DocumentSpecificModel.Props.CONTRACT_SMIT_END_DATE));
        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(DocumentSearchModel.Props.COMPLIENCE_DATE_BEGIN), //
                (Date) props.get(DocumentSearchModel.Props.COMPLIENCE_DATE_END), DocumentSpecificModel.Props.COMPLIENCE_DATE));
        @SuppressWarnings("unchecked")
        List<String> accessRestriction = (List<String>) props.get(DocumentSearchModel.Props.ACCESS_RESTRICTION);
        queryParts.add(generateMultiStringExactQuery(accessRestriction, DocumentCommonModel.Props.ACCESS_RESTRICTION));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.ACCESS_RESTRICTION_REASON),
                DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON));
        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(DocumentSearchModel.Props.ACCESS_RESTRICTION_BEGIN_DATE_BEGIN), //
                (Date) props.get(DocumentSearchModel.Props.ACCESS_RESTRICTION_BEGIN_DATE_END), DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE));
        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(DocumentSearchModel.Props.ACCESS_RESTRICTION_END_DATE_BEGIN), //
                (Date) props.get(DocumentSearchModel.Props.ACCESS_RESTRICTION_END_DATE_END), DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.ACCESS_RESTRICTION_END_DESC),
                DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.OWNER_NAME), DocumentCommonModel.Props.OWNER_NAME));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.OWNER_ORG_STRUCT_UNIT),
                DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.OWNER_JOB_TITLE),
                DocumentCommonModel.Props.OWNER_JOB_TITLE));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.SIGNER_NAME), DocumentCommonModel.Props.SIGNER_NAME));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.SIGNER_JOB_TITLE),
                DocumentCommonModel.Props.SIGNER_JOB_TITLE));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.KEYWORDS), DocumentCommonModel.Props.KEYWORDS));
        @SuppressWarnings("unchecked")
        List<String> storageType = (List<String>) props.get(DocumentSearchModel.Props.STORAGE_TYPE);
        queryParts.add(generateMultiStringExactQuery(storageType, DocumentCommonModel.Props.STORAGE_TYPE));
        @SuppressWarnings("unchecked")
        List<String> sendMode = (List<String>) props.get(DocumentSearchModel.Props.SEND_MODE);
        queryParts.add(generateMultiStringExactQuery(sendMode, DocumentCommonModel.Props.SEARCHABLE_SEND_MODE));
        @SuppressWarnings("unchecked")
        List<String> costManager = (List<String>) props.get(DocumentSearchModel.Props.COST_MANAGER);
        queryParts.add(generateMultiStringExactQuery(costManager, DocumentCommonModel.Props.SEARCHABLE_COST_MANAGER));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.APPLICANT_NAME),
                DocumentCommonModel.Props.SEARCHABLE_APPLICANT_NAME));
        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(DocumentSearchModel.Props.ERRAND_BEGIN_DATE_BEGIN), //
                (Date) props.get(DocumentSearchModel.Props.ERRAND_BEGIN_DATE_END), DocumentCommonModel.Props.SEARCHABLE_ERRAND_BEGIN_DATE));
        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(DocumentSearchModel.Props.ERRAND_END_DATE_BEGIN), //
                (Date) props.get(DocumentSearchModel.Props.ERRAND_END_DATE_END), DocumentCommonModel.Props.SEARCHABLE_ERRAND_END_DATE));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.ERRAND_COUNTRY),
                DocumentCommonModel.Props.SEARCHABLE_ERRAND_COUNTRY));
        @SuppressWarnings("unchecked")
        List<String> errandCounty = (List<String>) props.get(DocumentSearchModel.Props.ERRAND_COUNTY);
        queryParts.add(generateMultiStringExactQuery(errandCounty, DocumentCommonModel.Props.SEARCHABLE_ERRAND_COUNTY));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.ERRAND_CITY),
                DocumentCommonModel.Props.SEARCHABLE_ERRAND_CITY));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.RESPONSIBLE_NAME),
                DocumentSpecificModel.Props.RESPONSIBLE_NAME));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.CO_RESPONSIBLES),
                DocumentSpecificModel.Props.CO_RESPONSIBLES));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(DocumentSearchModel.Props.CONTACT_PERSON),
                DocumentSpecificModel.Props.FIRST_PARTY_CONTACT_PERSON, DocumentSpecificModel.Props.SECOND_PARTY_CONTACT_PERSON,
                DocumentSpecificModel.Props.THIRD_PARTY_CONTACT_PERSON));
        @SuppressWarnings("unchecked")
        List<String> procurementType = (List<String>) props.get(DocumentSearchModel.Props.PROCUREMENT_TYPE);
        queryParts.add(generateMultiStringExactQuery(procurementType, DocumentSpecificModel.Props.PROCUREMENT_TYPE));

        // Quick search
        String quickSearchInput = (String) props.get(DocumentSearchModel.Props.INPUT);
        if (StringUtils.isNotBlank(quickSearchInput)) {
            Pair<List<String>, List<Date>> quickSearchWordsAndDates = parseQuickSearchWordsAndDates(quickSearchInput);
            log.info("Quick search (document) - words: " + quickSearchWordsAndDates.getFirst().toString() + ", dates: " + quickSearchWordsAndDates.getSecond() + ", from string '" + quickSearchInput + "'");
            queryParts.addAll(generateQuickSearchDocumentQuery(quickSearchWordsAndDates.getFirst(), quickSearchWordsAndDates.getSecond()));
        }

        String query = generateDocumentSearchQuery(queryParts);
        if (log.isDebugEnabled()) {
            log.debug("Documents search query construction time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return query;
    }

    private String generateQuickSearchQuery(String searchString) {
        long startTime = System.currentTimeMillis();
        Pair<List<String>, List<Date>> quickSearchWordsAndDates = parseQuickSearchWordsAndDates(searchString);
        log.info("Quick search - words: " + quickSearchWordsAndDates.getFirst().toString() + ", dates: " + quickSearchWordsAndDates.getSecond() + ", from string '" + searchString + "'");
        String query = generateDocumentSearchQuery(generateQuickSearchDocumentQuery(quickSearchWordsAndDates.getFirst(), quickSearchWordsAndDates.getSecond()));
        if (log.isDebugEnabled()) {
            log.debug("Quick search query construction time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return query;
    }

    private List<String> generateQuickSearchDocumentQuery(List<String> searchWords, List<Date> searchDates) {
        // Fetch a list of all the properties from document type and it's subtypes.
        List<QName> searchProperties = new ArrayList<QName>(50);
        List<QName> searchPropertiesDate = new ArrayList<QName>();
        addDocumentProperties(searchProperties, searchPropertiesDate);
        return generateQuery(searchWords, searchDates, searchProperties, searchPropertiesDate);
    }

    private List<String> generateQuery(List<String> searchWords, List<Date> searchDates, List<QName> searchProperties, List<QName> searchPropertiesDate) {
        /*
         * Construct a query with following structure:
         * ASPECT:searchable AND (
         * (TYPE:document AND (@prop1:"*word1*" OR @prop2:"*word1*") AND (@prop1:"*word2*" OR @prop2:"*word2*")) OR
         * (ASPECT:file AND (@name:"*word1*" OR @content:"*word1*") AND (@name:"*word2*" OR @content:"*word2*"))
         * )
         * Note: Property values must be wrapped with " symbols. Alfresco LuceneQueryParser somehow produces different
         * results (something to do with multi-language fields, et locale and ALL locales). It doesn't replace non latin-1
         * characters with latin-1 characters if the values are without wrapping " symbols and this breaks searches with estonian
         * special characters because Alfresco indexed them with ISOLatin1AccentFilter.
         */

        List<String> queryParts = new ArrayList<String>();
        for (String searchWord : searchWords) {
            if (StringUtils.isNotBlank(searchWord)) {
                List<String> propQueries = new ArrayList<String>(searchProperties.size());

                for (QName property : searchProperties) {
                    propQueries.add(generatePropertyWildcardQuery(property, searchWord, false));
                }
                queryParts.add(joinQueryPartsOr(propQueries, false));
            }
        }
        for (Date searchDate: searchDates) {
            if (searchDate != null) {
                List<String> propQueries = new ArrayList<String>(searchPropertiesDate.size());
                for (QName property : searchPropertiesDate) {
                    propQueries.add(generatePropertyDateQuery(property, searchDate));
                }
                queryParts.add(joinQueryPartsOr(propQueries, false));
            }
        }
        return queryParts;
    }

    private void addDocumentProperties(List<QName> searchProperties, List<QName> searchPropertiesDate) {
        Collection<QName> docProperties = dictionaryService.getProperties(DocumentCommonModel.MODEL);
        docProperties.addAll(dictionaryService.getProperties(DocumentSpecificModel.MODEL));
        for (QName property : docProperties) {
            PropertyDefinition propDef = dictionaryService.getProperty(property);
            QName type = propDef.getDataType().getName();
            if (isStringProperty(type)) {
                searchProperties.add(property);
            } else if (isDateProperty(type)) {
                searchPropertiesDate.add(property);
            }
        }
    }

    private static boolean isStringProperty(QName dataType) {
        return dataType.equals(DataTypeDefinition.TEXT) || dataType.equals(DataTypeDefinition.INT) || dataType.equals(DataTypeDefinition.LONG) ||
                dataType.equals(DataTypeDefinition.FLOAT) || dataType.equals(DataTypeDefinition.DOUBLE) || dataType.equals(DataTypeDefinition.CONTENT);
    }

    private static boolean isDateProperty(QName dataType) {
        return dataType.equals(DataTypeDefinition.DATE) || dataType.equals(DataTypeDefinition.DATETIME);
    }

    private static String generateSeriesSearchQuery(List<String> queryParts) {
        queryParts.add(0, generateTypeQuery(SeriesModel.Types.SERIES));
        return joinQueryPartsAnd(queryParts);
    }

    private static String generateDocumentSearchQuery(List<String> queryParts) {
        if (isBlank(queryParts)) {
            return null;
        }
        queryParts.add(0, generateTypeQuery(DocumentCommonModel.Types.DOCUMENT));
        queryParts.add(1, generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE));
        return joinQueryPartsAnd(queryParts);
    }

    private static String generateVolumeSearchQuery(List<String> queryParts) {
        if (isBlank(queryParts)) {
            return null;
        }
        queryParts.add(0, generateTypeQuery(VolumeModel.Types.VOLUME));
        return joinQueryPartsAnd(queryParts);
    }

    private static String generateTaskSearchQuery(List<String> queryParts) {
        if (isBlank(queryParts)) {
            return null;
        }
        return joinQueryPartsAnd(queryParts);
    }

    @SuppressWarnings("unchecked")
    private String generateTaskSearchQuery(Node filter) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(20);
        Map<String, Object> props = filter.getProperties();

        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(TaskSearchModel.Props.STARTED_DATE_TIME_BEGIN), //
                (Date) props.get(TaskSearchModel.Props.STARTED_DATE_TIME_END), WorkflowCommonModel.Props.STARTED_DATE_TIME));
        queryParts.add(generateTypeQuery((List<QName>) props.get(TaskSearchModel.Props.TASK_TYPE)));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(TaskSearchModel.Props.OWNER_NAME), WorkflowCommonModel.Props.OWNER_NAME));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(TaskSearchModel.Props.CREATOR_NAME), WorkflowCommonModel.Props.CREATOR_NAME));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(TaskSearchModel.Props.ORGANIZATION_NAME),
                WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(TaskSearchModel.Props.JOB_TITLE), WorkflowCommonModel.Props.OWNER_JOB_TITLE));
        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(TaskSearchModel.Props.DUE_DATE_TIME_BEGIN), //
                (Date) props.get(TaskSearchModel.Props.DUE_DATE_TIME_END), WorkflowSpecificModel.Props.DUE_DATE));
        if (Boolean.TRUE.equals(props.get(TaskSearchModel.Props.ONLY_RESPONSIBLE))) {
            queryParts.add(generateAspectQuery(WorkflowSpecificModel.Aspects.RESPONSIBLE));
        }
        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(TaskSearchModel.Props.COMPLETED_DATE_TIME_BEGIN), //
                (Date) props.get(TaskSearchModel.Props.COMPLETED_DATE_TIME_END), WorkflowCommonModel.Props.COMPLETED_DATE_TIME));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(TaskSearchModel.Props.COMMENT), WorkflowCommonModel.Props.OUTCOME,
                WorkflowSpecificModel.Props.COMMENT));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(TaskSearchModel.Props.RESOLUTION), WorkflowSpecificModel.Props.RESOLUTION,
                WorkflowSpecificModel.Props.WORKFLOW_RESOLUTION));
        queryParts.add(generateMultiStringExactQuery((List<String>) props.get(TaskSearchModel.Props.STATUS), WorkflowCommonModel.Props.STATUS));
        if (Boolean.TRUE.equals(props.get(TaskSearchModel.Props.COMPLETED_OVERDUE))) {
            queryParts.add(generatePropertyBooleanQuery(WorkflowSpecificModel.Props.COMPLETED_OVERDUE, true));
        }
        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(TaskSearchModel.Props.STOPPED_DATE_TIME_BEGIN), //
                (Date) props.get(TaskSearchModel.Props.STOPPED_DATE_TIME_END), WorkflowCommonModel.Props.STOPPED_DATE_TIME));

        if (isBlank(queryParts)) {
            return null;
        }
        queryParts.add(0, generateTypeQuery(WorkflowCommonModel.Types.TASK));
        queryParts.add(1, generateAspectQuery(WorkflowSpecificModel.Aspects.SEARCHABLE));

        String query = joinQueryPartsAnd(queryParts);
        if (log.isDebugEnabled()) {
            log.debug("Tasks search query construction time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return query;
    }

    private interface SearchCallback<E> {
        E addResult(ResultSetRow row);
    }

    private List<Task> searchTasksImpl(String query, boolean limited, String queryName) {
        return searchGeneralImpl(query, limited, queryName, new SearchCallback<Task>() {

            @Override
            public Task addResult(ResultSetRow row) {
                return workflowService.getTask(row.getNodeRef(), true);
            }
        }, getAllStores());
    }

    private List<TaskInfo> searchTaskInfosImpl(String query, boolean limited, String queryName) {
        return searchGeneralImpl(query, limited, queryName, new SearchCallback<TaskInfo>() {
            @Override
            public TaskInfo addResult(ResultSetRow row) {
                // If we start having performance problems then maybe the following optimizations will help some:
                // 1) Load aspects only for task node, other nodes don't need aspects in task search results
                // 2) Load only task nodes and create TaskInfos with NULL values for workflow and document node
                // and after all the tasks are fetched then fetch workflow and document objects only one per
                // unique workflow and document. If there are many tasks linked to same workflow or same document
                // then we can avoid some lookup roundtrips.
                Node task = generalService.fetchNode(row.getNodeRef());
                Node workflow = generalService.fetchNode(nodeService.getPrimaryParent(task.getNodeRef()).getParentRef());
                NodeRef compoundWorkflowRef = nodeService.getPrimaryParent(workflow.getNodeRef()).getParentRef();
                Node document = generalService.fetchNode(nodeService.getPrimaryParent(compoundWorkflowRef).getParentRef());
                return new TaskInfo(task, workflow, document);
            }
        });
    }

    private List<Document> searchDocumentsImpl(String query, boolean limited, String queryName) {
        return searchDocumentsImpl(query, limited, queryName, null);
    }

    private List<Document> searchDocumentsImpl(String query, boolean limited, String queryName, Collection<StoreRef> storeRefs) {
        return searchGeneralImpl(query, limited, queryName, new SearchCallback<Document>() {

            @Override
            public Document addResult(ResultSetRow row) {
                return documentService.getDocumentByNodeRef(row.getNodeRef());
            }
        }, storeRefs);
    }

    private List<Document> searchDocumentsAndCaseTitlesImpl(String query, boolean limited, String queryName) {
        return searchGeneralImpl(query, limited, queryName, new SearchCallback<Document>() {

            @Override
            public Document addResult(ResultSetRow row) {
                final NodeRef nodeRef = row.getNodeRef();
                final QName resultType = nodeService.getType(nodeRef);
                if (!dictionaryService.isSubClass(resultType, DocumentCommonModel.Types.DOCUMENT)) {
                    final FakeDocument fakeDocument = new FakeDocument(nodeRef);
                    if(log.isDebugEnabled()) {
                        log.debug("fakeDocument="+fakeDocument);
                    }
                    return fakeDocument;
                }
                return documentService.getDocumentByNodeRef(nodeRef);
            }
        });
    }

    private List<Volume> searchVolumesImpl(String query, boolean limited, String queryName) {
        return searchGeneralImpl(query, limited, queryName, new SearchCallback<Volume>() {
            @Override
            public Volume addResult(ResultSetRow row) {
                return volumeService.getVolumeByNodeRef(row.getNodeRef());
            }
        });
    }

    private List<Series> searchSeriesImpl(String query, boolean limited, String queryName) {
        return searchGeneralImpl(query, limited, queryName, new SearchCallback<Series>() {
            @Override
            public Series addResult(ResultSetRow row) {
                return seriesService.getSeriesByNodeRef(row.getNodeRef());
            }
        });
    }

    private List<QName> searchAdrDocumentTypesImpl(String query, boolean limited, String queryName) {
        return searchGeneralImpl(query, limited, queryName, new SearchCallback<QName>() {
            @Override
            public QName addResult(ResultSetRow row) {
                return (QName) nodeService.getProperty(row.getNodeRef(), AdrModel.Props.DOCUMENT_TYPE);
            }
        });
    }

    private List<NodeRef> searchNodes(String query, boolean limited, String queryName) {
        ResultSet resultSet = doSearch(query, limited, queryName);
        try {
            return resultSet.getNodeRefs();
        } finally {
            try {
                resultSet.close();
            } catch (Exception e) {
                // Do nothing
            }
        }
    }
    
    private List<NodeRef> searchNodesFromAllStores(String query, boolean limited, String queryName) {
        List<ResultSet> resultSets = doSearches(query, limited, queryName, getAllStores());
        try {
            List<NodeRef> nodeRefs = new ArrayList<NodeRef>();
            for(ResultSet resultSet : resultSets){
                nodeRefs.addAll(resultSet.getNodeRefs());
            }
            return nodeRefs;
        } finally {
            try {
                for(ResultSet resultSet : resultSets){                
                    resultSet.close();
                }
            } catch (Exception e) {
                // Do nothing
            }
        }
    }    

    private <E extends Comparable<? super E>> List<E> searchGeneralImpl(String query, boolean limited, String queryName, SearchCallback<E> callback) {
        return searchGeneralImpl(query, limited, queryName, callback, null);
    }

    private <E extends Comparable<? super E>> List<E> searchGeneralImpl( //
            String query, boolean limited, String queryName, SearchCallback<E> callback, Collection<StoreRef> storeRefs) {
        if (StringUtils.isBlank(query)) {
            return Collections.emptyList();
        }
        StoreRef singleStoreRef = null;
        if (storeRefs == null) {
            singleStoreRef = generalService.getStore();
        } else if (storeRefs.size() == 1) {
            singleStoreRef = storeRefs.iterator().next();
        }
        long startTime = System.currentTimeMillis();
        final List<ResultSet> resultSets;
        if (singleStoreRef != null) {
            resultSets = Arrays.asList(doSearch(query, limited, queryName, singleStoreRef));
        } else {
            resultSets = doSearches(query, limited, queryName, storeRefs);
        }
        final List<E> extractResults = new ArrayList<E>();
        for (ResultSet resultSet : resultSets) {
            extractResults.addAll(extractResults(callback, startTime, resultSet));
        }
        Collections.sort(extractResults);
        return extractResults;
    }

    private <E> List<E> extractResults(SearchCallback<E> callback, long startTime, final ResultSet resultSet) {
        try {
            List<E> result = new ArrayList<E>(resultSet.length());
            if (log.isDebugEnabled()) {
                log.debug("Lucene search time " + (System.currentTimeMillis() - startTime) + " ms, results: " + resultSet.length());
                startTime = System.currentTimeMillis();
            }

            for (ResultSetRow row : resultSet) {
                E item = callback.addResult(row);
                if (item != null) {
                    result.add(item);
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("Results construction time " + (System.currentTimeMillis() - startTime) + " ms, final results: " + result.size());
            }
            return result;
        } finally {
            try {
                resultSet.close();
            } catch (Exception e) {
                // Do nothing
            }
        }
    }

    
    private List<StoreRef> getAllStores(){
        if(allStores == null){
            allStores = new ArrayList<StoreRef>(2);
            allStores.add(generalService.getStore());
            allStores.add(generalService.getArchivalsStoreRef());
        }
        return allStores;
    }
    /**
     * Sets up search parameters and queries
     * 
     * @param query
     * @param limited if true, only 100 first results are returned
     * @return query resultset
     */
    private ResultSet doSearch(String query, boolean limited, String queryName) {
        return doSearch(query, limited, queryName, null);
    }

    private ResultSet doSearch(String query, boolean limited, String queryName, StoreRef storeRef) {
        SearchParameters sp = buildSearchParameters(query, limited);
        sp.addStore(storeRef == null ? generalService.getStore() : storeRef);
        return doSearchQuery(sp, queryName);
    }

    private List<ResultSet> doSearches(String query, boolean limited, String queryName, Collection<StoreRef> storeRefs) {
        SearchParameters sp = buildSearchParameters(query, limited);
        if (storeRefs == null || storeRefs.size() == 0) {
            storeRefs = Arrays.asList(generalService.getStore());
        }
        final List<ResultSet> results = new ArrayList<ResultSet>(storeRefs.size());
        for (StoreRef storeRef : storeRefs) {
            sp.getStores().clear();
            sp.addStore(storeRef);
            results.add(doSearchQuery(sp, queryName));
        }
        return results;
    }

    private ResultSet doSearchQuery(SearchParameters sp, String queryName) {
        long startTime = System.currentTimeMillis();
        try {
            ResultSet resultSet = searchService.query(sp);

            if (log.isInfoEnabled()) {
                long duration = System.currentTimeMillis() - startTime;
                log.info("PERFORMANCE: query " + queryName + " - " + duration + " ms");
            }
            return resultSet;
        } catch (BooleanQuery.TooManyClauses e) {
            log.error("Search failed with TooManyClauses exception, query expanded over limit\n    queryName=" + queryName + "\n    store=" + sp.getStores()
                    + "\n    limit=" + sp.getLimit() + "\n    limitBy=" + sp.getLimitBy().toString() + "\n    exceptionMessage=" + e.getMessage());
            throw e;
        }
    }

    private SearchParameters buildSearchParameters(String query, boolean limited) {
        // build up the search parameters
        SearchParameters sp = new SearchParameters();
        sp.setLanguage(SearchService.LANGUAGE_LUCENE);
        sp.setQuery(query);
        if (limited) {
            sp.setLimit(100);
            sp.setLimitBy(LimitBy.FINAL_SIZE);
        } else {
            sp.setLimitBy(LimitBy.UNLIMITED);
        }
        return sp;
    }

    // START: getters / setters

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setSeriesService(SeriesService seriesService) {
        this.seriesService = seriesService;
    }

    public void setVolumeService(VolumeService volumeService) {
        this.volumeService = volumeService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }
    
    public void setUserService(UserService userService) {
        this.userService = userService;
    }  
    
    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }     

    // END: getters / setters

}

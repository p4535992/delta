package ee.webmedia.alfresco.document.search.service;

import static ee.webmedia.alfresco.common.search.DbSearchUtil.generateTaskDatePropertyRangeQuery;
import static ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType.INCOMING_LETTER;
import static ee.webmedia.alfresco.utils.SearchUtil.generateAndNotQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateAspectQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateDatePropertyRangeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateLuceneSearchParams;
import static ee.webmedia.alfresco.utils.SearchUtil.generateMultiNodeRefQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateMultiStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateNodeRefQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateNumberPropertyRangeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateParentQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyBooleanQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyDateQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyNotNullQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyNullQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyWildcardQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringNotEmptyQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringNullQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;
import static ee.webmedia.alfresco.utils.TextUtil.isBlank;
import static ee.webmedia.alfresco.workflow.model.Status.IN_PROGRESS;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isStatus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
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
import org.springframework.util.Assert;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.adr.model.AdrModel;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.classificator.enums.PublishToAdr;
import ee.webmedia.alfresco.classificator.enums.SendMode;
import ee.webmedia.alfresco.common.search.DbSearchUtil;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.docconfig.generator.fieldtype.DateGenerator;
import ee.webmedia.alfresco.docconfig.generator.fieldtype.DoubleGenerator;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.document.search.model.FakeDocument;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.type.service.DocumentTypeHelper;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.search.service.AbstractSearchServiceImpl;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.substitute.model.SubstitutionInfo;
import ee.webmedia.alfresco.substitute.web.SubstitutionBean;
import ee.webmedia.alfresco.user.model.Authority;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.search.model.TaskInfo;
import ee.webmedia.alfresco.workflow.search.model.TaskSearchModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.SendStatus;

/**
 * @author Alar Kvell
 * @author Erko Hansar
 */
public class DocumentSearchServiceImpl extends AbstractSearchServiceImpl implements DocumentSearchService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentSearchServiceImpl.class);

    private DocumentService documentService;
    private DocumentDynamicService _documentDynamicService;
    private SeriesService seriesService;
    private VolumeService volumeService;
    private WorkflowService workflowService;
    private NamespaceService namespaceService;
    private AuthorityService authorityService;
    private UserService userService;
    private LogService logService;

    private List<StoreRef> allStores = null;
    private List<StoreRef> allStoresWithArchivalStoreVOs = null; // XXX This is currently used only for tasks. If analysis for CL 186867 is complete then this might be refactored
                                                                 // to getAllStores()

    @Override
    public List<Document> searchDueContracts() {
        long startTime = System.currentTimeMillis();
        int contractDueDays = parametersService.getLongParameter(Parameters.CONTRACT_DUE_DATE_NOTIFICATION_DAYS).intValue();
        Calendar cal = Calendar.getInstance();
        Date now = cal.getTime();
        cal.add(Calendar.DATE, contractDueDays);
        Date dueDateLimit = cal.getTime();

        String query = generateDocumentSearchQuery(
                generateStringExactQuery(SystematicDocumentType.CONTRACT.getId(), DocumentAdminModel.Props.OBJECT_TYPE_ID)
                , joinQueryPartsOr(
                        generatePropertyNullQuery(DocumentSpecificModel.Props.DUE_DATE)
                        , generateDatePropertyRangeQuery(now, dueDateLimit, DocumentSpecificModel.Props.DUE_DATE)
                )
                );

        List<Document> contracts = searchDocumentsImpl(query, -1, /* queryName */"contractDueDate", getAllStoresWithArchivalStoreVOs()).getFirst();

        if (log.isDebugEnabled()) {
            log.debug("Search for contracts with due date took " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return contracts;
    }

    @Override
    public List<Document> searchDiscussionDocuments() {
        long startTime = System.currentTimeMillis();

        String query = generateDiscussionDocumentsQuery();
        List<Document> results = searchDocumentsImpl(query, -1, /* queryName */"discussionDocuments").getFirst();

        if (log.isDebugEnabled()) {
            log.debug("Discussion documents search total time " + (System.currentTimeMillis() - startTime) + " ms, results " + results.size() //
                    + ", query: " + query);
        }
        return results;
    }

    @Override
    public int getDiscussionDocumentsCount() {
        long startTime = System.currentTimeMillis();

        String query = generateDiscussionDocumentsQuery();
        ResultSet results = doSearch(query, -1, /* queryName */"discussionDocumentsCount", null);
        int count = countResults(Collections.singletonList(results));

        if (log.isDebugEnabled()) {
            log.debug("Discussion documents count search total time " + (System.currentTimeMillis() - startTime) + " ms, results " + count//
                    + ", query: " + query);
        }
        return count;
    }

    private String generateDiscussionDocumentsQuery() {
        String runAsUser = AuthenticationUtil.getRunAsUser();
        Set<String> authorities = new HashSet<String>(Arrays.asList(runAsUser));
        authorities.addAll(userService.getUsersGroups(runAsUser));

        return SearchUtil.joinQueryPartsAnd(generateAspectQuery(DocumentCommonModel.Aspects.FORUM_PARTICIPANTS)
                , generateStringExactQuery(DocumentStatus.WORKING.getValueName(), DocumentCommonModel.Props.DOC_STATUS)
                , SearchUtil.generatePropertyExactQuery(DocumentCommonModel.Props.FORUM_PARTICIPANTS, authorities, false));
    }

    @Override
    public List<NodeRef> searchAdrDocuments(Date modifiedDateBegin, Date modifiedDateEnd, Set<String> documentTypeIds) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(3);
        if (modifiedDateBegin != null && modifiedDateEnd != null) {
            queryParts.add(generateDatePropertyRangeQuery(modifiedDateBegin, modifiedDateEnd, ContentModel.PROP_MODIFIED));
        }

        String query = generateAdrDocumentSearchQuery(queryParts, documentTypeIds);
        // Only search from SpacesStore and ArchivalsStore to get correct document set (PPA).
        List<NodeRef> results = searchNodes(query, -1, /* queryName */"adrDocumentByModified1");
        results.addAll(searchNodes(query, -1, /* queryName */"adrDocumentByModified2", generalService.getArchivalsStoreRefs()).getFirst());
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
        List<NodeRef> results = searchNodes(query, -1, /* queryName */"adrDeletedDocuments");
        if (log.isDebugEnabled()) {
            log.debug("ADR deleted documents search total time " + (System.currentTimeMillis() - startTime) + " ms, results " + results.size() //
                    + ", query: " + query);
        }
        return results;
    }

    @Override
    public List<Document> searchInvoicesWithEmptySapAccount() {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(3);
        queryParts.add(generateTypeQuery(DocumentSubtypeModel.Types.INVOICE)); // TODO use generateDocumentSearchQuery + objectTypeId=invoice
        queryParts.add(generatePropertyNullQuery(DocumentSpecificModel.Props.SELLER_PARTY_SAP_ACCOUNT));
        queryParts.add(generatePropertyNotNullQuery(DocumentSpecificModel.Props.SELLER_PARTY_REG_NUMBER));

        String query = joinQueryPartsAnd(queryParts);
        List<Document> results = searchDocumentsImpl(query, -1, /* queryName */"searchInvoicesWithEmptySapAccount", getAllStoresWithArchivalStoreVOs()).getFirst();
        if (log.isDebugEnabled()) {
            log.debug("Invoices with empty sap account search total time " + (System.currentTimeMillis() - startTime) + " ms, results " + results.size() //
                    + ", query: " + query);
        }
        return results;
    }

    @Override
    public List<String> searchAdrDeletedDocumentTypes(Date deletedDateBegin, Date deletedDateEnd) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(2);
        queryParts.add(generateTypeQuery(AdrModel.Types.ADR_DELETED_DOCUMENT_TYPE));
        queryParts.add(generateDatePropertyRangeQuery(deletedDateBegin, deletedDateEnd, AdrModel.Props.DELETED_DATE_TIME));

        String query = joinQueryPartsAnd(queryParts);
        List<String> results = searchAdrDocumentTypesImpl(query, -1, /* queryName */"adrDeletedDocumentTypes");
        if (log.isDebugEnabled()) {
            log.debug("ADR deleted document types search total time " + (System.currentTimeMillis() - startTime) + " ms, results " + results.size()
                    + ", query: " + query);
        }
        return results;
    }

    @Override
    public List<String> searchAdrAddedDocumentTypes(Date addedDateBegin, Date addedDateEnd) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(2);
        queryParts.add(generateTypeQuery(AdrModel.Types.ADR_ADDED_DOCUMENT_TYPE));
        queryParts.add(generateDatePropertyRangeQuery(addedDateBegin, addedDateEnd, AdrModel.Props.DELETED_DATE_TIME));

        String query = joinQueryPartsAnd(queryParts);
        List<String> results = searchAdrDocumentTypesImpl(query, -1, /* queryName */"adrAddedDocumentTypes");
        if (log.isDebugEnabled()) {
            log.debug("ADR added document types search total time " + (System.currentTimeMillis() - startTime) + " ms, results " + results.size() //
                    + ", query: " + query);
        }
        return results;
    }

    private static String generateAdrDocumentSearchQuery(List<String> queryParts, Set<String> documentTypeIds) {
        if (documentTypeIds.size() == 0) {
            return null;
        }
        queryParts.add(generateMultiStringExactQuery(new ArrayList<String>(documentTypeIds), DocumentAdminModel.Props.OBJECT_TYPE_ID));

        // If document is of type incomingLetter, then it must be registered. All other documents must be finished
        queryParts.add(joinQueryPartsOr(Arrays.asList(
                generateAndNotQuery(generateStringExactQuery(DocumentStatus.FINISHED.getValueName(), DocumentCommonModel.Props.DOC_STATUS),
                        generateStringExactQuery(INCOMING_LETTER.getId(), DocumentAdminModel.Props.OBJECT_TYPE_ID))

                , joinQueryPartsAnd(Arrays.asList(
                        generateStringExactQuery(INCOMING_LETTER.getId(), DocumentAdminModel.Props.OBJECT_TYPE_ID),
                        generatePropertyNotNullQuery(DocumentCommonModel.Props.REG_NUMBER)
                        ))
                )));

        // Possible access restrictions
        queryParts.add(joinQueryPartsAnd(
                joinQueryPartsOr(
                        generateStringExactQuery(AccessRestriction.AK.getValueName(), DocumentCommonModel.Props.ACCESS_RESTRICTION)
                        , generateStringExactQuery(AccessRestriction.OPEN.getValueName(), DocumentCommonModel.Props.ACCESS_RESTRICTION))
                , joinQueryPartsOr(
                        generatePropertyNullQuery(DocumentDynamicModel.Props.PUBLISH_TO_ADR)
                        , generateStringExactQuery(PublishToAdr.TO_ADR.getValueName(), DocumentDynamicModel.Props.PUBLISH_TO_ADR)
                        , generateStringExactQuery(PublishToAdr.REQUEST_FOR_INFORMATION.getValueName(), DocumentDynamicModel.Props.PUBLISH_TO_ADR)
                )
                )
                );

        return generateDocumentSearchQuery(queryParts);
    }

    @Override
    public List<Document> searchIncomingLetterRegisteredDocuments(String senderRegNumber) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(4);
        queryParts.add(generateStringExactQuery(SystematicDocumentType.INCOMING_LETTER.getId(), DocumentAdminModel.Props.OBJECT_TYPE_ID));
        queryParts.add(generateStringNotEmptyQuery(DocumentCommonModel.Props.REG_DATE_TIME));
        queryParts.add(generateStringExactQuery(senderRegNumber, DocumentSpecificModel.Props.SENDER_REG_NUMBER));

        String query = generateDocumentSearchQuery(queryParts);
        List<Document> results = searchDocumentsImpl(query, -1, /* queryName */"incomingLetterRegisteredDocuments").getFirst();
        if (log.isDebugEnabled()) {
            log.debug("Registered incoming letter documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public Pair<List<Document>, Boolean> searchTodayRegisteredDocuments(String searchString, int limit) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>();
        String query;
        queryParts.add(generatePropertyDateQuery(DocumentCommonModel.Props.REG_DATE_TIME, new Date()));
        boolean quickSearchQuery = StringUtils.isNotBlank(searchString);
        if (quickSearchQuery) {
            queryParts.add(0, generateQuickSearchQuery(searchString));
            query = SearchUtil.joinQueryPartsAnd(queryParts, false);
        } else {
            query = generateDocumentSearchQuery(queryParts);
        }
        Pair<List<Document>, Boolean> results = searchDocumentsImpl(query, limit, /* queryName */"todayRegisteredDocuments");
        if (log.isDebugEnabled()) {
            log.debug("Today registered documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public List<Document> searchInProcessUserDocuments() {
        long startTime = System.currentTimeMillis();
        String query = getInProcessDocumentsOwnerQuery(AuthenticationUtil.getRunAsUser());
        List<Document> results = searchDocumentsImpl(query, -1, /* queryName */"inProcessUserDocuments", getAllStores()).getFirst();

        // Set workflow status for documents
        for (Document document : results) {
            Map<Workflow, List<String>> workflows = new HashMap<Workflow, List<String>>();
            for (CompoundWorkflow compoundWorkflow : workflowService.getCompoundWorkflows(document.getNodeRef())) {
                if (!isStatus(compoundWorkflow, IN_PROGRESS)) {
                    continue;
                }
                for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                    if (!isStatus(workflow, IN_PROGRESS)) {
                        continue;
                    }
                    List<String> taskOwners = new ArrayList<String>();
                    for (Task task : workflow.getTasks()) {
                        if (!isStatus(task, IN_PROGRESS)) {
                            continue;
                        }
                        taskOwners.add(task.getOwnerName());
                    }

                    if (!taskOwners.isEmpty()) {
                        workflows.put(workflow, taskOwners);
                    }
                }
            }
            StringBuilder status = new StringBuilder();
            for (Entry<Workflow, List<String>> entry : workflows.entrySet()) {
                if (status.length() > 0) {
                    status.append("; ");
                }
                status.append(MessageUtil.getMessage(entry.getKey().getType().getLocalName()))
                        .append(" (")
                        .append(StringUtils.join(entry.getValue(), ", "))
                        .append(")");
            }
            document.setWorkflowStatus(status.toString());
        }
        if (log.isDebugEnabled()) {
            log.debug("Current user's in process documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public int searchInProcessUserDocumentsCount() {
        long startTime = System.currentTimeMillis();
        String query = getInProcessDocumentsOwnerQuery(AuthenticationUtil.getRunAsUser());
        List<ResultSet> resultSets = doSearches(query, -1, "inProcessUserDocumentsCount", getAllStores());
        if (log.isDebugEnabled()) {
            log.debug("Current user's and WORKING documents count search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return countResults(resultSets);
    }

    @Override
    public List<Document> searchAccessRestictionEndsAfterDate(Date restrictionEndDate) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>();
        List<String> tempQueryParts = new ArrayList<String>();

        tempQueryParts.add(generateStringExactQuery(AccessRestriction.AK.getValueName(), DocumentCommonModel.Props.ACCESS_RESTRICTION));
        tempQueryParts.add(generatePropertyNullQuery(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE));
        tempQueryParts.add(generateStringNotEmptyQuery(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC));
        queryParts.add(joinQueryPartsAnd(tempQueryParts));
        tempQueryParts.clear();

        tempQueryParts.add(generateStringExactQuery(AccessRestriction.AK.getValueName(), DocumentCommonModel.Props.ACCESS_RESTRICTION));
        tempQueryParts.add(generateDatePropertyRangeQuery(null, restrictionEndDate, DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE));
        queryParts.add(joinQueryPartsAnd(tempQueryParts));
        tempQueryParts.clear();

        tempQueryParts.addAll(queryParts);
        queryParts.clear();
        queryParts.add(joinQueryPartsOr(tempQueryParts));

        String query = generateDocumentSearchQuery(queryParts);
        List<Document> results = searchDocumentsImpl(query, -1, /* queryName */"accessRestictionEndsAfterDate", getAllStoresWithArchivalStoreVOs()).getFirst();
        if (log.isDebugEnabled()) {
            log.debug("Search for documents with access restriction took " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public List<Document> searchRecipientFinishedDocuments() {
        long startTime = System.currentTimeMillis();
        String query = generateRecipientFinichedQuery();
        List<Document> results = searchDocumentsImpl(query, -1, /* queryName */"recipientFinishedDocuments").getFirst();

        if (log.isDebugEnabled()) {
            log.debug("FINISHED documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public int searchRecipientFinishedDocumentsCount() {
        long startTime = System.currentTimeMillis();
        String query = generateRecipientFinichedQuery();
        List<NodeRef> results = searchNodes(query, -1, /* queryName */"recipientFinishedDocumentsCount");

        if (log.isDebugEnabled()) {
            log.debug("FINISHED documents count search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results.size();
    }

    private String generateRecipientFinichedQuery() {
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateStringNotEmptyQuery(DocumentCommonModel.Props.RECIPIENT_NAME, DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME,
                DocumentSpecificModel.Props.PARTY_NAME /* on document node, duplicates partyName property values from all contractParty child-nodes */
                ));
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
        List<Series> results = searchSeriesImpl(query, -1, /* queryName */"seriesUnit");

        if (log.isDebugEnabled()) {
            log.debug("FINISHED documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public Series searchSeriesByIdentifier(String identifier) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateStringExactQuery(identifier, SeriesModel.Props.SERIES_IDENTIFIER));
        String query = generateSeriesSearchQuery(queryParts);
        List<Series> results = searchSeriesImpl(query, -1, /* queryName */"seriesByIdentifier");

        if (log.isDebugEnabled()) {
            log.debug("FINISHED series search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        if (results.size() > 0) {
            return results.get(0);
        }
        return null;
    }

    @Override
    public List<Task> searchCurrentUsersTasksInProgress(QName taskType) {
        long startTime = System.currentTimeMillis();
        Pair<List<String>, List<Object>> queryPartsAndArgs = getTaskQuery(taskType, AuthenticationUtil.getRunAsUser(), Status.IN_PROGRESS, false);
        addSubstitutionRestriction(queryPartsAndArgs);
        String query = generateTaskSearchQuery(queryPartsAndArgs.getFirst());
        Pair<List<Task>, Boolean> results = BeanHelper.getWorkflowDbService().searchTasksMainStore(query, queryPartsAndArgs.getSecond(), -1);
        if (log.isDebugEnabled()) {
            log.debug("Current user's and IN_PROGRESS tasks search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results.getFirst();
    }

    @Override
    public Map<NodeRef, Pair<String, String>> searchTaskBySendStatusQuery(QName taskType) {
        List<String> queryParts = new ArrayList<String>();
        List<Object> arguments = new ArrayList<Object>();
        addTaskTypeFieldExactQueryPartsAndArguments(queryParts, arguments, taskType);
        addTaskStringExactPartsAndArgs(queryParts, arguments, SendStatus.SENT.toString(), WorkflowSpecificModel.Props.SEND_STATUS);
        queryParts.add(DbSearchUtil.generateTaskPropertyNotNullQuery(WorkflowSpecificModel.Props.SENT_DVK_ID));
        String query = generateTaskSearchQuery(queryParts);
        return BeanHelper.getWorkflowDbService().searchTaskSendStatusInfo(query, arguments);
    }

    @Override
    public List<Task> searchTasksByOriginalDvkIdsQuery(Iterable<String> originalDvkIds) {
        if (!originalDvkIds.iterator().hasNext()) {
            return new ArrayList<Task>();
        }
        List<String> queryParts = new ArrayList<String>();
        List<Object> arguments = new ArrayList<Object>();
        List<String> queryDvkIdParts = new ArrayList<String>();
        for (String dvkId : originalDvkIds) {
            addTaskStringExactPartsAndArgs(queryDvkIdParts, arguments, dvkId, WorkflowSpecificModel.Props.ORIGINAL_DVK_ID);
        }
        queryParts.add(joinQueryPartsOr(queryDvkIdParts, true));
        String query = generateTaskSearchQuery(queryParts);
        return BeanHelper.getWorkflowDbService().searchTasksAllStores(query, arguments, -1).getFirst();
    }

    /**
     * Only used at importing external review task,
     * so if many tasks are found the one belonging to document with notEditable aspect is preferred
     */
    @Override
    public Task searchTaskByOriginalDvkIdQuery(String originalDvkId) {
        List<String> queryParts = new ArrayList<String>();
        List<Object> arguments = new ArrayList<Object>();
        addTaskStringExactPartsAndArgs(queryParts, arguments, originalDvkId, WorkflowSpecificModel.Props.ORIGINAL_DVK_ID);
        String query = generateTaskSearchQuery(queryParts);
        List<Task> tasks = BeanHelper.getWorkflowDbService().searchTasksAllStores(query, arguments, -1).getFirst();
        if (tasks.size() == 1) {
            return tasks.get(0);
        }
        for (Task task : tasks) {
            NodeRef compoundWorkflowRef = nodeService.getPrimaryParent(task.getParentNodeRef()).getParentRef();
            NodeRef docRef = workflowService.getCompoundWorkflow(compoundWorkflowRef).getParent();
            if (nodeService.hasAspect(docRef, DocumentSpecificModel.Aspects.NOT_EDITABLE)) {
                return task;
            }
        }
        return null;
    }

    @Override
    public List<Volume> searchVolumesDispositionedAfterDate(Date dispositionDate) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateDatePropertyRangeQuery(Calendar.getInstance().getTime(), dispositionDate, VolumeModel.Props.DISPOSITION_DATE));
        String query = generateVolumeSearchQuery(queryParts);
        List<Volume> results = searchVolumesImpl(query, -1, /* queryName */"volumesDispositionedAfterDate");
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
        List<Object> arguments = new ArrayList<Object>();
        addTaskStringExactPartsAndArgs(queryParts, arguments, Status.IN_PROGRESS.getName(), WorkflowCommonModel.Props.STATUS);
        queryParts.add(DbSearchUtil.generateTaskPropertyNotNullQuery(WorkflowCommonModel.Props.OWNER_ID));

        Pair<String, List<Object>> taskDatePropertyRangeQuery = DbSearchUtil.generateTaskDatePropertyRangeQuery(
                fromDate, dueDate, WorkflowSpecificModel.Props.DUE_DATE);
        addQueryPartsAndArguments(queryParts, arguments, taskDatePropertyRangeQuery);

        String query = generateTaskSearchQuery(queryParts);
        List<Task> results = BeanHelper.getWorkflowDbService().searchTasksAllStores(query, arguments, -1).getFirst();
        if (log.isDebugEnabled()) {
            log.debug("Due date passed tasks search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Authority> searchAuthorityGroups(String input, boolean returnAllGroups, boolean withAdminsAndDocManagers, int limit) {
        input = StringUtils.trimToEmpty(input);
        Set<String> results;
        List<Authority> authorities = new ArrayList<Authority>();
        if (input.length() == 0) {
            if (!returnAllGroups) {
                return Collections.emptyList();
            }
            results = authorityService.getAllAuthorities(AuthorityType.GROUP);
        } else {
            List<NodeRef> authorityRefs = searchAuthorityGroups(input, limit);
            results = new HashSet<String>(authorityRefs.size());
            results.addAll(CollectionUtils.collect(authorityRefs, new Transformer() {
                @Override
                public Object transform(Object arg0) {
                    NodeRef groupRef = (NodeRef) arg0;
                    return nodeService.getProperty(groupRef, ContentModel.PROP_AUTHORITY_NAME);
                }

            }));
        }
        for (String result : results) {
            if (withAdminsAndDocManagers || (!userService.getAdministratorsGroup().equals(result) && !userService.getDocumentManagersGroup().equals(result))) {
                authorities.add(userService.getAuthority(result));
            }
            if (authorities.size() == limit) {
                break;
            }
        }
        return authorities;
    }

    @Override
    public List<NodeRef> simpleSearch(String searchInputString, NodeRef parentRef, QName type, QName... props) {
        String query = joinQueryPartsAnd(Arrays.asList(
                generateTypeQuery(type)
                , generateStringWordsWildcardQuery(searchInputString, true, true, props)
                ));
        if (parentRef != null) {
            query = joinQueryPartsAnd(Arrays.asList(query, SearchUtil.generateParentQuery(parentRef)));
        }
        return searchNodes(query, -1, /* queryName */"simpleSearch", parentRef != null ? Collections.singletonList(parentRef.getStoreRef()) : null).getFirst();
    }

    @Override
    public boolean isMatch(String query) {
        return isMatch(query, false, "isMatch");
    }

    @Override
    public boolean isMatch(String query, boolean allStores, String queryName) {
        if (!allStores) {
            ResultSet resultSet = doSearchQuery(generateLuceneSearchParams(query, generalService.getStore(), 1), queryName);
            try {
                return resultSet.length() > 0;
            } finally {
                if (resultSet != null) {
                    resultSet.close();
                }
            }
        }
        List<ResultSet> results = doSearches(query, 1, queryName, getAllStoresWithArchivalStoreVOs());
        try {
            for (ResultSet result : results) {
                if (result.length() > 0) {
                    return true;
                }
            }
            return false;
        } finally {
            for (ResultSet result : results) {
                if (result != null) {
                    result.close();
                }
            }
        }
    }

    private List<NodeRef> searchAuthorityGroups(String groupName, int limit) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(2);
        queryParts.add(generateTypeQuery(ContentModel.TYPE_AUTHORITY_CONTAINER));
        // Use both left and right wildcard in user/group/contact/contactgroup/org.unit searches
        queryParts.add(generateStringWordsWildcardQuery(groupName, true, true, ContentModel.PROP_AUTHORITY_DISPLAY_NAME));

        String query = joinQueryPartsAnd(queryParts);
        List<NodeRef> results = searchNodes(query, limit, /* queryName */"authorityGroups");
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
        List<Object> arguments = new ArrayList<Object>();
        String ownerId = AuthenticationUtil.getRunAsUser();
        addTaskTypeFieldExactQueryPartsAndArguments(queryParts, arguments, taskType);
        addTaskStringExactPartsAndArgs(queryParts, arguments, Status.IN_PROGRESS.getName(), WorkflowCommonModel.Props.STATUS);
        addTaskStringExactPartsAndArgs(queryParts, arguments, ownerId, WorkflowCommonModel.Props.OWNER_ID);
        addSubstitutionRestriction(new Pair<List<String>, List<Object>>(queryParts, arguments));
        String query = generateTaskSearchQuery(queryParts);
        int count = BeanHelper.getWorkflowDbService().countTasks(query, arguments);
        if (log.isDebugEnabled()) {
            log.debug("Current user's and IN_PROGRESS tasks count search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return count;
    }

    @Override
    public Pair<List<TaskInfo>, Boolean> searchTasks(Node filter, int limit) {
        long startTime = System.currentTimeMillis();
        Pair<String, List<Object>> queryAndArguments = generateTaskSearchQuery(filter);
        List<TaskInfo> taskInfos = new ArrayList<TaskInfo>();
        boolean resultLimited = false;
        if (queryAndArguments != null) {
            Pair<List<Task>, Boolean> taskResults = BeanHelper.getWorkflowDbService().searchTasksMainStore(queryAndArguments.getFirst(), queryAndArguments.getSecond(), limit);
            for (Task task : taskResults.getFirst()) {
                Node workflow = generalService.fetchNode(task.getParentNodeRef());
                NodeRef compoundWorkflowRef = nodeService.getPrimaryParent(workflow.getNodeRef()).getParentRef();
                Node document = generalService.fetchNode(nodeService.getPrimaryParent(compoundWorkflowRef).getParentRef());
                taskInfos.add(new TaskInfo(task.getNode(), workflow, document));
            }
            resultLimited = taskResults.getSecond();
        }
        if (log.isDebugEnabled()) {
            log.debug("Tasks search total time " + (System.currentTimeMillis() - startTime) + " ms");
        }
        return new Pair<List<TaskInfo>, Boolean>(taskInfos, resultLimited);
    }

    @Override
    public List<NodeRef> searchTasksForReport(Node filter) {
        long startTime = System.currentTimeMillis();
        Pair<String, List<Object>> queryAndArguments = generateTaskSearchQuery(filter);
        List<NodeRef> results = BeanHelper.getWorkflowDbService().searchTaskNodeRefs(queryAndArguments.getFirst(), queryAndArguments.getSecond());
        if (log.isDebugEnabled()) {
            log.debug("Tasks search total time " + (System.currentTimeMillis() - startTime) + " ms");
        }
        return results;
    }

    private void addSubstitutionRestriction(Pair<List<String>, List<Object>> queryPartsAndArgs) {
        SubstitutionBean substitutionBean = (SubstitutionBean) FacesContextUtils.getRequiredWebApplicationContext( //
                FacesContext.getCurrentInstance()).getBean(SubstitutionBean.BEAN_NAME);
        SubstitutionInfo subInfo = substitutionBean.getSubstitutionInfo();
        if (subInfo.isSubstituting()) {
            Date start = DateUtils.truncate(subInfo.getSubstitution().getSubstitutionStartDate(), Calendar.DATE);
            Long daysForSubstitutionTasksCalc = parametersService.getLongParameter(Parameters.DAYS_FOR_SUBSTITUTION_TASKS_CALC);
            if (daysForSubstitutionTasksCalc.longValue() < 0) {
                return;
            }
            Date end = DateUtils.truncate(subInfo.getSubstitution().getSubstitutionEndDate(), Calendar.DATE);
            end = DateUtils.addDays(end, daysForSubstitutionTasksCalc.intValue());
            Pair<String, List<Object>> dateQueryAndArgs = generateTaskDatePropertyRangeQuery(start, end, WorkflowSpecificModel.Props.DUE_DATE);
            boolean hasDateQuery = dateQueryAndArgs != null;
            queryPartsAndArgs.getFirst().add(joinQueryPartsOr(Arrays.asList(
                    DbSearchUtil.generateTaskPropertyNullQuery(WorkflowSpecificModel.Props.DUE_DATE),
                    hasDateQuery ? dateQueryAndArgs.getFirst() : null)));
            if (hasDateQuery) {
                queryPartsAndArgs.getSecond().addAll(dateQueryAndArgs.getSecond());
            }
        }
    }

    private static String DOCUMENTS_FOR_REGISTERING_QUERY;

    static {
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateStringExactQuery(DocumentStatus.WORKING.getValueName(), DocumentCommonModel.Props.DOC_STATUS));
        queryParts.add(generateStringNullQuery(DocumentCommonModel.Props.REG_DATE_TIME));
        queryParts.add(generatePropertyBooleanQuery(DocumentCommonModel.Props.SEARCHABLE_HAS_ALL_FINISHED_COMPOUND_WORKFLOWS, true));
        DOCUMENTS_FOR_REGISTERING_QUERY = generateDocumentSearchQuery(queryParts);
    }

    @Override
    public List<Document> searchDocumentsForRegistering() {
        long startTime = System.currentTimeMillis();
        List<Document> results = searchDocumentsImpl(DOCUMENTS_FOR_REGISTERING_QUERY, -1, /* queryName */"documentsForRegistering").getFirst();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Documents for registering search total time %d ms, query: %s" //
                    , (System.currentTimeMillis() - startTime), DOCUMENTS_FOR_REGISTERING_QUERY));
        }
        return results;
    }

    @Override
    public int getCountOfDocumentsForRegistering() {
        long startTime = System.currentTimeMillis();
        int count = searchNodes(DOCUMENTS_FOR_REGISTERING_QUERY, -1, /* queryName */"documentsForRegisteringCount").size();

        if (log.isDebugEnabled()) {
            log.debug(String.format("Documents for registering count search total time %d ms, query: %s" //
                    , (System.currentTimeMillis() - startTime), DOCUMENTS_FOR_REGISTERING_QUERY));
        }
        return count;
    }

    @Override
    public Pair<List<Document>, Boolean> searchDocuments(Node filter, int limit) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> properties = filter.getProperties();
        @SuppressWarnings("unchecked")
        List<NodeRef> storeFunctionRootNodeRefs = (List<NodeRef>) properties.get(DocumentSearchModel.Props.STORE);
        List<StoreRef> storeRefs = new ArrayList<StoreRef>(storeFunctionRootNodeRefs.size());
        for (NodeRef nodeRef : storeFunctionRootNodeRefs) {
            storeRefs.add(nodeRef.getStoreRef());
        }
        String query = generateDocumentSearchQuery(filter);
        if (StringUtils.isBlank(query)) {
            throw new UnableToPerformException(UnableToPerformException.MessageSeverity.INFO, "docSearch_error_noInput");
        }
        try {
            Pair<List<Document>, Boolean> results = searchDocumentsImpl(query, limit, /* queryName */"documentsByFilter", storeRefs);
            if (results != null) {
                filterByStructUnit(results.getFirst(), filter);
            }
            if (log.isDebugEnabled()) {
                log.debug("Documents search total time " + (System.currentTimeMillis() - startTime) + " ms");
            }
            return results;
        } catch (RuntimeException e) {
            Map<QName, Serializable> filterProps = RepoUtil.getNotEmptyProperties(RepoUtil.toQNameProperties(properties));
            log.error("Document search failed: "
                    + e.getMessage()
                    + "\n  searchFilter=" + WmNode.toString(filterProps, namespaceService)
                    + "\n  query=" + query, e);
            throw e;
        }
    }

    @SuppressWarnings("rawtypes")
    private void filterByStructUnit(List<Document> documents, Node filter) {
        if (documents == null) {
            return;
        }
        Map<QName, Serializable> props = RepoUtil.toQNameProperties(filter.getProperties());
        for (Map.Entry<QName, Serializable> entry : props.entrySet()) {
            Serializable value = entry.getValue();
            if (value instanceof List) {
                QName propQName = entry.getKey();
                if (!DocumentDynamicModel.URI.equals(propQName.getNamespaceURI())) {
                    continue;
                }
                DynamicPropertyDefinition def = BeanHelper.getDocumentConfigService().getPropertyDefinitionById(propQName.getLocalName());
                if (!((List) value).isEmpty() && def != null && FieldType.STRUCT_UNIT == def.getFieldType()) {
                    @SuppressWarnings("unchecked")
                    List<String> searchStructUnits = (List<String>) value;
                    for (Iterator<Document> it = documents.iterator(); it.hasNext();) {
                        boolean hasStructUnit = false;
                        @SuppressWarnings("unchecked")
                        List<String> documentStructUnits = (List<String>) it.next().getProperties().get(propQName);
                        if (documentStructUnits != null) {
                            for (String searchStructUnit : searchStructUnits) {
                                if (documentStructUnits.contains(searchStructUnit)) {
                                    hasStructUnit = true;
                                    break;
                                }
                            }
                        }
                        if (!hasStructUnit) {
                            it.remove();
                        }
                    }
                }
            }
        }
    }

    @Override
    public Pair<List<NodeRef>, Boolean> searchAllDocumentsByParentRef(NodeRef parentRef, int limit) {
        String query = generateDocumentSearchQuery(
                SearchUtil.generateParentQuery(parentRef)
                );

        if (log.isDebugEnabled()) {
            log.debug("Documents by parent query: " + query);
        }

        return searchNodes(query, limit, "allDocumentsByParentRef", Collections.singletonList(parentRef.getStoreRef()));
    }

    @Override
    public List<NodeRef> searchDocumentsForReport(Node filter, StoreRef storeRef) {
        long startTime = System.currentTimeMillis();
        Assert.notNull(storeRef);
        String query = generateDocumentSearchQuery(filter);
        if (StringUtils.isBlank(query)) {
            // this should never happen, web layer must ensure we have some input
            throw new UnableToPerformException(UnableToPerformException.MessageSeverity.INFO, "docSearch_error_noInput");
        }
        List<NodeRef> results = new ArrayList<NodeRef>();
        results.addAll(searchNodes(query, -1, /* queryName */"searchDocumentsForReport", Collections.singletonList(storeRef)).getFirst());
        if (log.isDebugEnabled()) {
            log.debug("Document search total time " + (System.currentTimeMillis() - startTime) + " ms");
        }
        return results;
    }

    @Override
    public List<StoreRef> getStoresFromDocumentReportFilter(Map<String, Object> properties) {
        @SuppressWarnings("unchecked")
        List<String> storeFunctionRootNodeRefs = (List<String>) properties.get(DocumentSearchModel.Props.STORE);
        List<StoreRef> storeRefs = new ArrayList<StoreRef>(storeFunctionRootNodeRefs.size());
        for (String nodeRef : storeFunctionRootNodeRefs) {
            storeRefs.add(new NodeRef(nodeRef).getStoreRef());
        }
        if (storeRefs.isEmpty()) {
            storeRefs.add(generalService.getStore());
        }
        return storeRefs;
    }

    @Override
    public List<Document> searchDocumentsInOutbox() {
        String query = getDvkOutboxQuery();
        log.debug("searchDocumentsInOutbox with query '" + query + "'");
        return searchDocumentsBySendInfoImpl(query, -1, /* queryName */"documentsInOutbox");
    }

    @Override
    public List<Document> searchDocumentsByDvkId(String dvkId) {
        if (StringUtils.isBlank(dvkId)) {
            return new ArrayList<Document>();
        }
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateTypeQuery(DocumentCommonModel.Types.SEND_INFO));
        queryParts.add(generateStringExactQuery(dvkId, DocumentCommonModel.Props.SEND_INFO_DVK_ID));
        String query = joinQueryPartsAnd(queryParts, false);
        log.debug("searchDocumentsByDvkId with query '" + query + "'");
        return searchDocumentsBySendInfoImpl(query, -1, /* queryName */"documentsInOutbox");
    }

    @Override
    public int searchDocumentsInOutboxCount() {
        String query = getDvkOutboxQuery();
        log.debug("searchDocumentsInOutboxCount with query '" + query + "'");
        return searchDocumentsBySendInfoImplCount(query, -1, /* queryName */"documentsInOutboxCount");
    }

    @Override
    public Map<NodeRef /* sendInfo */, Pair<String /* dvkId */, String /* recipientRegNr */>> searchOutboxDvkIds() {
        String query = getDvkOutboxQuery();
        log.debug("searchDocumentsInOutbox with query '" + query + "'");
        return searchDhlIdsBySendInfoImpl(query, -1, /* queryName */"outboxDvkIds");
    }

    @Override
    public Pair<List<Document>, Boolean> searchDocumentsQuick(String searchValue, NodeRef containerNodeRef, int limit) {
        logService.addLogEntry(LogEntry.create(LogObject.SEARCH_DOC, userService, "applog_search_docs_quick", searchValue));
        return searchDocumentsAndOrCases(generateQuickSearchQuery(searchValue, containerNodeRef), searchValue, false, limit);
    }

    @Override
    public String generateDeletedSearchQuery(String searchValue, NodeRef containerNodeRef) {
        List<String> queryParts = generateQuickSearchDocumentQuery(parseQuickSearchWords(searchValue, 1));
        if (isBlank(queryParts)) {
            return null;
        }
        return generateDocumentSearchQuery(queryParts);
    }

    @Override
    public Pair<List<Document>, Boolean> searchDocumentsAndOrCases(String searchString, Date regDateTimeBegin, Date regDateTimeEnd, List<String> documentTypes,
            boolean trySearchCases) {
        boolean noRegDates = regDateTimeBegin == null && regDateTimeEnd == null;
        boolean noDocTypes = documentTypes == null || documentTypes.isEmpty();
        boolean includeCaseTitles = trySearchCases && noRegDates && noDocTypes;
        if (includeCaseTitles && StringUtils.isBlank(searchString)) {
            throw new UnableToPerformException("docSearch_error_noInput");
        }
        if (regDateTimeBegin != null && regDateTimeEnd != null && regDateTimeEnd.before(regDateTimeBegin)) {
            throw new UnableToPerformException("error_beginDateMustBeBeforeEndDate");
        }
        List<String> queryParts = new ArrayList<String>(5); // two elements are inserted by generateDocumentSearchQuery()
        queryParts.add(generateMultiStringExactQuery(documentTypes, DocumentAdminModel.Props.OBJECT_TYPE_ID));
        queryParts.add(generateQuickSearchQuery(searchString));
        queryParts.add(generateDatePropertyRangeQuery(regDateTimeBegin, regDateTimeEnd, DocumentCommonModel.Props.REG_DATE_TIME));
        String query = generateDocumentSearchQuery(queryParts);
        int limit = parametersService.getLongParameter(Parameters.MAX_SEARCH_RESULT_ROWS).intValue();
        return searchDocumentsAndOrCases(query, searchString, includeCaseTitles, limit);
    }

    private Pair<List<Document>, Boolean> searchDocumentsAndOrCases(String query, String searchString, boolean includeCaseTitles, int limit) {
        long startTime = System.currentTimeMillis();
        try {
            final Pair<List<Document>, Boolean> results;
            if (includeCaseTitles) {
                final String caseByTitleQuery = getCaseByTitleQuery(searchString);
                query = joinQueryPartsOr(Arrays.asList(query, caseByTitleQuery));
                results = searchDocumentsAndCaseTitlesImpl(query, limit, /* queryName */"documentsQuickAndCaseTitles");
            } else {
                results = searchDocumentsImpl(query, limit, /* queryName */"documentsQuick");
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
        return joinQueryPartsAnd(queryParts, false);
    }

    @Override
    public List<NodeRef> searchWorkingDocumentsByOwnerId(String ownerId, boolean isPreviousOwnerId) {
        long startTime = System.currentTimeMillis();
        String query = getWorkingDocumentsOwnerQuery(ownerId, isPreviousOwnerId);
        List<NodeRef> results = searchNodesFromAllStores(query, /* queryName */"workingDocumentsByOwnerId");
        if (log.isDebugEnabled()) {
            log.debug("User's " + ownerId + " working documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public List<NodeRef> searchNewTasksByOwnerId(String ownerId, boolean isPreviousOwnerId) {
        long startTime = System.currentTimeMillis();
        Pair<List<String>, List<Object>> taskQueryAndArgs = getTaskQuery(null, ownerId, Status.NEW, isPreviousOwnerId);
        String query = generateTaskSearchQuery(taskQueryAndArgs.getFirst());
        List<NodeRef> results = BeanHelper.getWorkflowDbService().searchTaskNodeRefs(query, taskQueryAndArgs.getSecond());
        if (log.isDebugEnabled()) {
            log.debug("User's " + ownerId + " new tasks search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public List<Document> searchSimilarInvoiceDocuments(String regNumber, String invoiceNumber, Date invoiceDate) {
        if (regNumber == null || invoiceNumber == null || invoiceDate == null) {
            return null;
        }
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(4);
        queryParts.add(SearchUtil.generateTypeQuery(DocumentSubtypeModel.Types.INVOICE)); // TODO use generateDocumentSearchQuery + objectTypeId=invoice
        queryParts.add(SearchUtil.generateStringExactQuery(regNumber, DocumentSpecificModel.Props.SELLER_PARTY_REG_NUMBER));
        queryParts.add(SearchUtil.generateStringExactQuery(invoiceNumber, DocumentSpecificModel.Props.INVOICE_NUMBER));
        queryParts.add(SearchUtil.generatePropertyDateQuery(DocumentSpecificModel.Props.INVOICE_DATE, invoiceDate));
        String query = SearchUtil.joinQueryPartsAnd(queryParts);
        List<Document> result = searchDocumentsImpl(query, -1, /* queryName */"similarInvoiceDocuments").getFirst();
        if (log.isDebugEnabled()) {
            log.debug("Similar invoice documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return result;
    }

    @Override
    public List<Document> searchContractsByRegNumber(String regNumber) {
        List<String> queryParts = new ArrayList<String>();
        long startTime = System.currentTimeMillis();
        queryParts.add(SearchUtil.generateTypeQuery(DocumentTypeHelper.CONTRACT_TYPES)); // TODO use generateDocumentSearchQuery + objectTypeId=...
        queryParts.add(SearchUtil.generateStringExactQuery(regNumber, DocumentCommonModel.Props.REG_NUMBER,
                DocumentSpecificModel.Props.SECOND_PARTY_CONTRACT_NUMBER));
        String query = SearchUtil.joinQueryPartsAnd(queryParts);
        List<Document> result = searchDocumentsImpl(query, -1, /* queryName */"contractsByRegNumber").getFirst();
        if (log.isDebugEnabled()) {
            log.debug("Contracts by reg.number search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return result;
    }

    @Override
    public List<Document> searchInvoiceBaseDocuments(String contractNumber, String sellerPartyName) {
        if (StringUtils.isBlank(sellerPartyName)) {
            return null;
        }
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(2);
        if (StringUtils.isNotBlank(contractNumber)) {
            List<String> contractQueryParts = new ArrayList<String>(4);
            contractQueryParts.add(SearchUtil.generateTypeQuery(DocumentTypeHelper.CONTRACT_TYPES)); // TODO use generateDocumentSearchQuery + objectTypeId=...
            contractQueryParts.add(SearchUtil.generateStringExactQuery(contractNumber, DocumentCommonModel.Props.REG_NUMBER));
            // on document node, duplicates partyName property values from all contractParty child-nodes
            contractQueryParts.add(generateStringWordsWildcardQuery(sellerPartyName, DocumentSpecificModel.Props.PARTY_NAME));
            // NB! If more contract types are added, endDate properties should be added here also
            contractQueryParts.add(SearchUtil.generateDatePropertyRangeQuery(new Date(), null, DocumentSpecificModel.Props.CONTRACT_MV_END_DATE,
                    DocumentSpecificModel.Props.CONTRACT_SIM_END_DATE, DocumentSpecificModel.Props.CONTRACT_SMIT_END_DATE));
            queryParts.add(SearchUtil.joinQueryPartsAnd(contractQueryParts));
        }
        List<String> instrumentOfDeliveryAndReceiptTypeQP = new ArrayList<String>(DocumentTypeHelper.outgoingLetterTypes.size());
        List<String> instrumentOfDeliveryAndReceiptPropQP = new ArrayList<String>(DocumentTypeHelper.outgoingLetterTypes.size());
        for (QName docType : DocumentTypeHelper.instrumentOfDeliveryAndReceipt) {
            instrumentOfDeliveryAndReceiptTypeQP.add(SearchUtil.generateTypeQuery(docType));
            Collection<QName> docProperties = new HashSet<QName>();
            Collection<QName> aspects = generalService.getDefaultAspects(docType);
            for (QName aspect : aspects) {
                for (Map.Entry<QName, PropertyDefinition> entry : dictionaryService.getPropertyDefs(aspect).entrySet()) {
                    PropertyDefinition propDef = entry.getValue();
                    QName prop = propDef.getName();
                    if (SearchUtil.isStringProperty(prop)
                            && (DocumentCommonModel.URI.equals(prop.getNamespaceURI()) || DocumentSpecificModel.URI.equals(prop.getNamespaceURI()))) {
                        docProperties.add(entry.getKey());
                    }
                }
            }

            instrumentOfDeliveryAndReceiptPropQP.add(generateStringWordsWildcardQuery(sellerPartyName, docProperties.toArray(new QName[docProperties.size()])));
        }
        List<String> instrumentOfDeliveryAndReceiptQP = new ArrayList<String>();
        instrumentOfDeliveryAndReceiptQP.add(SearchUtil.joinQueryPartsOr(instrumentOfDeliveryAndReceiptTypeQP));
        instrumentOfDeliveryAndReceiptQP.add(SearchUtil.joinQueryPartsOr(instrumentOfDeliveryAndReceiptPropQP));
        queryParts.add(SearchUtil.joinQueryPartsAnd(instrumentOfDeliveryAndReceiptQP));

        String query = SearchUtil.joinQueryPartsOr(queryParts);
        List<Document> result = searchDocumentsImpl(query, -1, /* queryName */"invoiceBaseDocuments").getFirst();
        if (log.isDebugEnabled()) {
            log.debug("Invoice base documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return result;
    }

    @Override
    public List<NodeRef> searchUsersByFirstNameLastName(String firstName, String lastName) {
        Assert.notNull(lastName);
        List<String> queryParts = new ArrayList<String>(3);
        queryParts.add(SearchUtil.generateTypeQuery(ContentModel.TYPE_PERSON));
        if (StringUtils.isNotEmpty(firstName)) {
            queryParts.add(SearchUtil.generateStringExactQuery(firstName, ContentModel.PROP_FIRSTNAME));
        }
        queryParts.add(SearchUtil.generateStringExactQuery(lastName, ContentModel.PROP_LASTNAME));
        String query = SearchUtil.joinQueryPartsAnd(queryParts);
        return searchNodes(query, -1, /* queryName */"usersByFirstNameLastName");
    }

    @Override
    public List<NodeRef> searchUsersByRelatedFundsCenter(String relatedFundsCenter) {
        Assert.notNull(relatedFundsCenter);
        List<String> queryParts = new ArrayList<String>(2);
        queryParts.add(SearchUtil.generateTypeQuery(ContentModel.TYPE_PERSON));
        queryParts.add(generateMultiStringExactQuery(Arrays.asList(relatedFundsCenter), ContentModel.PROP_RELATED_FUNDS_CENTER));
        String query = SearchUtil.joinQueryPartsAnd(queryParts);
        return searchNodes(query, -1, /* queryName */"usersByRelatedFundsCenter");
    }

    private Pair<List<String>, List<Object>> getTaskQuery(QName taskType, String ownerId, Status status, boolean isPreviousOwnerId) {
        QName ownerField = (isPreviousOwnerId) ? WorkflowCommonModel.Props.PREVIOUS_OWNER_ID : WorkflowCommonModel.Props.OWNER_ID;
        List<String> queryParts = new ArrayList<String>();
        List<Object> arguments = new ArrayList<Object>();
        if (taskType != null) {
            addTaskTypeFieldExactQueryPartsAndArguments(queryParts, arguments, taskType);
        } else {
            addTaskTypeFieldExactQueryPartsAndArguments(queryParts, arguments);
        }
        addTaskStringExactPartsAndArgs(queryParts, arguments, status.getName(), WorkflowCommonModel.Props.STATUS);
        addTaskStringExactPartsAndArgs(queryParts, arguments, ownerId, ownerField);
        return new Pair<List<String>, List<Object>>(queryParts, arguments);
    }

    private void addTaskStringExactPartsAndArgs(List<String> queryParts, List<Object> arguments, String value, QName propName) {
        if (StringUtils.isBlank(value) || propName == null) {
            return;
        }
        queryParts.add(DbSearchUtil.generateTaskPropertyExactQuery(propName));
        arguments.add(value);
    }

    private String getWorkingDocumentsOwnerQuery(String ownerId, boolean isPreviousOwnerId) {
        List<String> queryParts = new ArrayList<String>();
        QName ownerField = (isPreviousOwnerId) ? DocumentCommonModel.Props.PREVIOUS_OWNER_ID : DocumentCommonModel.Props.OWNER_ID;
        queryParts.add(generateStringExactQuery(ownerId, ownerField));
        queryParts.add(generateStringExactQuery(DocumentStatus.WORKING.getValueName(), DocumentCommonModel.Props.DOC_STATUS));
        return generateDocumentSearchQuery(queryParts);
    }

    private String getInProcessDocumentsOwnerQuery(String ownerId) {
        String incomingLetterTypesQuery = generateStringExactQuery("incomingLetter", DocumentAdminModel.Props.OBJECT_TYPE_ID);
        String notIncomingLetterTypesQuery = generateAndNotQuery(generateStringExactQuery(DocumentStatus.WORKING.getValueName(),
                DocumentCommonModel.Props.DOC_STATUS), generateStringExactQuery("incomingLetter", DocumentAdminModel.Props.OBJECT_TYPE_ID));
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateStringExactQuery(ownerId, DocumentCommonModel.Props.OWNER_ID));
        String hasNoStartedCompoundWorkflowsQuery = joinQueryPartsOr(Arrays.asList(
                generatePropertyBooleanQuery(DocumentCommonModel.Props.SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS, false)
                , generatePropertyNullQuery(DocumentCommonModel.Props.SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS)));

        queryParts.add(joinQueryPartsOr(
                Arrays.asList(
                        joinQueryPartsAnd(Arrays.asList(incomingLetterTypesQuery, hasNoStartedCompoundWorkflowsQuery))
                        , notIncomingLetterTypesQuery
                        )
                ));
        return generateDocumentSearchQuery(queryParts);
    }

    private String getDvkOutboxQuery() {
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateTypeQuery(DocumentCommonModel.Types.SEND_INFO));
        queryParts.add(generateStringExactQuery(SendMode.DVK.getValueName(), DocumentCommonModel.Props.SEND_INFO_SEND_MODE));
        queryParts.add(generateStringExactQuery(SendStatus.SENT.toString(), DocumentCommonModel.Props.SEND_INFO_SEND_STATUS));
        return joinQueryPartsAnd(queryParts, false);
    }

    private Map<NodeRef /* sendInfo */, Pair<String /* dvkId */, String /* recipientRegNr */>> searchDhlIdsBySendInfoImpl(String query, int limit,
            String queryName) {
        final HashMap<NodeRef, Pair<String, String>> refsAndDvkIds = new HashMap<NodeRef, Pair<String, String>>();
        searchGeneralImpl(query, limit, queryName, new SearchCallback<String>() {
            @Override
            public String addResult(ResultSetRow row) {
                final NodeRef sendInfoRef = row.getNodeRef();
                Pair<String, String> dvkIdAndRecipientregNr =
                        new Pair<String, String>((String) nodeService.getProperty(sendInfoRef, DocumentCommonModel.Props.SEND_INFO_DVK_ID),
                                (String) nodeService.getProperty(sendInfoRef, DocumentCommonModel.Props.SEND_INFO_RECIPIENT_REG_NR));
                refsAndDvkIds.put(sendInfoRef, dvkIdAndRecipientregNr);
                return null;
            }
        }, getAllStoresWithArchivalStoreVOs());
        return refsAndDvkIds;
    }

    private List<Document> searchDocumentsBySendInfoImpl(String query, int limit, String queryName) {
        return searchGeneralImpl(query, limit, queryName, new SearchCallback<Document>() {
            private final Set<String> nodeIds = new HashSet<String>();

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
        }, getAllStores()).getFirst();
    }

    private int searchDocumentsBySendInfoImplCount(String query, int limit, String queryName) {
        final Set<String> nodeIds = new HashSet<String>();
        searchGeneralImpl(query, limit, queryName, new SearchCallback<String>() {
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
        Map<QName, Serializable> props = RepoUtil.toQNameProperties(filter.getProperties(), true);

        // START: special cases
        // Dok liik
        @SuppressWarnings("unchecked")
        List<String> documentTypes = (List<String>) props.get(DocumentSearchModel.Props.DOCUMENT_TYPE);
        queryParts.add(generateMultiStringExactQuery(documentTypes, DocumentAdminModel.Props.OBJECT_TYPE_ID));
        // Saatmisviis
        @SuppressWarnings("unchecked")
        List<String> sendMode = (List<String>) props.get(DocumentSearchModel.Props.SEND_MODE);
        queryParts.add(generateMultiStringExactQuery(sendMode, DocumentCommonModel.Props.SEARCHABLE_SEND_MODE, DocumentSpecificModel.Props.TRANSMITTAL_MODE));
        // Dokumendi reg. number
        queryParts.add(generateStringExactQuery((String) props.get(DocumentCommonModel.Props.SHORT_REG_NUMBER), DocumentCommonModel.Props.SHORT_REG_NUMBER));
        // Projekt
        @SuppressWarnings("unchecked")
        List<String> fund = (List<String>) props.get(DocumentSearchModel.Props.FUND);
        queryParts.add(generateMultiStringExactQuery(fund, DocumentCommonModel.Props.SEARCHABLE_FUND));
        // EA Yksus
        @SuppressWarnings("unchecked")
        List<String> fundsCenter = (List<String>) props.get(DocumentSearchModel.Props.FUNDS_CENTER);
        queryParts.add(generateMultiStringExactQuery(fundsCenter, DocumentCommonModel.Props.SEARCHABLE_FUNDS_CENTER));
        // EA konto
        @SuppressWarnings("unchecked")
        List<String> eaCommitmentItem = (List<String>) props.get(DocumentSearchModel.Props.EA_COMMITMENT_ITEM);
        queryParts.add(generateMultiStringExactQuery(eaCommitmentItem, DocumentCommonModel.Props.SEARCHABLE_EA_COMMITMENT_ITEM));
        // Loomise aeg
        Date dateCreatedBegin = (Date) props.get(DocumentSearchModel.Props.DOCUMENT_CREATED);
        Date dateCreatedEnd = (Date) props.get(DocumentSearchModel.Props.DOCUMENT_CREATED_END_DATE);
        queryParts.add(generateDatePropertyRangeQuery(dateCreatedBegin, dateCreatedEnd, ContentModel.PROP_CREATED));
        props.remove(DocumentSearchModel.Props.DOCUMENT_CREATED);

        // END: special cases

        fillQueryFromProps(queryParts, props);

        String searchFilter = WmNode.toHumanReadableStringIfPossible(RepoUtil.getNotEmptyProperties(props), namespaceService, BeanHelper.getDocumentAdminService());
        log.info("Documents search filter: " + searchFilter);
        logService.addLogEntry(LogEntry.create(LogObject.SEARCH_DOC, userService, "applog_search_docs", searchFilter));

        // Quick search (Otsisõna)
        String quickSearchInput = (String) props.get(DocumentSearchModel.Props.INPUT);
        if (StringUtils.isNotBlank(quickSearchInput)) {
            List<String> quickSearchWords = parseQuickSearchWords(quickSearchInput);
            log.info("Quick search (document) - words: " + quickSearchWords.toString() + ", from string '" + quickSearchInput + "'");
            queryParts.addAll(generateQuickSearchDocumentQuery(quickSearchWords));
        }

        String query = generateDocumentSearchQuery(queryParts);
        if (log.isDebugEnabled()) {
            log.debug("Documents search query construction time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return query;
    }

    private void fillQueryFromProps(List<String> queryParts, Map<QName, Serializable> props) {
        // dynamic generation
        for (Entry<QName, Serializable> entry : props.entrySet()) {
            QName propQName = entry.getKey();
            if (DocumentLocationGenerator.CASE_LABEL_EDITABLE.equals(propQName)) { // caseLabelEditable value is the title of the case, but the property is a NodeRef
                String caseLabel = (String) entry.getValue();
                if (StringUtils.isBlank(caseLabel)) {
                    continue;
                }
                NodeRef volumeRef = (NodeRef) props.get(DocumentCommonModel.Props.VOLUME);
                String query = joinQueryPartsAnd(generatePropertyWildcardQuery(CaseModel.Props.TITLE, caseLabel.trim(), true, false, true),
                        generateParentQuery(volumeRef));
                ResultSet result = null;
                try {
                    result = doSearch(query, -1, "searchCaseByLabelForDocumentSearch", volumeRef.getStoreRef());
                    queryParts.add(generateMultiNodeRefQuery(result.getNodeRefs(), DocumentCommonModel.Props.CASE));
                } finally {
                    if (result != null) {
                        result.close();
                    }
                }
                continue;
            } else if (propQName.getLocalName().contains("_")) {
                if (DateGenerator.isEndDate(propQName)) {
                    QName originalQname = DateGenerator.getOriginalQName(propQName);
                    if (DateGenerator.isEndDate(propQName) && props.get(originalQname) == null) {
                        propQName = originalQname;
                    } else {
                        continue;
                    }
                } else if (DoubleGenerator.isEndNumber(propQName)) {
                    QName originalQname = DoubleGenerator.getOriginalQName(propQName);
                    if (DoubleGenerator.isEndNumber(propQName) && props.get(originalQname) == null) {
                        propQName = originalQname;
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            } else if (DocumentCommonModel.Props.CASE.equals(propQName)) {
                continue;
            }
            if (propQName.equals(DocumentCommonModel.Props.SHORT_REG_NUMBER) || !propQName.getNamespaceURI().equals(DocumentDynamicModel.URI)) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof String) {
                queryParts.add(generateStringWordsWildcardQuery((String) value, propQName));
            } else if (value instanceof List) {
                // including docAdminService in context.xml creates a circular dependency because docAdminService includes DocSearchService
                DocumentAdminService ser = BeanHelper.getDocumentAdminService();
                // TODO DLSeadist -- this is probably slow, because it loads fieldDefinitions every time from repo
                FieldDefinition def = ser.getFieldDefinition(propQName.getLocalName());
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) value;
                if (StringUtils.isNotBlank(def.getClassificator())) {
                    queryParts.add(generateMultiStringExactQuery(list, propQName));
                } else {
                    queryParts.add(generateMultiStringWordsWildcardQuery(list, propQName));
                }
            } else if (value instanceof Date) {
                Date endDate = (Date) props.get(DateGenerator.getEndDateQName(propQName));
                queryParts.add(generateDatePropertyRangeQuery((Date) props.get(propQName), endDate, propQName));
            } else if (value instanceof Double || value instanceof Integer || value instanceof Long) {
                Number maxValue = (Number) props.get(DoubleGenerator.getEndNumberQName(propQName));
                queryParts.add(generateNumberPropertyRangeQuery((Number) props.get(propQName), maxValue, propQName));
            } else if (value instanceof Boolean) {
                Boolean value2 = (Boolean) value;
                if (value2) {
                    queryParts.add(generatePropertyBooleanQuery(propQName, value2));
                }
            } else if (value instanceof NodeRef) {
                queryParts.add(generateNodeRefQuery((NodeRef) value, propQName));
            }
        }
    }

    private String generateQuickSearchQuery(String searchString) {
        return generateQuickSearchQuery(searchString, null);
    }

    private String generateQuickSearchQuery(String searchString, NodeRef containerNodeRef) {
        long startTime = System.currentTimeMillis();
        List<String> quickSearchWords = parseQuickSearchWords(searchString);
        log.info("Quick search - words: " + quickSearchWords.toString() + ", from string '" + searchString + "'");
        String query = generateDocumentSearchQuery(generateQuickSearchDocumentQuery(quickSearchWords));
        if (containerNodeRef != null) {
            query = joinQueryPartsAnd(
                    generateNodeRefQuery(containerNodeRef, DocumentCommonModel.Props.FUNCTION, DocumentCommonModel.Props.SERIES, DocumentCommonModel.Props.VOLUME,
                            DocumentCommonModel.Props.CASE),
                    query);
        }
        if (log.isDebugEnabled()) {
            log.debug("Quick search query construction time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return query;
    }

    private List<String> generateQuickSearchDocumentQuery(List<String> searchWords) {
        List<String> queryParts = new ArrayList<String>(searchWords.size());
        for (String searchWord : searchWords) {
            if (StringUtils.isNotBlank(searchWord)) {
                queryParts.add(joinQueryPartsOr(Arrays.asList(
                        SearchUtil.generateValueQuery(searchWord, false),
                        SearchUtil.generatePropertyWildcardQuery(DocumentCommonModel.Props.FILE_CONTENTS, searchWord, false, false, true))));
            }
        }
        return queryParts;
    }

    private static String generateDocumentSearchQuery(String... queryParts) {
        return generateDocumentSearchQuery(new ArrayList<String>(Arrays.asList(queryParts)));
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

    private static String generateSeriesSearchQuery(List<String> queryParts) {
        if (isBlank(queryParts)) {
            return null;
        }
        queryParts.add(0, generateTypeQuery(SeriesModel.Types.SERIES));
        return joinQueryPartsAnd(queryParts);
    }

    private static String generateTaskSearchQuery(List<String> queryParts) {
        if (isBlank(queryParts)) {
            return null;
        }
        return joinQueryPartsAnd(queryParts);
    }

    @SuppressWarnings({ "unchecked" })
    private Pair<String, List<Object>> generateTaskSearchQuery(Node filter) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(20);
        List<Object> arguments = new ArrayList<Object>();
        Map<String, Object> props = filter.getProperties();

        // TODO: Riina - verify that it is okay NOT to use left wildcard any more.
        // Use both left and right wildcard in task searches
        addDateQueryPartsAndArguments(queryParts, arguments, props, TaskSearchModel.Props.STARTED_DATE_TIME_BEGIN, TaskSearchModel.Props.STARTED_DATE_TIME_END,
                WorkflowCommonModel.Props.STARTED_DATE_TIME);

        addTaskMultiStringWordsWildcardPartsAndArgs(queryParts, arguments, (List<String>) props.get(TaskSearchModel.Props.OWNER_NAME), WorkflowCommonModel.Props.OWNER_NAME);

        addTaskMultiStringExactPartsAndArgs(queryParts, arguments, props, TaskSearchModel.Props.OUTCOME, WorkflowCommonModel.Props.OUTCOME);
        addTaskStringWordsWildcardPartsAndArgs(queryParts, arguments, (String) props.get(TaskSearchModel.Props.CREATOR_NAME), WorkflowCommonModel.Props.CREATOR_NAME);
        addTaskMultiStringExactPartsAndArgs(queryParts, arguments, props, TaskSearchModel.Props.DOC_TYPE, WorkflowCommonModel.Props.DOCUMENT_TYPE);

        addTaskMultiStringArrayPartsAndArgs(queryParts, arguments, (List<String>) props.get(TaskSearchModel.Props.ORGANIZATION_NAME),
                WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME);

        addTaskStringWordsWildcardPartsAndArgs(queryParts, arguments, (String) props.get(TaskSearchModel.Props.JOB_TITLE), WorkflowCommonModel.Props.OWNER_JOB_TITLE);
        addDateQueryPartsAndArguments(queryParts, arguments, props, TaskSearchModel.Props.DUE_DATE_TIME_BEGIN, TaskSearchModel.Props.DUE_DATE_TIME_END,
                WorkflowSpecificModel.Props.DUE_DATE);
        if (Boolean.TRUE.equals(props.get(TaskSearchModel.Props.ONLY_RESPONSIBLE))) {
            queryParts.add(DbSearchUtil.generateTaskFieldNotNullQuery(DbSearchUtil.ACTIVE_FIELD));
        }
        addDateQueryPartsAndArguments(queryParts, arguments, props, TaskSearchModel.Props.COMPLETED_DATE_TIME_BEGIN, TaskSearchModel.Props.COMPLETED_DATE_TIME_END,
                WorkflowCommonModel.Props.COMPLETED_DATE_TIME);
        addTaskStringWordsWildcardPartsAndArgs(queryParts, arguments, (String) props.get(TaskSearchModel.Props.COMMENT), WorkflowSpecificModel.Props.COMMENT);
        addTaskStringWordsWildcardPartsAndArgs(queryParts, arguments, (String) props.get(TaskSearchModel.Props.RESOLUTION), WorkflowSpecificModel.Props.RESOLUTION,
                WorkflowSpecificModel.Props.WORKFLOW_RESOLUTION);
        addTaskMultiStringExactPartsAndArgs(queryParts, arguments, props, TaskSearchModel.Props.STATUS, WorkflowCommonModel.Props.STATUS);
        if (Boolean.TRUE.equals(props.get(TaskSearchModel.Props.COMPLETED_OVERDUE))) {
            addBooleanQueryPartsAndArguments(queryParts, arguments, props, WorkflowSpecificModel.Props.COMPLETED_OVERDUE, TaskSearchModel.Props.COMPLETED_OVERDUE);
        }
        addDateQueryPartsAndArguments(queryParts, arguments, props, TaskSearchModel.Props.STOPPED_DATE_TIME_BEGIN, TaskSearchModel.Props.STOPPED_DATE_TIME_END,
                WorkflowCommonModel.Props.STOPPED_DATE_TIME);
        List<QName> types = (List<QName>) props.get(TaskSearchModel.Props.TASK_TYPE);
        addTaskTypeFieldExactQueryPartsAndArguments(queryParts, arguments, types.toArray(new QName[types.size()]));
        String searchFilter = WmNode.toHumanReadableStringIfPossible(RepoUtil.getNotEmptyProperties(RepoUtil.toQNameProperties(props)), namespaceService, null);
        log.info("Tasks search filter: " + searchFilter);

        // Searches are made by user. Reports are generated by DHS. We need to log only searches.
        if (userService.getCurrentUserName() != null) {
            logService.addLogEntry(LogEntry.create(LogObject.SEARCH_TASK, userService, "applog_search_task", searchFilter));
        }

        if (isBlank(queryParts)) {
            return null;
        }

        String query = joinQueryPartsAnd(queryParts);
        if (log.isDebugEnabled()) {
            log.debug("Tasks search query construction time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return new Pair<String, List<Object>>(query, arguments);
    }

    private void addTaskTypeFieldExactQueryPartsAndArguments(List<String> queryParts, List<Object> arguments, QName... types) {
        if (types == null || types.length == 0) {
            return;
        }
        List<String> subQueryParts = new ArrayList<String>();
        for (QName type : types) {
            subQueryParts.add(DbSearchUtil.generateTaskFieldExactQuery(DbSearchUtil.TASK_TYPE_FIELD));
            arguments.add(type.getLocalName());
        }
        queryParts.add(SearchUtil.joinQueryPartsOr(subQueryParts));
    }

    private void addTaskMultiStringArrayPartsAndArgs(List<String> queryParts, List<Object> arguments, List<String> list, QName ownerOrganizationName) {
        Pair<String, List<Object>> taskMultiStringExactQueryAndArguments = DbSearchUtil.generateTaskMultiStringArrayQuery(list, ownerOrganizationName);
        if (taskMultiStringExactQueryAndArguments != null) {
            queryParts.add(taskMultiStringExactQueryAndArguments.getFirst());
            arguments.addAll(taskMultiStringExactQueryAndArguments.getSecond());
        }
    }

    public void addTaskMultiStringWordsWildcardPartsAndArgs(List<String> queryParts, List<Object> arguments, List<String> tsQueryInputs, QName... taskProps) {
        if (tsQueryInputs == null || tsQueryInputs.isEmpty()) {
            return;
        }
        List<String> subQueryParts = new ArrayList<String>(tsQueryInputs.size());
        for (String tsQueryInput : tsQueryInputs) {
            addTaskStringWordsWildcardPartsAndArgs(subQueryParts, arguments, tsQueryInput, taskProps);
        }
        queryParts.add(SearchUtil.joinQueryPartsOr(subQueryParts));
    }

    private void addTaskStringWordsWildcardPartsAndArgs(List<String> queryParts, List<Object> arguments, String tsQueryInput, QName... taskProps) {
        if (StringUtils.isBlank(tsQueryInput) || taskProps == null || taskProps.length == 0) {
            return;
        }
        String tsquery = generalService.getTsquery(tsQueryInput);
        if (StringUtils.isNotBlank(tsquery)) {
            queryParts.add(DbSearchUtil.generateTaskStringWordsWildcardQuery(taskProps));
            arguments.add(tsquery);
        }
    }

    private void addTaskMultiStringExactPartsAndArgs(List<String> queryParts, List<Object> arguments, Map<String, Object> props, QName searchProp, QName prop) {
        Pair<String, List<Object>> taskMultiStringExactQueryAndArguments = DbSearchUtil.generateTaskMultiStringExactQuery((List<String>) props.get(searchProp), prop);
        if (taskMultiStringExactQueryAndArguments != null) {
            queryParts.add(taskMultiStringExactQueryAndArguments.getFirst());
            arguments.addAll(taskMultiStringExactQueryAndArguments.getSecond());
        }
    }

    private void addDateQueryPartsAndArguments(List<String> queryParts, List<Object> arguments, Map<String, Object> props, QName beginProp, QName endProp, QName propToSearch) {
        Pair<String, List<Object>> generateTaskDatePropertyRangeQuery = DbSearchUtil.generateTaskDatePropertyRangeQuery(
                (Date) props.get(beginProp),
                (Date) props.get(endProp), propToSearch);
        addQueryPartsAndArguments(queryParts, arguments, generateTaskDatePropertyRangeQuery);
    }

    private void addBooleanQueryPartsAndArguments(List<String> queryParts, List<Object> arguments, Map<String, Object> props, QName prop, QName propToSearch) {
        Pair<String, List<Object>> generateTaskBooleanPropertyQuery = DbSearchUtil.generateTaskPropertyBooleanQuery(prop, (Boolean) props.get(propToSearch));
        addQueryPartsAndArguments(queryParts, arguments, generateTaskBooleanPropertyQuery);
    }

    private void addQueryPartsAndArguments(List<String> queryParts, List<Object> arguments, Pair<String, List<Object>> taskDatePropertyRangeQuery) {
        if (taskDatePropertyRangeQuery != null) {
            queryParts.add(taskDatePropertyRangeQuery.getFirst());
            arguments.addAll(taskDatePropertyRangeQuery.getSecond());
        }
    }

    private Pair<List<Document>, Boolean> searchDocumentsImpl(String query, int limit, String queryName) {
        return searchDocumentsImpl(query, limit, queryName, null);
    }

    private Pair<List<Document>, Boolean> searchDocumentsImpl(String query, int limit, String queryName, Collection<StoreRef> storeRefs) {
        return searchGeneralImpl(query, limit, queryName, new SearchCallback<Document>() {

            @Override
            public Document addResult(ResultSetRow row) {
                return documentService.getDocumentByNodeRef(row.getNodeRef());
            }
        }, storeRefs);
    }

    private Pair<List<Document>, Boolean> searchDocumentsAndCaseTitlesImpl(String query, int limit, String queryName) {
        return searchGeneralImpl(query, limit, queryName, new SearchCallback<Document>() {

            @Override
            public Document addResult(ResultSetRow row) {
                final NodeRef nodeRef = row.getNodeRef();
                final QName resultType = nodeService.getType(nodeRef);
                if (!dictionaryService.isSubClass(resultType, DocumentCommonModel.Types.DOCUMENT)) {
                    final FakeDocument fakeDocument = new FakeDocument(nodeRef);
                    if (log.isDebugEnabled()) {
                        log.debug("fakeDocument=" + fakeDocument);
                    }
                    return fakeDocument;
                }
                return documentService.getDocumentByNodeRef(nodeRef);
            }
        });
    }

    private List<Volume> searchVolumesImpl(String query, int limit, String queryName) {
        return searchGeneralImpl(query, limit, queryName, new SearchCallback<Volume>() {
            @Override
            public Volume addResult(ResultSetRow row) {
                return volumeService.getVolumeByNodeRef(row.getNodeRef());
            }
        }, getAllStoresWithArchivalStoreVOs()).getFirst();
    }

    private List<Series> searchSeriesImpl(String query, int limit, String queryName) {
        return searchGeneralImpl(query, limit, queryName, new SearchCallback<Series>() {
            @Override
            public Series addResult(ResultSetRow row) {
                return seriesService.getSeriesByNodeRef(row.getNodeRef());
            }
        }).getFirst();
    }

    private List<String> searchAdrDocumentTypesImpl(String query, int limit, String queryName) {
        return searchGeneralImpl(query, limit, queryName, new SearchCallback<String>() {
            @Override
            public String addResult(ResultSetRow row) {
                return ((QName) nodeService.getProperty(row.getNodeRef(), AdrModel.Props.DOCUMENT_TYPE)).getLocalName();
            }
        }).getFirst();
    }

    @Override
    public List<NodeRef> searchNodes(String query, int limit, String queryName) {
        return searchNodes(query, limit, queryName, null).getFirst();
    }

    private List<NodeRef> searchNodesFromAllStores(String query, String queryName) {
        return searchNodes(query, -1, queryName, getAllStores()).getFirst();
    }

    private List<StoreRef> getAllStores() {
        if (allStores == null) {
            List<StoreRef> storeList = new ArrayList<StoreRef>(2);
            storeList.add(generalService.getStore());
            storeList.add(generalService.getArchivalsStoreRef());
            allStores = storeList;
        }
        return allStores;
    }

    private List<StoreRef> getAllStoresWithArchivalStoreVOs() {
        if (allStoresWithArchivalStoreVOs == null) {
            allStoresWithArchivalStoreVOs = new ArrayList<StoreRef>(generalService.getAllWithArchivalsStoreRefs());
        }
        return allStoresWithArchivalStoreVOs;
    }

    // START: getters / setters
    public DocumentDynamicService getDocumentDynamicService() {
        if (_documentDynamicService == null) {
            _documentDynamicService = BeanHelper.getDocumentDynamicService();
        }
        return _documentDynamicService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
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

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setLogService(LogService logService) {
        this.logService = logService;
    }

    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    // END: getters / setters

}

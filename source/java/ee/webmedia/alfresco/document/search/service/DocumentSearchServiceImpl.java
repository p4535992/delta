package ee.webmedia.alfresco.document.search.service;

import static ee.webmedia.alfresco.common.search.DbSearchUtil.generateNotQuery;
import static ee.webmedia.alfresco.common.search.DbSearchUtil.generateTaskDatePropertyRangeQuery;
import static ee.webmedia.alfresco.common.search.DbSearchUtil.generateTaskFieldExactQuery;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNotificationService;
import static ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType.INCOMING_LETTER;
import static ee.webmedia.alfresco.utils.SearchUtil.generateAndNotQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateAspectQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateDatePropertyRangeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateLuceneSearchParams;
import static ee.webmedia.alfresco.utils.SearchUtil.generateMultiNodeRefQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateMultiStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateNodeRefQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateNumberPropertyRangeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyBooleanQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyDateQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyExactQuery;
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
import java.util.LinkedHashSet;
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
import org.alfresco.util.EqualsHelper;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.util.Assert;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.adr.model.AdrModel;
import ee.webmedia.alfresco.archivals.model.ArchivalsModel;
import ee.webmedia.alfresco.archivals.model.ArchivalsStoreVO;
import ee.webmedia.alfresco.archivals.web.ArchivalActivity;
import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.casefile.service.CaseFile;
import ee.webmedia.alfresco.casefile.service.CaseFileService;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.classificator.enums.PublishToAdr;
import ee.webmedia.alfresco.classificator.enums.SendMode;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
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
import ee.webmedia.alfresco.document.search.model.AssocSearchObjectType;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.document.search.model.FakeDocument;
import ee.webmedia.alfresco.document.search.web.DocumentDynamicSearchDialog;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.type.service.DocumentTypeHelper;
import ee.webmedia.alfresco.eventplan.model.EventPlanModel;
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
import ee.webmedia.alfresco.volume.model.VolumeOrCaseFile;
import ee.webmedia.alfresco.volume.search.model.VolumeSearchModel;
import ee.webmedia.alfresco.volume.service.VolumeService;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowWithObject;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.search.model.CompoundWorkflowSearchModel;
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
    private boolean finishedIncomingLettersAreNotShown;

    private List<StoreRef> allStores = null;

    @Override
    public List<NodeRef> searchActiveLocks() {
        String query = SearchUtil.joinQueryPartsAnd(SearchUtil.generateAspectQuery(ContentModel.ASPECT_LOCKABLE),
                SearchUtil.generateTypeQuery(DocumentCommonModel.Types.DOCUMENT, ContentModel.TYPE_CONTENT),
                SearchUtil.generatePropertyNotNullQuery(ContentModel.PROP_LOCK_OWNER),
                SearchUtil.generateDatePropertyRangeQuery(new Date(), null, ContentModel.PROP_EXPIRY_DATE));

        return searchNodesFromAllStores(query, "searchActiveLocks");
    }

    @Override
    public List<Document> searchDueContracts() {
        long startTime = System.currentTimeMillis();
        int contractDueDays = parametersService.getLongParameter(Parameters.CONTRACT_DUE_DATE_NOTIFICATION_DAYS).intValue();
        Calendar cal = Calendar.getInstance();
        Date now = cal.getTime();
        cal.add(Calendar.DATE, contractDueDays);
        Date dueDateLimit = cal.getTime();

        String query = generateDocumentSearchQuery(getAllStoresWithArchivalStoreVOs(),
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
                , generatePropertyExactQuery(DocumentCommonModel.Props.FORUM_PARTICIPANTS, authorities));
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
    public List<NodeRef> searchAdrDeletedDocument(NodeRef originalDocumentRef) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(2);
        queryParts.add(generateTypeQuery(AdrModel.Types.ADR_DELETED_DOCUMENT));
        queryParts.add(generateStringExactQuery(originalDocumentRef.toString(), AdrModel.Props.NODEREF));

        String query = joinQueryPartsAnd(queryParts);
        List<NodeRef> results = searchNodes(query, -1, /* queryName */"adrDeletedDocuments");
        if (log.isDebugEnabled()) {
            log.debug("ADR deleted document search total time " + (System.currentTimeMillis() - startTime) + " ms, results " + results.size() //
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

    @Override
    public String generateAdrDocumentSearchQuery(List<String> queryParts, Set<String> documentTypeIds) {
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

        return generateDocumentSearchQueryWithoutRestriction(queryParts);
    }

    @Override
    public List<Document> searchIncomingLetterRegisteredDocuments(String senderRegNumber) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(4);
        queryParts.add(generateStringExactQuery(SystematicDocumentType.INCOMING_LETTER.getId(), DocumentAdminModel.Props.OBJECT_TYPE_ID));
        queryParts.add(generateStringNotEmptyQuery(DocumentCommonModel.Props.REG_DATE_TIME));
        queryParts.add(generateStringExactQuery(senderRegNumber, DocumentSpecificModel.Props.SENDER_REG_NUMBER));

        String query = generateDocumentSearchQuery(queryParts, null);
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
            queryParts.add(0, generateQuickSearchQuery(searchString, null));
            query = SearchUtil.joinQueryPartsAnd(queryParts, false);
        } else {
            query = generateDocumentSearchQuery(queryParts, null);
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

        LinkedHashSet<StoreRef> storeRefs = getAllStoresWithArchivalStoreVOs();
        String query = generateDocumentSearchQuery(queryParts, storeRefs);
        List<Document> results = searchDocumentsImpl(query, -1, /* queryName */"accessRestictionEndsAfterDate", storeRefs).getFirst();
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
        queryParts.add(joinQueryPartsOr(
                generatePropertyBooleanQuery(DocumentCommonModel.Props.DOCUMENT_IS_IMPORTED, false)
                , generatePropertyNullQuery(DocumentCommonModel.Props.DOCUMENT_IS_IMPORTED)));
        return generateDocumentSearchQuery(queryParts, null);
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
    public List<NodeRef> searchSeriesByEventPlan(NodeRef eventPlanRef) {
        String query = SearchUtil.joinQueryPartsAnd(SearchUtil.generateTypeQuery(SeriesModel.Types.SERIES),
                SearchUtil.generateNodeRefQuery(eventPlanRef, SeriesModel.Props.EVENT_PLAN));
        return searchNodesFromAllStores(query, "seriesByEventPlan");
    }

    @Override
    public List<NodeRef> searchVolumesByEventPlan(NodeRef eventPlanRef, String inputTitle, List<String> inputStatus, List<NodeRef> location) {
        Collection<StoreRef> stores = toStoreRefList(location, true);
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(SearchUtil.generateNodeRefQuery(eventPlanRef, EventPlanModel.Props.EVENT_PLAN));
        queryParts.add(generateStringWordsWildcardQuery(inputTitle, VolumeModel.Props.TITLE));
        queryParts.add(SearchUtil.generateMultiStringExactQuery(inputStatus, VolumeModel.Props.STATUS));
        String query = generateVolumeOrCaseFileSearchQuery(queryParts);
        return searchNodes(query, -1, "volumesByEventPlan", stores).getFirst();
    }

    @Override
    public List<Task> searchCurrentUsersTasksInProgress(QName taskType) {
        long startTime = System.currentTimeMillis();
        Pair<List<String>, List<Object>> queryPartsAndArgs = getTaskQuery(taskType, AuthenticationUtil.getRunAsUser(), Status.IN_PROGRESS, false);
        addSubstitutionRestriction(queryPartsAndArgs);
        String query = generateTaskSearchQuery(queryPartsAndArgs.getFirst());
        Pair<List<Task>, Boolean> results = BeanHelper.getWorkflowDbService().searchTasksAllStores(query, queryPartsAndArgs.getSecond(), -1);
        if (log.isDebugEnabled()) {
            log.debug("Current user's and IN_PROGRESS tasks search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results.getFirst();
    }

    @Override
    public List<Task> searchCurrentUsersTaskInProgressWithoutParents(QName taskType, boolean allStoresSearch) {
        long startTime = System.currentTimeMillis();
        Pair<List<String>, List<Object>> queryPartsAndArguments = getTaskQuery(taskType, AuthenticationUtil.getRunAsUser(), Status.IN_PROGRESS, false);
        addSubstitutionRestriction(queryPartsAndArguments);
        String query = generateTaskSearchQuery(queryPartsAndArguments.getFirst());
        Pair<List<Task>, Boolean> results;
        if (allStoresSearch) {
            results = BeanHelper.getWorkflowDbService().searchTasksAllStores(query, queryPartsAndArguments.getSecond(), -1);
        } else {
            results = BeanHelper.getWorkflowDbService().searchTasksMainStore(query, queryPartsAndArguments.getSecond(), -1);
        }
        if (log.isDebugEnabled()) {
            log.debug("Current user's and IN_PROGRESS linked tasks search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results.getFirst();
    }

    @Override
    public List<CompoundWorkflowWithObject> searchCurrentUserCompoundWorkflows() {
        List<NodeRef> compoundWorkflows = searchCurrentUserCompoundWorkflowRefs();
        List<CompoundWorkflowWithObject> result = new ArrayList<CompoundWorkflowWithObject>(compoundWorkflows.size());
        for (NodeRef compoundWorkflowRef : compoundWorkflows) {
            result.add(workflowService.getCompoundWorkflowWithObject(compoundWorkflowRef));
        }
        return result;
    }

    @Override
    public int getCurrentUserCompoundWorkflowsCount() {
        return searchCurrentUserCompoundWorkflowRefs().size();
    }

    private List<NodeRef> searchCurrentUserCompoundWorkflowRefs() {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = Arrays.asList(
                generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW),
                generatePropertyExactQuery(WorkflowCommonModel.Props.STATUS, Arrays.asList(Status.NEW.getName(), Status.IN_PROGRESS.getName(), Status.STOPPED.getName())),
                generateStringExactQuery(AuthenticationUtil.getRunAsUser(), WorkflowCommonModel.Props.OWNER_ID));
        String query = joinQueryPartsAnd(queryParts);
        List<NodeRef> compoundWorkflows = new ArrayList<NodeRef>();
        compoundWorkflows.addAll(searchNodes(query, -1, /* queryName */"currentUserCompoundWorkflowsFromMainStore"));
        compoundWorkflows.addAll(searchNodes(query, -1, /* queryName */"currentUserCompoundWorkflowsFromArchivalsStore",
                Collections.singletonList(generalService.getArchivalsStoreRef())).getFirst());
        if (log.isDebugEnabled()) {
            log.debug("Current user's compound workflows search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return compoundWorkflows;
    }

    private List<NodeRef> searchCurrentUserCaseFilesRefs() {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = Arrays.asList(
                generateTypeQuery(CaseFileModel.Types.CASE_FILE),
                generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE),
                generateStringExactQuery(DocListUnitStatus.OPEN.getValueName(), DocumentDynamicModel.Props.STATUS),
                generateStringExactQuery(AuthenticationUtil.getRunAsUser(), DocumentDynamicModel.Props.OWNER_ID));
        String query = joinQueryPartsAnd(queryParts);
        List<NodeRef> caseFiles = searchNodes(query, -1, /* queryName */"currentUserCaseFileFromMainStore");
        if (log.isDebugEnabled()) {
            log.debug("Current user's case file search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return caseFiles;
    }

    @Override
    public int getCurrentUserCaseFilesCount() {
        return searchCurrentUserCaseFilesRefs().size();
    }

    @Override
    public List<CaseFile> searchCurrentUserCaseFiles() {
        CaseFileService cs = BeanHelper.getCaseFileService();
        List<NodeRef> caseFiles = searchCurrentUserCaseFilesRefs();
        List<CaseFile> result = new ArrayList<CaseFile>(caseFiles.size());
        for (NodeRef caseFileRef : caseFiles) {
            result.add(cs.getCaseFile(caseFileRef));
        }
        return result;
    }

    @Override
    public Map<NodeRef, Pair<String, String>> searchTaskBySendStatusQuery(QName taskType) {
        List<String> queryParts = new ArrayList<String>();
        List<Object> arguments = new ArrayList<Object>();
        addTaskTypeFieldExactQueryPartsAndArguments(queryParts, arguments, taskType);
        addTaskStringExactPartsAndArgs(queryParts, arguments, SendStatus.SENT.toString(), WorkflowSpecificModel.Props.SEND_STATUS);
        queryParts.add(DbSearchUtil.generateTaskPropertyNotNullQuery(WorkflowSpecificModel.Props.SENT_DVK_ID));

        final boolean isReviewTaskSearch = WorkflowSpecificModel.Types.REVIEW_TASK.equals(taskType);
        String query = generateTaskSearchQuery(queryParts);
        if (isReviewTaskSearch) {
            query = joinQueryPartsAnd(query, DbSearchUtil.generateTaskPropertyNotQuery(WorkflowCommonModel.Props.STATUS),
                    DbSearchUtil.generateTaskStringHasLengthQuery(WorkflowSpecificModel.Props.INSTITUTION_CODE),
                    DbSearchUtil.generateTaskStringHasLengthQuery(WorkflowSpecificModel.Props.CREATOR_INSTITUTION_CODE),
                    DbSearchUtil.generateTaskPropertiesNotEqualQuery(WorkflowSpecificModel.Props.INSTITUTION_CODE, WorkflowSpecificModel.Props.CREATOR_INSTITUTION_CODE));
            arguments.add(Status.NEW.getName());
        }

        return BeanHelper.getWorkflowDbService().searchTaskSendStatusInfo(query, arguments);
    }

    @Override
    public List<Task> searchReviewTaskToResendQuery() {

        List<String> queryParts = new ArrayList<String>();
        List<Object> arguments = new ArrayList<Object>();
        addTaskTypeFieldExactQueryPartsAndArguments(queryParts, arguments, WorkflowSpecificModel.Types.REVIEW_TASK);
        queryParts.add(DbSearchUtil.generateTaskPropertyNullQuery(WorkflowSpecificModel.Props.SEND_STATUS));
        queryParts.add(DbSearchUtil.generateTaskPropertyNullQuery(WorkflowSpecificModel.Props.SENT_DVK_ID));
        queryParts.add(DbSearchUtil.generateNotQuery(DbSearchUtil.generateTaskPropertyExactQuery(WorkflowCommonModel.Props.STATUS)));
        arguments.add(Status.NEW.getName());

        String query = generateTaskSearchQuery(queryParts);
        List<Task> tasks = BeanHelper.getWorkflowDbService().searchTasksAllStores(query, arguments, -1).getFirst();

        for (Iterator<Task> i = tasks.iterator(); i.hasNext();) {
            Task task = i.next();
            String institutionCode = task.getInstitutionCode();
            String creatorInstitutionCode = task.getCreatorInstitutionCode();
            if ((StringUtils.isBlank(institutionCode) || StringUtils.isBlank(creatorInstitutionCode)) || EqualsHelper.nullSafeEquals(institutionCode, creatorInstitutionCode)) {
                i.remove();
            }
        }
        List<Task> tasksWithParents = new ArrayList<Task>();
        for (Task task : tasks) {
            tasksWithParents.add(workflowService.getTask(task.getNodeRef(), true));
        }

        return tasksWithParents;
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
        queryParts.add(joinQueryPartsOr(queryDvkIdParts));
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
            NodeRef workflowNodeRef = task.getWorkflowNodeRef();
            if (workflowNodeRef == null) {
                continue;
            }
            NodeRef compoundWorkflowRef = nodeService.getPrimaryParent(workflowNodeRef).getParentRef();
            NodeRef docRef = workflowService.getCompoundWorkflow(compoundWorkflowRef).getParent();
            if (nodeService.hasAspect(docRef, DocumentCommonModel.Aspects.NOT_EDITABLE)) {
                return task;
            }
        }
        return null;
    }

    @Override
    public NodeRef getIndependentCompoundWorkflowByProcedureId(String procedureId) {
        String query = joinQueryPartsAnd(Arrays.asList(generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW),
                generateStringExactQuery(procedureId, WorkflowCommonModel.Props.PROCEDURE_ID),
                generateStringExactQuery(CompoundWorkflowType.INDEPENDENT_WORKFLOW.name(), WorkflowCommonModel.Props.TYPE)));
        List<NodeRef> result = searchNodes(query, -1, "independentCompoundWorkflowByProcedureId");
        if (!result.isEmpty()) {
            return result.get(0);
        }
        return null;
    }

    @Override
    public List<Volume> searchVolumesDispositionedAfterDate(Date dispositionDate) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateDatePropertyRangeQuery(Calendar.getInstance().getTime(), dispositionDate, EventPlanModel.Props.RETAIN_UNTIL_DATE));
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

        queryParts.add(generateNotQuery(generateTaskFieldExactQuery(DbSearchUtil.TASK_TYPE_FIELD)));
        arguments.add(WorkflowSpecificModel.Types.LINKED_REVIEW_TASK.getLocalName());
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
        boolean einvoiceEnabled = BeanHelper.getEInvoiceService().isEinvoiceEnabled();
        String accountantsGroup = userService.getAccountantsGroup();
        for (String result : results) {
            if (!einvoiceEnabled && accountantsGroup.equals(result)) {
                continue;
            }
            if (withAdminsAndDocManagers || (!userService.getAdministratorsGroup().equals(result) && !userService.getDocumentManagersGroup().equals(result))) {
                authorities.add(userService.getAuthority(result));
            }
            if (authorities.size() == limit) {
                break;
            }
        }
        return authorities;
    }

    private List<String> getAuthorityNameCollection(List<NodeRef> authorityRefs, String groupName) {
        List<String> authorityNames = new ArrayList<String>();
        if (StringUtils.isBlank(groupName)) {
            return authorityNames;
        }
        for (NodeRef authorityRef : authorityRefs) {
            String authorityName = (String) nodeService.getProperty(authorityRef, ContentModel.PROP_AUTHORITY_DISPLAY_NAME);
            if (groupName.equalsIgnoreCase(authorityName)) {
                authorityNames.add((String) nodeService.getProperty(authorityRef, ContentModel.PROP_AUTHORITY_NAME));
            }
        }
        return authorityNames;
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
    public boolean isMatchAllStoresWithTrashcan(String query) {
        return isMatch(query, true, "isMatch");
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
        List<ResultSet> results = doSearches(query, 1, queryName, generalService.getAllStoreRefsWithTrashCan());
        try {
            for (ResultSet result : results) {
                if (result.length() > 0) {
                    return true;
                }
            }
            return false;
        } finally {
            for (ResultSet resultSet : results) {
                try {
                    resultSet.close();
                } catch (Exception e) {
                    log.error("Closing resultSet failed, continuing", e);
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

    @SuppressWarnings("unchecked")
    @Override
    public List<String> searchAuthorityGroupsByExactName(String groupName) {
        long startTime = System.currentTimeMillis();
        String query = joinQueryPartsAnd(generateTypeQuery(ContentModel.TYPE_AUTHORITY_CONTAINER),
                generateStringExactQuery(groupName, ContentModel.PROP_AUTHORITY_DISPLAY_NAME));
        List<NodeRef> results = searchNodes(query, -1, /* queryName */"authorityGroups");
        if (log.isDebugEnabled()) {
            log.debug("Authority groups search total time " + (System.currentTimeMillis() - startTime) + " ms, results " + results.size() //
                    + ", query: " + query);
        }
        List<String> authorityNames = new ArrayList<String>();
        authorityNames.addAll(getAuthorityNameCollection(results, groupName));
        return authorityNames;
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
            Pair<List<Task>, Boolean> taskResults = BeanHelper.getWorkflowDbService().searchTasksAllStores(queryAndArguments.getFirst(), queryAndArguments.getSecond(), limit);
            for (Task task : taskResults.getFirst()) {
                NodeRef workflowNodeRef = task.getWorkflowNodeRef();
                Node workflow = null;
                Node document = null;
                if (workflowNodeRef != null) {
                    workflow = generalService.fetchNode(workflowNodeRef);
                    NodeRef compoundWorkflowRef = nodeService.getPrimaryParent(workflow.getNodeRef()).getParentRef();
                    if (CompoundWorkflowType.DOCUMENT_WORKFLOW == BeanHelper.getWorkflowService().getCompoundWorkflowType(compoundWorkflowRef)) {
                        document = generalService.fetchNode(nodeService.getPrimaryParent(compoundWorkflowRef).getParentRef());
                    }
                }
                taskInfos.add(new TaskInfo(task.getNode(), workflow, document));
            }
            resultLimited = taskResults.getSecond();
        }
        Map<NodeRef, CompoundWorkflow> compoundWorkflows = new HashMap<NodeRef, CompoundWorkflow>(taskInfos.size());
        for (TaskInfo taskInfo : taskInfos) {
            if (taskInfo.getWorkflow() == null) {
                continue;
            }
            NodeRef compoundWorkflowRef = nodeService.getPrimaryParent(taskInfo.getWorkflow().getNodeRef()).getParentRef();
            CompoundWorkflow compoundWorkflow = compoundWorkflows.get(compoundWorkflowRef);
            if (compoundWorkflow == null) {
                compoundWorkflow = workflowService.getCompoundWorkflow(compoundWorkflowRef);
                compoundWorkflow.setNumberOfDocuments(workflowService.getCompoundWorkflowDocumentCount(compoundWorkflowRef));
                compoundWorkflows.put(compoundWorkflowRef, compoundWorkflow);
            }
            taskInfo.setCompoundWorkflow(compoundWorkflow);
        }
        if (log.isDebugEnabled()) {
            log.debug("Tasks search total time " + (System.currentTimeMillis() - startTime) + " ms");
        }
        return new Pair<List<TaskInfo>, Boolean>(taskInfos, resultLimited);
    }

    @Override
    public NodeRef searchLinkedReviewTaskByOriginalNoderefId(String noderefId) {
        long startTime = System.currentTimeMillis();
        if (StringUtils.isBlank(noderefId)) {
            return null;
        }
        List<String> queryParts = new ArrayList<String>();
        List<Object> arguments = new ArrayList<Object>();
        addTaskTypeFieldExactQueryPartsAndArguments(queryParts, arguments, WorkflowSpecificModel.Types.LINKED_REVIEW_TASK);
        addTaskStringExactPartsAndArgs(queryParts, arguments, noderefId, WorkflowSpecificModel.Props.ORIGINAL_NODEREF_ID);
        String query = generateTaskSearchQuery(queryParts);
        List<NodeRef> result = BeanHelper.getWorkflowDbService().searchTaskNodeRefs(query, arguments);
        if (log.isDebugEnabled()) {
            log.debug("Linked review task search total time " + (System.currentTimeMillis() - startTime) + " ms");
        }
        if (result.isEmpty()) {
            return null;
        }
        if (result.size() > 1) {
            log.error("There is more than one linkedReviewTask with originalNoderefId=" + noderefId + "; updating first task, all tasks nodeRefs=" + result);
        }
        return result.get(0);
    }

    @Override
    public Pair<List<CompoundWorkflow>, Boolean> searchCompoundWorkflows(Node filter, int limit) {
        long startTime = System.currentTimeMillis();
        String query = generateCompoundWorkflowSearchQuery(filter);
        Pair<List<CompoundWorkflow>, Boolean> results = searchCompoundWorkflowsImpl(query, limit, /* queryName */"compoundWorkflowByFilter");
        if (log.isDebugEnabled()) {
            log.debug("CompoundWorkflow search total time " + (System.currentTimeMillis() - startTime) + " ms");
        }
        return results;
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
        if (!getNotificationService().isSubstitutionTaskEndDateRestricted()) {
            return;
        }
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

    private String generateDocumentsForRegisteringQuery() {
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateStringExactQuery(DocumentStatus.WORKING.getValueName(), DocumentCommonModel.Props.DOC_STATUS));
        queryParts.add(generateStringNullQuery(DocumentCommonModel.Props.REG_DATE_TIME));
        queryParts.add(generatePropertyBooleanQuery(DocumentCommonModel.Props.SEARCHABLE_HAS_ALL_FINISHED_COMPOUND_WORKFLOWS, true));
        return generateDocumentSearchQuery(queryParts, null);
    }

    @Override
    public List<Document> searchDocumentsForRegistering() {
        long startTime = System.currentTimeMillis();
        String query = generateDocumentsForRegisteringQuery();
        List<Document> results = searchDocumentsImpl(query, -1, /* queryName */"documentsForRegistering").getFirst();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Documents for registering search total time %d ms, query: %s" //
                    , (System.currentTimeMillis() - startTime), query));
        }
        return results;
    }

    @Override
    public int getCountOfDocumentsForRegistering() {
        long startTime = System.currentTimeMillis();
        String query = generateDocumentsForRegisteringQuery();
        int count = searchNodes(query, -1, /* queryName */"documentsForRegisteringCount").size();

        if (log.isDebugEnabled()) {
            log.debug(String.format("Documents for registering count search total time %d ms, query: %s" //
                    , (System.currentTimeMillis() - startTime), query));
        }
        return count;
    }

    private void filterBySendMode(List<Document> results, Map<String, Object> properties) {
        @SuppressWarnings("unchecked")
        List<String> sendModes = (List<String>) properties.get(DocumentSearchModel.Props.SEND_MODE.toString());

        if (sendModes != null && !sendModes.isEmpty()) {
            for (Iterator<Document> it = results.iterator(); it.hasNext();) {
                Document doc = it.next();
                List<String> modes = doc.getSendModesAsList();
                if (!CollectionUtils.containsAny(sendModes, modes)) {
                    it.remove();
                }
            }
        }
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
        String query = generateDocumentSearchQuery(filter, storeRefs);
        if (StringUtils.isBlank(query)) {
            throw new UnableToPerformException(UnableToPerformException.MessageSeverity.INFO, "docSearch_error_noInput");
        }
        try {
            Pair<List<Document>, Boolean> results = searchDocumentsImpl(query, limit, /* queryName */"documentsByFilter", storeRefs);
            if (results != null) {
                filterByStructUnit(results.getFirst(), filter);
            }
            filterBySendMode(results.getFirst(), properties);
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
                        List<String> documentStructUnits = (List<String>) RepoUtil.flatten((Serializable) it.next().getProperties().get(propQName));
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
        NodeRef seriesRef = generalService.getAncestorNodeRefWithType(parentRef, SeriesModel.Types.SERIES);
        List<NodeRef> restrictedSeries = new ArrayList<NodeRef>(1);
        if (seriesRef != null && Boolean.FALSE.equals(nodeService.getProperty(seriesRef, SeriesModel.Props.DOCUMENTS_VISIBLE_FOR_USERS_WITHOUT_ACCESS))) {
            restrictedSeries.add(seriesRef);
        }

        String query = generateDocumentSearchQueryWithoutRestriction(new ArrayList<String>(Arrays.asList(
                SearchUtil.generateParentQuery(parentRef),
                SearchUtil.generateDocAccess(restrictedSeries, null))));

        if (log.isDebugEnabled()) {
            log.debug("Documents by parent query: " + query);
        }

        return searchNodes(query, limit, "allDocumentsByParentRef", Collections.singletonList(parentRef.getStoreRef()));
    }

    @Override
    public Pair<List<VolumeOrCaseFile>, Boolean> searchVolumes(Node filter, int limit) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> properties = filter.getProperties();
        List<StoreRef> storeRefs = getVolumeSearchStoreRefs(properties);
        String query = generateVolumeSearchQuery(filter);
        if (StringUtils.isBlank(query)) {
            throw new UnableToPerformException(UnableToPerformException.MessageSeverity.INFO, "volSearch_error_noInput");
        }
        try {
            Pair<List<VolumeOrCaseFile>, Boolean> results = searchVolumesImpl(query, limit, /* queryName */"volumesByFilter", storeRefs);
            if (log.isDebugEnabled()) {
                log.debug("Volumes search total time " + (System.currentTimeMillis() - startTime) + " ms");
            }
            return results;
        } catch (RuntimeException e) {
            handleFilterSearchFailure(properties, query, "Volume search failed: ", e);
        }
        // this is never reached because handleVolumeSearchFailure throws RuntimeException
        return null;
    }

    public void handleFilterSearchFailure(Map<String, Object> properties, String query, String logMessagePrefix, RuntimeException e) {
        Map<QName, Serializable> filterProps = RepoUtil.getNotEmptyProperties(RepoUtil.toQNameProperties(properties));
        log.error(logMessagePrefix
                + e.getMessage()
                + "\n  searchFilter=" + WmNode.toString(filterProps, namespaceService)
                + "\n  query=" + query, e);
        throw e;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<StoreRef> getVolumeSearchStoreRefs(Map<String, Object> properties) {
        List storeFunctionRootNodeRefs = (List) properties.get(VolumeSearchModel.Props.STORE);
        boolean storRefsNotNull = storeFunctionRootNodeRefs != null && !storeFunctionRootNodeRefs.isEmpty();
        List<StoreRef> storeRefs = new ArrayList<StoreRef>(storRefsNotNull ? storeFunctionRootNodeRefs.size() : 0);
        if (storRefsNotNull) {
            if (storeFunctionRootNodeRefs.get(0) instanceof String) {
                storeFunctionRootNodeRefs = convertStringsToNodeRefs(storeFunctionRootNodeRefs);
            }
            for (NodeRef nodeRef : (List<NodeRef>) storeFunctionRootNodeRefs) {
                storeRefs.add(nodeRef.getStoreRef());
            }
        }
        return storeRefs;
    }

    private List<NodeRef> convertStringsToNodeRefs(List<String> stores) {
        List<NodeRef> storeFunctionRootNodeRefs = new ArrayList<NodeRef>();
        for (String storeStr : stores) {
            storeFunctionRootNodeRefs.add(new NodeRef(storeStr));
        }
        return storeFunctionRootNodeRefs;
    }

    @Override
    public List<Volume> searchVolumesForArchiveList(Node filter, List<NodeRef> defaultStores) {
        return searchVolumesForArchiveList(filter, false, false, defaultStores);
    }

    @Override
    public List<Volume> searchVolumesForArchiveList(Node filter, boolean hasArchivalValueOrRetainPermanent, boolean isWaitingForDestructionQuery, List<NodeRef> defaultStores) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> properties = filter.getProperties();
        List<StoreRef> storeRefs;
        @SuppressWarnings("unchecked")
        List<NodeRef> storeNodeRefs = (List<NodeRef>) properties.get(VolumeSearchModel.Props.STORE.toString());
        if (storeNodeRefs == null || storeNodeRefs.isEmpty()) {
            storeRefs = new ArrayList<StoreRef>();
            for (NodeRef nodeRef : defaultStores) {
                storeRefs.add(nodeRef.getStoreRef());
            }
        } else {
            storeRefs = getVolumeSearchStoreRefs(properties);
        }
        List<String> queryParts = getVolumeArchiveListSearchQueryParts(filter);
        if (hasArchivalValueOrRetainPermanent) {
            queryParts.add(joinQueryPartsOr(generatePropertyBooleanQuery(EventPlanModel.Props.HAS_ARCHIVAL_VALUE, Boolean.TRUE),
                    generatePropertyBooleanQuery(EventPlanModel.Props.RETAIN_PERMANENT, Boolean.TRUE)));
        }

        if (isWaitingForDestructionQuery) {
            queryParts.add(generateVolumeWaitingForDestructionQuery());
        }
        String query = generateVolumeOrCaseFileSearchQuery(queryParts);
        try {
            Pair<List<Volume>, Boolean> results = searchVolumesAndCaseFilesImpl(query, -1, /* queryName */"volumesForArchiveList", storeRefs);
            if (log.isDebugEnabled()) {
                log.debug("Volumes archive list search total time " + (System.currentTimeMillis() - startTime) + " ms");
            }
            return results.getFirst();
        } catch (RuntimeException e) {
            handleFilterSearchFailure(properties, query, "Volume search failed: ", e);
        }
        // This is the place to have fun without fearing to be caught, beacause handleFilterSearchFailure will throw exception anyhow
        // Still, if it should not do so...
        throw new RuntimeException("OH MY GOD!! This IS happening...!!! The alians ARE inviding the earth at the very moment...!!!");
    }

    @Override
    public List<Pair<NodeRef, String>> getAllVolumeSearchStores() {
        List<Pair<NodeRef, String>> storesWithName = new ArrayList<Pair<NodeRef, String>>();
        storesWithName.add(new Pair<NodeRef, String>(BeanHelper.getFunctionsService().getFunctionsRoot(), MessageUtil.getMessage("functions_title")));
        for (ArchivalsStoreVO archivalsStoreVO : getGeneralService().getArchivalsStoreVOs()) {
            storesWithName.add(new Pair<NodeRef, String>(archivalsStoreVO.getNodeRef(), archivalsStoreVO.getTitle()));
        }
        return storesWithName;
    }

    private String generateVolumeWaitingForDestructionQuery() {
        return joinQueryPartsOr(Arrays.asList(
                joinQueryPartsAnd(generatePropertyBooleanQuery(EventPlanModel.Props.RETAIN_PERMANENT, Boolean.TRUE),
                        generatePropertyBooleanQuery(EventPlanModel.Props.TRANSFER_CONFIRMED, Boolean.TRUE)),
                joinQueryPartsAnd(generatePropertyBooleanQuery(EventPlanModel.Props.HAS_ARCHIVAL_VALUE, Boolean.TRUE),
                        generatePropertyBooleanQuery(EventPlanModel.Props.TRANSFER_CONFIRMED, Boolean.TRUE)),
                joinQueryPartsAnd(generatePropertyBooleanQuery(EventPlanModel.Props.RETAIN_PERMANENT, Boolean.FALSE),
                        generatePropertyBooleanQuery(EventPlanModel.Props.HAS_ARCHIVAL_VALUE, Boolean.FALSE),
                        generateDatePropertyRangeQuery(null, new Date(), EventPlanModel.Props.RETAIN_UNTIL_DATE))));

    }

    private List<String> getVolumeArchiveListSearchQueryParts(Node filter) {
        List<String> queryParts = new ArrayList<String>(10);
        Map<QName, Serializable> props = RepoUtil.toQNameProperties(filter.getProperties(), true);

        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(VolumeSearchModel.Props.VALID_TO), (Date) props.get(VolumeSearchModel.Props.VALID_TO_END_DATE),
                VolumeModel.Props.VALID_TO));
        queryParts.add(generateNodeRefQuery((NodeRef) props.get(VolumeSearchModel.Props.EVENT_PLAN), EventPlanModel.Props.EVENT_PLAN));

        queryParts.add(generateMultiStringExactQuery((List<String>) props.get(VolumeSearchModel.Props.STATUS), VolumeModel.Props.STATUS));
        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(VolumeSearchModel.Props.NEXT_EVENT_DATE),
                (Date) props.get(VolumeSearchModel.Props.NEXT_EVENT_DATE_END_DATE), EventPlanModel.Props.NEXT_EVENT_DATE));
        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(VolumeSearchModel.Props.RETAIN_UNTIL_DATE),
                (Date) props.get(VolumeSearchModel.Props.RETAIN_UNTIL_DATE_END_DATE), EventPlanModel.Props.RETAIN_UNTIL_DATE));

        addBooleanPropQuery(queryParts, props, VolumeSearchModel.Props.HAS_ARCHIVAL_VALUE, EventPlanModel.Props.HAS_ARCHIVAL_VALUE);
        addBooleanPropQuery(queryParts, props, VolumeSearchModel.Props.RETAIN_PERMANENT, EventPlanModel.Props.RETAIN_PERMANENT);

        addBooleanPropQuery(queryParts, props, VolumeSearchModel.Props.MARKED_FOR_TRANSFER, EventPlanModel.Props.MARKED_FOR_TRANSFER);
        addBooleanPropQuery(queryParts, props, VolumeSearchModel.Props.EXPORTED_FOR_UAM, EventPlanModel.Props.EXPORTED_FOR_UAM);

        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(VolumeSearchModel.Props.EXPORTED_FOR_UAM_DATE_TIME),
                (Date) props.get(VolumeSearchModel.Props.EXPORTED_FOR_UAM_DATE_TIME_END_DATE), EventPlanModel.Props.EXPORTED_FOR_UAM_DATE_TIME));
        queryParts.add(generateMultiStringExactQuery((List<String>) props.get(VolumeSearchModel.Props.NEXT_EVENT), EventPlanModel.Props.NEXT_EVENT));
        addBooleanPropQuery(queryParts, props, VolumeSearchModel.Props.MARKED_FOR_DESTRUCTION, EventPlanModel.Props.MARKED_FOR_DESTRUCTION);
        addBooleanPropQuery(queryParts, props, VolumeSearchModel.Props.DISPOSAL_ACT_CREATED, EventPlanModel.Props.DISPOSAL_ACT_CREATED);

        addBooleanPropQuery(queryParts, props, VolumeSearchModel.Props.IS_APPRAISED, EventPlanModel.Props.IS_APPRAISED);
        addBooleanPropQuery(queryParts, props, VolumeSearchModel.Props.TRANSFER_CONFIRMED, EventPlanModel.Props.TRANSFER_CONFIRMED);

        return queryParts;
    }

    private void addBooleanPropQuery(List<String> queryParts, Map<QName, Serializable> props, QName filterPropName, QName volumeProp) {
        Boolean value = (Boolean) props.get(filterPropName);
        if (value != null) {
            queryParts.add(generatePropertyBooleanQuery(volumeProp, value));
        }
    }

    @Override
    public List<ArchivalActivity> searchArchivalActivities(Node filter) {
        long startTime = System.currentTimeMillis();
        Map<QName, Serializable> props = RepoUtil.toQNameProperties(filter.getProperties());
        String query = joinQueryPartsAnd(
                generateTypeQuery(ArchivalsModel.Types.ARCHIVAL_ACTIVITY),
                generateDatePropertyRangeQuery((Date) props.get(ArchivalsModel.Props.FILTER_CREATED), (Date) props.get(ArchivalsModel.Props.FILTER_CREATED_END_DATE),
                        ArchivalsModel.Props.CREATED),
                generateStringExactQuery((String) props.get(ArchivalsModel.Props.FILTER_ACTIVITY_TYPE), ArchivalsModel.Props.ACTIVITY_TYPE));
        try {
            List<ArchivalActivity> results = searchArchivalActivitiesImpl(query, -1, /* queryName */"archivalActivitiesByFilter",
                    Collections.singletonList(generalService.getStore()));
            if (log.isDebugEnabled()) {
                log.debug("Archival activities search total time " + (System.currentTimeMillis() - startTime) + " ms");
            }
            return results;
        } catch (RuntimeException e) {
            handleFilterSearchFailure(filter.getProperties(), query, "Archival activities search failed:", e);
        }
        return null;
    }

    @Override
    public List<NodeRef> searchDocumentsForReport(Node filter, StoreRef storeRef, String userId) {
        long startTime = System.currentTimeMillis();
        Assert.notNull(storeRef);
        String query = generateDocumentSearchQuery(filter, Collections.singletonList(storeRef), userId);
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
    public List<NodeRef> searchVolumesForReport(Node filter) {
        long startTime = System.currentTimeMillis();
        String query = generateVolumeSearchQuery(filter);
        @SuppressWarnings("unchecked")
        List<String> storeListProp = (List<String>) filter.getProperties().get(VolumeSearchModel.Props.STORE);
        List<StoreRef> storeRefs = new ArrayList<StoreRef>();
        if (storeListProp == null || storeListProp.isEmpty()) {
            storeRefs.add(generalService.getStore());
        } else {
            storeRefs = toStoreRefList(storeListProp);
        }
        if (StringUtils.isBlank(query)) {
            // this should never happen, web layer must ensure we have some input
            throw new UnableToPerformException(UnableToPerformException.MessageSeverity.INFO, "volSearch_error_noInput");
        }
        List<NodeRef> results = searchNodes(query, -1, /* queryName */"searchVolumesForReport", storeRefs).getFirst();
        if (log.isDebugEnabled()) {
            log.debug("Volume search total time " + (System.currentTimeMillis() - startTime) + " ms");
        }
        return results;
    }

    @Override
    public List<StoreRef> getStoresFromDocumentReportFilter(Map<String, Object> properties) {
        @SuppressWarnings("unchecked")
        List<String> storeFunctionRootNodeRefs = (List<String>) properties.get(DocumentSearchModel.Props.STORE);
        return toStoreRefList(storeFunctionRootNodeRefs);
    }

    private List<StoreRef> toStoreRefList(List<String> storeListProp) {
        List<StoreRef> storeRefs = new ArrayList<StoreRef>(storeListProp.size());
        for (String nodeRef : storeListProp) {
            storeRefs.add(new NodeRef(nodeRef).getStoreRef());
        }
        if (storeRefs.isEmpty()) {
            storeRefs.add(generalService.getStore());
        }
        return storeRefs;
    }

    private List<StoreRef> toStoreRefList(List<NodeRef> storeListProp, boolean defaultToAllStoresWithArchivalStoreVOs) {
        Set<StoreRef> storeRefs = new LinkedHashSet<StoreRef>(storeListProp.size());
        for (NodeRef nodeRef : storeListProp) {
            storeRefs.add(nodeRef.getStoreRef());
        }
        if (storeRefs.isEmpty()) {
            if (defaultToAllStoresWithArchivalStoreVOs) {
                storeRefs.addAll(getAllStoresWithArchivalStoreVOs());
            } else {
                storeRefs.add(generalService.getStore());
            }
        }
        return new ArrayList<StoreRef>(storeRefs);
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
        logService.addLogEntry(LogEntry.create(LogObject.SEARCH_DOCUMENTS, userService, "applog_search_docs_quick", searchValue));
        return searchDocumentsAndOrCases(generateQuickSearchQuery(searchValue, containerNodeRef, null), searchValue, false, limit);
    }

    @Override
    public String generateDeletedSearchQuery(String searchValue, NodeRef containerNodeRef) {
        List<String> queryParts = generateQuickSearchDocumentQuery(parseQuickSearchWords(searchValue, 1));
        if (isBlank(queryParts)) {
            return null;
        }
        return joinQueryPartsOr(
                generateDocumentSearchQueryWithoutRestriction(queryParts),
                generateStringWordsWildcardQuery(searchValue, ContentModel.PROP_NAME));
    }

    @Override
    public List<AssocBlockObject> searchAssocObjects(Node objectFilter) {
        Map<String, Object> filterProps = objectFilter.getProperties();
        String objectTypeStr = (String) filterProps.get(DocumentSearchModel.Props.OBJECT_TYPE);
        AssocSearchObjectType objectType = null;
        if (StringUtils.isNotBlank(objectTypeStr)) {
            objectType = AssocSearchObjectType.valueOf(objectTypeStr);
        }
        Date docCreatedStart = (Date) filterProps.get(DocumentSearchModel.Props.DOCUMENT_CREATED);
        Date docCreatedEnd = (Date) filterProps.get(DocumentSearchModel.Props.DOCUMENT_CREATED_END_DATE);
        boolean docCreatedIsNull = docCreatedStart == null && docCreatedEnd == null;

        Date docRegisteredStart = (Date) filterProps.get(DocumentCommonModel.Props.REG_DATE_TIME);
        Date docRegisteredEnd = (Date) filterProps.get(DateGenerator.getEndDateQName(DocumentCommonModel.Props.REG_DATE_TIME));
        boolean docRegisteredIsNull = docRegisteredStart == null && docRegisteredEnd == null;
        boolean volumeIsNull = filterProps.get(DocumentCommonModel.Props.VOLUME) == null;
        @SuppressWarnings("unchecked")
        List<String> documentTypes = (List<String>) filterProps.get(DocumentSearchModel.Props.DOCUMENT_TYPE);
        boolean docTypeIsNull = documentTypes == null || documentTypes.isEmpty();
        String searchString = (String) filterProps.get(DocumentSearchModel.Props.INPUT);
        String objectTitle = (String) filterProps.get(DocumentSearchModel.Props.OBJECT_TITLE);
        Object function = filterProps.get(DocumentCommonModel.Props.FUNCTION);
        Object series = filterProps.get(DocumentCommonModel.Props.SERIES);
        ArrayList<NodeRef> stores = (ArrayList<NodeRef>) filterProps.get(DocumentDynamicSearchDialog.SELECTED_STORES);
        Set<StoreRef> storeRefs = null;
        if (stores != null && !stores.isEmpty()) {
            storeRefs = new HashSet<StoreRef>();
            for (NodeRef ref : stores) {
                storeRefs.add(ref.getStoreRef());
            }
        }
        if (objectType == null
                || (docTypeIsNull && volumeIsNull && docCreatedIsNull && docRegisteredIsNull
                        && StringUtils.isBlank(searchString) && StringUtils.isBlank(objectTitle)
                        && function == null && series == null)) {
            throw new UnableToPerformException("docSearch_error_noInput");
        }
        ArrayList<AssocBlockObject> results = new ArrayList<AssocBlockObject>();
        if (AssocSearchObjectType.DOCUMENT != objectType && !(docCreatedIsNull && docRegisteredIsNull && docTypeIsNull)) {
            return results;
        }
        String documentModelQuickSearchQuery = generateQuickSearchQuery(searchString, null);
        String functionQuery = generateNodeRefQuery((NodeRef) function, DocumentCommonModel.Props.FUNCTION);
        String seriesQuery = generateNodeRefQuery((NodeRef) series, DocumentCommonModel.Props.SERIES);
        String volumeQuery = generateNodeRefQuery((NodeRef) filterProps.get(DocumentCommonModel.Props.VOLUME), DocumentCommonModel.Props.VOLUME);
        if (AssocSearchObjectType.DOCUMENT == objectType) {
            String query = joinQueryPartsAnd(Arrays.asList(
                    generateTypeQuery(DocumentCommonModel.Types.DOCUMENT),
                    generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE),
                    documentModelQuickSearchQuery,
                    generateStringWordsWildcardQuery(objectTitle, DocumentCommonModel.Props.DOC_NAME),
                    generateDatePropertyRangeQuery(docCreatedStart, docCreatedEnd, ContentModel.PROP_CREATED),
                    generateDatePropertyRangeQuery(docRegisteredStart, docRegisteredEnd, DocumentCommonModel.Props.REG_DATE_TIME),
                    generateMultiStringExactQuery(documentTypes, DocumentAdminModel.Props.OBJECT_TYPE_ID),
                    functionQuery, seriesQuery, volumeQuery));
            List<NodeRef> resultNodeRefs = searchAssociatedObjects(query, storeRefs);
            for (NodeRef nodeRef : resultNodeRefs) {
                results.add(new AssocBlockObject(documentService.getDocumentByNodeRef(nodeRef)));
            }
        } else if (AssocSearchObjectType.CASE == objectType) {
            String query = joinQueryPartsAnd(Arrays.asList(
                    generateTypeQuery(CaseModel.Types.CASE),
                    generateStringWordsWildcardQuery(searchString, CaseModel.Props.TITLE), // There are no other meaningful properties besides TITLE to perform "quickSearch" on
                    generateStringWordsWildcardQuery(objectTitle, CaseModel.Props.TITLE),
                    functionQuery, seriesQuery, volumeQuery));
            List<NodeRef> resultNodeRefs = searchAssociatedObjects(query, storeRefs);
            for (NodeRef nodeRef : resultNodeRefs) {
                results.add(new AssocBlockObject(BeanHelper.getCaseService().getCaseByNoderef(nodeRef)));
            }
        } else if (AssocSearchObjectType.VOLUME == objectType) {
            String query = joinQueryPartsAnd(Arrays.asList(
                    generateTypeQuery(VolumeModel.Types.VOLUME),
                    documentModelQuickSearchQuery,
                    generateStringWordsWildcardQuery(objectTitle, VolumeModel.Props.TITLE),
                    functionQuery, seriesQuery, volumeQuery));
            List<NodeRef> resultNodeRefs = searchAssociatedObjects(query, storeRefs);
            for (NodeRef nodeRef : resultNodeRefs) {
                results.add(new AssocBlockObject(BeanHelper.getVolumeService().getVolumeByNodeRef(nodeRef)));
            }
            query = joinQueryPartsAnd(Arrays.asList(
                    generateTypeQuery(CaseFileModel.Types.CASE_FILE),
                    generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE),
                    documentModelQuickSearchQuery,
                    generateStringWordsWildcardQuery(objectTitle, DocumentDynamicModel.Props.TITLE),
                    functionQuery, seriesQuery, volumeQuery));
            resultNodeRefs = searchAssociatedObjects(query, storeRefs);
            for (NodeRef nodeRef : resultNodeRefs) {
                results.add(new AssocBlockObject(BeanHelper.getVolumeService().getVolumeByNodeRef(nodeRef)));
            }
        }

        return results;
    }

    private List<NodeRef> searchAssociatedObjects(String query, Set<StoreRef> storeRefs) {
        int limit = parametersService.getLongParameter(Parameters.MAX_SEARCH_RESULT_ROWS).intValue();
        return searchNodes(query, limit, "searchAssocObjects", storeRefs).getFirst();
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
    public List<NodeRef> searchCompoundWorkflowsOwnerId(String ownerId, boolean isPreviousOwnerId) {
        long startTime = System.currentTimeMillis();
        String query = getCompoundWorkflowOwnerQuery(ownerId, isPreviousOwnerId);
        List<NodeRef> results = searchNodesFromAllStores(query, /* queryName */"newCompoundWorkflowsByOwnerId");
        if (log.isDebugEnabled()) {
            log.debug("User's " + ownerId + " new cwf search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public List<NodeRef> searchOpenCaseFilesOwnerId(String ownerId, boolean isPreviousOwnerId) {
        long startTime = System.currentTimeMillis();
        String query = getOpenCaseFileOwnerQuery(ownerId, isPreviousOwnerId);
        List<NodeRef> results = searchNodesFromAllStores(query, /* queryName */"openCaseFilesByOwnerId");
        if (log.isDebugEnabled()) {
            log.debug("User's " + ownerId + " open caseFile search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
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

    @Override
    public boolean isFieldByOriginalIdExists(String fieldId) {
        String query = SearchUtil.joinQueryPartsAnd(SearchUtil.generatePropertyExactQuery(DocumentAdminModel.Props.FIELD_ID, fieldId),
                SearchUtil.generateTypeQuery(DocumentAdminModel.Types.FIELD));
        query = SearchUtil.generateAndNotQuery(query, SearchUtil.generateTypeQuery(DocumentAdminModel.Types.FIELD_DEFINITION));
        return isMatch(query);
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
        return generateDocumentSearchQuery(queryParts, getAllStores());
    }

    private String getCompoundWorkflowOwnerQuery(String ownerId, boolean isPreviousOwnerId) {
        List<String> queryParts = new ArrayList<String>();
        QName ownerField = (isPreviousOwnerId) ? WorkflowCommonModel.Props.PREVIOUS_OWNER_ID : WorkflowCommonModel.Props.OWNER_ID;
        queryParts.add(generateStringExactQuery(ownerId, ownerField));
        queryParts.add(joinQueryPartsOr(
                generateStringExactQuery(Status.NEW.getName(), WorkflowCommonModel.Props.STATUS)
                , generateStringExactQuery(Status.IN_PROGRESS.getName(), WorkflowCommonModel.Props.STATUS)
                , generateStringExactQuery(Status.STOPPED.getName(), WorkflowCommonModel.Props.STATUS)
                , generateStringExactQuery(Status.UNFINISHED.getName(), WorkflowCommonModel.Props.STATUS)
                ));
        queryParts.add(0, joinQueryPartsAnd(Arrays.asList(generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW),
                SearchUtil.generateNotTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION)), false));
        return joinQueryPartsAnd(queryParts);
    }

    private String getOpenCaseFileOwnerQuery(String ownerId, boolean isPreviousOwnerId) {
        List<String> queryParts = new ArrayList<String>();
        QName ownerField = (isPreviousOwnerId) ? DocumentCommonModel.Props.PREVIOUS_OWNER_ID : DocumentCommonModel.Props.OWNER_ID;
        queryParts.add(generateStringExactQuery(ownerId, ownerField));
        queryParts.add(generateStringExactQuery(DocListUnitStatus.OPEN.getValueName(), DocumentDynamicModel.Props.STATUS));
        queryParts.add(0, generateTypeQuery(CaseFileModel.Types.CASE_FILE));
        queryParts.add(1, generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE));
        return joinQueryPartsAnd(queryParts);
    }

    private String getInProcessDocumentsOwnerQuery(String ownerId) {
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateStringExactQuery(ownerId, DocumentCommonModel.Props.OWNER_ID));
        if (finishedIncomingLettersAreNotShown) {
            queryParts.add(generateStringExactQuery(DocumentStatus.WORKING.getValueName(), DocumentCommonModel.Props.DOC_STATUS));
        } else {
            String incomingLetterTypesQuery = generateStringExactQuery(SystematicDocumentType.INCOMING_LETTER.getId(), DocumentAdminModel.Props.OBJECT_TYPE_ID);
            String notIncomingLetterTypesQuery = generateAndNotQuery(generateStringExactQuery(DocumentStatus.WORKING.getValueName(),
                    DocumentCommonModel.Props.DOC_STATUS), generateStringExactQuery(SystematicDocumentType.INCOMING_LETTER.getId(), DocumentAdminModel.Props.OBJECT_TYPE_ID));
            String hasNoStartedCompoundWorkflowsQuery = joinQueryPartsOr(Arrays.asList(
                    generatePropertyBooleanQuery(DocumentCommonModel.Props.SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS, false)
                    , generatePropertyNullQuery(DocumentCommonModel.Props.SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS)));
            queryParts.add(
                    joinQueryPartsOr(
                            joinQueryPartsAnd(incomingLetterTypesQuery, hasNoStartedCompoundWorkflowsQuery)
                            , notIncomingLetterTypesQuery
                    ));
        }
        return generateDocumentSearchQuery(queryParts, getAllStores());
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

    private String generateDocumentSearchQuery(Node filter, List<StoreRef> storeRefs) {
        return generateDocumentSearchQuery(filter, storeRefs, null);
    }

    private String generateDocumentSearchQuery(Node filter, List<StoreRef> storeRefs, String userId) {
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
        // Saaja
        String sendInfoRecipient = (String) props.get(DocumentSearchModel.Props.SEND_INFO_RECIPIENT);
        if (sendInfoRecipient != null) {
            queryParts.add(generatePropertyWildcardQuery(DocumentCommonModel.Props.SEARCHABLE_SEND_INFO_RECIPIENT, sendInfoRecipient, false, true));
        }
        // Saatmise ajavahemik
        Date sendDateTime = (Date) props.get(DocumentSearchModel.Props.SEND_INFO_SEND_DATE_TIME);
        Date sendDateTimeEnd = (Date) props.get(DocumentSearchModel.Props.SEND_INFO_SEND_DATE_TIME_END);
        if (sendDateTime != null || sendDateTimeEnd != null) {
            queryParts.add(generateDatePropertyRangeQuery(sendDateTime, sendDateTimeEnd, DocumentCommonModel.Props.SEARCHABLE_SEND_INFO_SEND_DATE_TIME));
        }
        // Saatmise resolutsioon
        String sendInfoResolution = (String) props.get(DocumentSearchModel.Props.SEND_INFO_RESOLUTION);
        if (StringUtils.isNotBlank(sendInfoResolution)) {
            List<String> searchWords = parseQuickSearchWords(sendInfoResolution);
            for (String searchWord : searchWords) {
                if (StringUtils.isNotBlank(searchWord)) {
                    queryParts.add(joinQueryPartsOr(SearchUtil.generateValuesWildcardQuery(searchWord)));
                }
            }
        }
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
        // Märksõnad
        generateHierarchicalKeywordQuery(queryParts, props, getDocumentAdminService().getSearchableDocumentFieldDefinitions());

        // END: special cases

        fillQueryFromProps(queryParts, props, storeRefs);

        String searchFilter = WmNode.toHumanReadableStringIfPossible(RepoUtil.getNotEmptyProperties(props), namespaceService, BeanHelper.getDocumentAdminService());
        log.info("Documents search filter: " + searchFilter);
        logService.addLogEntry(LogEntry.create(LogObject.SEARCH_DOCUMENTS, userService, "applog_search_docs", searchFilter));

        // Quick search (Otsisõna)
        String quickSearchInput = (String) props.get(DocumentSearchModel.Props.INPUT);
        if (StringUtils.isNotBlank(quickSearchInput)) {
            List<String> quickSearchWords = parseQuickSearchWords(quickSearchInput);
            log.info("Quick search (document) - words: " + quickSearchWords.toString() + ", from string '" + quickSearchInput + "'");
            queryParts.addAll(generateQuickSearchDocumentQuery(quickSearchWords));
        }

        String query = generateDocumentSearchQuery(queryParts, storeRefs, userId);
        if (log.isDebugEnabled()) {
            log.debug("Documents search query construction time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }

        return query;
    }

    private String generateVolumeSearchQuery(Node filter) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(50);
        Map<QName, Serializable> props = RepoUtil.toQNameProperties(filter.getProperties(), true);

        @SuppressWarnings("unchecked")
        List<String> volTypes = (List<String>) props.get(VolumeSearchModel.Props.VOLUME_TYPE);
        queryParts.add(joinQueryPartsOr(generateMultiStringExactQuery(volTypes, VolumeModel.Props.VOLUME_TYPE), volTypes.contains(VolumeType.CASE_FILE.name())
                ? joinQueryPartsAnd(generateTypeQuery(CaseFileModel.Types.CASE_FILE), generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE)) : ""));

        @SuppressWarnings("unchecked")
        List<String> caseFileTypes = (List<String>) props.get(VolumeSearchModel.Props.CASE_FILE_TYPE);
        queryParts.add(generateMultiStringExactQuery(caseFileTypes, DocumentAdminModel.Props.OBJECT_TYPE_ID));

        // Märksõnad
        generateHierarchicalKeywordQuery(queryParts, props, getDocumentAdminService().getSearchableVolumeFieldDefinitions());

        fillQueryFromProps(queryParts, props, getVolumeSearchStoreRefs(filter.getProperties()));

        String searchFilter = WmNode.toHumanReadableStringIfPossible(RepoUtil.getNotEmptyProperties(props), namespaceService, BeanHelper.getDocumentAdminService());
        log.info("Volumes search filter: " + searchFilter);
        logService.addLogEntry(LogEntry.create(LogObject.SEARCH_VOLUMES, userService, "applog_search_volumes", searchFilter));

        // Quick search (Otsisõna)
        String quickSearchInput = (String) props.get(VolumeSearchModel.Props.INPUT);
        if (StringUtils.isNotBlank(quickSearchInput)) {
            List<String> quickSearchWords = parseQuickSearchWords(quickSearchInput);
            log.info("Quick search (document) - words: " + quickSearchWords.toString() + ", from string '" + quickSearchInput + "'");
            queryParts.addAll(generateQuickSearchDocumentQuery(quickSearchWords));
        }

        String query = generateVolumeOrCaseFileSearchQuery(queryParts);
        if (log.isDebugEnabled()) {
            log.debug("Volumes search query construction time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return query;
    }

    private void generateHierarchicalKeywordQuery(List<String> queryParts, Map<QName, Serializable> props, List<FieldDefinition> searchableFieldDefinitions) {
        List<QName> l1Fields = new ArrayList<QName>();
        List<QName> l2Fields = new ArrayList<QName>();
        for (FieldDefinition fieldDefinition : searchableFieldDefinitions) {
            String originalFieldId = fieldDefinition.getOriginalFieldId();
            if (DocumentDynamicModel.Props.FIRST_KEYWORD_LEVEL.getLocalName().equals(originalFieldId)) {
                l1Fields.add(fieldDefinition.getQName());
                continue;
            }
            if (DocumentDynamicModel.Props.SECOND_KEYWORD_LEVEL.getLocalName().equals(originalFieldId)) {
                l2Fields.add(fieldDefinition.getQName());
                continue;
            }
        }

        @SuppressWarnings("unchecked")
        List<String> keywordsL1 = (List<String>) props.get(DocumentDynamicModel.Props.FIRST_KEYWORD_LEVEL);
        @SuppressWarnings("unchecked")
        List<String> keywordsL2 = (List<String>) props.get(DocumentDynamicModel.Props.SECOND_KEYWORD_LEVEL);
        queryParts.add(SearchUtil.joinQueryPartsOr(generateMultiStringExactQuery(keywordsL1, l1Fields.toArray(new QName[l1Fields.size()])),
                generateMultiStringExactQuery(keywordsL2, l2Fields.toArray(new QName[l2Fields.size()]))));
        props.remove(DocumentDynamicModel.Props.FIRST_KEYWORD_LEVEL);
        props.remove(DocumentDynamicModel.Props.SECOND_KEYWORD_LEVEL);
        props.remove(DocumentDynamicModel.Props.THESAURUS);
    }

    private void fillQueryFromProps(List<String> queryParts, Map<QName, Serializable> props, List<StoreRef> storeRefs) {
        // dynamic generation
        for (Entry<QName, Serializable> entry : props.entrySet()) {
            QName propQName = entry.getKey();
            if (DocumentLocationGenerator.CASE_LABEL_EDITABLE.equals(propQName)) { // caseLabelEditable value is the title of the case, but the property is a NodeRef
                String caseLabel = (String) entry.getValue();
                if (StringUtils.isBlank(caseLabel)) {
                    continue;
                }
                String query = generatePropertyWildcardQuery(CaseModel.Props.TITLE, caseLabel.trim(), false, true);
                List<ResultSet> results = null;
                try {
                    results = doSearches(query, -1, "searchCaseByLabelForDocumentSearch", storeRefs);
                    for (ResultSet result : results) {
                        queryParts.add(generateMultiNodeRefQuery(result.getNodeRefs(), DocumentCommonModel.Props.CASE));
                    }
                } finally {
                    if (results != null) {
                        for (ResultSet result : results) {
                            if (result != null) {
                                result.close();
                            }
                        }
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
                queryParts.add(generateStringWordsWildcardQuery((String) value, 2, propQName));
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
                    queryParts.add(generateMultiStringWordsWildcardQuery(list, 2, propQName));
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

    private String generateQuickSearchQuery(String searchString, Collection<StoreRef> storeRefs) {
        return generateQuickSearchQuery(searchString, null, storeRefs);
    }

    private String generateQuickSearchQuery(String searchString, NodeRef containerNodeRef, Collection<StoreRef> storeRefs) {
        long startTime = System.currentTimeMillis();
        List<String> quickSearchWords = parseQuickSearchWords(searchString);
        log.info("Quick search - words: " + quickSearchWords.toString() + ", from string '" + searchString + "'");
        String query = generateDocumentSearchQuery(generateQuickSearchDocumentQuery(quickSearchWords), storeRefs);
        if (StringUtils.isNotBlank(query) && containerNodeRef != null) {
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
                        SearchUtil.generateValuesWildcardQuery(searchWord),
                        SearchUtil.generatePropertyWildcardQuery(DocumentCommonModel.Props.FILE_CONTENTS, searchWord, false, true))));
            }
        }
        return queryParts;
    }

    private String generateDocumentSearchQuery(Collection<StoreRef> storeRefs, String... queryParts) {
        return generateDocumentSearchQuery(new ArrayList<String>(Arrays.asList(queryParts)), storeRefs);
    }

    private String generateDocumentSearchQuery(List<String> queryParts, Collection<StoreRef> storeRefs) {
        return generateDocumentSearchQuery(queryParts, storeRefs, null);
    }

    private String generateDocumentSearchQuery(List<String> queryParts, Collection<StoreRef> storeRefs, String userId) {
        if (isBlank(queryParts)) {
            return null;
        }

        List<NodeRef> seriesRefs = new ArrayList<NodeRef>();
        if (storeRefs == null) {
            ResultSet result = null;
            try {
                result = doSearch(SearchUtil.QUERY_RESTRICTED_SERIES, -1, "restrictedSeries", generalService.getStore());
                seriesRefs.addAll(result.getNodeRefs());
            } finally {
                if (result != null) {
                    result.close();
                }
            }
        } else {
            List<ResultSet> result = doSearches(SearchUtil.QUERY_RESTRICTED_SERIES, -1, "restrictedSeries", storeRefs);
            try {
                for (ResultSet resultSet : result) {
                    seriesRefs.addAll(resultSet.getNodeRefs());
                }
            } finally {
                for (ResultSet resultSet : result) {
                    try {
                        resultSet.close();
                    } catch (Exception e) {
                        log.error("Closing resultSet failed, continuing", e);
                    }
                }
            }
        }

        queryParts.add(SearchUtil.generateDocAccess(seriesRefs, userId));
        return generateDocumentSearchQueryWithoutRestriction(queryParts);
    }

    private String generateDocumentSearchQueryWithoutRestriction(List<String> queryParts) {
        if (isBlank(queryParts)) {
            return null;
        }
        queryParts.add(0, generateTypeQuery(DocumentCommonModel.Types.DOCUMENT));
        queryParts.add(1, generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE));
        String query = joinQueryPartsAnd(queryParts);
        if (log.isDebugEnabled()) {
            log.debug("Document search query: " + query);
        }
        return query;
    }

    private static String generateVolumeSearchQuery(List<String> queryParts) {
        if (isBlank(queryParts)) {
            return null;
        }
        queryParts.add(0, generateTypeQuery(VolumeModel.Types.VOLUME));
        return joinQueryPartsAnd(queryParts);
    }

    private static String generateVolumeOrCaseFileSearchQuery(List<String> queryParts) {
        if (isBlank(queryParts)) {
            return null;
        }
        queryParts.add(0, joinQueryPartsOr(
                generateTypeQuery(VolumeModel.Types.VOLUME),
                joinQueryPartsAnd(
                        generateTypeQuery(CaseFileModel.Types.CASE_FILE),
                        generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE))));
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

    @SuppressWarnings("unchecked")
    private String generateCompoundWorkflowSearchQuery(Node filter) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(15);
        getCompoundWorkflowSearchPropQueryParts(filter, queryParts);
        if (isBlank(queryParts)) {
            return null;
        }
        queryParts.add(0, joinQueryPartsAnd(
                Arrays.asList(generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW),
                        SearchUtil.generateNotTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION)), false));
        String query = joinQueryPartsAnd(queryParts);
        if (log.isDebugEnabled()) {
            log.debug("CompoundWorkflow search query construction time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return query;
    }

    private void getCompoundWorkflowSearchPropQueryParts(Node filter, List<String> queryParts) {
        Map<String, Object> props = filter.getProperties();

        queryParts.add(generateStringExactQuery((String) props.get(CompoundWorkflowSearchModel.Props.TYPE), WorkflowCommonModel.Props.TYPE));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(CompoundWorkflowSearchModel.Props.TITLE), true, true, 2, WorkflowCommonModel.Props.TITLE));
        queryParts.add(generateMultiStringWordsWildcardQuery((List<String>) props.get(CompoundWorkflowSearchModel.Props.OWNER_NAME), false, true, 2,
                WorkflowCommonModel.Props.OWNER_NAME));

        queryParts.add(generateStringWordsWildcardQuery((String) props.get(CompoundWorkflowSearchModel.Props.JOB_TITLE), true, true, 2, WorkflowCommonModel.Props.OWNER_JOB_TITLE));
        queryParts
                .add(generatePropertyExactQuery(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME, (List<String>) props.get(CompoundWorkflowSearchModel.Props.STRUCT_UNIT)));

        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(CompoundWorkflowSearchModel.Props.CREATED_DATE),
                (Date) props.get(CompoundWorkflowSearchModel.Props.CREATED_DATE_END), WorkflowCommonModel.Props.CREATED_DATE_TIME));
        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(CompoundWorkflowSearchModel.Props.IGNITION_DATE),
                (Date) props.get(CompoundWorkflowSearchModel.Props.IGNITION_DATE_END), WorkflowCommonModel.Props.STARTED_DATE_TIME));
        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(CompoundWorkflowSearchModel.Props.STOPPED_DATE),
                (Date) props.get(CompoundWorkflowSearchModel.Props.STOPPED_DATE_END), WorkflowCommonModel.Props.STOPPED_DATE_TIME));
        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(CompoundWorkflowSearchModel.Props.ENDING_DATE),
                (Date) props.get(CompoundWorkflowSearchModel.Props.ENDING_DATE_END), WorkflowCommonModel.Props.FINISHED_DATE_TIME));

        String statusString = (String) props.get(CompoundWorkflowSearchModel.Props.STATUS);
        if (StringUtils.isNotBlank(statusString)) {
            queryParts.add(generateStringExactQuery(Status.valueOf(statusString).getName(), WorkflowCommonModel.Props.STATUS));
        }
        String searchFilter = WmNode.toHumanReadableStringIfPossible(RepoUtil.getNotEmptyProperties(RepoUtil.toQNameProperties(props)), namespaceService, null);
        log.info("Tasks search filter: " + searchFilter);
        // Searches are made by user. Reports are generated by DHS. We need to log only searches.
        if (userService.getCurrentUserName() != null) {
            logService.addLogEntry(LogEntry.create(LogObject.SEARCH_COMPOUND_WORKFLOWS, userService, "applog_search_compoundWorkflows", searchFilter));
        }
    }

    private void getTaskCompoundWorkflowSearchPropQueryParts(Node filter, List<String> queryParts, List<Object> arguments) {
        Map<String, Object> props = filter.getProperties();

        addTaskStringExactPartsAndArgs(queryParts, arguments, (String) props.get(CompoundWorkflowSearchModel.Props.TYPE),
                WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_TYPE);

        addTaskStringWordsWildcardPartsAndArgs(queryParts, arguments, (String) props.get(CompoundWorkflowSearchModel.Props.TITLE),
                WorkflowSpecificModel.Props.COMPOUND_WORKFLOW_TITLE);

        addTaskMultiStringWordsWildcardPartsAndArgs(queryParts, arguments, (List<String>) props.get(CompoundWorkflowSearchModel.Props.OWNER_NAME),
                WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_OWNER_NAME);

        addTaskStringWordsWildcardPartsAndArgs(queryParts, arguments, (String) props.get(CompoundWorkflowSearchModel.Props.JOB_TITLE),
                WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_OWNER_JOB_TITLE);
        addTaskMultiStringArrayPartsAndArgs(queryParts, arguments, (List<String>) props.get(CompoundWorkflowSearchModel.Props.STRUCT_UNIT),
                WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_OWNER_ORGANIZATION_NAME);

        addDateQueryPartsAndArguments(queryParts, arguments, props, CompoundWorkflowSearchModel.Props.CREATED_DATE, CompoundWorkflowSearchModel.Props.CREATED_DATE_END,
                WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_CREATED_DATE_TIME);
        addDateQueryPartsAndArguments(queryParts, arguments, props, CompoundWorkflowSearchModel.Props.IGNITION_DATE, CompoundWorkflowSearchModel.Props.IGNITION_DATE_END,
                WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_STARTED_DATE_TIME);
        addDateQueryPartsAndArguments(queryParts, arguments, props, CompoundWorkflowSearchModel.Props.STOPPED_DATE, CompoundWorkflowSearchModel.Props.STOPPED_DATE_END,
                WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_STOPPED_DATE_TIME);
        addDateQueryPartsAndArguments(queryParts, arguments, props, CompoundWorkflowSearchModel.Props.ENDING_DATE, CompoundWorkflowSearchModel.Props.ENDING_DATE_END,
                WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_FINISHED_DATE_TIME);

        String statusString = (String) props.get(CompoundWorkflowSearchModel.Props.STATUS);
        if (StringUtils.isNotBlank(statusString)) {
            addTaskStringExactPartsAndArgs(queryParts, arguments, Status.valueOf(statusString).getName(), WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_STATUS);
        }
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
            logService.addLogEntry(LogEntry.create(LogObject.SEARCH_TASKS, userService, "applog_search_tasks", searchFilter));
        }

        getTaskCompoundWorkflowSearchPropQueryParts(filter, queryParts, arguments);

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

    private void addTaskMultiStringWordsWildcardPartsAndArgs(List<String> queryParts, List<Object> arguments, List<String> tsQueryInputs, QName... taskProps) {
        if (tsQueryInputs == null || tsQueryInputs.isEmpty()) {
            return;
        }
        List<String> subQueryParts = new ArrayList<String>(tsQueryInputs.size());
        for (String tsQueryInput : tsQueryInputs) {
            addTaskStringWordsWildcardPartsAndArgs(subQueryParts, arguments, tsQueryInput, taskProps);
        }
        queryParts.add(SearchUtil.joinQueryPartsOr(subQueryParts));
    }

    private Pair<List<CompoundWorkflow>, Boolean> searchCompoundWorkflowsImpl(String query, int limit, String queryName) {
        return searchGeneralImpl(query, limit, queryName, new SearchCallback<CompoundWorkflow>() {
            @Override
            public CompoundWorkflow addResult(ResultSetRow row) {
                NodeRef nodeRef = row.getNodeRef();
                CompoundWorkflow compoundWorkflow = workflowService.getCompoundWorkflow(nodeRef);
                return compoundWorkflow;
            }
        });
    }

    private void addTaskStringWordsWildcardPartsAndArgs(List<String> queryParts, List<Object> arguments, String tsQueryInput, QName... taskProps) {
        if (StringUtils.isBlank(tsQueryInput) || taskProps == null || taskProps.length == 0) {
            return;
        }
        String tsquery = generalService.getTsquery(tsQueryInput, 2);
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

    private Pair<List<VolumeOrCaseFile>, Boolean> searchVolumesImpl(String query, int limit, String queryName, Collection<StoreRef> storeRefs) {
        return searchGeneralImpl(query, limit, queryName, new SearchCallback<VolumeOrCaseFile>() {

            @Override
            public VolumeOrCaseFile addResult(ResultSetRow row) {
                NodeRef ref = row.getNodeRef();
                QName type = nodeService.getType(ref);
                if (type.equals(VolumeModel.Types.VOLUME)) {
                    return volumeService.getVolumeByNodeRef(row.getNodeRef());
                }
                return BeanHelper.getCaseFileService().getCaseFile(ref); // BeanCreationException if service is injected
            }
        }, storeRefs);
    }

    private Pair<List<Volume>, Boolean> searchVolumesAndCaseFilesImpl(String query, int limit, String queryName, Collection<StoreRef> storeRefs) {
        return searchGeneralImpl(query, limit, queryName, new SearchCallback<Volume>() {

            @Override
            public Volume addResult(ResultSetRow row) {
                return volumeService.getVolumeByNodeRef(row.getNodeRef());
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

    private List<ArchivalActivity> searchArchivalActivitiesImpl(String query, int limit, String queryName, Collection<StoreRef> storeRefs) {
        return searchGeneralImpl(query, limit, queryName, new SearchCallback<ArchivalActivity>() {
            @Override
            public ArchivalActivity addResult(ResultSetRow row) {
                return BeanHelper.getArchivalsService().getArchivalActivity(row.getNodeRef());
            }
        }, storeRefs).getFirst();
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
    public List<NodeRef> searchNodesByTypeAndProps(String input, QName type, Set<QName> props, int limit) {
        return searchNodesByTypeAndProps(input, type, props, limit, null);
    }

    @Override
    public List<NodeRef> searchNodesByTypeAndProps(String input, QName type, Set<QName> props, int limit, String queryAndAddition) {
        limit = limit < 0 ? 100 : limit;
        String query = joinQueryPartsAnd(
                type != null ? generateTypeQuery(type) : "",
                generateStringWordsWildcardQuery(input, props.toArray(new QName[props.size()])),
                queryAndAddition
                );
        return searchNodes(query, limit, "searchNodesByTypeAndProps");
    }

    @Override
    public List<NodeRef> searchNodes(String query, int limit, String queryName) {
        return searchNodes(query, limit, queryName, null).getFirst();
    }

    private List<NodeRef> searchNodesFromAllStores(String query, String queryName) {
        return searchNodes(query, -1, queryName, getAllStores()).getFirst();
    }

    @Override
    public List<NodeRef> filterUsersInUserGroup(String exactGroup, Set<String> children) {
        String groupFilterQuery = null;
        if (StringUtils.isNotBlank(exactGroup)) {
            List<String> groupNames = BeanHelper.getDocumentSearchService().searchAuthorityGroupsByExactName(exactGroup);
            groupFilterQuery = SearchUtil.generatePropertyExactQuery(ContentModel.PROP_USERNAME, userService.getUserNamesInGroup(groupNames));
        }
        String query = SearchUtil.joinQueryPartsAnd(Arrays.asList(groupFilterQuery,
                SearchUtil.generatePropertyExactQuery(ContentModel.PROP_USERNAME, children)));
        return searchNodes(query, -1, "filter-users-in-struct-unit");
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

    @Override
    public LinkedHashSet<StoreRef> getAllStoresWithArchivalStoreVOs() {
        return generalService.getAllWithArchivalsStoreRefs();
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

    public void setFinishedIncomingLettersAreNotShown(boolean finishedIncomingLettersAreNotShown) {
        this.finishedIncomingLettersAreNotShown = finishedIncomingLettersAreNotShown;
    }

    // END: getters / setters

}
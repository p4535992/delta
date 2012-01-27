package ee.webmedia.alfresco.document.search.service;

import static ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType.INCOMING_LETTER;
import static ee.webmedia.alfresco.utils.SearchUtil.generateAndNotQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateAspectQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateDatePropertyRangeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateLuceneSearchParams;
import static ee.webmedia.alfresco.utils.SearchUtil.generateMultiStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateNodeRefQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateNumberPropertyRangeQuery;
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
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
import ee.webmedia.alfresco.archivals.model.ArchivalsStoreVO;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.classificator.enums.SendMode;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.docconfig.generator.fieldtype.DateGenerator;
import ee.webmedia.alfresco.docconfig.generator.fieldtype.DoubleGenerator;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.document.search.model.FakeDocument;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.type.service.DocumentTypeHelper;
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
    private ParametersService parametersService;
    private NamespaceService namespaceService;
    private AuthorityService authorityService;
    private UserService userService;

    private List<StoreRef> allStores = null;
    private List<StoreRef> allStoresWithArchivalStoreVOs = null; // XXX This is currently used only for tasks. If analysis for CL 186867 is complete then this might be refactored
                                                                 // to getAllStores()
    private QName[] notIncomingLetterTypes;

    @Override
    public List<Document> searchDiscussionDocuments() {
        long startTime = System.currentTimeMillis();

        String query = generateDiscussionDocumentsQuery();
        List<Document> results = searchGeneralImpl(query, -1, /* queryName */"discussionDocuments", new SearchCallback<Document>() {

            @Override
            public Document addResult(ResultSetRow row) {
                return documentService.getDocumentByNodeRef(row.getNodeRef());
            }

        });

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
        int count = searchGeneralImpl(query, -1, /* queryName */"discussionDocumentsCount", new SearchCallback<Integer>() {

            @Override
            public Integer addResult(ResultSetRow row) {
                return row.getIndex();
            }

        }).size();

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
    public List<DocumentDynamic> searchAdrDocuments(Date regDateBegin, Date regDateEnd, String docTypeId, String searchString, Set<String> documentTypeIds) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(5);
        if (regDateBegin != null || regDateEnd != null) {
            queryParts.add(generateDatePropertyRangeQuery(regDateBegin, regDateEnd //
                    , DocumentCommonModel.Props.REG_DATE_TIME, DocumentSpecificModel.Props.SENDER_REG_DATE));
        }
        queryParts.add(generateQuickSearchQuery(searchString));
        if (docTypeId != null) {
            if (!documentTypeIds.contains(docTypeId)) {
                return Collections.emptyList();
            }
            queryParts.add(generateStringExactQuery(docTypeId, DocumentAdminModel.Props.OBJECT_TYPE_ID));
        }
        // If parameters generate no search query, then return nothing
        // Note: you can still get ALL documents very easily if you specify a very broad date range for example
        if (isBlank(queryParts)) {
            return Collections.emptyList();
        }

        String query = generateAdrDocumentSearchQuery(queryParts, documentTypeIds);
        // Only search from SpacesStore and ArchivalsStore to get correct document set (PPA).
        List<DocumentDynamic> results = searchDocumentDynamicsImpl(query, -1, /* queryName */"adrDocuments1");
        results.addAll(searchDocumentDynamicsImpl(query, -1, /* queryName */"adrDocuments2", Arrays.asList(generalService.getArchivalsStoreRef())));
        if (log.isDebugEnabled()) {
            log.debug("ADR documents search total time " + (System.currentTimeMillis() - startTime) + " ms, results " + results.size() + ", query: " + query);
        }
        return results;
    }

    @Override
    public List<DocumentDynamic> searchAdrDocuments(String regNumber, Date regDate, Set<String> documentTypeIds) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(4);
        queryParts.add(generatePropertyDateQuery(DocumentCommonModel.Props.REG_DATE_TIME, regDate));
        queryParts.add(generateStringExactQuery(regNumber, DocumentCommonModel.Props.REG_NUMBER));

        String query = generateAdrDocumentSearchQuery(queryParts, documentTypeIds);
        // Only search from SpacesStore and ArchivalsStore to get correct document set (PPA).
        List<DocumentDynamic> results = searchDocumentDynamicsImpl(query, -1, /* queryName */"adrDocumentByReg1");
        results.addAll(searchDocumentDynamicsImpl(query, -1, /* queryName */"adrDocumentByReg2", Arrays.asList(generalService.getArchivalsStoreRef())));
        if (log.isDebugEnabled()) {
            log.debug("ADR document details search total time " + (System.currentTimeMillis() - startTime) + " ms, results " + results.size() //
                    + ", query: " + query);
        }
        return results;
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
        results.addAll(searchNodes(query, -1, /* queryName */"adrDocumentByModified2", generalService.getArchivalsStoreRef()));
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
        queryParts.add(generateTypeQuery(DocumentSubtypeModel.Types.INVOICE));
        queryParts.add(generatePropertyNullQuery(DocumentSpecificModel.Props.SELLER_PARTY_SAP_ACCOUNT));
        queryParts.add(generatePropertyNotNullQuery(DocumentSpecificModel.Props.SELLER_PARTY_REG_NUMBER));

        String query = joinQueryPartsAnd(queryParts);
        List<Document> results = searchDocumentsImpl(query, -1, /* queryName */"searchInvoicesWithEmptySapAccount");
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
        queryParts.add(generateTypeQuery(DocumentCommonModel.Types.DOCUMENT));
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
        queryParts.add(generateStringExactQuery(SystematicDocumentType.INCOMING_LETTER.getId(), DocumentAdminModel.Props.OBJECT_TYPE_ID));
        queryParts.add(generateStringNotEmptyQuery(DocumentCommonModel.Props.REG_DATE_TIME));
        queryParts.add(generateStringExactQuery(senderRegNumber, DocumentSpecificModel.Props.SENDER_REG_NUMBER));

        String query = generateDocumentSearchQuery(queryParts);
        List<Document> results = searchDocumentsImpl(query, -1, /* queryName */"incomingLetterRegisteredDocuments");
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
        List<Document> results = searchDocumentsImpl(query, -1, /* queryName */"todayRegisteredDocuments");
        if (log.isDebugEnabled()) {
            log.debug("Today registered documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public List<Document> searchInProcessUserDocuments() {
        long startTime = System.currentTimeMillis();
        String query = getInProcessDocumentsOwnerQuery(AuthenticationUtil.getRunAsUser());
        List<Document> results = searchDocumentsImpl(query, -1, /* queryName */"inProcessUserDocuments", getAllStores());

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
        List<Document> results = searchDocumentsImpl(query, -1, /* queryName */"accessRestictionEndsAfterDate");
        if (log.isDebugEnabled()) {
            log.debug("Search for documents with access restriction took " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public List<Document> searchRecipientFinishedDocuments() {
        long startTime = System.currentTimeMillis();
        String query = generateRecipientFinichedQuery();
        List<Document> results = searchDocumentsImpl(query, -1, /* queryName */"recipientFinishedDocuments");

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
                DocumentSpecificModel.Props.SECOND_PARTY_NAME, DocumentSpecificModel.Props.THIRD_PARTY_NAME,
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
        List<String> queryParts = getTaskQuery(taskType, AuthenticationUtil.getRunAsUser(), Status.IN_PROGRESS, false);
        addSubstitutionRestriction(queryParts);
        String query = generateTaskSearchQuery(queryParts);
        List<Task> results = searchTasksImpl(query, -1, /* queryName */"CurrentUsersTasksInProgress");
        if (log.isDebugEnabled()) {
            log.debug("Current user's and IN_PROGRESS tasks search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public Map<NodeRef, Pair<String, String>> searchTaskBySendStatusQuery(QName taskType) {
        if (taskType == null) {
            taskType = WorkflowCommonModel.Types.TASK;
        }
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateTypeQuery(taskType));
        queryParts.add(generateStringExactQuery(SendStatus.SENT.toString(), WorkflowSpecificModel.Props.SEND_STATUS));
        queryParts.add(generateStringNotEmptyQuery(WorkflowSpecificModel.Props.SENT_DVK_ID));
        String query = generateTaskSearchQuery(queryParts);

        final Map<NodeRef, Pair<String, String>> nodeRefAndDvkIds = new HashMap<NodeRef, Pair<String, String>>();
        searchGeneralImpl(query, -1, "TaskBySendStatus", new SearchCallback<String>() {
            @Override
            public String addResult(ResultSetRow row) {
                final NodeRef sendInfoRef = row.getNodeRef();
                Pair<String, String> dvkIdAndRecipient = new Pair<String, String>((String) nodeService.getProperty(sendInfoRef,
                        WorkflowSpecificModel.Props.SENT_DVK_ID),
                        (String) nodeService.getProperty(sendInfoRef, WorkflowSpecificModel.Props.INSTITUTION_CODE));
                nodeRefAndDvkIds.put(sendInfoRef, dvkIdAndRecipient);
                return null;
            }
        });

        return nodeRefAndDvkIds;
    }

    @Override
    public List<Task> searchTasksByOriginalDvkIdsQuery(Iterable<String> originalDvkIds) {
        List<Task> dvkIdsAndTasks = new ArrayList<Task>();
        if (!originalDvkIds.iterator().hasNext()) {
            return dvkIdsAndTasks;
        }
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateTypeQuery(WorkflowCommonModel.Types.TASK));
        List<String> queryDvkIdParts = new ArrayList<String>();
        for (String dvkId : originalDvkIds) {
            queryDvkIdParts.add(generateStringExactQuery(dvkId, WorkflowSpecificModel.Props.ORIGINAL_DVK_ID));
        }
        queryParts.add(joinQueryPartsOr(queryDvkIdParts, true));
        String query = generateTaskSearchQuery(queryParts);
        List<Task> tasks = searchTasksImpl(query, -1, /* queryName */"TasksByOriginalDvkIds");
        for (Task task : tasks) {
            dvkIdsAndTasks.add(task);
        }
        return dvkIdsAndTasks;
    }

    /**
     * Only used at importing external review task,
     * so if many tasks are found the one belonging to document with notEditable aspect is preferred
     */
    @Override
    public Task searchTaskByOriginalDvkIdQuery(String originalDvkId) {
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateTypeQuery(WorkflowCommonModel.Types.TASK));
        queryParts.add(generateStringExactQuery(originalDvkId, WorkflowSpecificModel.Props.ORIGINAL_DVK_ID));
        String query = generateTaskSearchQuery(queryParts);
        List<Task> tasks = searchTasksImpl(query, -1, /* queryName */"TaskByOriginalDvkId");
        if (tasks.size() == 1) {
            return tasks.get(0);
        }
        for (Task task : tasks) {
            NodeRef compoundWorkflowRef = nodeService.getPrimaryParent(task.getParent().getNodeRef()).getParentRef();
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
        queryParts.add(generateStringExactQuery(Status.IN_PROGRESS.getName(), WorkflowCommonModel.Props.STATUS));
        queryParts.add(generateStringNotEmptyQuery(WorkflowCommonModel.Props.OWNER_ID));
        queryParts.add(generateDatePropertyRangeQuery(fromDate, dueDate, WorkflowSpecificModel.Props.DUE_DATE));
        String query = generateTaskSearchQuery(queryParts);
        List<Task> results = searchTasksImpl(query, -1, /* queryName */"tasksDueAfterDate");
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
            query = joinQueryPartsAnd(Arrays.asList(query, SearchUtil.generateParentQuery(parentRef, generalService.getStore())));
        }
        return searchNodes(query, -1, /* queryName */"simpleSearch");
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
        List<ResultSet> results = doSearches(query, 1, queryName, getAllStores());
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
        String ownerId = AuthenticationUtil.getRunAsUser();
        queryParts.add(generateTypeQuery(taskType));
        queryParts.add(generateStringExactQuery(Status.IN_PROGRESS.getName(), WorkflowCommonModel.Props.STATUS));
        queryParts.add(generateStringExactQuery(ownerId, WorkflowCommonModel.Props.OWNER_ID));
        addSubstitutionRestriction(queryParts);
        String query = generateTaskSearchQuery(queryParts);
        List<ResultSet> resultSets = doSearches(query, -1, "currentUsersTaskCount", getAllStoresWithArchivalStoreVOs());
        int count = countResults(resultSets);
        if (log.isDebugEnabled()) {
            log.debug("Current user's and IN_PROGRESS tasks count search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return count;
    }

    @Override
    public List<TaskInfo> searchTasks(Node filter) {
        long startTime = System.currentTimeMillis();
        String query = generateTaskSearchQuery(filter);
        List<TaskInfo> results = searchTaskInfosImpl(query, RESULTS_LIMIT, /* queryName */"tasksByFilter");
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
            if (daysForSubstitutionTasksCalc.longValue() < 0) {
                return;
            }
            Date end = DateUtils.truncate(subInfo.getSubstitution().getSubstitutionEndDate(), Calendar.DATE);
            end = DateUtils.addDays(end, daysForSubstitutionTasksCalc.intValue());
            queryParts.add(joinQueryPartsOr(Arrays.asList(
                    generatePropertyNullQuery(WorkflowSpecificModel.Props.DUE_DATE),
                    generateDatePropertyRangeQuery(start, end, WorkflowSpecificModel.Props.DUE_DATE))));
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
        List<Document> results = searchGeneralImpl(DOCUMENTS_FOR_REGISTERING_QUERY, -1, /* queryName */"documentsForRegistering",
                new SearchCallback<Document>() {
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
        int count = searchGeneralImpl(DOCUMENTS_FOR_REGISTERING_QUERY, -1, /* queryName */"documentsForRegisteringCount", new SearchCallback<String>() {
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
        List<NodeRef> storeFunctionRootNodeRefs = (List<NodeRef>) filter.getProperties().get(DocumentSearchModel.Props.STORE);
        List<StoreRef> storeRefs = new ArrayList<StoreRef>(storeFunctionRootNodeRefs.size());
        for (NodeRef nodeRef : storeFunctionRootNodeRefs) {
            storeRefs.add(nodeRef.getStoreRef());
        }
        String query = generateDocumentSearchQuery(filter);
        if (StringUtils.isBlank(query)) {
            throw new UnableToPerformException(UnableToPerformException.MessageSeverity.INFO, "docSearch_error_noInput");
        }
        try {
            List<Document> results = searchDocumentsImpl(query, RESULTS_LIMIT, /* queryName */"documentsByFilter", storeRefs);
            if (log.isDebugEnabled()) {
                log.debug("Documents search total time " + (System.currentTimeMillis() - startTime) + " ms");
            }
            return results;
        } catch (RuntimeException e) {
            Map<QName, Serializable> filterProps = RepoUtil.getNotEmptyProperties(RepoUtil.toQNameProperties(filter.getProperties()));
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
    public List<Document> searchDocumentsQuick(String searchValue, NodeRef containerNodeRef) {
        return searchDocumentsAndOrCases(generateQuickSearchQuery(searchValue, containerNodeRef), searchValue, false);
    }

    @Override
    public List<Document> searchDocumentsAndOrCases(String searchString, Date regDateTimeBegin, Date regDateTimeEnd, List<String> documentTypes, boolean trySearchCases) {
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
        return searchDocumentsAndOrCases(query, searchString, includeCaseTitles);
    }

    private List<Document> searchDocumentsAndOrCases(String query, String searchString, boolean includeCaseTitles) {
        long startTime = System.currentTimeMillis();
        try {
            final List<Document> results;
            if (includeCaseTitles) {
                final String caseByTitleQuery = getCaseByTitleQuery(searchString);
                query = joinQueryPartsOr(Arrays.asList(query, caseByTitleQuery));
                results = searchDocumentsAndCaseTitlesImpl(query, RESULTS_LIMIT, /* queryName */"documentsQuickAndCaseTitles");
            } else {
                results = searchDocumentsImpl(query, RESULTS_LIMIT, /* queryName */"documentsQuick");
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
        String query = generateTaskSearchQuery(getTaskQuery(null, ownerId, Status.NEW, isPreviousOwnerId));
        List<NodeRef> results = searchNodesFromAllStores(query, /* queryName */"newTasksByOwnerId");
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
        queryParts.add(SearchUtil.generateTypeQuery(DocumentSubtypeModel.Types.INVOICE));
        queryParts.add(SearchUtil.generateStringExactQuery(regNumber, DocumentSpecificModel.Props.SELLER_PARTY_REG_NUMBER));
        queryParts.add(SearchUtil.generateStringExactQuery(invoiceNumber, DocumentSpecificModel.Props.INVOICE_NUMBER));
        queryParts.add(SearchUtil.generatePropertyDateQuery(DocumentSpecificModel.Props.INVOICE_DATE, invoiceDate));
        String query = SearchUtil.joinQueryPartsAnd(queryParts);
        List<Document> result = searchDocumentsImpl(query, -1, /* queryName */"similarInvoiceDocuments");
        if (log.isDebugEnabled()) {
            log.debug("Similar invoice documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return result;
    }

    @Override
    public List<Document> searchContractsByRegNumber(String regNumber) {
        List<String> queryParts = new ArrayList<String>();
        long startTime = System.currentTimeMillis();
        queryParts.add(SearchUtil.generateTypeQuery(DocumentTypeHelper.CONTRACT_TYPES));
        queryParts.add(SearchUtil.generateStringExactQuery(regNumber, DocumentCommonModel.Props.REG_NUMBER,
                DocumentSpecificModel.Props.SECOND_PARTY_CONTRACT_NUMBER));
        String query = SearchUtil.joinQueryPartsAnd(queryParts);
        List<Document> result = searchDocumentsImpl(query, -1, /* queryName */"contractsByRegNumber");
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
            contractQueryParts.add(SearchUtil.generateTypeQuery(DocumentTypeHelper.CONTRACT_TYPES));
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
        List<Document> result = searchDocumentsImpl(query, -1, /* queryName */"invoiceBaseDocuments");
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

    private List<String> getTaskQuery(QName taskType, String ownerId, Status status, boolean isPreviousOwnerId) {
        if (taskType == null) {
            taskType = WorkflowCommonModel.Types.TASK;
        }
        QName ownerField = (isPreviousOwnerId) ? WorkflowCommonModel.Props.PREVIOUS_OWNER_ID : WorkflowCommonModel.Props.OWNER_ID;
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateTypeQuery(taskType));
        queryParts.add(generateStringExactQuery(status.getName(), WorkflowCommonModel.Props.STATUS));
        queryParts.add(generateStringExactQuery(ownerId, ownerField));
        return queryParts;
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
        });
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
        }, getAllStores());
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
        Map<String, Object> props = filter.getProperties();

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

        // END: special cases

        // dynamic generation
        for (Entry<String, Object> entry : props.entrySet()) {
            QName propQName = QName.createQName(entry.getKey());
            if (DocumentLocationGenerator.CASE_LABEL_EDITABLE.equals(propQName)) {
                propQName = DocumentCommonModel.Props.CASE;
            } else if (propQName.getLocalName().contains("_")) {
                continue;
            }
            if (!propQName.equals(DocumentCommonModel.Props.SHORT_REG_NUMBER) && propQName.getNamespaceURI().equals(DocumentDynamicModel.URI)) {
                Object value = entry.getValue();
                if (value instanceof String) {
                    queryParts.add(generateStringWordsWildcardQuery((String) value, propQName));
                } else if (value instanceof List) {
                    DocumentAdminService ser = BeanHelper.getDocumentAdminService(); // including docAdminService in context.xml creates an exception because docAdminService
                    // includes DocSearchService
                    FieldDefinition def = ser.getFieldDefinition(propQName.getLocalName());
                    @SuppressWarnings("unchecked")
                    List<String> list = (List<String>) value;
                    if (StringUtils.isBlank(def.getClassificator())) {
                        queryParts.add(generateMultiStringExactQuery(list, propQName));
                    } else {
                        queryParts.add(generateMultiStringWordsWildcardQuery(list, propQName));
                    }
                } else if (value instanceof Date) {
                    Date endDate = (Date) props.get(DateGenerator.getEndDateQName(propQName));
                    queryParts.add(generateDatePropertyRangeQuery((Date) value, endDate, propQName));
                } else if (value instanceof Double || value instanceof Integer || value instanceof Long) {
                    Number maxValue = (Number) props.get(DoubleGenerator.getEndNumberQName(propQName));
                    generateNumberPropertyRangeQuery((Number) value, maxValue, propQName);
                } else if (value instanceof Boolean) {
                    queryParts.add(SearchUtil.generatePropertyBooleanQuery(propQName, (Boolean) value));
                } else if (value instanceof NodeRef) {
                    queryParts.add(generateNodeRefQuery((NodeRef) value, propQName));
                }
            }
        }

        log.info("Documents search filter: " + WmNode.toString(RepoUtil.getNotEmptyProperties(RepoUtil.toQNameProperties(props)), namespaceService));

        // Quick search (Otsisõna)
        String quickSearchInput = (String) props.get(DocumentSearchModel.Props.INPUT);
        if (StringUtils.isNotBlank(quickSearchInput)) {
            Pair<List<String>, List<Date>> quickSearchWordsAndDates = parseQuickSearchWordsAndDates(quickSearchInput);
            log.info("Quick search (document) - words: " + quickSearchWordsAndDates.getFirst().toString() + ", dates: " + quickSearchWordsAndDates.getSecond()
                    + ", from string '" + quickSearchInput + "'");
            queryParts.addAll(generateQuickSearchDocumentQuery(quickSearchWordsAndDates.getFirst(), quickSearchWordsAndDates.getSecond()));
        }

        String query = generateDocumentSearchQuery(queryParts);
        if (log.isDebugEnabled()) {
            log.debug("Documents search query construction time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return query;
    }

    private String generateQuickSearchQuery(String searchString) {
        return generateQuickSearchQuery(searchString, null);
    }

    private String generateQuickSearchQuery(String searchString, NodeRef containerNodeRef) {
        long startTime = System.currentTimeMillis();
        Pair<List<String>, List<Date>> quickSearchWordsAndDates = parseQuickSearchWordsAndDates(searchString);
        log.info("Quick search - words: " + quickSearchWordsAndDates.getFirst().toString() + ", dates: " + quickSearchWordsAndDates.getSecond()
                + ", from string '" + searchString + "'");
        String query = generateDocumentSearchQuery(generateQuickSearchDocumentQuery(quickSearchWordsAndDates.getFirst(), quickSearchWordsAndDates.getSecond()));
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
                    // Use only right wildcard in document searches
                    propQueries.add(generatePropertyWildcardQuery(property, searchWord, false, false, true));
                }
                queryParts.add(joinQueryPartsOr(propQueries, false));
            }
        }
        for (Date searchDate : searchDates) {
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
        // TODO DLSeadist -- this is probably slow, because it loads fieldDefinitions every time from repo
        // TODO is this specced somewhere?
        for (FieldDefinition fieldDefinition : BeanHelper.getDocumentAdminService().getFieldDefinitions()) {
            FieldType type = fieldDefinition.getFieldTypeEnum();
            if (type == FieldType.DATE) {
                searchPropertiesDate.add(fieldDefinition.getQName());
            } else if (type != FieldType.CHECKBOX) {
                searchProperties.add(fieldDefinition.getQName());
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

    private static String generateDocumentSearchQuery(List<String> queryParts) {
        if (isBlank(queryParts)) {
            return null;
        }
        queryParts.add(0, generateTypeQuery(DocumentCommonModel.Types.DOCUMENT)); // XXX is this accurate for dynamic documents?
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

    @SuppressWarnings("unchecked")
    private String generateTaskSearchQuery(Node filter) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(20);
        Map<String, Object> props = filter.getProperties();

        // Use both left and right wildcard in task searches
        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(TaskSearchModel.Props.STARTED_DATE_TIME_BEGIN),
                (Date) props.get(TaskSearchModel.Props.STARTED_DATE_TIME_END), WorkflowCommonModel.Props.STARTED_DATE_TIME));
        queryParts.add(generateTypeQuery((List<QName>) props.get(TaskSearchModel.Props.TASK_TYPE)));
        queryParts.add(generateMultiStringWordsWildcardQuery((List<String>) props.get(TaskSearchModel.Props.OWNER_NAME), true, true, WorkflowCommonModel.Props.OWNER_NAME));
        queryParts.add(generateMultiStringWordsWildcardQuery((List<String>) props.get(TaskSearchModel.Props.OUTCOME), true, true, WorkflowCommonModel.Props.OUTCOME));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(TaskSearchModel.Props.CREATOR_NAME), true, true, WorkflowCommonModel.Props.CREATOR_NAME));
        queryParts.add(generateMultiStringExactQuery((List<String>) props.get(TaskSearchModel.Props.DOC_TYPE), WorkflowCommonModel.Props.DOCUMENT_TYPE));
        queryParts.add(generateMultiStringExactQuery((List<String>) props.get(TaskSearchModel.Props.ORGANIZATION_NAME), WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(TaskSearchModel.Props.JOB_TITLE), true, true, WorkflowCommonModel.Props.OWNER_JOB_TITLE));
        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(TaskSearchModel.Props.DUE_DATE_TIME_BEGIN),
                (Date) props.get(TaskSearchModel.Props.DUE_DATE_TIME_END), WorkflowSpecificModel.Props.DUE_DATE));
        if (Boolean.TRUE.equals(props.get(TaskSearchModel.Props.ONLY_RESPONSIBLE))) {
            queryParts.add(generateAspectQuery(WorkflowSpecificModel.Aspects.RESPONSIBLE));
        }
        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(TaskSearchModel.Props.COMPLETED_DATE_TIME_BEGIN),
                (Date) props.get(TaskSearchModel.Props.COMPLETED_DATE_TIME_END), WorkflowCommonModel.Props.COMPLETED_DATE_TIME));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(TaskSearchModel.Props.COMMENT), true, true, WorkflowSpecificModel.Props.COMMENT));
        queryParts.add(generateStringWordsWildcardQuery((String) props.get(TaskSearchModel.Props.RESOLUTION), true, true, WorkflowSpecificModel.Props.RESOLUTION,
                WorkflowSpecificModel.Props.WORKFLOW_RESOLUTION));
        queryParts.add(generateMultiStringExactQuery((List<String>) props.get(TaskSearchModel.Props.STATUS), WorkflowCommonModel.Props.STATUS));
        if (Boolean.TRUE.equals(props.get(TaskSearchModel.Props.COMPLETED_OVERDUE))) {
            queryParts.add(generatePropertyBooleanQuery(WorkflowSpecificModel.Props.COMPLETED_OVERDUE, true));
        }
        queryParts.add(generateDatePropertyRangeQuery((Date) props.get(TaskSearchModel.Props.STOPPED_DATE_TIME_BEGIN),
                (Date) props.get(TaskSearchModel.Props.STOPPED_DATE_TIME_END), WorkflowCommonModel.Props.STOPPED_DATE_TIME));

        log.info("Tasks search filter: " + WmNode.toString(RepoUtil.getNotEmptyProperties(RepoUtil.toQNameProperties(props)), namespaceService));

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

    private List<Task> searchTasksImpl(String query, int limit, String queryName) {
        return searchGeneralImpl(query, limit, queryName, new SearchCallback<Task>() {

            @Override
            public Task addResult(ResultSetRow row) {
                return workflowService.getTask(row.getNodeRef(), true);
            }
        }, getAllStoresWithArchivalStoreVOs());
    }

    private List<TaskInfo> searchTaskInfosImpl(String query, int limit, String queryName) {
        return searchGeneralImpl(query, limit, queryName, new SearchCallback<TaskInfo>() {
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

    private List<Document> searchDocumentsImpl(String query, int limit, String queryName) {
        return searchDocumentsImpl(query, limit, queryName, null);
    }

    private List<Document> searchDocumentsImpl(String query, int limit, String queryName, Collection<StoreRef> storeRefs) {
        return searchGeneralImpl(query, limit, queryName, new SearchCallback<Document>() {

            @Override
            public Document addResult(ResultSetRow row) {
                return documentService.getDocumentByNodeRef(row.getNodeRef());
            }
        }, storeRefs);
    }

    private List<DocumentDynamic> searchDocumentDynamicsImpl(String query, int limit, String queryName) {
        return searchDocumentDynamicsImpl(query, limit, queryName, null);
    }

    private List<DocumentDynamic> searchDocumentDynamicsImpl(String query, int limit, String queryName, Collection<StoreRef> storeRefs) {
        return searchGeneralImpl(query, limit, queryName, new SearchCallback<DocumentDynamic>() {

            @Override
            public DocumentDynamic addResult(ResultSetRow row) {
                return getDocumentDynamicService().getDocument(row.getNodeRef());
            }
        }, storeRefs);
    }

    private List<Document> searchDocumentsAndCaseTitlesImpl(String query, int limit, String queryName) {
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
        });
    }

    private List<Series> searchSeriesImpl(String query, int limit, String queryName) {
        return searchGeneralImpl(query, limit, queryName, new SearchCallback<Series>() {
            @Override
            public Series addResult(ResultSetRow row) {
                return seriesService.getSeriesByNodeRef(row.getNodeRef());
            }
        });
    }

    private List<String> searchAdrDocumentTypesImpl(String query, int limit, String queryName) {
        return searchGeneralImpl(query, limit, queryName, new SearchCallback<String>() {
            @Override
            public String addResult(ResultSetRow row) {
                return ((QName) nodeService.getProperty(row.getNodeRef(), AdrModel.Props.DOCUMENT_TYPE)).getLocalName();
            }
        });
    }

    @Override
    public List<NodeRef> searchNodes(String query, int limit, String queryName) {
        return searchNodes(query, limit, queryName, null);
    }

    private List<NodeRef> searchNodesFromAllStores(String query, String queryName) {
        List<ResultSet> resultSets = doSearches(query, -1, queryName, getAllStores());
        try {
            List<NodeRef> nodeRefs = new ArrayList<NodeRef>();
            for (ResultSet resultSet : resultSets) {
                nodeRefs.addAll(resultSet.getNodeRefs());
            }
            return removeNonExistingNodeRefs(nodeRefs);
        } finally {
            try {
                for (ResultSet resultSet : resultSets) {
                    resultSet.close();
                }
            } catch (Exception e) {
                // Do nothing
            }
        }
    }

    private <E extends Comparable<? super E>> List<E> searchGeneralImpl(String query, int limit, String queryName, SearchCallback<E> callback) {
        return searchGeneralImpl(query, limit, queryName, callback, null);
    }

    private <E extends Comparable<? super E>> List<E> searchGeneralImpl( //
            String query, int limit, String queryName, SearchCallback<E> callback, Collection<StoreRef> storeRefs) {
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
            resultSets = Arrays.asList(doSearch(query, limit, queryName, singleStoreRef));
        } else {
            resultSets = doSearches(query, limit, queryName, storeRefs);
        }
        final List<E> extractResults = extractResults(callback, startTime, resultSets, limit);
        Collections.sort(extractResults);
        return extractResults;
    }

    private <E> List<E> extractResults(SearchCallback<E> callback, long startTime, final List<ResultSet> resultSets, int limit) {
        try {
            List<E> result = new ArrayList<E>();
            if (log.isDebugEnabled()) {
                long resultsCount = 0;
                for (ResultSet resultSet : resultSets) {
                    resultsCount += resultSet.length();
                }
                log.debug("Lucene search time " + (System.currentTimeMillis() - startTime) + " ms, results: " + resultsCount);
                startTime = System.currentTimeMillis();
            }

            for (ResultSet resultSet : resultSets) {
                for (ResultSetRow row : resultSet) {
                    if (!nodeService.exists(row.getNodeRef())) {
                        continue;
                    }
                    E item = callback.addResult(row);
                    if (item != null) {
                        result.add(item);
                    }
                    if (limit > -1 && result.size() >= limit) {
                        break;
                    }
                }
                if (limit > -1 && result.size() >= limit) {
                    break;
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("Results construction time " + (System.currentTimeMillis() - startTime) + " ms, final results: " + result.size());
            }
            return result;
        } finally {
            try {
                for (ResultSet resultSet : resultSets) {
                    resultSet.close();
                }
            } catch (Exception e) {
                // Do nothing
            }
        }
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
            List<StoreRef> storeList = new ArrayList<StoreRef>();
            storeList.add(generalService.getStore());
            for (ArchivalsStoreVO storeVO : generalService.getArchivalsStoreVOs()) {
                storeList.add(storeVO.getStoreRef());
            }
            allStoresWithArchivalStoreVOs = storeList;
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

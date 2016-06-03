package ee.webmedia.alfresco.document.search.service;

import static ee.webmedia.alfresco.common.search.DbSearchUtil.generateNotQuery;
import static ee.webmedia.alfresco.common.search.DbSearchUtil.generateTaskDatePropertyRangeQuery;
import static ee.webmedia.alfresco.common.search.DbSearchUtil.generateTaskFieldExactQuery;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType.INCOMING_LETTER;
import static ee.webmedia.alfresco.utils.SearchUtil.generateAndNotQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateAspectQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateDatePropertyRangeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateLuceneSearchParams;
import static ee.webmedia.alfresco.utils.SearchUtil.generateMultiNodeRefQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateMultiStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateNodeRefQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateNumberPropertyRangeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateParentPathQuery;
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
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel;
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
import ee.webmedia.alfresco.common.service.ApplicationConstantsBean;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.UnmodifiableFieldDefinition;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.docconfig.generator.fieldtype.DateGenerator;
import ee.webmedia.alfresco.docconfig.generator.fieldtype.DoubleGenerator;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.search.model.AssocSearchObjectType;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.document.search.web.DocumentDynamicSearchDialog;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.eventplan.model.EventPlanModel;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.search.service.AbstractSearchServiceImpl;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.model.UnmodifiableSeries;
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
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.search.model.CompoundWorkflowSearchModel;
import ee.webmedia.alfresco.workflow.search.model.TaskSearchModel;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.SendStatus;

public class DocumentSearchServiceImpl extends AbstractSearchServiceImpl implements DocumentSearchService {

    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentSearchServiceImpl.class);

    private static final Set<QName> DOCUMENT_SERIES_PROP_QNAME_SET = new HashSet<QName>();
    private static final Set<QName> SERIES_DOCUMENTS_VISIBLE_FOR_USERS_WITHOUT_ACCESS_QNAME_SET = new HashSet<QName>();

    private DocumentService documentService;
    private DocumentDynamicService _documentDynamicService;
    private SeriesService seriesService;
    private VolumeService volumeService;
    private WorkflowService _workflowService;
    private NamespaceService namespaceService;
    private AuthorityService authorityService;
    private UserService userService;
    private LogService logService;
    private ApplicationConstantsBean applicationConstantsBean;
    private boolean finishedIncomingLettersAreNotShown;

    static {
        DOCUMENT_SERIES_PROP_QNAME_SET.add(DocumentCommonModel.Props.SERIES);
        SERIES_DOCUMENTS_VISIBLE_FOR_USERS_WITHOUT_ACCESS_QNAME_SET.add(SeriesModel.Props.DOCUMENTS_VISIBLE_FOR_USERS_WITHOUT_ACCESS);
    }

    private static final List<String> PRELOADED_RECIPIENT_FINISHED_QUERY_PARTS;

    static {
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(SearchUtil.generateUnsentDocQuery());
        PRELOADED_RECIPIENT_FINISHED_QUERY_PARTS = Collections.unmodifiableList(queryParts);
    }

    private List<StoreRef> allStores = null;

    @Override
    public List<NodeRef> searchActiveLocks() {
        String query = SearchUtil.joinQueryPartsAnd(SearchUtil.generateAspectQuery(ContentModel.ASPECT_LOCKABLE),
                SearchUtil.generateTypeQuery(DocumentCommonModel.Types.DOCUMENT, ContentModel.TYPE_CONTENT, WorkflowCommonModel.Types.COMPOUND_WORKFLOW),
                SearchUtil.generatePropertyNotNullQuery(ContentModel.PROP_LOCK_OWNER),
                SearchUtil.generateDatePropertyRangeQuery(new Date(), null, ContentModel.PROP_EXPIRY_DATE));

        return searchNodesFromAllStores(query, "searchActiveLocks");
    }

    @Override
    public List<NodeRef> searchDueContracts() {
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

        List<NodeRef> contracts = searchDocumentsImpl(query, -1, /* queryName */"contractDueDate", getAllStoresWithArchivalStoreVOs()).getFirst();

        if (log.isDebugEnabled()) {
            log.debug("Search for contracts with due date took " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return contracts;
    }

    @Override
    public List<NodeRef> searchDiscussionDocuments() {
        long startTime = System.currentTimeMillis();

        String query = generateDiscussionDocumentsQuery();
        List<NodeRef> results = searchDocumentsImpl(query, -1, /* queryName */"discussionDocuments").getFirst();

        if (log.isDebugEnabled()) {
            log.debug("Discussion documents search total time " + (System.currentTimeMillis() - startTime) + " ms, results " + results.size() //
                    + ", query: " + query);
        }
        return results;
    }

    @Override
    public int getDiscussionDocumentsCount(int limit) {
        long startTime = System.currentTimeMillis();

        String query = generateDiscussionDocumentsQuery();
        ResultSet results = doSearch(query, limit, /* queryName */"discussionDocumentsCount", null);
        int count = countResults(Collections.singletonList(results));

        if (log.isDebugEnabled()) {
            log.debug("Discussion documents count search total time " + (System.currentTimeMillis() - startTime) + " ms, results " + count//
                    + ", query: " + query);
        }
        return count;
    }

    @Override
    public NodeRef searchOrganizationNodeRef(String orgEmail, String orgName) {
        long startTime = System.currentTimeMillis();
        String query = joinQueryPartsAnd(generateTypeQuery(AddressbookModel.Types.ORGANIZATION),
                SearchUtil.generatePropertyExactQuery(AddressbookModel.Props.EMAIL, orgEmail),
                SearchUtil.generatePropertyExactQuery(AddressbookModel.Props.ORGANIZATION_NAME, orgName));

        ResultSet resultSet = doSearch(query, 2, "searchOrganizationNodeRef", null);
        List<NodeRef> orgRefs = resultSet.getNodeRefs();
        int orgRefsSize = orgRefs != null ? orgRefs.size() : 0;
        if (orgRefsSize == 1) {
            return orgRefs.get(0);
        } else if (orgRefsSize > 1) {
            log.warn("Organization nodeRef search returned more than 1 result, returning null");
        }
        if (log.isDebugEnabled()) {
            log.debug("Organization nodeRef search total time " + (System.currentTimeMillis() - startTime) + " ms, results " + orgRefs.size()
                    + ", query: " + query);
        }
        return null;
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
    public Set<NodeRef> searchAdrDocuments(Date modifiedDateBegin, Date modifiedDateEnd, Set<String> documentTypeIds) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>();
        if (modifiedDateBegin != null && modifiedDateEnd != null) {
            queryParts.add(generateDatePropertyRangeQuery(modifiedDateBegin, modifiedDateEnd, ContentModel.PROP_MODIFIED));
        }

        String query = generateAdrDocumentSearchQuery(queryParts, documentTypeIds);
        // Only search from SpacesStore and ArchivalsStore to get correct document set (PPA).
        Set<NodeRef> results = new HashSet<>(searchNodes(query, -1, /* queryName */"adrDocumentByModified1"));
        results.addAll(searchNodes(query, -1, /* queryName */"adrDocumentByModified2", generalService.getArchivalsStoreRefs(), true).getFirst());
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
    public List<NodeRef> searchIncomingLetterRegisteredDocuments(String senderRegNumber) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(4);
        queryParts.add(generateStringExactQuery(SystematicDocumentType.INCOMING_LETTER.getId(), DocumentAdminModel.Props.OBJECT_TYPE_ID));
        queryParts.add(generateStringNotEmptyQuery(DocumentCommonModel.Props.REG_DATE_TIME));
        queryParts.add(generateStringExactQuery(senderRegNumber, DocumentSpecificModel.Props.SENDER_REG_NUMBER));

        String query = generateDocumentSearchQuery(queryParts, null);
        List<NodeRef> results = searchDocumentsImpl(query, -1, /* queryName */"incomingLetterRegisteredDocuments").getFirst();
        if (log.isDebugEnabled()) {
            log.debug("Registered incoming letter documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public Pair<List<NodeRef>, Boolean> searchTodayRegisteredDocuments(String searchString, int limit) {
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
        Pair<List<NodeRef>, Boolean> results = searchDocumentsImpl(query, limit, /* queryName */"todayRegisteredDocuments");
        if (log.isDebugEnabled()) {
            log.debug("Today registered documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public List<NodeRef> searchInProcessUserDocuments() {
        long startTime = System.currentTimeMillis();
        String query = getInProcessDocumentsOwnerQuery(AuthenticationUtil.getRunAsUser());
        List<NodeRef> results = searchDocumentsImpl(query, -1, /* queryName */"inProcessUserDocuments", getAllStores()).getFirst();
        if (log.isDebugEnabled()) {
            log.debug("Current user's in process documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public int searchInProcessUserDocumentsCount(int limit) {
        long startTime = System.currentTimeMillis();
        String query = getInProcessDocumentsOwnerQuery(AuthenticationUtil.getRunAsUser());
        List<ResultSet> resultSets = doSearches(query, limit, "inProcessUserDocumentsCount", getAllStores());
        if (log.isDebugEnabled()) {
            log.debug("Current user's and WORKING documents count search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return countResults(resultSets);
    }

    @Override
    public List<NodeRef> searchAccessRestictionEndsAfterDate(Date restrictionEndDate) {
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
        List<NodeRef> results = searchDocumentsImpl(query, -1, /* queryName */"accessRestictionEndsAfterDate", storeRefs).getFirst();
        if (log.isDebugEnabled()) {
            log.debug("Search for documents with access restriction took " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public List<NodeRef> searchRecipientFinishedDocuments() {
        long startTime = System.currentTimeMillis();
        String query = generateRecipientFinichedQuery();
        List<NodeRef> results = searchDocumentsImpl(query, -1, /* queryName */"recipientFinishedDocuments").getFirst();

        if (log.isDebugEnabled()) {
            log.debug("FINISHED documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public int searchRecipientFinishedDocumentsCount(int limit) {
        long startTime = System.currentTimeMillis();
        String query = generateRecipientFinichedQuery();
        List<NodeRef> results = searchNodes(query, limit, /* queryName */"recipientFinishedDocumentsCount");

        if (log.isDebugEnabled()) {
            log.debug("FINISHED documents count search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results.size();
    }

    private String generateRecipientFinichedQuery() {
        return generateDocumentSearchQuery(new ArrayList<>(PRELOADED_RECIPIENT_FINISHED_QUERY_PARTS), null);
    }

    @Override
    public List<UnmodifiableSeries> searchSeriesUnit(String unit) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateStringExactQuery(unit, SeriesModel.Props.STRUCT_UNIT));
        String query = generateSeriesSearchQuery(queryParts);
        List<UnmodifiableSeries> results = searchSeriesImpl(query, -1, /* queryName */"seriesUnit");

        if (log.isDebugEnabled()) {
            log.debug("FINISHED documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results;
    }

    @Override
    public NodeRef searchSeriesByIdentifier(String identifier) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateStringExactQuery(identifier, SeriesModel.Props.SERIES_IDENTIFIER));
        String query = generateSeriesSearchQuery(queryParts);
        List<NodeRef> results = searchNodes(query, -1, /* queryName */"seriesByIdentifier");

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
    public List<Pair<NodeRef, QName>> searchCurrentUsersInProgressTaskRefs(boolean onlyOverdueOrToday, QName... taskType) {
        long startTime = System.currentTimeMillis();
        Pair<List<String>, List<Object>> queryPartsAndArgs = new Pair<List<String>, List<Object>>(new ArrayList<String>(), new ArrayList<>());
        String query = generateUserInProgressTaskQuery(onlyOverdueOrToday, false, queryPartsAndArgs, taskType);
        String orderClause = " order by wfs_due_date desc nulls last";
        Pair<List<Pair<NodeRef, QName>>, Boolean> results = BeanHelper.getWorkflowDbService().searchTaskNodeRefAndType(query, orderClause, queryPartsAndArgs.getSecond(), -1);
        if (log.isDebugEnabled()) {
            log.debug("Current user's and IN_PROGRESS tasks search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results.getFirst();
    }

    @Override
    public <T extends Object> List<T> searchCurrentUsersTasksInProgress(RowMapper<T> rowMapper, QName... taskType) {
        long startTime = System.currentTimeMillis();
        Pair<List<String>, List<Object>> queryPartsAndArgs = new Pair<List<String>, List<Object>>(new ArrayList<String>(), new ArrayList<>());
        String query = generateUserInProgressTaskQuery(false, rowMapper != null, queryPartsAndArgs, taskType);
        Pair<List<T>, Boolean> results = BeanHelper.getWorkflowDbService().searchTasksAllStores(query, queryPartsAndArgs.getSecond(), -1, rowMapper);
        if (log.isDebugEnabled()) {
            log.debug("Current user's and IN_PROGRESS tasks search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return results.getFirst();
    }

    private String generateUserInProgressTaskQuery(boolean onlyOverdueOrToday, boolean excludeCaseFileWorkflows, Pair<List<String>, List<Object>> queryPartsAndArgs,
            QName... taskType) {
        getTaskQuery(AuthenticationUtil.getRunAsUser(), Status.IN_PROGRESS, false, queryPartsAndArgs, taskType);
        addSubstitutionRestriction(queryPartsAndArgs);
        if (onlyOverdueOrToday) {
            addTaskOverdueCondition(queryPartsAndArgs);
        }
        if (excludeCaseFileWorkflows) {
            queryPartsAndArgs.getFirst().add(DbSearchUtil.generateTaskPropertyNotQuery(WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_TYPE));
            queryPartsAndArgs.getSecond().add(CompoundWorkflowType.CASE_FILE_WORKFLOW.toString());
        }
        String query = generateTaskSearchQuery(queryPartsAndArgs.getFirst());
        return query;
    }

    private void addTaskOverdueCondition(Pair<List<String>, List<Object>> queryPartsAndArgs) {
        Date today = Calendar.getInstance().getTime();
        today.setHours(23);
        today.setMinutes(59);
        today.setSeconds(59);
        queryPartsAndArgs.getFirst().add(" wfs_due_date <= ? ");
        queryPartsAndArgs.getSecond().add(today);
    }

    @Override
    public List<Task> searchCurrentUsersTaskInProgressWithoutParents(QName taskType, boolean allStoresSearch) {
        long startTime = System.currentTimeMillis();
        Pair<List<String>, List<Object>> queryPartsAndArguments = getTaskQuery(AuthenticationUtil.getRunAsUser(), Status.IN_PROGRESS, false, taskType);
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
    public int getCurrentUserCompoundWorkflowsCount(int limit) {
        return searchCurrentUserCompoundWorkflowRefs(limit).size();
    }

    private List<NodeRef> searchCurrentUserCompoundWorkflowRefs(int limit) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = Arrays.asList(
                generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW),
                generatePropertyExactQuery(WorkflowCommonModel.Props.STATUS, Arrays.asList(Status.NEW.getName(), Status.IN_PROGRESS.getName(), Status.STOPPED.getName())),
                generateStringExactQuery(AuthenticationUtil.getRunAsUser(), WorkflowCommonModel.Props.OWNER_ID));
        String query = joinQueryPartsAnd(queryParts);
        List<NodeRef> compoundWorkflows = new ArrayList<NodeRef>();
        compoundWorkflows.addAll(searchNodes(query, limit, /* queryName */"currentUserCompoundWorkflowsFromMainStore"));
        compoundWorkflows.addAll(searchNodes(query, limit, /* queryName */"currentUserCompoundWorkflowsFromArchivalsStore",
                Collections.singletonList(generalService.getArchivalsStoreRef())).getFirst());
        if (log.isDebugEnabled()) {
            log.debug("Current user's compound workflows search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return compoundWorkflows;
    }

    @Override
    public List<NodeRef> searchCurrentUserCompoundWorkflowRefs() {
        return searchCurrentUserCompoundWorkflowRefs(-1);
    }

    private List<NodeRef> searchCurrentUserCaseFilesRefs(int limit) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = Arrays.asList(
                generateTypeQuery(CaseFileModel.Types.CASE_FILE),
                generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE),
                generateStringExactQuery(DocListUnitStatus.OPEN.getValueName(), DocumentDynamicModel.Props.STATUS),
                generateStringExactQuery(AuthenticationUtil.getRunAsUser(), DocumentDynamicModel.Props.OWNER_ID));
        String query = joinQueryPartsAnd(queryParts);
        List<NodeRef> caseFiles = searchNodes(query, limit, /* queryName */"currentUserCaseFileFromMainStore");
        if (log.isDebugEnabled()) {
            log.debug("Current user's case file search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return caseFiles;
    }

    private List<NodeRef> searchCurrentUserCaseFilesRefs() {
        return searchCurrentUserCaseFilesRefs(-1);
    }

    @Override
    public int getCurrentUserCaseFilesCount(int limit) {
        return searchCurrentUserCaseFilesRefs(limit).size();
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
        queryParts.add(DbSearchUtil.generateTaskPropertyNotNullQuery(WorkflowSpecificModel.Props.INSTITUTION_CODE));
        queryParts.add(DbSearchUtil.generateTaskPropertyNotNullQuery(WorkflowSpecificModel.Props.CREATOR_INSTITUTION_CODE));
        queryParts.add(" lower(" + DbSearchUtil.getDbFieldNameFromPropQName(WorkflowSpecificModel.Props.INSTITUTION_CODE) + ") not like lower("
                + DbSearchUtil.getDbFieldNameFromPropQName(WorkflowSpecificModel.Props.CREATOR_INSTITUTION_CODE) + ")");
        arguments.add(Status.NEW.getName());

        String query = generateTaskSearchQuery(queryParts);
        List<Task> tasks = BeanHelper.getWorkflowDbService().searchTasksAllStores(query, arguments, -1).getFirst();

        List<Task> tasksWithParents = new ArrayList<>();
        for (Task task : tasks) {
            tasksWithParents.add(getWorkflowService().getTask(task.getNodeRef(), true));
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
            NodeRef docRef = getWorkflowService().getCompoundWorkflow(compoundWorkflowRef).getParent();
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
            results = new HashSet<>(searchAuthorityGroups(input, limit));
        }

        if (!BeanHelper.getApplicationConstantsBean().isEinvoiceEnabled()) {
            String accountantsGroup = userService.getAccountantsGroup();
            results.remove(accountantsGroup);
        }

        String administratorsGroup = userService.getAdministratorsGroup();
        String documentManagersGroup = userService.getDocumentManagersGroup();
        for (String name : results) {
            if (withAdminsAndDocManagers || (!administratorsGroup.equals(name) && !documentManagersGroup.equals(name))) {
                authorities.add(userService.getAuthority(name));
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
        Map<QName, Pair<Long, QName>> propertyTypes = new HashMap<QName, Pair<Long, QName>>();
        for (NodeRef authorityRef : authorityRefs) {
            String authorityName = (String) nodeService.getProperty(authorityRef, ContentModel.PROP_AUTHORITY_DISPLAY_NAME, propertyTypes);
            if (groupName.equalsIgnoreCase(authorityName)) {
                authorityNames.add((String) nodeService.getProperty(authorityRef, ContentModel.PROP_AUTHORITY_NAME, propertyTypes));
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

    private List<String> searchAuthorityGroups(String groupName, int limit) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>(2);
        queryParts.add(generateTypeQuery(ContentModel.TYPE_AUTHORITY_CONTAINER));
        // Use both left and right wildcard in user/group/contact/contactgroup/org.unit searches
        queryParts.add(generateStringWordsWildcardQuery(groupName, true, true, ContentModel.PROP_AUTHORITY_DISPLAY_NAME));

        String query = joinQueryPartsAnd(queryParts);
        List<String> results = searchProperty(query, limit, /* queryName */"authorityGroups", ContentModel.PROP_AUTHORITY_NAME, String.class).getFirst();
        if (log.isDebugEnabled()) {
            log.debug("Authority groups search total time " + (System.currentTimeMillis() - startTime) + " ms, results " + results.size() //
                    + ", query: " + query);
        }
        return results;
    }

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
    public Map<QName, Integer> getCurrentUserTaskCountByType(QName... taskType) {
        Assert.notEmpty(taskType, "Must specify at least one task type!");

        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>();
        List<Object> arguments = new ArrayList<Object>();
        String ownerId = AuthenticationUtil.getRunAsUser();

        addTaskStringExactPartsAndArgs(queryParts, arguments, Status.IN_PROGRESS.getName(), WorkflowCommonModel.Props.STATUS);
        addTaskStringExactPartsAndArgs(queryParts, arguments, ownerId, WorkflowCommonModel.Props.OWNER_ID);
        queryParts.add(DbSearchUtil.generateTaskPropertyNotQuery(WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_TYPE));
        arguments.add(CompoundWorkflowType.CASE_FILE_WORKFLOW.toString());
        addSubstitutionRestriction(new Pair<List<String>, List<Object>>(queryParts, arguments));
        String query = generateTaskSearchQuery(queryParts);

        Map<QName, Integer> counts = BeanHelper.getWorkflowDbService().countTasksByType(query, arguments, taskType);
        if (log.isDebugEnabled()) {
            log.debug("Current user's and IN_PROGRESS tasks count by type search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return counts;
    }

    @Override
    public int getCurrentUsersTaskCount(QName taskType) {
        return getCurrentUsersTaskCount(false, taskType);
    }

    @Override
    public int getCurrentUsersUnseenTasksCount(QName[] taskTypes) {
        return getCurrentUsersTaskCount(true, taskTypes);
    }

    private int getCurrentUsersTaskCount(boolean isViewedByOwner, QName... taskType) {
        long startTime = System.currentTimeMillis();
        List<String> queryParts = new ArrayList<String>();
        List<Object> arguments = new ArrayList<Object>();
        String ownerId = AuthenticationUtil.getRunAsUser();
        if (taskType != null) {
            addTaskTypeFieldExactQueryPartsAndArguments(queryParts, arguments, taskType);
        }
        if (isViewedByOwner) {
            queryParts.add(
                    SearchUtil.joinQueryPartsAnd(
                            SearchUtil.joinQueryPartsOr(
                                    DbSearchUtil.generateTaskPropertyExactQuery(WorkflowCommonModel.Props.VIEWED_BY_OWNER),
                                    DbSearchUtil.generateTaskPropertyNullQuery(WorkflowCommonModel.Props.VIEWED_BY_OWNER)),
                            DbSearchUtil.generateTaskPropertyNotQuery(WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_TYPE))
                    );
            arguments.add(Boolean.FALSE);
            arguments.add(CompoundWorkflowType.CASE_FILE_WORKFLOW.toString());
        }
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
        List<NodeRef> result = BeanHelper.getWorkflowDbService().searchTaskNodeRefs(query, arguments, -1).getFirst();
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
    public Pair<List<NodeRef>, Boolean> queryCompoundWorkflows(Node filter, int limit) {
        long startTime = System.currentTimeMillis();
        
        Pair<List<NodeRef>, Boolean> results;
        
        if (userService.isGuest()) {
        	Pair<String, List<Object>> queryAndArguments = generateTaskSearchQuery(filter);
            if (queryAndArguments == null) {
                return new Pair<>(Collections.<NodeRef> emptyList(), Boolean.FALSE);
            }
            results = BeanHelper.getWorkflowDbService().searchTaskCompoundWorkflowsNodeRefs(queryAndArguments.getFirst(), AuthenticationUtil.getRunAsUser(), queryAndArguments.getSecond(), limit);
        } else {
        	String query = generateCompoundWorkflowSearchQuery(filter);
        	results = searchNodes(query, limit, /* queryName */"compoundWorkflowByFilter", null, false);
        }
        
        
        if (log.isDebugEnabled()) {
            log.debug("CompoundWorkflow search total time " + (System.currentTimeMillis() - startTime) + " ms");
        }
        return results;
    }

    @Override
    public List<NodeRef> searchTasksForReport(Node filter, String userNamme) {
        return searchTaskRefs(filter, userNamme, -1).getFirst();
    }

    @Override
    public Pair<List<NodeRef>, Boolean> searchTaskRefs(Node filter, String username, int limit) {
        long startTime = System.currentTimeMillis();
        Pair<String, List<Object>> queryAndArguments = generateTaskSearchQuery(filter);
        if (queryAndArguments == null) {
            return new Pair<>(Collections.<NodeRef> emptyList(), Boolean.FALSE);
        }
        Pair<List<NodeRef>, Boolean> results;
        if (!BeanHelper.getWorkflowConstantsBean().isDocumentWorkflowEnabled()) {
            results = BeanHelper.getWorkflowDbService().searchTaskNodeRefs(queryAndArguments.getFirst(), queryAndArguments.getSecond(), limit);
        } else {
            results = BeanHelper.getWorkflowDbService().searchTaskNodeRefsCheckLimitedSeries(queryAndArguments.getFirst(), username, queryAndArguments.getSecond(), limit);
        }
        if (log.isDebugEnabled()) {
            log.debug("Tasks search total time " + (System.currentTimeMillis() - startTime) + " ms");
        }
        return results;
    }

    private void addSubstitutionRestriction(Pair<List<String>, List<Object>> queryPartsAndArgs) {
        if (!applicationConstantsBean.isSubstitutionTaskEndDateRestricted()) {
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
    public List<NodeRef> searchDocumentsForRegistering() {
        long startTime = System.currentTimeMillis();
        String query = generateDocumentsForRegisteringQuery();
        List<NodeRef> results = searchDocumentsImpl(query, -1, /* queryName */"documentsForRegistering").getFirst();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Documents for registering search total time %d ms, query: %s" //
                    , (System.currentTimeMillis() - startTime), query));
        }
        return results;
    }

    @Override
    public int getCountOfDocumentsForRegistering(int limit) {
        long startTime = System.currentTimeMillis();
        String query = generateDocumentsForRegisteringQuery();
        int count = searchNodes(query, limit, /* queryName */"documentsForRegisteringCount").size();

        if (log.isDebugEnabled()) {
            log.debug(String.format("Documents for registering count search total time %d ms, query: %s" //
                    , (System.currentTimeMillis() - startTime), query));
        }
        return count;
    }
    
    /**
     * query documents with storeRefs provided
     * @param filter
     * @param limit
     * @param storeRefs
     * @return
     */
    private List<NodeRef> queryDocuments(Node filter, int limit, List<StoreRef> storeRefs) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> properties = filter.getProperties();
        
        String query = generateDocumentSearchQuery(filter, storeRefs);
        if (StringUtils.isBlank(query)) {
            throw new UnableToPerformException(UnableToPerformException.MessageSeverity.INFO, "docSearch_error_noInput");
        }
        try {
            Pair<List<NodeRef>, Boolean> results = searchDocumentsImpl(query, limit, /* queryName */"documentsByFilter", storeRefs);
            
            
            if (log.isDebugEnabled()) {
                log.debug("Documents search total time " + (System.currentTimeMillis() - startTime) + " ms");
            }
            return results.getFirst();
        } catch (RuntimeException e) {
            Map<QName, Serializable> filterProps = RepoUtil.getNotEmptyProperties(RepoUtil.toQNameProperties(properties));
            log.error("Document search failed: "
                    + e.getMessage()
                    + "\n  searchFilter=" + WmNode.toString(filterProps, namespaceService)
                    + "\n  query=" + query, e);
            throw e;
        }
    }
    
    @Override
    public Pair<List<NodeRef>, Boolean> queryDocuments(Node filter, int limit) {
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
            Pair<List<NodeRef>, Boolean> results = searchDocumentsImpl(query, limit, /* queryName */"documentsByFilter", storeRefs);
            
            
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

    @Override
    public List<NodeRef> searchAllDocumentRefsByParentRef(NodeRef parentRef) {
        return searchAllDocumentsByParentRef(parentRef, -1).getFirst();
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
        Pair<List<NodeRef>, Boolean> results = searchNodes(query, limit, "allDocumentsByParentRef", Collections.singletonList(parentRef.getStoreRef()));
     // in case of GUEST user take out open for all documents if guest user or group is not set
        if (results != null && userService.isGuest()) {
        	results.setFirst(filterDocumentsForGuest(results.getFirst()));
        }
        return results;
    }

    @Override
    public Pair<List<NodeRef>, Boolean> searchAllDocumentRefsByParentRefCheckExists(NodeRef parentRef, int limit) {
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

        Pair<List<NodeRef>, Boolean> result = searchNodes(query, limit, "allDocumentsByParentRef", Collections.singletonList(parentRef.getStoreRef()));
        // in case of GUEST user take out open for all documents if guest user or group is not set
        if (result != null && userService.isGuest()) {
        	result.setFirst(filterDocumentsForGuest(result.getFirst()));
        }
        List<NodeRef> childrenFromDb = bulkLoadNodeService.loadChildDocNodeRefs(parentRef);
        for (Iterator<NodeRef> i = result.getFirst().iterator(); i.hasNext();) {
            if (!childrenFromDb.contains(i.next())) {
                i.remove();
            }
        }
        return result;
    }

    @Override
    public Pair<List<VolumeOrCaseFile>, Boolean> queryVolumes(Node filter, int limit) {
    	int startFrom = 0;
        long startTime = System.currentTimeMillis();
        Map<String, Object> properties = filter.getProperties();
        List<StoreRef> storeRefs = getVolumeSearchStoreRefs(properties);
        String query = generateVolumeSearchQuery(filter);
        if (StringUtils.isBlank(query)) {
            throw new UnableToPerformException(UnableToPerformException.MessageSeverity.INFO, "volSearch_error_noInput");
        }
        try {
            Pair<List<VolumeOrCaseFile>, Boolean> results = searchVolumesImpl(query, startFrom, limit, /* queryName */"volumesByFilter", storeRefs);
            
            // in case of GUESTS users do extra filtering
            if (userService.isGuest() && results != null && results.getFirst() != null && !results.getFirst().isEmpty()) {
            	List<VolumeOrCaseFile> foundVolumeOrCaseFiles = results.getFirst();
            	Set<VolumeOrCaseFile> filteredVolumeOrCaseFiles = new HashSet<>();
            	Date dateCreatedBegin = new Date();
            	for (VolumeOrCaseFile volumeOrCaseFile: foundVolumeOrCaseFiles) {
            		if (dateCreatedBegin.after(volumeOrCaseFile.getValidFrom())) {
            			dateCreatedBegin = volumeOrCaseFile.getValidFrom();
            		}
            	}
            	
            	List<String> queryParts = new ArrayList<>();
                Date dateCreatedEnd = new Date();
                queryParts.add(generateDatePropertyRangeQuery(dateCreatedBegin, dateCreatedEnd, ContentModel.PROP_CREATED));
            	String docQuery = generateDocumentSearchQuery(queryParts, storeRefs, null);
            	List<NodeRef> docNodeRefs = new ArrayList<>();
            	try {
                    Pair<List<NodeRef>, Boolean> docResults = searchDocumentsImpl(docQuery, -1, /* queryName */"documentsByFilter", storeRefs);
                    
                    if (log.isDebugEnabled()) {
                        log.debug("Documents search total time " + (System.currentTimeMillis() - startTime) + " ms");
                    }
                    docNodeRefs = docResults.getFirst();
                } catch (RuntimeException e) {
                    Map<QName, Serializable> filterProps = RepoUtil.getNotEmptyProperties(RepoUtil.toQNameProperties(properties));
                    log.error("Document search failed: "
                            + e.getMessage()
                            + "\n  searchFilter=" + WmNode.toString(filterProps, namespaceService)
                            + "\n  query=" + query, e);
                    throw e;
                }
            	Set<NodeRef> volumeCaseFileRefs = new HashSet<>();
            	for (NodeRef docRef: docNodeRefs) {
            		NodeRef volRef = (NodeRef) nodeService.getProperty(docRef, DocumentCommonModel.Props.VOLUME);
            		NodeRef caseFileRef = (NodeRef) nodeService.getProperty(docRef, DocumentCommonModel.Props.CASE);
            		if (volRef != null) {
            			volumeCaseFileRefs.add(volRef);
            		}
            		if (caseFileRef != null) {
            			volumeCaseFileRefs.add(caseFileRef);
            		}
            	}
            	if (!volumeCaseFileRefs.isEmpty()) {
            		boolean needMoreSearch = true;
            		while (needMoreSearch) {
	            		if (foundVolumeOrCaseFiles.size() >= limit) {
	            			needMoreSearch = true;
	            		} else {
	            			needMoreSearch = false;
	            		}
	            		for (VolumeOrCaseFile volOrCasefile: foundVolumeOrCaseFiles) {
	            			NodeRef nodeRef = null;
	            			if (volOrCasefile instanceof CaseFile) {
	            				nodeRef = ((CaseFile)volOrCasefile).getNodeRef();
	            			} else if (volOrCasefile instanceof Volume) {
	            				nodeRef = ((Volume)volOrCasefile).getNodeRef();
	            			}
	            			if (nodeRef != null && volumeCaseFileRefs.contains(nodeRef)) {
	            				if (filteredVolumeOrCaseFiles.size() >= limit) {
	            					needMoreSearch = false;
	            					break;
	            				}
	            				filteredVolumeOrCaseFiles.add(volOrCasefile);
	            			}
	            		}
	            		if (needMoreSearch) {
	            			startFrom += limit;
	            			results = searchVolumesImpl(query, startFrom, limit, /* queryName */"volumesByFilter", storeRefs);
	            			foundVolumeOrCaseFiles = results.getFirst();
	            		}
            		}
            	}
            	results.setFirst(new ArrayList<>(filteredVolumeOrCaseFiles));
            }
            
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
    /*
    private boolean isValidVolumeCaseFile(VolumeOrCaseFile volOrCasefile, Node filter) {
    	long startTime = System.currentTimeMillis();
    	boolean valid = true;
        Map<QName, Serializable> props = RepoUtil.toQNameProperties(filter.getProperties(), true);

        @SuppressWarnings("unchecked")
        List<String> volTypes = (List<String>) props.get(VolumeSearchModel.Props.VOLUME_TYPE);
        if (!volTypes.isEmpty()) {
        	boolean found = false;
        	for (String type: volTypes) {
        		if (type.equals(volOrCasefile.getType())) {
        			found = true;
        		}
        	}
        	valid = found;
        }
        if (!valid) {
        	return false;
        }
        
        @SuppressWarnings("unchecked")
        List<String> caseFileTypes = (List<String>) props.get(VolumeSearchModel.Props.CASE_FILE_TYPE);
        if (!volTypes.isEmpty()) {
        	boolean found = false;
        	for (String type: caseFileTypes) {
        		if (type.equals((String)volOrCasefile.getProperties().get(DocumentAdminModel.Props.OBJECT_TYPE_ID))) {
        			found = true;
        		}
        	}
        	valid = found;
        }
        if (!valid) {
        	return false;
        }

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
    	return false;
    }
    */

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
            if (!value) {
                queryParts.add(joinQueryPartsOr(generatePropertyBooleanQuery(volumeProp, value), generatePropertyNullQuery(volumeProp)));
            } else {
                queryParts.add(generatePropertyBooleanQuery(volumeProp, value));
            }
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
    public List<NodeRef> searchDocumentsInOutbox() {
        String query = getDvkOutboxQuery();
        log.debug("searchDocumentsInOutbox with query '" + query + "'");
        return searchDocumentsBySendInfoImpl(query, -1, /* queryName */"documentsInOutbox");
    }

    @Override
    public int searchDocumentsInOutboxCount(int limit) {
        String query = getDvkOutboxQuery();
        log.debug("searchDocumentsInOutboxCount with query '" + query + "'");
        return searchDocumentsBySendInfoImpl(query, limit, /* queryName */"documentsInOutboxCount").size();
    }

    @Override
    public Map<NodeRef /* sendInfo */, Pair<String /* dvkId */, String /* recipientRegNr */>> searchOutboxDvkIds() {
        String query = getDvkOutboxQuery();
        log.debug("searchDocumentsInOutbox with query '" + query + "'");
        return searchDhlIdsBySendInfoImpl(query, -1, /* queryName */"outboxDvkIds");
    }

    @Override
    public Map<NodeRef, Pair<String, String>> searchForwardedDecDocumentsDvkIds(SendStatus status) {
        String query = joinQueryPartsAnd(Arrays.asList(generateParentPathQuery("/doccom:forwardedDecDocuments"), getDvkOutboxQuery(status)), false);
        log.debug("searchDocumentsInOutbox with query '" + query + "'");
        return searchDhlIdsBySendInfoImpl(query, -1, "forwardedDecDocumentsDvkIds", Collections.singleton(generalService.getStore()));
    }

    @Override
    public Map<NodeRef, Pair<String, String>> searchUnopenedAditDocs() {
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateTypeQuery(DocumentCommonModel.Types.SEND_INFO));
        queryParts.add(generateStringExactQuery(SendMode.STATE_PORTAL_EESTI_EE.getValueName(), DocumentCommonModel.Props.SEND_INFO_SEND_MODE));
        queryParts.add(SearchUtil.generatePropertyNotNullQuery(DocumentCommonModel.Props.SEND_INFO_RECEIVED_DATE_TIME));
        queryParts.add(SearchUtil.generatePropertyNullQuery(DocumentCommonModel.Props.SEND_INFO_OPENED_DATE_TIME));
        String query = joinQueryPartsAnd(queryParts, false);
        log.debug("queryUnopenedAditDocs with query '" + query + "'");
        return searchDhlIdsBySendInfoImpl(query, -1, "searchUnopenedAditDocs");
    }

    @Override
    public Pair<List<NodeRef>, Boolean> quickSearchDocuments(String searchValue, NodeRef containerNodeRef, int limit) {
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
            // in case of GUEST user take out open for all documents if guest user or group is not set
            if (resultNodeRefs != null && userService.isGuest()) {
            	resultNodeRefs = filterDocumentsForGuest(resultNodeRefs);
            }
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
            
            // in case of GUESTS users do extra filtering
            if (userService.isGuest() && resultNodeRefs != null && !resultNodeRefs.isEmpty()) {
            	String docQuery = joinQueryPartsAnd(Arrays.asList(
                        generateTypeQuery(DocumentCommonModel.Types.DOCUMENT),
                        generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE),
                        documentModelQuickSearchQuery,
                        generateDatePropertyRangeQuery(docCreatedStart, docCreatedEnd, ContentModel.PROP_CREATED),
                        generateDatePropertyRangeQuery(docRegisteredStart, docRegisteredEnd, DocumentCommonModel.Props.REG_DATE_TIME),
                        generateMultiStringExactQuery(documentTypes, DocumentAdminModel.Props.OBJECT_TYPE_ID),
                        functionQuery, seriesQuery, volumeQuery));
            	List<NodeRef> docNodeRefs = searchDocumentsImpl(docQuery, -1, "documentsByFilter", storeRefs).getFirst();
            	List<NodeRef> filteredCaseFiles = new ArrayList<>();
            	Set<NodeRef> caseFileRefs = new HashSet<>();
            	for (NodeRef docRef: docNodeRefs) {
            		NodeRef caseFileRef = (NodeRef) nodeService.getProperty(docRef, DocumentCommonModel.Props.CASE);
            		if (caseFileRef != null) {
            			caseFileRefs.add(caseFileRef);
            		}
            	}
            	if (!caseFileRefs.isEmpty()) {
            		for (NodeRef caseRef: resultNodeRefs) {
            			if (caseFileRefs.contains(caseRef)) {
            				filteredCaseFiles.add(caseRef);
            			}
            		}
            	}
            	resultNodeRefs = filteredCaseFiles;
            }
            
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
            Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
            for (NodeRef nodeRef : resultNodeRefs) {
                results.add(new AssocBlockObject(BeanHelper.getVolumeService().getVolumeByNodeRef(nodeRef, propertyTypes)));
            }
            query = joinQueryPartsAnd(Arrays.asList(
                    generateTypeQuery(CaseFileModel.Types.CASE_FILE),
                    generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE),
                    documentModelQuickSearchQuery,
                    generateStringWordsWildcardQuery(objectTitle, DocumentDynamicModel.Props.TITLE),
                    functionQuery, seriesQuery, volumeQuery));
            resultNodeRefs = searchAssociatedObjects(query, storeRefs);
            
            // in case of GUESTS users do extra filtering
            if (userService.isGuest() && resultNodeRefs != null && !resultNodeRefs.isEmpty()) {
            	String docQuery = joinQueryPartsAnd(Arrays.asList(
                        generateTypeQuery(DocumentCommonModel.Types.DOCUMENT),
                        generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE),
                        documentModelQuickSearchQuery,
                        generateDatePropertyRangeQuery(docCreatedStart, docCreatedEnd, ContentModel.PROP_CREATED),
                        generateDatePropertyRangeQuery(docRegisteredStart, docRegisteredEnd, DocumentCommonModel.Props.REG_DATE_TIME),
                        generateMultiStringExactQuery(documentTypes, DocumentAdminModel.Props.OBJECT_TYPE_ID),
                        functionQuery, seriesQuery, volumeQuery));
            	List<NodeRef> docNodeRefs = searchDocumentsImpl(docQuery, -1, "documentsByFilter", storeRefs).getFirst();
            	List<NodeRef> filteredVolumes = new ArrayList<>();
            	Set<NodeRef> volumeRefs = new HashSet<>();
            	for (NodeRef docRef: docNodeRefs) {
            		NodeRef volRef = (NodeRef) nodeService.getProperty(docRef, DocumentCommonModel.Props.VOLUME);
            		if (volRef != null) {
            			volumeRefs.add(volRef);
            		}
            	}
            	if (!volumeRefs.isEmpty()) {
            		for (NodeRef volRef: resultNodeRefs) {
            			if (volRef != null && volumeRefs.contains(volRef)) {
            				filteredVolumes.add(volRef);
            			}
            		}
            	}
            	resultNodeRefs = filteredVolumes;
            }
            
            for (NodeRef nodeRef : resultNodeRefs) {
                results.add(new AssocBlockObject(BeanHelper.getVolumeService().getVolumeByNodeRef(nodeRef, propertyTypes)));
            }
        }

        return results;
    }

    private List<NodeRef> searchAssociatedObjects(String query, Set<StoreRef> storeRefs) {
        int limit = parametersService.getLongParameter(Parameters.MAX_SEARCH_RESULT_ROWS).intValue();
        return searchNodes(query, limit, "searchAssocObjects", storeRefs).getFirst();
    }

    private Pair<List<NodeRef>, Boolean> searchDocumentsAndOrCases(String query, String searchString, boolean includeCaseTitles, int limit) {
        long startTime = System.currentTimeMillis();
        try {
            final Pair<List<NodeRef>, Boolean> results;
            if (includeCaseTitles) {
                final String caseByTitleQuery = getCaseByTitleQuery(searchString);
                query = joinQueryPartsOr(Arrays.asList(query, caseByTitleQuery));
                results = searchDocumentsImpl(query, limit, /* queryName */"documentsQuickAndCaseTitles");
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
        Pair<List<String>, List<Object>> taskQueryAndArgs = getTaskQuery(ownerId, Status.NEW, isPreviousOwnerId);
        String query = generateTaskSearchQuery(taskQueryAndArgs.getFirst());
        List<NodeRef> results = BeanHelper.getWorkflowDbService().searchTaskNodeRefs(query, taskQueryAndArgs.getSecond(), -1).getFirst();
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
    public List<NodeRef> searchSimilarInvoiceDocuments(String regNumber, String invoiceNumber, Date invoiceDate) {
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
        List<NodeRef> result = searchDocumentsImpl(query, -1, /* queryName */"similarInvoiceDocuments").getFirst();
        if (log.isDebugEnabled()) {
            log.debug("Similar invoice documents search total time " + (System.currentTimeMillis() - startTime) + " ms, query: " + query);
        }
        return result;
    }

    @Override
    public List<Document> searchInvoiceBaseDocuments(String contractNumber, String sellerPartyName) {
        throw new RuntimeException("E-invoice functionality is not supported!");
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

    private Pair<List<String>, List<Object>> getTaskQuery(String ownerId, Status status, boolean isPreviousOwnerId, QName... taskType) {
        return getTaskQuery(ownerId, status, isPreviousOwnerId, new Pair<List<String>, List<Object>>(new ArrayList<String>(), new ArrayList<>()), taskType);
    }

    private Pair<List<String>, List<Object>> getTaskQuery(String ownerId, Status status, boolean isPreviousOwnerId, Pair<List<String>, List<Object>> queryPartsAndArgs,
            QName... taskType) {
        List<String> queryParts = queryPartsAndArgs.getFirst();
        List<Object> arguments = queryPartsAndArgs.getSecond();
        QName ownerField = (isPreviousOwnerId) ? WorkflowCommonModel.Props.PREVIOUS_OWNER_ID : WorkflowCommonModel.Props.OWNER_ID;
        if (taskType != null) {
            addTaskTypeFieldExactQueryPartsAndArguments(queryParts, arguments, taskType);
        } else {
            addTaskTypeFieldExactQueryPartsAndArguments(queryParts, arguments);
        }
        addTaskStringExactPartsAndArgs(queryParts, arguments, status.getName(), WorkflowCommonModel.Props.STATUS);
        addTaskStringExactPartsAndArgs(queryParts, arguments, ownerId, ownerField);
        return queryPartsAndArgs;
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
        return getDvkOutboxQuery(SendStatus.SENT);
    }

    private String getDvkOutboxQuery(SendStatus status) {
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateTypeQuery(DocumentCommonModel.Types.SEND_INFO));
        queryParts.add(SearchUtil.generatePropertyExactQuery(DocumentCommonModel.Props.SEND_INFO_SEND_MODE,
                Arrays.asList(SendMode.STATE_PORTAL_EESTI_EE.getValueName(), SendMode.DVK.getValueName())));
        queryParts.add(generateStringExactQuery(status.toString(), DocumentCommonModel.Props.SEND_INFO_SEND_STATUS));
        return joinQueryPartsAnd(queryParts, true);
    }

    private Map<NodeRef /* sendInfo */, Pair<String /* dvkId */, String /* recipientRegNr */>> searchDhlIdsBySendInfoImpl(String query, int limit, String queryName) {
        return searchDhlIdsBySendInfoImpl(query, limit, queryName, null);
    }

    private Map<NodeRef, Pair<String, String>> searchDhlIdsBySendInfoImpl(String query, int limit, String queryName, Set<StoreRef> stores) {
        final HashMap<NodeRef, Pair<String, String>> refsAndDvkIds = new HashMap<NodeRef, Pair<String, String>>();
        if (stores == null) {
            stores = getAllStoresWithArchivalStoreVOs();
        }
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
        }, stores);
        return refsAndDvkIds;
    }

    private List<NodeRef> searchDocumentsBySendInfoImpl(String query, int limit, String queryName) {
        List<StoreRef> stores = getAllStores();
        final Set<NodeRef> documentRefs = new HashSet<>();
        searchGeneralImplWithoutSort(query, -1, queryName, new SearchCallback<NodeRef>() {
            @Override
            public NodeRef addResult(ResultSetRow row) {
                documentRefs.add(row.getChildAssocRef().getParentRef());
                return null;
            }
        }, stores, false).getFirst();

        if (documentRefs.isEmpty()) {
            return new ArrayList<NodeRef>();
        }
        Map<NodeRef, Node> allDocuments = bulkLoadNodeService.loadNodes(new ArrayList<NodeRef>(documentRefs), DOCUMENT_SERIES_PROP_QNAME_SET);
        List<NodeRef> seriesRefs = new ArrayList<NodeRef>();
        for (Node document : allDocuments.values()) {
            NodeRef seriesRef = (NodeRef) document.getProperties().get(DocumentCommonModel.Props.SERIES);
            if (seriesRef != null && !seriesRefs.contains(seriesRef)) {
                seriesRefs.add(seriesRef);
            }
        }
        List<NodeRef> documentsForPermissionCheck = new ArrayList<NodeRef>();
        if (!seriesRefs.isEmpty()) {
            Map<NodeRef, Node> allSeries = bulkLoadNodeService.loadNodes(new ArrayList<NodeRef>(seriesRefs), SERIES_DOCUMENTS_VISIBLE_FOR_USERS_WITHOUT_ACCESS_QNAME_SET);
            for (Node document : allDocuments.values()) {
                NodeRef seriesRef = (NodeRef) document.getProperties().get(DocumentCommonModel.Props.SERIES);
                Node series = allSeries.get(seriesRef);
                if (series != null && Boolean.FALSE.equals(series.getProperties().get(SeriesModel.Props.DOCUMENTS_VISIBLE_FOR_USERS_WITHOUT_ACCESS))) {
                    documentsForPermissionCheck.add(document.getNodeRef());
                }
            }

        }

        if (limit > 0) {
            if ((allDocuments.size() - documentsForPermissionCheck.size()) > limit) {
                List<NodeRef> docs = new ArrayList<>(allDocuments.keySet());
                docs.removeAll(documentsForPermissionCheck);
                return docs;
            }
        }

        if (!documentsForPermissionCheck.isEmpty()) {
            String sqlQuery = SearchUtil.generateSearchableDocListAccess(documentsForPermissionCheck, AuthenticationUtil.getRunAsUser());
            if (StringUtils.isNotBlank(sqlQuery)) {
                List<NodeRef> filteredDocuments = searchNodes(sqlQuery, -1, "searchDocumentsBySendInfoImpl:checkDocPermissions", stores, false).getFirst();
                for (NodeRef documentToCheck : documentsForPermissionCheck) {
                    if (!filteredDocuments.contains(documentToCheck)) {
                        allDocuments.remove(documentToCheck);
                    }
                }
            }
        }

        return new ArrayList<NodeRef>(allDocuments.keySet());
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
        queryParts.add(generateMultiStringExactQuery(sendMode, false, DocumentCommonModel.Props.SEARCHABLE_SEND_MODE, DocumentSpecificModel.Props.TRANSMITTAL_MODE));
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

    private void generateHierarchicalKeywordQuery(List<String> queryParts, Map<QName, Serializable> props, List<UnmodifiableFieldDefinition> list) {
        List<QName> l1Fields = new ArrayList<QName>();
        List<QName> l2Fields = new ArrayList<QName>();
        for (UnmodifiableFieldDefinition fieldDefinition : list) {
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
                UnmodifiableFieldDefinition def = ser.getFieldDefinition(propQName.getLocalName());
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) value;
                if (StringUtils.isNotBlank(def.getClassificator())) {
                    queryParts.add(generateMultiStringExactQuery(list, propQName));
                } else if (!list.isEmpty() && def != null && FieldType.STRUCT_UNIT == def.getFieldTypeEnum()) {
                    queryParts.add(generateMultiStringExactQuery(list, false, propQName));
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

        List<NodeRef> seriesRefs = searchRestrictedSeries(storeRefs);

        queryParts.add(SearchUtil.generateDocAccess(seriesRefs, userId));
        return generateDocumentSearchQueryWithoutRestriction(queryParts);
    }

    @Override
    public List<NodeRef> searchRestrictedSeries(Collection<StoreRef> storeRefs) {
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
        return seriesRefs;
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

        // TODO: verify that it is okay NOT to use left wildcard any more.
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
        if (types != null) {
        	addTaskTypeFieldExactQueryPartsAndArguments(queryParts, arguments, types.toArray(new QName[types.size()]));
        }
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

    private Pair<List<NodeRef>, Boolean> searchDocumentsImpl(String query, int limit, String queryName) {
        return searchDocumentsImpl(query, limit, queryName, null);
    }

    private Pair<List<NodeRef>, Boolean> searchDocumentsImpl(String query, int limit, String queryName, Collection<StoreRef> storeRefs) {
    	Pair<List<NodeRef>, Boolean> results = searchNodes(query, limit, queryName, storeRefs, false);
    	
    	// in case of GUEST user take out open for all documents if guest user or group is not set
        if (results != null && userService.isGuest()) {
        	results.setFirst(filterDocumentsForGuest(results.getFirst()));
        }
        return results;
    }
    
    /**
     * Check all found documents for  Privilege.VIEW_DOCUMENT_META_DATA for current user
     * @param foundResults
     */
    private List<NodeRef> filterDocumentsForGuest(List<NodeRef> foundResults) {
    	if (foundResults != null && !foundResults.isEmpty()) {
    		String currentUserName = AuthenticationUtil.getRunAsUser();
	        Set<String> currentUserGroups = userService.getUsersGroups(currentUserName);
	        if (currentUserGroups == null) {
	        	currentUserGroups = new HashSet<>();
	        }
	        Set<String> groupsToRemove = new HashSet<>();
	        for (String userGroup: currentUserGroups) {
		        if (userGroup.startsWith(PermissionService.ROLE_PREFIX) || PermissionService.ALL_AUTHORITIES.equals(userGroup)) {
		        	groupsToRemove.add(userGroup);
		        }
	        }
	        currentUserGroups.removeAll(groupsToRemove);
	        currentUserGroups.add(currentUserName);
    		List<NodeRef> filteredResults = new ArrayList<>();
    		for (NodeRef docNode: foundResults) {
    			/*
    			NodeRef seriesRef = (NodeRef) nodeService.getProperty(docNode, DocumentCommonModel.Props.SERIES);
                if (seriesRef != null && !nodeService.exists(seriesRef)) {
                    log.warn("Document " + docNode + " references nonexistent series " + seriesRef);
                    seriesRef = null;
                }
                if (seriesRef != null && Boolean.TRUE.equals(nodeService.getProperty(seriesRef, SeriesModel.Props.DOCUMENTS_VISIBLE_FOR_USERS_WITHOUT_ACCESS))) {
                */
    				if (nodeService.exists(docNode)) {
	                	List<String> authorities = BeanHelper.getPrivilegeService().getAuthoritiesWithPrivilege(docNode, Privilege.VIEW_DOCUMENT_META_DATA);
	                	String ownerId = (String) nodeService.getProperty(docNode, DocumentCommonModel.Props.OWNER_ID);
	                	if (StringUtils.isNotBlank(ownerId)) {
	                		authorities.add(ownerId);
	                	}
	                    for (String authority : authorities) {
	                    	if (currentUserGroups.contains(authority)) {
	                    		filteredResults.add(docNode);
	                    		break;
	                    	}
	                    }
    				}
                /*    
                } else {
                	filteredResults.add(docNode);
                }
                */
    		}
    		return filteredResults;
    		
    	}
    	return foundResults;
    }

    private Pair<List<VolumeOrCaseFile>, Boolean> searchVolumesImpl(String query, int startFrom, int limit, String queryName, Collection<StoreRef> storeRefs) {
        final Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
        return searchGeneralImpl(query, startFrom, limit, queryName, new SearchCallback<VolumeOrCaseFile>() {

            @Override
            public VolumeOrCaseFile addResult(ResultSetRow row) {
                NodeRef ref = row.getNodeRef();
                QName type = nodeService.getType(ref);
                if (type.equals(VolumeModel.Types.VOLUME)) {
                    return volumeService.getVolumeByNodeRef(row.getNodeRef(), propertyTypes);
                }
                return BeanHelper.getCaseFileService().getCaseFile(ref); // BeanCreationException if service is injected
            }
        }, storeRefs);
    }

    private Pair<List<Volume>, Boolean> searchVolumesAndCaseFilesImpl(String query, int limit, String queryName, Collection<StoreRef> storeRefs) {
        final Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
        return searchGeneralImpl(query, limit, queryName, new SearchCallback<Volume>() {

            @Override
            public Volume addResult(ResultSetRow row) {
                return volumeService.getVolumeByNodeRef(row.getNodeRef(), propertyTypes);
            }
        }, storeRefs);
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
        final Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
        return searchGeneralImpl(query, limit, queryName, new SearchCallback<Volume>() {
            @Override
            public Volume addResult(ResultSetRow row) {
                return volumeService.getVolumeByNodeRef(row.getNodeRef(), propertyTypes);
            }
        }, getAllStoresWithArchivalStoreVOs()).getFirst();
    }

    private List<UnmodifiableSeries> searchSeriesImpl(String query, int limit, String queryName) {
        final Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
        return searchGeneralImpl(query, limit, queryName, new SearchCallback<UnmodifiableSeries>() {
            @Override
            public UnmodifiableSeries addResult(ResultSetRow row) {
                return seriesService.getUnmodifiableSeries(row.getNodeRef(), propertyTypes);
            }
        }).getFirst();
    }

    private List<String> searchAdrDocumentTypesImpl(String query, int limit, String queryName) {
        final Map<QName, Pair<Long, QName>> propertyTypes = new HashMap<QName, Pair<Long, QName>>();
        return searchGeneralImpl(query, limit, queryName, new SearchCallback<String>() {
            @Override
            public String addResult(ResultSetRow row) {
                return ((QName) nodeService.getProperty(row.getNodeRef(), AdrModel.Props.DOCUMENT_TYPE, propertyTypes)).getLocalName();
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
    public List<String> searchUserNamesByTypeAndProps(String input, QName type, Set<QName> props, int limit, String queryAndAddition) {
        limit = limit < 0 ? 100 : limit;
        String query = joinQueryPartsAnd(
                type != null ? generateTypeQuery(type) : "",
                generateStringWordsWildcardQuery(input, props.toArray(new QName[props.size()])),
                queryAndAddition
                );
        return searchProperty(query, limit, "searchUserNamesByTypeAndProps", ContentModel.PROP_USERNAME, String.class).getFirst();
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

    private WorkflowService getWorkflowService() {
        if (_workflowService == null) {
            _workflowService = BeanHelper.getWorkflowService();
        }
        return _workflowService;
    }

    public void setApplicationConstantsBean(ApplicationConstantsBean applicationConstantsBean) {
        this.applicationConstantsBean = applicationConstantsBean;
    }

    // END: getters / setters

}

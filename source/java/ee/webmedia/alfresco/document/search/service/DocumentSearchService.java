package ee.webmedia.alfresco.document.search.service;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.springframework.jdbc.core.RowMapper;

import ee.webmedia.alfresco.archivals.web.ArchivalActivity;
import ee.webmedia.alfresco.casefile.service.CaseFile;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.series.model.UnmodifiableSeries;
import ee.webmedia.alfresco.user.model.Authority;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeOrCaseFile;
import ee.webmedia.alfresco.workflow.service.Task;
import com.nortal.jroad.client.dhl.DhlXTeeService.SendStatus;

/**
 * Note: Use method names that start with "query" for methods that require a read-write transaction.
 * Methods names that start with "search" are by default made in read-only transaction unless specifically declared otherwise.
 */
public interface DocumentSearchService {

    String BEAN_NAME = "DocumentSearchService";

    /**
     * Escape symbols and use only 10 first unique words which contain at least 3 characters.
     */
    List<String> parseQuickSearchWords(String searchString);

    /**
     * Searches for documents where:
     * + search string matches against any Document property value (supported types: text, int, long, float, double, date, datetime)
     * + or file name
     * + or file content
     * It returns maximum of 100 entries. It is possible that the method returns less than 100 Documents even when there
     * are more than 100 matches in the repository because we search for 200 matches and then filter out duplicate documents
     * where multiple files under the same document matched the search criteria.
     *
     * @param searchString
     * @param containerNodeRef if not null, only documents with given parent container nodeRef are returned
     * @param limit
     * @return list of matching documents (max 100 entries)
     */
    Pair<List<NodeRef>, Boolean> quickSearchDocuments(String searchString, NodeRef containerNodeRef, int limit);

    public List<AssocBlockObject> searchAssocObjects(Node objectFilter);

    /**
     * Searches for documents using a search filter.
     * It returns maximum of 100 entries. It is possible that the method returns less than 100 Documents even when there
     * are more than 100 matches in the repository because we search for 200 matches and then filter out duplicate documents
     * where multiple files under the same document matched the search criteria.
     *
     * @param filter
     * @param limit
     * @param sortBy
     * @param ascending
     * @return list of matching documents (max 100 entries)
     */
    Pair<List<NodeRef>, Boolean> queryDocuments(Node filter, int limit, QName sortBy, boolean ascending);

    Pair<List<VolumeOrCaseFile>, Boolean> queryVolumes(Node filter, int limit);

    /**
     * Searches for documents using a search filter.
     * Query must be exactly the same as in searchDocuments,
     * but returns all documents (no limit for returned result rows)
     * and for performance reasons only nodeRefs are returned.
     * Ignore filter storeRefs and use parameter storeRef
     * as we want to add checkpoints between queries to different stores
     * outside this service.
     */
    List<NodeRef> searchDocumentsForReport(Node filter, StoreRef storeRef, String userId);

    /**
     * @return documents being sent but not delivered to ALL recipients
     */
    List<NodeRef> searchDocumentsInOutbox();

    int searchDocumentsInOutboxCount(int limit);

    /**
     * @return dvkId's by sendInfos(aka dhl_id's - assigned to documents by DVK when sent to DVK, to be able to ask sending statuses)
     */
    Map<NodeRef, Pair<String, String>> searchOutboxDvkIds();

    /** @return {@code Map<sendInfo, Pair<dvkId, recipientRegNr>>} */
    public Map<NodeRef, Pair<String, String>> searchForwardedDecDocumentsDvkIds(SendStatus status);

    /**
     * @return {@code Map<sendInfoNodeRef, Pair<dvkId, recipientRegNr>> }
     */
    Map<NodeRef, Pair<String, String>> searchUnopenedAditDocs();

    /**
     * Fetches list of documents where ownerId = logged in userId and (
     * (docType = incomingLetter* && dokumendi regNumber = null)
     * OR
     * (docType != incomingLetter* && !hasCompoundWorkflows(document))
     * )
     *
     * @return list of Document objects
     */
    List<NodeRef> searchInProcessUserDocuments();

    /**
     * @return count of {@link #searchInProcessUserDocuments()} without fetching documents
     */
    int searchInProcessUserDocumentsCount(int limit);

    /**
     * Fetches list of documents where date in regDateTime property is current date
     *
     * @param limit
     * @return list of Document objects
     */
    Pair<List<NodeRef>, Boolean> searchTodayRegisteredDocuments(String searchString, int limit);

    /**
     * Fetches a list of documents where recipient or additional recipient is present and docStatus is finished.
     * The documents are filtered out if they have sendInfo child associations.
     *
     * @return list of Document objects
     */
    List<NodeRef> searchRecipientFinishedDocuments();

    int searchRecipientFinishedDocumentsCount(int limit);

    /**
     * Fetches a list of Series where series' structUnit is unit
     *
     * @param unit
     * @return list of Series objects
     */
    List<UnmodifiableSeries> searchSeriesUnit(String unit);

    /**
     * Searches documents are available for registering.
     *
     * @return list of documents
     */
    List<NodeRef> searchDocumentsForRegistering();

    /**
     * Gets the count of documents available for registering.
     *
     * @return count
     */
    int getCountOfDocumentsForRegistering(int limit);

    /**
     * Returns all tasks that are in progress for currently logged in user
     *
     * @param taskType
     */
    List<Pair<NodeRef, QName>> searchCurrentUsersInProgressTaskRefs(boolean onlyOverdueOrToday, QName... taskType);

    <T extends Object> List<T> searchCurrentUsersTasksInProgress(RowMapper<T> rowMapper, QName... taskType);

    List<Task> searchCurrentUsersTaskInProgressWithoutParents(QName taskType, boolean allStoresSearch);

    /**
     * Returns number of tasks of specified type that are assigned to currently logged in user
     *
     * @param taskType
     * @return
     */
    int getCurrentUsersTaskCount(QName taskType);

    /**
     * Returns number of unseen tasks assigned to currently logged in user
     *
     * @param taskTypes task types to count
     * @return count
     */
    int getCurrentUsersUnseenTasksCount(QName[] taskTypes);

    /**
     * Counts number of in progress tasks for current user by task type.
     *
     * @param taskType task types that should be counted
     * @return A map where task type is key and number of tasks is value.
     */
    Map<QName, Integer> getCurrentUserTaskCountByType(QName... taskType);

    /**
     * Searches for compoundWorkflows using a search filter.
     *
     * @param filter
     * @return list of matching compound workflows
     */
    Pair<List<NodeRef>, Boolean> queryCompoundWorkflows(Node filter, int limit);

    /**
     * Searches for tasks using a search filter.
     * Query must be exactly the same as in searchTasks,
     * but returns all tasks (no limit for returned result rows)
     * and for performance reasons only nodeRefs are returned.
     */
    List<NodeRef> searchTasksForReport(Node filter, String userName);

    /**
     * If due date is null, then list with due tasks is returned (dueDate < sysDate)
     *
     * @param dueDate
     * @return
     */
    List<Task> searchTasksDueAfterDate(Date dueDate);

    List<Volume> searchVolumesDispositionedAfterDate(Date dispositionDate);

    /**
     * Search for documents of type INCOMING_LETTER or INCOMING_LETTER_MV, where register data and number is not empty
     * and sender's reg numbers are same.
     *
     * @param senderRegNumber
     * @return list of found documents
     */
    List<NodeRef> searchIncomingLetterRegisteredDocuments(String senderRegNumber);

    List<NodeRef> searchAccessRestictionEndsAfterDate(Date restrictionEndDate);

    List<NodeRef> searchWorkingDocumentsByOwnerId(String ownerId, boolean isPreviousOwnerId);

    List<NodeRef> searchNewTasksByOwnerId(String ownerId, boolean isPreviousOwnerId);

    Set<NodeRef> searchAdrDocuments(Date modifiedDateBegin, Date modifiedDateEnd, Set<String> documentTypeIds);

    List<NodeRef> searchAdrDeletedDocuments(Date deletedDateBegin, Date deletedDateEnd);

    List<String> searchAdrDeletedDocumentTypes(Date deletedDateBegin, Date deletedDateEnd);

    List<String> searchAdrAddedDocumentTypes(Date addedDateBegin, Date addedDateEnd);

    Map<NodeRef, Pair<String, String>> searchTaskBySendStatusQuery(QName taskType);

    List<Task> searchTasksByOriginalDvkIdsQuery(Iterable<String> originalDvkIds);

    Task searchTaskByOriginalDvkIdQuery(String originalDvkId);

    NodeRef searchSeriesByIdentifier(String identifier);

    /**
     * Searches for groups by name. If {@code input} is empty, all groups are returned if {@code returnAllGroups} is {@code true}, otherwise an empty list is
     * returned.
     *
     * @param withAdminsAndDocManagers - should administrators and document managers groups be included or filtered out
     * @param limit
     */
    List<Authority> searchAuthorityGroups(String groupName, boolean returnAllGroups, boolean withAdminsAndDocManagers, int limit);

    List<NodeRef> searchSimilarInvoiceDocuments(String regNumber, String invoiceNumber, Date invoiceDate);

    /**
     * @param firstName - in case firstName is null or empty, search users only by lastName
     * @param lastName
     * @return
     */
    List<NodeRef> searchUsersByFirstNameLastName(String firstName, String lastName);

    List<Document> searchInvoiceBaseDocuments(String contractNumber, String sellerPartyName);

    List<NodeRef> searchUsersByRelatedFundsCenter(String relatedFundsCenter);

    // TODO not document specific
    List<NodeRef> simpleSearch(String searchInputString, NodeRef parentRef, QName type, QName... props);

    /**
     * @param query - lucene query
     * @param limited - should results be limited to DocumentSearchServiceImpl.RESULTS_LIMIT results?
     * @param queryName - arbitary name used in logging statements
     * @return
     */
    // TODO not document specific
    List<NodeRef> searchNodes(String query, int limit, String queryName);

    List<NodeRef> filterUsersInUserGroup(String structUnit, Set<String> children);

    /**
     * @param query
     * @return true if at least one result could be found based on query (from default store)
     */
    // TODO not document specific
    boolean isMatch(String query);

    boolean isMatchAllStoresWithTrashcan(String query);

    boolean isMatch(String query, boolean allStores, String queryName);

    /**
     * Searches for working documents that have a discussion that involves current user
     *
     * @return
     */
    List<NodeRef> searchDiscussionDocuments();

    int getDiscussionDocumentsCount(int limit);

    NodeRef searchOrganizationNodeRef(String orgEmail, String orgName);

    List<NodeRef> searchDueContracts();

    List<StoreRef> getStoresFromDocumentReportFilter(Map<String, Object> properties);

    LinkedHashSet<StoreRef> getAllStoresWithArchivalStoreVOs();

    int getCurrentUserCompoundWorkflowsCount(int limit);

    String generateDeletedSearchQuery(String searchValue, NodeRef containerNodeRef);

    NodeRef getIndependentCompoundWorkflowByProcedureId(String procedureId);

    public List<CaseFile> searchCurrentUserCaseFiles();

    int getCurrentUserCaseFilesCount(int limit);

    List<NodeRef> searchVolumesForReport(Node filter);

    List<NodeRef> searchCompoundWorkflowsOwnerId(String ownerId, boolean isPreviousOwnerId);

    List<NodeRef> searchOpenCaseFilesOwnerId(String ownerId, boolean isPreviousOwnerId);

    List<NodeRef> searchAdrDeletedDocument(NodeRef originalDocumentRef);

    String generateAdrDocumentSearchQuery(List<String> queryParts, Set<String> documentTypeIds);

    List<NodeRef> searchAllDocumentRefsByParentRef(NodeRef parentRef);

    Pair<List<NodeRef>, Boolean> searchAllDocumentsByParentRef(NodeRef parentRef, int limit);

    NodeRef searchLinkedReviewTaskByOriginalNoderefId(String noderefId);

    List<Task> searchReviewTaskToResendQuery();

    List<NodeRef> searchActiveLocks();

    List<String> searchAuthorityGroupsByExactName(String groupName);

    List<Volume> searchVolumesForArchiveList(Node filter, List<NodeRef> defaultStores);

    List<Volume> searchVolumesForArchiveList(Node filter, boolean hasArchivalValueOrRetainPermanent, boolean isWaitingForDestructionQuery, List<NodeRef> defaultStores);

    List<Pair<NodeRef, String>> getAllVolumeSearchStores();

    List<NodeRef> searchSeriesByEventPlan(NodeRef eventPlanRef);

    List<NodeRef> searchVolumesByEventPlan(NodeRef eventPlanRef, String inputTitle, List<String> inputStatus, List<NodeRef> location);

    List<ArchivalActivity> searchArchivalActivities(Node filter);

    List<NodeRef> searchNodesByTypeAndProps(String input, QName type, Set<QName> props, int limit);

    List<NodeRef> searchNodesByTypeAndProps(String input, QName type, Set<QName> props, int limit, String queryAndAddition);

    List<String> searchUserNamesByTypeAndProps(String input, QName type, Set<QName> props, int limit, String queryAndAddition);

    boolean isFieldByOriginalIdExists(String fieldId);

    Pair<List<NodeRef>, Boolean> searchAllDocumentRefsByParentRefCheckExists(NodeRef parentRef, int limit);

    Pair<List<NodeRef>, Boolean> searchTaskRefs(Node filter, String username, int limit);

    List<NodeRef> searchRestrictedSeries(Collection<StoreRef> storeRefs);
    
    List<NodeRef> searchByQuery(Collection<StoreRef> storeRefs, String query, String queryName);

    List<NodeRef> searchCurrentUserCompoundWorkflowRefs();

}

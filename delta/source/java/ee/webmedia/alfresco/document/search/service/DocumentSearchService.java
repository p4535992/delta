package ee.webmedia.alfresco.document.search.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.user.model.Authority;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.workflow.search.model.TaskInfo;
import ee.webmedia.alfresco.workflow.service.Task;

/**
 * @author Alar Kvell
 * @author Erko Hansar
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
     * @return list of matching documents (max 100 entries)
     */
    List<Document> searchDocumentsQuick(String searchString);

    List<Document> searchDocumentsAndOrCases(String searchValue, Date regDateTimeBegin, Date regDateTimeEnd, List<QName> documentTypes, boolean includeCaseTitles);

    /**
     * Searches for documents using a search filter.
     * It returns maximum of 100 entries. It is possible that the method returns less than 100 Documents even when there
     * are more than 100 matches in the repository because we search for 200 matches and then filter out duplicate documents
     * where multiple files under the same document matched the search criteria.
     * 
     * @param filter
     * @return list of matching documents (max 100 entries)
     */
    List<Document> searchDocuments(Node filter);

    /**
     * @return documents being sent but not delivered to ALL recipients
     */
    List<Document> searchDocumentsInOutbox();

    int searchDocumentsInOutboxCount();

    /**
     * @return dvkId's by sendInfos(aka dhl_id's - assigned to documents by DVK when sent to DVK, to be able to ask sending statuses)
     */
    Map<NodeRef, Pair<String, String>> searchOutboxDvkIds();

    /**
     * Fetches list of documents where ownerId = logged in userId and (
     * (docType = incomingLetter* && dokumendi regNumber = null)
     * OR
     * (docType != incomingLetter* && !hasCompoundWorkflows(document))
     * )
     * 
     * @return list of Document objects
     */
    // CL_TASK 152338 new version of searchUserWorkingDocuments()
    // List<Document> searchInProcessUserDocuments();

    /**
     * @see #searchInProcessUserDocuments()
     */
    // CL_TASK 152338 new version of searchUserWorkingDocumentsCount()
    // int searchInProcessUserDocumentsCount();

    /**
     * Fetches list of documents where ownerId = logged in userId and docStatus is working
     * 
     * @return list of Document objects
     */
    // CL_TASK 152338 old version of searchInProcessUserDocuments()
    List<Document> searchUserWorkingDocuments();

    // CL_TASK 152338 old version of searchInProcessUserDocumentsCount()
    int searchUserWorkingDocumentsCount();

    /**
     * Fetches list of documents where date in regDateTime property is current date
     * 
     * @return list of Document objects
     */
    List<Document> searchTodayRegisteredDocuments();

    /**
     * Fetches a list of documents where recipient or additional recipient is present and docStatus is finished.
     * The documents are filtered out if they have sendInfo child associations.
     * 
     * @return list of Document objects
     */
    List<Document> searchRecipientFinishedDocuments();

    int searchRecipientFinishedDocumentsCount();

    /**
     * Fetches a list of Series where series' structUnit is unit
     * 
     * @param unit
     * @return list of Series objects
     */
    List<Series> searchSeriesUnit(String unit);

    /**
     * Searches documents are available for registering.
     * 
     * @return list of documents
     */
    List<Document> searchDocumentsForRegistering();

    /**
     * Gets the count of documents available for registering.
     * 
     * @return count
     */
    int getCountOfDocumentsForRegistering();

    /**
     * Returns all tasks that are in progress for currently logged in user
     * 
     * @param taskType
     */
    List<Task> searchCurrentUsersTasksInProgress(QName taskType);

    /**
     * Returns number of tasks of specified type that are assigned to currently logged in user
     * 
     * @param taskType
     * @return
     */
    int getCurrentUsersTaskCount(QName taskType);

    /**
     * Searches for tasks using a search filter.
     * 
     * @param filter
     * @return list of matching tasks
     */
    List<TaskInfo> searchTasks(Node filter);

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
    List<Document> searchIncomingLetterRegisteredDocuments(String senderRegNumber, QName documentType);

    List<Document> searchAccessRestictionEndsAfterDate(Date restrictionEndDate);

    List<NodeRef> searchWorkingDocumentsByOwnerId(String ownerId);

    List<NodeRef> searchNewTasksByOwnerId(String ownerId);

    /**
     * Used by ADR web service to search documents.
     * 
     * @param regDateBegin
     * @param regDateEnd
     * @param docType
     * @param searchString
     * @return
     */
    List<Document> searchAdrDocuments(Date regDateBegin, Date regDateEnd, QName docType, String searchString, Set<QName> documentTypes);

    /**
     * Used by ADR web service to search document details.
     * 
     * @param regNumber
     * @param regDate
     * @return
     */
    List<Document> searchAdrDocuments(String regNumber, Date regDate, Set<QName> documentTypes);

    List<Document> searchAdrDocuments(Date modifiedDateBegin, Date modifiedDateEnd, Set<QName> documentTypes);

    List<NodeRef> searchAdrDeletedDocuments(Date deletedDateBegin, Date deletedDateEnd);

    List<QName> searchAdrDeletedDocumentTypes(Date deletedDateBegin, Date deletedDateEnd);

    List<QName> searchAdrAddedDocumentTypes(Date addedDateBegin, Date addedDateEnd);

    Map<NodeRef, Pair<String, String>> searchTaskBySendStatusQuery(QName taskType);

    List<Task> searchTasksByOriginalDvkIdsQuery(Iterable<String> originalDvkIds);

    Task searchTaskByOriginalDvkIdQuery(String originalDvkId);

    Series searchSeriesByIdentifier(String identifier);

    /**
     * Searches for groups by name. If {@code input} is empty, all groups are returned if {@code returnAllGroups} is {@code true}, otherwise an empty list is
     * returned.
     */    
    List<Authority> searchAuthorityGroups(String groupName, boolean returnAllGroups);

}

package ee.webmedia.alfresco.document.search.service;

import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.series.model.Series;

/**
 * @author Alar Kvell
 * @author Erko Hansar
 */
public interface DocumentSearchService {

    String BEAN_NAME = "DocumentSearchService";

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

    /**
     * @return dvkId's by sendInfos(aka dhl_id's - assigned to documents by DVK when sent to DVK, to be able to ask sending statuses)
     */
    Map<NodeRef /*sendInfo*/, String /*dvkId*/> searchOutboxDvkIds();

    /**
     * Fetches list of documents where ownerId = logged in userId and docStatus is working
     * 
     * @return list of Document objects
     */
    List<Document> searchUserWorkingDocuments();

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

    /**
     * Fetches a list of Series where series' structUnit is unit
     * 
     * @param unit
     * @return list of Series objects
     */
    List<Series> searchSeriesUnit(String unit);

}

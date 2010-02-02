package ee.webmedia.alfresco.dvk.service;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.dvk.model.DvkSendDocuments;
import ee.webmedia.xtee.client.service.DhlXTeeService.ContentToSend;

/**
 * @author Ats Uiboupin
 */
public interface DvkService {
    String BEAN_NAME = "DvkService";

    /**
     * @return a map with regNum's and orgName's of organizations that are capable of receiving documents using DVK
     */
    Map<String /* regNum */, String /* orgName */> getSendingOptions();

    void updateOrganizationList();

    int updateDocSendStatuses();

    Set<String> sendDocuments(NodeRef document, Collection<ContentToSend> contentsToSend, DvkSendDocuments sendDocument);

    /**
     * Receive all documents from DVK server(using multiple service calls, if server has more documents than can be fetched at a time)
     */
    Collection<String> receiveDocuments();

}

package ee.webmedia.alfresco.dvk.service;

import java.util.Collection;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.dvk.model.DvkSendDocuments;
import ee.webmedia.xtee.client.service.DhlXTeeService.ContentToSend;

/**
 * @author Ats Uiboupin
 *
 */
public interface DvkService {
    String BEAN_NAME = "DvkService";

    void updateOrganizationList();

    List<String> sendDocuments(NodeRef document, Collection<ContentToSend> contentsToSend, DvkSendDocuments sendDocument);

    /**
     * Receive all documents from DVK server(using multiple service calls, if server has more documents than can be fetched at a time)
     */
    Collection<String> receiveDocuments();

}

package ee.webmedia.alfresco.dvk.service;

import java.util.Collection;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.dvk.model.DvkSendDocuments;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.ContentToSend;

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

    String sendDocuments(NodeRef document, Collection<ContentToSend> contentsToSend, DvkSendDocuments sendDocument);

    /**
     * Receive all documents from DVK server(using multiple service calls, if server has more documents than can be fetched at a time)
     */
    Collection<String> receiveDocuments();

    /**
     * Set ab:organization property dvkCapable=true if organization is capable to receive documents using DVK("DokumendiVahetusKeskus")
     * @return number of organizations in the addressbook that are capable to receive documents using DVK
     */
    int updateOrganizationsDvkCapability();

    String getCorruptDvkDocumentsPath();

}

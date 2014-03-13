package ee.webmedia.alfresco.dvk.service;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.einvoice.model.Transaction;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.dvk.model.DvkSendLetterDocuments;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.ContentToSend;
import ee.webmedia.xtee.client.dhl.types.ee.sk.digiDoc.v13.DataFileType;

public interface DvkService {
    String BEAN_NAME = "DvkService";

    /**
     * @return a map with regNum's and orgName's of organizations that are capable of receiving documents using DVK
     */
    Map<String /* regNum */, String /* orgName */> getSendingOptions();

    void updateOrganizationList();

    int updateDocAndTaskSendStatuses();

    String sendLetterDocuments(NodeRef document, Collection<ContentToSend> contentsToSend, DvkSendLetterDocuments sendDocument);

    /**
     * Receive all documents from DVK server(using multiple service calls, if server has more documents than can be fetched at a time)
     */
    Collection<String> receiveDocuments();

    /**
     * Set ab:organization property dvkCapable=true if organization is capable to receive documents using DVK("DokumendiVahetusKeskus")
     * 
     * @return number of organizations in the addressbook that are capable to receive documents using DVK
     */
    int updateOrganizationsDvkCapability();

    String getCorruptDvkDocumentsPath();

    /**
     * @param docNodeRef document to send
     * @param compoundWorkflowRef if not null, only this compund workflow recipients are sent updates,
     *            otherwise all document's compound workflows recipients are sent updates
     */
    void sendDvkTasksWithDocument(NodeRef docNodeRef, NodeRef compoundWorkflowRef, Map<NodeRef, List<String>> additionalRecipients);

    void sendDvkTask(Task task);

    String getInstitutionCode();

    boolean isXmlMimetype(DataFileType dataFile);

    String sendInvoiceFileToSap(Node document, File file);

    String generateAndSendInvoiceFileToSap(Node node, List<Transaction> transactions) throws IOException;

}

package ee.webmedia.alfresco.document.sendout.service;

import java.io.Serializable;
<<<<<<< HEAD
import java.util.ArrayList;
=======
>>>>>>> develop-5.1
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.sendout.model.SendInfo;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.ContentToSend;

/**
 * Provides service methods for sending out documents and managing the sendInfo blocks.
<<<<<<< HEAD
 * 
 * @author Erko Hansar
=======
>>>>>>> develop-5.1
 */
public interface SendOutService {

    String BEAN_NAME = "SendOutService";

    /**
     * Returns all the sendInfo nodes associated with given document.
     * 
     * @param document document NodeRef
     * @return list of sendInfo nodes associated with given document
     */
    List<SendInfo> getDocumentSendInfos(NodeRef document);

    /**
<<<<<<< HEAD
     * Update searchableSendMode property according to document's sendInfo.sendMode values
     * 
     * @param document document NodeRef
     */
    void updateSearchableSendMode(NodeRef document);

    /**
     * Build searchableSendMode list from document's sendInfo.sendMode values
     * 
     * @param document document NodeRef
     * @return List of document's sendInfo.sendMode values
     */
    ArrayList<String> buildSearchableSendMode(NodeRef document);
=======
     * Update searchable send info properties according to document's sendInfo child nodes
     * 
     * @param document document NodeRef
     */
    void updateSearchableSendInfo(NodeRef document);

    /**
     * Build searchable send info data from document's sendInfo child nodes
     * 
     * @param document document NodeRef
     * @return Map with documents properties populated with document's sendInfo values
     */
    Map<QName, Serializable> buildSearchableSendInfo(NodeRef document);
>>>>>>> develop-5.1

    /**
     * Sends out document.
     * Inspects all the given recipients and based on send mode sends out the document through DVK to those who support it (based on addressbook) and through email to others.
     * Registers sendInfo child entries under document and checks if given document is a reply outgoing letter and updates originating document info if needed.
     * 
     * @param document subject document for sending out
     * @param names list of recipient names
     * @param emails list of recipient email addresses
     * @param modes list of recipient send modes
<<<<<<< HEAD
     * @param fromEmail from email address
     * @param subject mail subject
     * @param content mail content text
     * @param fileNodeRefs list of file node refs as strings to match those files which should be sent out as attachments from given document
     * @param zipIt if attachments should be zipped into single file, or sent as separate files
     * @return true
     */
    boolean sendOut(NodeRef document, List<String> names, List<String> emails, List<String> modes, List<String> encryptionIdCodes, String fromEmail, String subject,
            String content, List<NodeRef> fileRefs, boolean zipIt);

    NodeRef addSendinfo(NodeRef document, Map<QName, Serializable> props);

=======
     * @param idCodes TODO
     * @param fromEmail from email address
     * @param subject mail subject
     * @param content mail content text
     * @param zipIt if attachments should be zipped into single file, or sent as separate files
     * @param fileNodeRefs list of file node refs as strings to match those files which should be sent out as attachments from given document
     * @return true
     */
    boolean sendOut(NodeRef document, List<String> names, List<String> emails, List<String> modes, List<String> idCodes, List<String> encryptionIdCodes, String fromEmail,
            String subject, String content, List<NodeRef> fileRefs, boolean zipIt);

    NodeRef addSendinfo(NodeRef document, Map<QName, Serializable> props);

    /**
     * If updateSearchableSendInfo is false then updateSearchableSendInfo() must manually be called later
     */
    NodeRef addSendinfo(NodeRef document, Map<QName, Serializable> props, boolean updateSearchableSendInfo);

>>>>>>> develop-5.1
    List<ContentToSend> prepareContents(NodeRef document, List<NodeRef> fileRefs, boolean zipIt);

    List<SendInfo> getDocumentAndTaskSendInfos(NodeRef document, List<CompoundWorkflow> compoundWorkflows);

    void addSapSendInfo(Node document, String dvkId);

    boolean hasDocumentSendInfos(NodeRef document);

<<<<<<< HEAD
    void sendDocumentForInformation(List<String> authorityIds, Node docNode, String emailTemplate);
=======
    void sendForInformation(List<String> authorityIds, Node docNode, String emailTemplate, String subject, String content);
>>>>>>> develop-5.1

}

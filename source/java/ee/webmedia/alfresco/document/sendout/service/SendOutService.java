package ee.webmedia.alfresco.document.sendout.service;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.sendout.model.SendInfo;
import ee.webmedia.alfresco.email.model.EmailAttachment;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.ContentToSend;

/**
 * Provides service methods for sending out documents and managing the sendInfo blocks.
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

    /**
     * Sends out document.
     * Inspects all the given recipients and based on send mode sends out the document through DVK to those who support it (based on addressbook) and through email to others.
     * Registers sendInfo child entries under document and checks if given document is a reply outgoing letter and updates originating document info if needed.
     *
     * @param document subject document for sending out
     * @param names list of recipient names
     * @param emails list of recipient email addresses
     * @param modes list of recipient send modes
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

    /** @return {@code List<Pair<recipientName, recipientRegistrationNr>> } */
    List<Pair<String, String>> forward(NodeRef document, List<String> names, List<String> emails, List<String> modes, String fromEmail, String content, List<NodeRef> fileRefs);

    NodeRef addSendinfo(NodeRef document, Map<QName, Serializable> props);

    /**
     * If updateSearchableSendInfo is false then updateSearchableSendInfo() must manually be called later
     */
    NodeRef addSendinfo(NodeRef document, Map<QName, Serializable> props, boolean updateSearchableSendInfo);

    List<ContentToSend> prepareContents(NodeRef document, List<NodeRef> fileRefs, boolean zipIt);
    
    List<ContentToSend> prepareContents(List<EmailAttachment> attachments);

    void addSapSendInfo(Node document, String dvkId);

    boolean hasDocumentSendInfos(NodeRef document);

    Long sendForInformation(List<String> authorityIds, Node docNode, String emailTemplate, String subject, String content);

    Date getEarliestSendInfoDate(NodeRef docRef);

}

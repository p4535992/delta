package ee.webmedia.alfresco.document.sendout.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.sendout.model.SendInfo;

/**
 * Provides service methods for sending out documents and managing the sendInfo blocks.
 * 
 * @author Erko Hansar
 */
public interface SendOutService {
    
    String BEAN_NAME = "SendOutService";
    
    String SEND_MODE_DVK = "DVK";

    /**
     * Returns all the sendInfo nodes associated with given document. 
     * 
     * @param document document NodeRef
     * @return list of sendInfo nodes associated with given document
     */
    List<SendInfo> getSendInfos(NodeRef document);

    /**
     * Sends out document.
     * 
     * Inspects all the given recipients and based on send mode sends out the document through DVK to those who support it (based on addressbook) and through email to others.
     * Registers sendInfo child entries under document and checks if given document is a reply outgoing letter and updates originating document info if needed. 
     * 
     * @param document subject document for sending out
     * @param names list of recipient names
     * @param emails list of recipient email addresses
     * @param modes list of recipient send modes
     * @param fromEmail from email address
     * @param subject mail subject
     * @param content mail content text
     * @param fileNodeRefs list of file node refs as strings to match those files which should be sent out as attachments from given document
     * @param zipIt if attachments should be zipped into single file, or sent as separate files
     * @return true
     */
    boolean sendOut(NodeRef document, List<String> names, List<String> emails, List<String> modes, String fromEmail, String subject, String content, List<String> fileNodeRefs, boolean zipIt);

    NodeRef addSendinfo(NodeRef document, Map<QName, Serializable> props);
}

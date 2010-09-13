package ee.webmedia.alfresco.imap.model;

import org.alfresco.service.namespace.QName;

/**
 * @author Romet Aidla
 */
public interface ImapModel {
    String URI = "http://alfresco.webmedia.ee/model/imap/1.0";
    String IMAP_EXT_MODEL_PREFIX = "imap-ext";

    public interface Repo {
        String IMAP_PARENT = "/";
        String IMAP_ROOT = "imap-root";
        String IMAP_SPACE = IMAP_PARENT + IMAP_EXT_MODEL_PREFIX + ":" + IMAP_ROOT;
        String INCOMING_SPACE = IMAP_SPACE + "/" + IMAP_EXT_MODEL_PREFIX + ":" + "incoming";
        String ATTACHMENT_SPACE =  IMAP_SPACE + "/" + IMAP_EXT_MODEL_PREFIX + ":" + "attachments";
        String SENT_SPACE = IMAP_SPACE + "/" + IMAP_EXT_MODEL_PREFIX + ":" + "sent";
    }

    interface Types {
        QName IMAP_FOLDER = QName.createQName(URI, "imapFolder");
        QName ATTACHMENTS = QName.createQName(URI, "attachments");
    }

    interface Aspects {
        QName BEHAVIOURS = QName.createQName(URI, "behaviours");
    }

    interface Properties {
        QName APPEND_BEHAVIOUR = QName.createQName(URI, "appendBehaviour");
    }
}

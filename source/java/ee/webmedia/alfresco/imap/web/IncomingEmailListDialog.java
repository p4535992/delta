<<<<<<< HEAD
package ee.webmedia.alfresco.imap.web;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.imap.model.ImapModel;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Riina Tens
 */
public class IncomingEmailListDialog extends AbstractEmailListDialog {

    private static final long serialVersionUID = 1L;

    @Override
    protected NodeRef getMainFolderRef() {
        return BeanHelper.getGeneralService().getNodeRef(ImapModel.Repo.INCOMING_SPACE);
    }

    @Override
    public String getEmailDateTimeColumnMsg() {
        return MessageUtil.getMessage("document_emailDateTime_received");
    }

}
=======
package ee.webmedia.alfresco.imap.web;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.imap.model.ImapModel;

public class IncomingEmailListDialog extends AbstractEmailListDialog {

    private static final long serialVersionUID = 1L;

    @Override
    protected NodeRef getMainFolderRef() {
        return BeanHelper.getGeneralService().getNodeRef(ImapModel.Repo.INCOMING_SPACE);
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

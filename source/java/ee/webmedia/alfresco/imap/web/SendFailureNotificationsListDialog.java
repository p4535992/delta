<<<<<<< HEAD
package ee.webmedia.alfresco.imap.web;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.imap.model.ImapModel;

public class SendFailureNotificationsListDialog extends AttachmentListDialog {

    private static final long serialVersionUID = 1L;

    @Override
    protected NodeRef getMainFolderRef() {
        return BeanHelper.getGeneralService().getNodeRef(ImapModel.Repo.SEND_FAILURE_NOTICE_SPACE);
    }

}
=======
package ee.webmedia.alfresco.imap.web;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.imap.model.ImapModel;

public class SendFailureNotificationsListDialog extends AttachmentListDialog {

    private static final long serialVersionUID = 1L;

    @Override
    protected NodeRef getMainFolderRef() {
        return BeanHelper.getGeneralService().getNodeRef(ImapModel.Repo.SEND_FAILURE_NOTICE_SPACE);
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

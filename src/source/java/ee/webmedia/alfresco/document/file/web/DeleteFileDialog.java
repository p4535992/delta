package ee.webmedia.alfresco.document.file.web;

import javax.faces.context.FacesContext;

import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.content.DeleteContentDialog;

/**
 * @author Dmitri Melnikov
 */
public class DeleteFileDialog extends DeleteContentDialog {

    private static final long serialVersionUID = 1L;

    @Override
    protected String doPostCommitProcessing(FacesContext context, String outcome) {
        this.browseBean.setDocument(null);
        return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
    }
}

package ee.webmedia.alfresco.document.web;

import java.util.Collections;
<<<<<<< HEAD
import java.util.List;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import java.util.Map;

import javax.faces.event.ActionEvent;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;

<<<<<<< HEAD
import ee.webmedia.alfresco.casefile.service.CaseFile;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowWithObject;

/**
 * @author Alar Kvell
 */
=======
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public class FavoritesDocumentListDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 1L;
    private NodeRef containerNodeRef;
    private String dirName;
<<<<<<< HEAD
    private List<CompoundWorkflowWithObject> workflows;
    private List<CaseFile> caseFiles;
    private boolean hadSetup = false;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
<<<<<<< HEAD
        if (!hadSetup) {
            containerNodeRef = null;
            dirName = null;
        }
        hadSetup = false;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        restored();
    }

    @Override
    public void restored() {
<<<<<<< HEAD
        if (containerNodeRef != null && !BeanHelper.getNodeService().exists(containerNodeRef)) {
            // May occur when moved from favorites view to document view, deleted document
            // and this was the only document in the favorites folder, so folder was also deleted.
            // In that case restore main favorites view
            containerNodeRef = null;
            dirName = null;
        }
        documents = BeanHelper.getDocumentFavoritesService().getDocumentFavorites(containerNodeRef);
        workflows = BeanHelper.getCompoundWorkflowFavoritesService().getCompoundWorkflowFavorites(containerNodeRef);
        caseFiles = BeanHelper.getCaseFileFavoritesService().getCaseFileFavorites(containerNodeRef);
        Collections.sort(documents);
    }

    @Override
    public String cancel() {
        return super.cancel();
=======
        documents = getDocumentService().getFavorites(containerNodeRef);
        Collections.sort(documents);
        containerNodeRef = null;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    public void setup(ActionEvent event) {
        containerNodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        String param = ActionUtil.getParam(event, "dirName");
        dirName = param;
<<<<<<< HEAD
        hadSetup = true;
    }

    @Override
    public String getContainerTitle() {
        String documentsString = MessageUtil.getMessage("document_myFavorites");
        if (StringUtils.isBlank(dirName)) {
            return documentsString;
        }
        String title = dirName;
        return documentsString + " - " + title;
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage("documents");
=======
    }

    @Override
    public String getListTitle() {
        if (StringUtils.isBlank(dirName)) {
            return MessageUtil.getMessage("document_my_favorites");
        }
        String title = dirName;
        dirName = null;
        return title;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    }

    public NodeRef getUserNodeRef() {
        return BeanHelper.getUserService().getUser(AuthenticationUtil.getRunAsUser()).getNodeRef();
    }

<<<<<<< HEAD
    public List<CompoundWorkflowWithObject> getWorkflows() {
        return workflows;
    }

    public List<CaseFile> getCaseFiles() {
        return caseFiles;
    }

    public boolean getHasWorkflows() {
        return !workflows.isEmpty();
    }

    public boolean getHasCaseFiles() {
        return !caseFiles.isEmpty();
    }

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}

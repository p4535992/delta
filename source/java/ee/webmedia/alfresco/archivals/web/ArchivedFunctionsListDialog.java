package ee.webmedia.alfresco.archivals.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getFunctionsService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getMenuBean;

import java.util.Collections;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.archivals.model.ArchivalsStoreVO;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.web.FunctionsListDialog;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.WebUtil;

/**
 * Dialog bean for archived functions
<<<<<<< HEAD
 * 
 * @author Romet Aidla
=======
>>>>>>> develop-5.1
 */
public class ArchivedFunctionsListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ArchivedFunctionsListDialog.class);

    protected NodeRef nodeRef;
    protected String title;
    protected List<Function> functions;

    public void setup(ActionEvent event) {
        nodeRef = ActionUtil.getParam(event, "nodeRef", NodeRef.class);
        loadFunctions();
        title = "";
        for (ArchivalsStoreVO archivalsStoreVO : getGeneralService().getArchivalsStoreVOs()) {
            if (nodeRef.equals(archivalsStoreVO.getNodeRef())) {
                title = archivalsStoreVO.getTitle();
                break;
            }
        }
        getMenuBean().updateTree(event);
        WebUtil.navigateTo(AlfrescoNavigationHandler.DIALOG_PREFIX + "archivedFunctionsListDialog");
    }

    public void exportArchivalsConsolidatedList(@SuppressWarnings("unused") ActionEvent event) {
        FunctionsListDialog.exportConsolidatedList(nodeRef);
    }

    @Override
    public void restored() {
        loadFunctions();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // save button not used or shown
        return null;
    }

    @Override
    public String cancel() {
        functions = null;
        return super.cancel();
    }

    @Override
    public Object getActionsContext() {
        return null;
    }

    public List<Function> getFunctions() {
        return functions;
    }

    protected void loadFunctions() {
        functions = getFunctionsService().getFunctions(nodeRef);
        Collections.sort(functions);
    }

    @Override
    public String getContainerTitle() {
        return title;
    }

}

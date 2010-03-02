package ee.webmedia.alfresco.archivals.web;

import ee.webmedia.alfresco.archivals.service.ArchivalsService;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.functions.web.FunctionsListDialog;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import javax.faces.context.FacesContext;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Dialog bean for archived functions
 *
 * @author Romet Aidla
 */
public class ArchivedFunctionsListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    private transient ArchivalsService archivalsService;
    protected List<Function> functions;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        loadFunctions();
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
        functions = getArchivalsService().getArchivedFunctions();
        Collections.sort(functions);
    }

    private ArchivalsService getArchivalsService() {
        if (archivalsService == null) {
            archivalsService = (ArchivalsService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(ArchivalsService.BEAN_NAME);
        }
        return archivalsService;
    }
}

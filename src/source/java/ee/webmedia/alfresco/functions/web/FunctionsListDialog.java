package ee.webmedia.alfresco.functions.web;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.service.FunctionsService;

public class FunctionsListDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    private transient FunctionsService functionsService;
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
        return new Node(getFunctionsService().getFunctionsRoot());
    }

    // START: private methods
    protected void loadFunctions() {
        functions = getFunctionsService().getAllFunctions();
        Collections.sort(functions);
    }
    // END: private methods

    // START: getters / setters
    /**
     * Used in JSP pages.
     */
    public List<Function> getFunctions() {
        return functions;
    }

    protected FunctionsService getFunctionsService() {
        if (functionsService == null) {
            functionsService = (FunctionsService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(FunctionsService.BEAN_NAME);
        }
        return functionsService;
    }

    public void setFunctionsService(FunctionsService functionsService) {
        this.functionsService = functionsService;
    }
    // END: getters / setters
}

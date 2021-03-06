package ee.webmedia.alfresco.series.web.mydocuments;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.functions.model.UnmodifiableFunction;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.series.model.UnmodifiableSeries;
import ee.webmedia.alfresco.user.service.UserService;

public class MySeriesListDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    private transient UserService userService;
    private transient FunctionsService functionsService;
    private transient DocumentSearchService documentSearchService;

    private List<SeriesFunction> seriesFunction;

    @Override
    public void init(Map<String, String> parameters) {
        super.init(parameters);
        seriesFunction = new ArrayList<SeriesFunction>();
        String userName = AuthenticationUtil.getRunAsUser();
        String orgstructId = (String) getUserService().getUserProperties(userName).get(ContentModel.PROP_ORGID);

        if (StringUtils.isBlank(orgstructId)) {
            return;
        }

        List<UnmodifiableSeries> series = getDocumentSearchService().searchSeriesUnit(orgstructId);
        Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
        for (UnmodifiableSeries sr : series) {
            UnmodifiableFunction fn = getFunctionsService().getUnmodifiableFunction(sr.getFunctionRef(), propertyTypes);
            seriesFunction.add(new SeriesFunction(sr, fn));
        }
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // finish button not used
        return null;
    }

    // START: getters / setters
    public List<SeriesFunction> getSeriesFunction() {
        return seriesFunction;
    }

    protected UserService getUserService() {
        if (userService == null) {
            userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext(//
                    FacesContext.getCurrentInstance()).getBean(UserService.BEAN_NAME);
        }
        return userService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    protected FunctionsService getFunctionsService() {
        if (functionsService == null) {
            functionsService = (FunctionsService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(FunctionsService.BEAN_NAME);
        }
        return functionsService;
    }

    public void setFunctionsService(FunctionsService functionsService) {
        this.functionsService = functionsService;
    }

    protected DocumentSearchService getDocumentSearchService() {
        if (documentSearchService == null) {
            documentSearchService = (DocumentSearchService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(DocumentSearchService.BEAN_NAME);
        }
        return documentSearchService;
    }

    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    // END: getters / setters

    public static class SeriesFunction implements Serializable {
        private static final long serialVersionUID = 1L;
        private final UnmodifiableSeries series;
        private final String functionTitle;

        public SeriesFunction(UnmodifiableSeries series, UnmodifiableFunction function) {
            functionTitle = function.getFunctionLabel();
            this.series = series;
        }

        public UnmodifiableSeries getSeries() {
            return series;
        }

        public String getFunctionTitle() {
            return functionTitle;
        }
    }

}

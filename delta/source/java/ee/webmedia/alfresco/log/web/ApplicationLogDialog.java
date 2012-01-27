package ee.webmedia.alfresco.log.web;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.filter.web.AbstractSearchFilterBlockBean;
import ee.webmedia.alfresco.log.model.LogFilter;
import ee.webmedia.alfresco.log.model.LogSearchModel;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.log.service.LogSetup;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.utils.WebUtil;

/**
 * Dialog for editing logging settings and for performing log entries search.
 * 
 * @author Martti Tamm
 */
public class ApplicationLogDialog extends AbstractSearchFilterBlockBean<LogService> {

    private static final long serialVersionUID = 1L;

    private transient LogService logService;

    private LogSetup logSetup;

    public LogService getLogService() {
        if (logService == null) {
            logService = (LogService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(LogService.BEAN_NAME);
        }
        return logService;
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        logSetup = getLogService().getCurrentLogSetup();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
    }

    public void processCreatorSearchResults(String username) {
        Map<QName, Serializable> personProps = getUserService().getUserProperties(username);
        filter.getProperties().put(LogSearchModel.Props.CREATOR_NAME.toString(), UserUtil.getPersonFullName1(personProps));
    }

    public void save(@SuppressWarnings("unused") ActionEvent event) {
        getLogService().saveLogSetup(logSetup);

        String msg = Application.getMessage(FacesContext.getCurrentInstance(), "applog_setup_saved");
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(msg));
    }

    public void search(@SuppressWarnings("unused") ActionEvent event) {
        WebUtil.navigateTo("dialog:applicationLogList");
        BeanHelper.getAppLogListDialog().search(getLogService().getLogEntries(getLogFilter()));
    }

    @Override
    protected LogService getFilterService() {
        return getLogService();
    }

    public LogSetup getLogSetup() {
        return logSetup;
    }

    @Override
    protected Node getNewFilter() {
        return new TransientNode(LogSearchModel.Types.LOG_FILTER, null, null);
    }

    private LogFilter getLogFilter() {
        Map<String, Object> props = ((TransientNode) getFilter()).getProperties();
        LogFilter result = new LogFilter();
        result.setLogEntryId((String) props.get(LogSearchModel.Props.ENTRY_ID));
        result.setDateCreatedStart((Date) props.get(LogSearchModel.Props.DATE_CREATED_START));
        result.setDateCreatedEnd((Date) props.get(LogSearchModel.Props.DATE_CREATED_END));
        result.setCreatorName((String) props.get(LogSearchModel.Props.CREATOR_NAME));
        result.setComputerId((String) props.get(LogSearchModel.Props.COMPUTER_ID));
        result.setDescription((String) props.get(LogSearchModel.Props.DESCRIPTION));
        result.setObjectName((String) props.get(LogSearchModel.Props.OBJECT_NAME));
        result.setObjectId((String) props.get(LogSearchModel.Props.OBJECT_ID));
        return result;
    }

    @Override
    protected String getFilterModifyDeniedMessageKey() {
        return null;
    }

    @Override
    protected String getBlankFilterNameMessageKey() {
        return null;
    }

    @Override
    protected String getFilterDeleteDeniedMessageKey() {
        return null;
    }

    @Override
    protected String getNewFilterSelectItemMessageKey() {
        return null;
    }
}

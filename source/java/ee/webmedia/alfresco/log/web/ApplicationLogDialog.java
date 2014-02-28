package ee.webmedia.alfresco.log.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getAppLogListDialog;
import static ee.webmedia.alfresco.common.web.BeanHelper.getLogService;
import static ee.webmedia.alfresco.utils.MessageUtil.addInfoMessage;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;

import ee.webmedia.alfresco.filter.web.AbstractSearchFilterBlockBean;
import ee.webmedia.alfresco.log.model.LogFilter;
import ee.webmedia.alfresco.log.model.LogSearchModel;
import ee.webmedia.alfresco.log.model.LogSetup;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.utils.WebUtil;

/**
 * Dialog for editing logging settings and for performing log entries search.
 * 
 * @author Martti Tamm
 */
public class ApplicationLogDialog extends AbstractSearchFilterBlockBean<LogService> {

    private static final long serialVersionUID = 1L;
    private static final String SERIES_PARAM = "seriesRef";
    private static final String NODEREF_PARAM = "nodeRef";

    private LogSetup logSetup;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        logSetup = getLogService().getCurrentLogSetup();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return null;
    }

    public void processCreatorSearchResults(String username) {
        Map<QName, Serializable> personProps = getUserService().getUserProperties(username);
        filter.getProperties().put(LogSearchModel.Props.CREATOR_NAME.toString(), UserUtil.getPersonFullName1(personProps));
    }

    public void save(@SuppressWarnings("unused") ActionEvent event) {
        getLogService().saveLogSetup(logSetup);
        addInfoMessage("applog_setup_saved");
    }

    public void search(@SuppressWarnings("unused") ActionEvent event) {
        getAppLogListDialog().search(getLogService().getLogEntries(getLogFilter()));
        WebUtil.navigateTo("dialog:applicationLogListDialog");
    }

    public void searchNodeRefEntries(ActionEvent event) {
        LogFilter filter = new LogFilter();
        filter.setObjectId(Collections.singletonList(ActionUtil.getParam(event, NODEREF_PARAM)));
        getAppLogListDialog().search(getLogService().getLogEntries(filter));
    }

    public void searchSeriesEntries(ActionEvent event) {
        LogFilter filter = new LogFilter();
        filter.setObjectId(Collections.singletonList(ActionUtil.getParam(event, SERIES_PARAM)));
        getAppLogListDialog().search(getLogService().getLogEntries(filter));
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
        return new TransientNode(getFilterType(), null, null);
    }

    @Override
    public QName getFilterType() {
        return LogSearchModel.Types.LOG_FILTER;
    }

    private LogFilter getLogFilter() {
        Map<String, Object> props = ((TransientNode) getFilter()).getProperties();
        LogFilter result = new LogFilter();
        result.setLogEntryId((String) props.get(LogSearchModel.Props.LOG_ENTRY_ID));
        result.setDateCreatedStart((Date) props.get(LogSearchModel.Props.DATE_CREATED_START));
        result.setDateCreatedEnd((Date) props.get(LogSearchModel.Props.DATE_CREATED_END));
        result.setCreatorName((String) props.get(LogSearchModel.Props.CREATOR_NAME));
        result.setComputerId((String) props.get(LogSearchModel.Props.COMPUTER_ID));
        result.setDescription((String) props.get(LogSearchModel.Props.DESCRIPTION));
        result.setObjectName((String) props.get(LogSearchModel.Props.OBJECT_NAME));
        result.setObjectId(Collections.singletonList((String) props.get(LogSearchModel.Props.OBJECT_ID)));
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

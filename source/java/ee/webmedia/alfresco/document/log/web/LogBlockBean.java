package ee.webmedia.alfresco.document.log.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getLogService;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicBlock;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogFilter;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.MessageUtil;

public class LogBlockBean implements DocumentDynamicBlock {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "LogBlockBean";

    private transient DocumentLogService documentLogService;
    private transient DictionaryService dictionaryService;

    private NodeRef parentRef;
    protected List<LogEntry> logs;

    protected QName parentNodeType;

    @Override
    public void resetOrInit(DialogDataProvider provider) {
        if (provider == null) {
            reset();
        } else {
            init(provider.getNode());
        }
    }

    public void init(Node node) {
        reset();
        parentRef = node.getNodeRef();
        parentNodeType = node.getType();
        restore();
    }

    @SuppressWarnings("unchecked")
    public void restore() {
        @SuppressWarnings("rawtypes")
        List tmpLog;
        if (SeriesModel.Types.SERIES.equals(parentNodeType)) {
            tmpLog = getLogService().getLogEntries(getSeriesLogFilter());
        } else if (getDictionaryService().isSubClass(parentNodeType, DocumentCommonModel.Types.DOCUMENT)) {
            tmpLog = getLogService().getLogEntries(getDocumentLogFilter());
        } else {
            throw new IllegalArgumentException("Unexpected type of parent node for loging block. type='" + parentNodeType + "'");
        }
        logs = tmpLog;
    }

    private LogFilter getDocumentLogFilter() {
        LogFilter logFilter = new LogFilter();
        Set<String> excludedDescriptions = new HashSet<String>(2);
        excludedDescriptions.add(MessageUtil.getMessage("document_log_status_opened_not_inEditMode"));
        excludedDescriptions.add(MessageUtil.getMessage("file_opened", "%"));
        logFilter.setExcludedDescriptions(excludedDescriptions);
        logFilter.setObjectId(Collections.singletonList(parentRef.toString()));
        logFilter.setExactObjectId(true);
        return logFilter;
    }

    private LogFilter getSeriesLogFilter() {
        LogFilter logFilter = new LogFilter();
        Set<String> excludedDescriptions = new HashSet<String>(1);
        excludedDescriptions.add(MessageUtil.getMessage("applog_space_open", "%", "%"));
        logFilter.setExcludedDescriptions(excludedDescriptions);
        logFilter.setObjectId(Collections.singletonList(parentRef.toString()));
        logFilter.setExactObjectId(true);
        return logFilter;
    }

    public void reset() {
        parentRef = null;
        logs = null;
        parentNodeType = null;
    }

    public NodeRef getParentRef() {
        return parentRef;
    }

    public boolean isRendered() {
        return logs != null && logs.size() > 0;
    }

    public String getListTitle() {
        return MessageUtil.getMessage("document_log_title");
    }

    public String getCreatedDateColumnTitle() {
        return MessageUtil.getMessage("document_log_date");
    }

    public String getEventColumnTitle() {
        return MessageUtil.getMessage("document_log_event");
    }

    public boolean isShowLogDetailsLink() {
        return BeanHelper.getUserService().isSupervisor();
    }

    // START: getters / setters

    public List<LogEntry> getLogs() {
        return logs;
    }

    public DocumentLogService getDocumentLogService() {
        if (documentLogService == null) {
            documentLogService = (DocumentLogService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(DocumentLogService.BEAN_NAME);
        }
        return documentLogService;
    }

    protected DictionaryService getDictionaryService() {
        if (dictionaryService == null) {
            dictionaryService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getDictionaryService();
        }
        return dictionaryService;
    }

    // END: getters / setters
}
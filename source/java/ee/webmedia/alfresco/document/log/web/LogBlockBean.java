package ee.webmedia.alfresco.document.log.web;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicBlock;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.log.model.LogFilter;
import ee.webmedia.alfresco.log.web.LogEntryDataProvider;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.MessageUtil;

public class LogBlockBean implements DocumentDynamicBlock {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "LogBlockBean";

    private transient DictionaryService dictionaryService;

    private NodeRef parentRef;
    protected LogEntryDataProvider logs;

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
        LogFilter filter;
        if (SeriesModel.Types.SERIES.equals(parentNodeType)) {
            filter = getSeriesLogFilter();
        } else if (getDictionaryService().isSubClass(parentNodeType, DocumentCommonModel.Types.DOCUMENT)) {
            filter = getDocumentLogFilter();
        } else {
            throw new IllegalArgumentException("Unexpected type of parent node for logging block. type='" + parentNodeType + "'");
        }
        logs = new LogEntryDataProvider(filter, false);
    }

    private LogFilter getDocumentLogFilter() {
        LogFilter logFilter = new LogFilter();
        Set<String> excludedDescriptions = new HashSet<>(2);
        excludedDescriptions.add(MessageUtil.getMessage("document_log_status_opened_not_inEditMode"));
        excludedDescriptions.add(MessageUtil.getMessage("file_opened", "%"));
        excludedDescriptions.add(MessageUtil.getMessage("applog_compoundWorkflow_view"));
        logFilter.setExcludedDescriptions(excludedDescriptions);
        // in 3.6 branch, compound workflow and task log is also added,
        // in 3.13.* branch this is not needed
        logFilter.setObjectId(Collections.singletonList(parentRef.toString()));
        logFilter.setExactObjectId(true);
        return logFilter;
    }

    private LogFilter getSeriesLogFilter() {
        LogFilter logFilter = new LogFilter();
        Set<String> excludedDescriptions = new HashSet<>(1);
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

    @Override
    public void clean() {
        reset();
    }

    public NodeRef getParentRef() {
        return parentRef;
    }

    public boolean isRendered() {
        return logs != null;
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

    public LogEntryDataProvider getLogs() {
        return logs;
    }

    protected DictionaryService getDictionaryService() {
        if (dictionaryService == null) {
            dictionaryService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getDictionaryService();
        }
        return dictionaryService;
    }

    // END: getters / setters
}

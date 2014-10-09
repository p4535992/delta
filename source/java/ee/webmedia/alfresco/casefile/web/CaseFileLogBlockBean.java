package ee.webmedia.alfresco.casefile.web;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ee.webmedia.alfresco.casefile.service.CaseFile;
import ee.webmedia.alfresco.document.log.web.LogBlockBean;
import ee.webmedia.alfresco.log.model.LogFilter;
import ee.webmedia.alfresco.log.web.LogEntryDataProvider;
import ee.webmedia.alfresco.utils.MessageUtil;

public class CaseFileLogBlockBean extends LogBlockBean {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "CaseFileLogBlockBean";

    private CaseFile caseFile;

    public void init(CaseFile caseFile) {
        if (caseFile != null) {
            this.caseFile = caseFile;
            init(caseFile.getNode());
        } else {
            reset();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void restore() {
        if (caseFile.isSaved()) {
            logs = new LogEntryDataProvider(getCaseFileLogFilter());
        } else {
            logs = new LogEntryDataProvider();
        }
    }

    @Override
    public void clean() {
        super.clean();
        caseFile = null;
    }

    private LogFilter getCaseFileLogFilter() {
        LogFilter logFilter = new LogFilter();
        Set<String> excludedDescriptions = new HashSet<String>(1);
        excludedDescriptions.add(MessageUtil.getMessage("applog_casefile_view"));
        logFilter.setExcludedDescriptions(excludedDescriptions);
        List<String> objectIds = new ArrayList<String>();
        objectIds.add(caseFile.getNodeRef().toString());
        logFilter.setObjectId(objectIds);
        logFilter.setExactObjectId(true);
        return logFilter;
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage("casefile_log_title");
    }
}

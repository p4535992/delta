package ee.webmedia.alfresco.casefile.web;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ee.webmedia.alfresco.casefile.service.CaseFile;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.log.web.LogBlockBean;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogFilter;
import ee.webmedia.alfresco.utils.MessageUtil;

<<<<<<< HEAD
/**
 * @author Priit Pikk
 */
=======
>>>>>>> develop-5.1

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
            logs = (List) BeanHelper.getLogService().getLogEntries(getCaseFileLogFilter());
        } else {
            logs = new ArrayList<LogEntry>();
        }
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

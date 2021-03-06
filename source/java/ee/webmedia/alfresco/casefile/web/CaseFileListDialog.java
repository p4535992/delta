package ee.webmedia.alfresco.casefile.web;

import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.casefile.service.CaseFile;
import ee.webmedia.alfresco.common.web.BeanHelper;

public class CaseFileListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "CaseFileListDialog";
    private List<CaseFile> caseFiles;

    @Override
    public void restored() {
        caseFiles = BeanHelper.getDocumentSearchService().searchCurrentUserCaseFiles();
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        restored();
    }

    @Override
    public String cancel() {
        clean();
        return super.cancel();
    }

    @Override
    public void clean() {
        caseFiles = null;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        throw new RuntimeException("OK button not supported here.");
    }

    /**
     * Getter for JSP.
     */
    public List<CaseFile> getCaseFiles() {
        return caseFiles;
    }
}

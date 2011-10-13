package ee.webmedia.alfresco.thesaurus.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;

import org.alfresco.util.Pair;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.FileUploadBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;

import ee.webmedia.alfresco.common.web.AbstractImportDialog;
import ee.webmedia.alfresco.thesaurus.model.HierarchicalKeyword;
import ee.webmedia.alfresco.thesaurus.model.Thesaurus;
import ee.webmedia.alfresco.thesaurus.service.ThesaurusService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;

/**
 * @author Kaarel Jõgeva
 */
public class ThesaurusImportDialog extends AbstractImportDialog {

    protected ThesaurusImportDialog() {
        super("xml", "thesaurus_import_wrong_extension");
    }

    private static final long serialVersionUID = 1L;
    private transient ThesaurusService thesaurusService;
    private List<Thesaurus> importedThesauri = new ArrayList<Thesaurus>();

    @Override
    public String getFileUploadSuccessMsg() {
        return null;
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return StringUtils.isBlank(getFileName());
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        boolean changed = getThesaurusService().importThesauri(importedThesauri);
        String message = changed ? "thesaurus_import_success" : "thesaurus_import_no_changes";
        MessageUtil.addInfoMessage(message);
        return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
    }

    public List<Pair<String, String>> getImportableThesauri() {
        FileUploadBean fileBean = getFileUploadBean();
        File upFile = fileBean.getFile();

        XStream xstream = new XStream();
        xstream.processAnnotations(Thesaurus.class);
        xstream.processAnnotations(HierarchicalKeyword.class);
        try {
            @SuppressWarnings("unchecked")
            List<Thesaurus> tmp = (List<Thesaurus>) xstream.fromXML(new FileInputStream(upFile));
            importedThesauri = tmp;
        } catch (StreamException e1) {
            // veateate näitamine ja sulgemine millegi pärast ei toimi
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "ccccc_error_wrongFileContent", getFileName());
            WebUtil.navigateTo(AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME, context);
            reset();
            return null;
        } catch (FileNotFoundException e1) {
            throw new RuntimeException("Failed to read thesauri from file '" + upFile.getName() + "'", e1);
        }

        // Generate overview of the changes
        ArrayList<Pair<String, String>> statuses = new ArrayList<Pair<String, String>>();
        final List<Thesaurus> repoThesauri = getThesaurusService().getThesauri(true);
        Thesaurus repoThesaurus;

        String statusBase = "thesaurus_import";
        for (Thesaurus thesaurus : importedThesauri) {
            String statusMsg = "";
            int indexOf = repoThesauri.indexOf(thesaurus);

            if (indexOf > -1) {
                repoThesaurus = repoThesauri.get(indexOf);
                // Check for added keywords ...
                if (CollectionUtils.subtract(repoThesaurus.getKeywords(), thesaurus.getKeywords()).size() > 0) {
                    statusMsg += "_keywords";
                }
                // ... and changed description
                if (!StringUtils.equals(repoThesaurus.getDescription(), thesaurus.getDescription())) {
                    statusMsg += "_description";
                }
            } else { // new thesaurus
                statusMsg += "_new";

            }

            if (statusMsg.length() > 0) {
                statuses.add(new Pair<String, String>(thesaurus.getName(), statusBase + statusMsg));
            }
        }

        return statuses;
    }

    // START: getters / setters

    public void setThesaurusService(ThesaurusService thesaurusService) {
        this.thesaurusService = thesaurusService;
    }

    protected ThesaurusService getThesaurusService() {
        if (thesaurusService == null) {
            thesaurusService = (ThesaurusService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(ThesaurusService.BEAN_NAME);
        }
        return thesaurusService;
    }
    // END: getters / setters

}

package ee.webmedia.alfresco.thesaurus.web;

import static ee.webmedia.alfresco.app.AppConstants.CHARSET;
import static ee.webmedia.alfresco.utils.ComponentUtil.addDefault;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.commons.io.IOUtils;
import org.apache.myfaces.application.jsp.JspStateManagerImpl;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.docadmin.web.FieldGroupDetailsDialog;
import ee.webmedia.alfresco.thesaurus.model.Thesaurus;
import ee.webmedia.alfresco.thesaurus.service.ThesaurusService;

public class ThesaurusListDialog extends BaseDialogBean {

    public static final String BEAN_NAME = "ThesaurusListDialog";

    private static final long serialVersionUID = 1L;
    private transient ThesaurusService thesaurusService;

    private List<Thesaurus> thesauri;

    @Override
    public void init(Map<String, String> parameters) {
        super.init(parameters);
        restored();
    }

    @Override
    public void restored() {
        thesauri = null;
    }

    @Override
    public void clean() {
        thesauri = null;
    }

    public void export(@SuppressWarnings("unused") ActionEvent event) {
        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
        response.setCharacterEncoding(CHARSET);
        OutputStream outputStream = null;
        OutputStreamWriter writer = null;
        try {
            response.setContentType("text/xml; charset=" + CHARSET);
            response.setHeader("Expires", "0");
            response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
            response.setHeader("Pragma", "public");
            response.setHeader("Content-disposition", "attachment;filename=thesauri.xml");
            outputStream = response.getOutputStream();
            writer = new OutputStreamWriter(outputStream, CHARSET);
            getThesaurusService().exportThesauri(writer);
            writer.flush();
            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to export thesauri", e);
        } finally {
            IOUtils.closeQuietly(outputStream);
            IOUtils.closeQuietly(writer);
            FacesContext.getCurrentInstance().responseComplete();

            // hack for incorrect view id in the next request
            JspStateManagerImpl.ignoreCurrentViewSequenceHack();
        }
    }

    @Override
    public Object getActionsContext() {
        return null;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return null;
    }

    // START: getters / setters

    public void setThesaurusService(ThesaurusService thesaurusService) {
        this.thesaurusService = thesaurusService;
    }

    public List<Thesaurus> getThesauri() {
        if (thesauri == null) {
            thesauri = getThesaurusService().getThesauri(false);
        }
        return thesauri;
    }

    public void setThesauri(List<Thesaurus> thesauri) {
        this.thesauri = thesauri;
    }

    protected ThesaurusService getThesaurusService() {
        if (thesaurusService == null) {
            thesaurusService = (ThesaurusService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(ThesaurusService.BEAN_NAME);
        }
        return thesaurusService;
    }

    /** used by other property sheet (for example, to generate {@link FieldGroupDetailsDialog}) */
    public List<SelectItem> getThesauriSelectItems(FacesContext context, @SuppressWarnings("unused") UIInput selectComponent) {
        List<Thesaurus> thesauriList = getThesauri();
        Collections.sort(thesauriList);
        List<SelectItem> results = new ArrayList<SelectItem>(thesauriList.size() + 1);
        addDefault(results, context);
        for (Thesaurus thesaurus : thesauriList) {
            SelectItem selectItem = new SelectItem(thesaurus.getName(), thesaurus.getName());
            results.add(selectItem);
        }
        return results;
    }
    // END: getters / setters
}

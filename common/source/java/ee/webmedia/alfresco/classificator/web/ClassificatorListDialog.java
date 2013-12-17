package ee.webmedia.alfresco.classificator.web;

import static ee.webmedia.alfresco.app.AppConstants.CHARSET;
import static ee.webmedia.alfresco.common.web.BeanHelper.getClassificatorService;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.ui.common.component.PickerSearchParams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.application.jsp.JspStateManagerImpl;

import ee.webmedia.alfresco.classificator.model.Classificator;
import ee.webmedia.alfresco.docadmin.web.FieldDetailsDialog;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;

public class ClassificatorListDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    private List<Classificator> classificators;
    private String searchCriteria = "";

    /**
     * Used in JSP pages.
     */
    public List<Classificator> getClassificators() {
        return classificators;
    }

    /** @param event */
    public void export(ActionEvent event) {
        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
        response.setCharacterEncoding(CHARSET);
        OutputStream outputStream = null;
        OutputStreamWriter writer = null;
        try {
            response.setContentType("text/xml; charset=" + CHARSET);
            response.setHeader("Expires", "0");
            response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
            response.setHeader("Pragma", "public");
            response.setHeader("Content-disposition", "attachment;filename=classificators.xml");
            outputStream = response.getOutputStream();
            writer = new OutputStreamWriter(outputStream, CHARSET);
            writer.write("<?xml version='1.0' encoding='" + CHARSET + "'?>\n");
            getClassificatorService().exportClassificators(writer);
            writer.flush();
            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to export classificators", e);
        } finally {
            IOUtils.closeQuietly(outputStream);
            IOUtils.closeQuietly(writer);
            FacesContext.getCurrentInstance().responseComplete();

            // Erko hack for incorrect view id in the next request
            JspStateManagerImpl.ignoreCurrentViewSequenceHack();
        }
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // finish button not shown or used
        return null;
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        restored();
    }

    @Override
    public void restored() {
        classificators = getClassificatorService().getAllClassificators();
        searchCriteria = "";
    }

    @Override
    public String cancel() {
        classificators = null;
        searchCriteria = "";
        return super.cancel();
    }

    public void search() {
        if (StringUtils.isNotBlank(getSearchCriteria())) {
            clearRichList();
            classificators = getClassificatorService().search(getSearchCriteria());
        } else {
            MessageUtil.addInfoMessage("classificators_error_emptySearchField");
        }
    }

    private void clearRichList() {
        classificators = null;
    }

    public void showAll() {
        clearRichList();
        setSearchCriteria("");
        classificators = getClassificatorService().getAllClassificators();
    }

    @Override
    public Object getActionsContext() {
        return null;
    }

    public void setSearchCriteria(String searchCriteria) {
        this.searchCriteria = searchCriteria;
    }

    public String getSearchCriteria() {
        return searchCriteria;
    }

    /**
     * Used by propertysheet of the {@link FieldDetailsDialog}.
     * Query callBack method executed by the Generic Picker component.
     * This method is part of the contract to the Generic Picker, it is up to the backing bean
     * to execute whatever query is appropriate and return the results.
     * 
     * @param params Search parameters
     * @return An array of SelectItem objects containing the results to display in the picker.
     */
    public SelectItem[] searchClassificators(PickerSearchParams params) { // XXX why not use lucene search??
        List<Classificator> allClassificators = getClassificatorService().getAllClassificators();
        List<SelectItem> results = new ArrayList<SelectItem>(allClassificators.size());
        boolean doFilter = StringUtils.isNotBlank(params.getSearchString());
        for (Classificator node : allClassificators) {
            String classificatorName = node.getName();
            if (!doFilter || StringUtils.containsIgnoreCase(classificatorName, params.getSearchString())) {
                results.add(new SelectItem(classificatorName, classificatorName));
            }
            if (results.size() == params.getLimit()) {
                break;
            }
        }
        WebUtil.sort(results);
        return results.toArray(new SelectItem[results.size()]);
    }
}

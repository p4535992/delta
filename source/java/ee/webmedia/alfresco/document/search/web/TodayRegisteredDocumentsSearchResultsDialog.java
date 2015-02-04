package ee.webmedia.alfresco.document.search.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getJsfBindingHelper;

import java.util.Map;

import javax.faces.component.UIPanel;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;
import ee.webmedia.alfresco.utils.MessageUtil;

public class TodayRegisteredDocumentsSearchResultsDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "TodayRegisteredDocumentsSearchResultsDialog";
    private String searchValue;
    private boolean quickSearch;

    @Override
    public void init(Map<String, String> params) {
        quickSearch = false;
        super.init(params);
    }

    /** @param event */
    public void setup(ActionEvent event) {
        searchValue = "";
        quickSearch = false;
        resetLimit(false);
        doInitialSearch();
        BeanHelper.getVisitedDocumentsBean().clearVisitedDocuments();
    }

    /** @param event */
    public void setupSearch(ActionEvent event) {
        quickSearch = true;
        resetLimit(false);
        doInitialSearch();
        BeanHelper.getVisitedDocumentsBean().clearVisitedDocuments();
    }

    @Override
    protected void limitChangedEvent() {
        doInitialSearch();
        BeanHelper.getVisitedDocumentsBean().clearVisitedDocuments();
    }

    @Override
    public void restored() {
        BeanHelper.getVisitedDocumentsBean().resetVisitedDocuments(documentProvider);
    }

    @Override
    public void clean() {
        super.clean();
        searchValue = null;
    }

    private void doInitialSearch() {
        documentProvider = new DocumentListDataProvider(setLimited(getDocumentSearchService().searchTodayRegisteredDocuments(quickSearch ? searchValue : null, getLimit())), true,
                DOC_PROPS_TO_LOAD);
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_registeredToday");
    }

    @Override
    public void setPanel(UIPanel panel) {
        getJsfBindingHelper().addBinding(getPanelBindingName(), panel);
    }

    @Override
    public UIPanel getPanel() {
        UIPanel panelComponent = (UIPanel) getJsfBindingHelper().getComponentBinding(getPanelBindingName());
        if (panelComponent == null) {
            panelComponent = new UIPanel();
            getJsfBindingHelper().addBinding(getPanelBindingName(), panelComponent);
        }
        return panelComponent;
    }

    public void setSearchValue(String searchValue) {
        this.searchValue = searchValue;
    }

    @Override
    public String getInfoMessage() {
        if (quickSearch && getSearchValue().length() < 3) {
            return MessageUtil.getMessage("document_quick_search_query_3_char_min");
        }
        return super.getInfoMessage();
    }

    public String getSearchValue() {
        return searchValue;
    }

}

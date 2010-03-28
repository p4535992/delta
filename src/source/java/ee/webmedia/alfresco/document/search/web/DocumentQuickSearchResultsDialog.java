package ee.webmedia.alfresco.document.search.web;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.app.servlet.FacesHelper;

import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Alar Kvell
 */
public class DocumentQuickSearchResultsDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 1L;

    private String searchValue;

    public void setup(ActionEvent event) {
        documents = getDocumentSearchService().searchDocumentsQuick(searchValue);

        // Quick search must "reset the current dialog stack" and put the document list dialog as the base dialog into the stack.
        // Also in case of quick search the cancel button is not displayed (the whole button container is not rendered through 
        // container.jsp hack for DocumentListDialog). If there are more beans that need to sometimes display some buttons and sometimes
        // not. Then the hack should be refactored into new DialogManager isAnyButtonVisible method that asks this from then current 
        // bean (BaseDialogBean always returns true).
        MenuBean.clearViewStack(String.valueOf(MenuBean.DOCUMENT_REGISTER_ID), null);

        MenuBean menuBean = (MenuBean) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), MenuBean.BEAN_NAME);
        menuBean.collapseMenuItems(null);
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_quick_search_results");
    }

    // START: getters / setters

    public String getSearchValue() {
        return searchValue;
    }

    public void setSearchValue(String searchValue) {
        this.searchValue = searchValue;
    }

    // END: getters / setters
}

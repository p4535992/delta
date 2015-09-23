package ee.webmedia.alfresco.document.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getJsfBindingHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.faces.component.UIPanel;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.search.web.DocumentListDataProvider;
import ee.webmedia.alfresco.document.service.DocumentService;

public abstract class BaseDocumentListDialog extends BaseLimitedListDialog {
    private static final long serialVersionUID = 1L;

    private transient DocumentService documentService;
    private transient DocumentSearchService documentSearchService;

    protected DocumentListDataProvider documentProvider;
    private Map<NodeRef, Boolean> listCheckboxes = new HashMap<>();
    protected static final Set<QName> SENDER_PROPS = new HashSet<>();
    protected static final Set<QName> DOC_PROPS_TO_LOAD = new HashSet<>();

    static {
        SENDER_PROPS.add(DocumentSpecificModel.Props.SENDER_DETAILS_NAME);
        SENDER_PROPS.add(DocumentDynamicModel.Props.SENDER_PERSON_NAME);
        SENDER_PROPS.add(DocumentSpecificModel.Props.SELLER_PARTY_NAME);
        SENDER_PROPS.add(DocumentSpecificModel.Props.SECOND_PARTY_NAME);
        SENDER_PROPS.add(DocumentSpecificModel.Props.THIRD_PARTY_NAME);
        SENDER_PROPS.add(DocumentSpecificModel.Props.PARTY_NAME);
        SENDER_PROPS.add(DocumentCommonModel.Props.RECIPIENT_NAME);
        SENDER_PROPS.add(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME);
        SENDER_PROPS.add(DocumentDynamicModel.Props.RECIPIENT_PERSON_NAME);
        SENDER_PROPS.add(DocumentDynamicModel.Props.ADDITIONAL_RECIPIENT_PERSON_NAME);

        DOC_PROPS_TO_LOAD.add(DocumentCommonModel.Props.REG_NUMBER);
        DOC_PROPS_TO_LOAD.add(DocumentCommonModel.Props.REG_DATE_TIME);
        DOC_PROPS_TO_LOAD.add(DocumentCommonModel.Props.DOC_NAME);
        DOC_PROPS_TO_LOAD.add(DocumentCommonModel.Props.REG_DATE_TIME);
        DOC_PROPS_TO_LOAD.add(DocumentAdminModel.Props.OBJECT_TYPE_ID);
        DOC_PROPS_TO_LOAD.add(DocumentCommonModel.Props.VOLUME);
        DOC_PROPS_TO_LOAD.add(DocumentCommonModel.Props.CASE);
        DOC_PROPS_TO_LOAD.addAll(SENDER_PROPS);
        DOC_PROPS_TO_LOAD.add(DocumentCommonModel.Props.OWNER_ID);
        DOC_PROPS_TO_LOAD.add(DocumentCommonModel.Props.OWNER_NAME);
        DOC_PROPS_TO_LOAD.add(DocumentSpecificModel.Props.DUE_DATE);
        DOC_PROPS_TO_LOAD.add(DocumentSpecificModel.Props.COMPLIENCE_DATE);
        DOC_PROPS_TO_LOAD.add(DocumentCommonModel.Props.ACCESS_RESTRICTION);
        DOC_PROPS_TO_LOAD.add(ContentModel.PROP_CREATED);
        DOC_PROPS_TO_LOAD.add(DocumentCommonModel.Props.EMAIL_DATE_TIME);
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        clearRichList();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // finish button is not used
        return null; // but in case someone clicks finish button twice on the previous dialog,
        // then silently ignore it and stay on the same page
    }

    @Override
    public String cancel() {
        clean();
        return super.cancel();
    }

    @Override
    public void clean() {
        clearRichList();
        listCheckboxes = new HashMap<NodeRef, Boolean>();
        documentProvider = null;
    }

    public boolean isShowCheckboxes() {
        return false;
    }

    @Override
    public Object getActionsContext() {
        return null; // since we are using actions, but not action context, we don't need instance of NavigationBean that is used in the overloadable method
    }

    public abstract String getListTitle();

    public void getAllDocsWithoutLimit(ActionEvent event) {
        restored();
    }

    public String getInfoMessage() {
        return ""; // Subclasses can override if necessary
    }

    public boolean isInfoMessageVisible() {
        return getInfoMessage().length() > 0;
    }

    public boolean isShowOrgStructColumn() {
        return false;
    }

    public boolean isShowComplienceDateColumn() {
        return true;
    }

    /**
     * Returns the file name to import as document list columns
     * Subclasses can override if necessary.
     *
     * @return String path to JSP file
     */
    public String getColumnsFile() {
        return "/WEB-INF/classes/ee/webmedia/alfresco/document/web/document-list-dialog-columns.jsp";
    }

    public String getDocumentListValueBinding() {
        return "#{DialogManager.bean.documents}";
    }

    // START: getters / setters

    public DocumentListDataProvider getDocuments() {
        return documentProvider;
    }

    protected DocumentService getDocumentService() {
        if (documentService == null) {
            documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(DocumentService.BEAN_NAME);
        }
        return documentService;
    }

    protected DocumentSearchService getDocumentSearchService() {
        if (documentSearchService == null) {
            documentSearchService = (DocumentSearchService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(DocumentSearchService.BEAN_NAME);
        }
        return documentSearchService;
    }

    public void setRichList(UIRichList richList) {
        getJsfBindingHelper().addBinding(getRichListBindingName(), richList);
    }

    public UIRichList getRichList() {
        return (UIRichList) getJsfBindingHelper().getComponentBinding(getRichListBindingName());
    }

    protected void clearRichList() {
        UIRichList richList2 = (UIRichList) getJsfBindingHelper().getComponentBinding(getRichListBindingName());
        if (richList2 != null) {
            richList2.setValue(null);
        }
    }

    public Map<NodeRef, Boolean> getListCheckboxes() {
        return listCheckboxes;
    }

    public void setListCheckboxes(Map<NodeRef, Boolean> listCheckboxes) {
        this.listCheckboxes = listCheckboxes;
    }

    public void setPanel(UIPanel panel) {
        getJsfBindingHelper().addBinding(getPanelBindingName(), panel);
    }

    public UIPanel getPanel() {
        UIPanel panelComponent = (UIPanel) getJsfBindingHelper().getComponentBinding(getPanelBindingName());
        if (panelComponent == null) {
            panelComponent = new UIPanel();
            getJsfBindingHelper().addBinding(getPanelBindingName(), panelComponent);
        }
        return panelComponent;
    }

    protected String getPanelBindingName() {
        return getBindingName("panel");
    }

    public boolean isContainsCases() {
        return false;
    }

    // END: getters / setters

}

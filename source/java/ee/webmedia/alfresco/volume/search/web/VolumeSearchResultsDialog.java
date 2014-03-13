package ee.webmedia.alfresco.volume.search.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIPanel;
import javax.faces.component.UIParameter;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.data.UIColumn;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.alfresco.web.ui.common.component.data.UISortLink;
import org.alfresco.web.ui.common.tag.data.ColumnTag;
import org.alfresco.web.ui.repo.component.UIActions;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.myfaces.application.jsp.JspStateManagerImpl;
import org.apache.myfaces.shared_impl.taglib.UIComponentTagUtils;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.search.web.DocumentDynamicSearchDialog;
import ee.webmedia.alfresco.document.search.web.DocumentSearchResultsDialog;
import ee.webmedia.alfresco.document.web.BaseLimitedListDialog;
import ee.webmedia.alfresco.simdhs.CSVExporter;
import ee.webmedia.alfresco.simdhs.DataReader;
import ee.webmedia.alfresco.simdhs.RichListDataReader;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.volume.model.VolumeOrCaseFile;
import ee.webmedia.alfresco.volume.search.model.VolumeSearchModel;

public class VolumeSearchResultsDialog extends BaseLimitedListDialog {
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentSearchResultsDialog.class);
    public static final String BEAN_NAME = "VolumeSearchResultsDialog";

    protected Node searchFilter;
    private transient UIRichList richList;
    private transient UIPanel panel;

    protected List<VolumeOrCaseFile> volumes;

    public String setup(Node filter) {
        searchFilter = filter;
        resetLimit(true);
        doInitialSearch();
        return "volumeSearchResultsDialog";
    }

    @Override
    protected void limitChangedEvent() {
        doInitialSearch();
    }

    public String getListTitle() {
        return MessageUtil.getMessage("volume_search_results");
    }

    protected void doInitialSearch() {
        try {
            DocumentSearchService documentSearchService = BeanHelper.getDocumentSearchService();
            volumes = setLimited(documentSearchService.queryVolumes(searchFilter, getLimit()));
        } catch (BooleanQuery.TooManyClauses e) {
            Map<QName, Serializable> filterProps = RepoUtil.getNotEmptyProperties(RepoUtil.toQNameProperties(searchFilter.getProperties()));
            // filterProps.remove(DocumentSearchModel.Props.OUTPUT);
            log.error("Volume search failed: "
                    + e.getMessage()
                    + "\n  searchFilter="
                    + WmNode.toString(filterProps, getNamespaceService())); // stack trace is logged in the service
            volumes = setLimitedEmpty();
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "volume_search_toomanyclauses");
        } catch (Hits.TooLongQueryException e) {
            Map<QName, Serializable> filterProps = RepoUtil.getNotEmptyProperties(RepoUtil.toQNameProperties(searchFilter.getProperties()));
            // filterProps.remove(DocumentSearchModel.Props.OUTPUT);
            log.error("Document search failed: "
                    + e.getMessage()
                    + "\n  searchFilter="
                    + WmNode.toString(filterProps, getNamespaceService())); // stack trace is logged in the service
            volumes = setLimitedEmpty();
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "volume_search_toolongquery");
        }
        clearRichList();
    }

    // TODO LATER could be made into an interface?
    /** @param event */
    public void exportAsCsv(ActionEvent event) {
        DataReader dataReader = new RichListDataReader();
        CSVExporter exporter = new CSVExporter(dataReader);
        exporter.export("volumeSearchResultsList");

        // hack for incorrect view id in the next request
        JspStateManagerImpl.ignoreCurrentViewSequenceHack();
    }

    /**
     * @param richList - partially preconfigured RichList from jsp
     */
    public void setRichList(UIRichList richList) {
        this.richList = richList;
        if (!richList.getChildren().isEmpty()) {
            return;
        }
        QName caseFileTypeBoolean = getLabelBoolean(VolumeSearchModel.Props.CASE_FILE_TYPE);

        FacesContext context = FacesContext.getCurrentInstance();
        Map<String, Object> props = searchFilter.getProperties();
        List<FieldDefinition> searchableFields = getDocumentAdminService().getSearchableVolumeFieldDefinitions();
        searchableFields.add(DocumentDynamicSearchDialog.createThesaurusField());
        final Map<String, String> titleLinkParams = new HashMap<String, String>(2);
        titleLinkParams.put("nodeRef", "#{r.node.nodeRef}");
        titleLinkParams.put("volumeNodeRef", "#{r.node.nodeRef}");

        NamespaceService namespaceService = getNamespaceService();
        if (Boolean.TRUE.equals(props.get(caseFileTypeBoolean.toString()))) {
            List<UIComponent> valueComponent = new ArrayList<UIComponent>();
            String valueBinding = "#{r.type}";
            valueComponent.add(createActionLink(context, valueBinding, null, null, "#{VolumeListDialog.showVolumeContents}", "#{!r.dynamic}", titleLinkParams));
            valueComponent.add(createActionLink(context, valueBinding, null, null, "#{CaseFileDialog.openFromDocumentList}", "#{r.dynamic}", titleLinkParams));
            createAndAddColumn(context, richList, MessageUtil.getMessage("volume_search_case_volume_type"), "type", false,
                    valueComponent.toArray(new UIComponent[valueComponent.size()]));
        }

        Set<String> keys = props.keySet();
        for (FieldDefinition fieldDefinition : searchableFields) {
            QName primaryQName = fieldDefinition.getQName();
            QName tamperedQName = RepoUtil.createTransientProp(primaryQName.getLocalName() + "LabelEditable");
            if (!(keys.contains(getLabelBoolean(primaryQName).toString()) || keys.contains(tamperedQName.toString()))) {
                // the original field is not visible or the qname has been tampered with
                log.error("field vanished: " + primaryQName);
                continue;
            }
            if (!(Boolean.TRUE.equals(props.get(getLabelBoolean(primaryQName))) || Boolean.TRUE.equals(props.get(getLabelBoolean(tamperedQName))))) {
                continue;
            }
            String fieldTitle = fieldDefinition.getName();
            String valueBinding;
            String sortValue = null;
            String primaryQNamePrefixString = primaryQName.toPrefixString(getNamespaceService());
            boolean isStructUnit = fieldDefinition.getFieldTypeEnum().equals(FieldType.STRUCT_UNIT);
            if (primaryQName.equals(DocumentCommonModel.Props.CASE)) {
                valueBinding = "#{r.caseLabel}";
                sortValue = "caseLabel";
            } else if (primaryQName.equals(DocumentCommonModel.Props.FUNCTION)) {
                valueBinding = "#{r.functionLabel}";
                sortValue = "functionLabel";
            } else if (primaryQName.equals(DocumentCommonModel.Props.SERIES)) {
                valueBinding = "#{r.seriesLabel}";
                sortValue = "seriesLabel";
            } else if (primaryQName.equals(DocumentDynamicModel.Props.THESAURUS)) {
                valueBinding = "#{r.hierarchicalKeywords}";
                fieldTitle = MessageUtil.getMessage("thesaurus_keywords");
                sortValue = "hierarchicalKeywords";
            } else if (isStructUnit) {
                valueBinding = "#{r.unitStrucPropsConvertedMap['" + primaryQName.toPrefixString(namespaceService) + "']}";
                sortValue = "unitStrucPropsConvertedMap;" + primaryQNamePrefixString;
            } else {
                valueBinding = "#{r.convertedPropsMap['" + primaryQName.toPrefixString(namespaceService) + "']}";
                String fieldType = fieldDefinition.getFieldType();
                if (!FieldType.DATE.name().equals(fieldType) && !FieldType.DOUBLE.name().equals(fieldType) && !FieldType.LONG.name().equals(fieldType)) {
                    sortValue = "convertedPropsMap;" + primaryQNamePrefixString;
                } else {
                    sortValue = "properties;" + primaryQNamePrefixString;
                }
            }
            List<UIComponent> valueComponent = new ArrayList<UIComponent>();
            valueComponent.add(createActionLink(context, valueBinding, null, null, "#{VolumeListDialog.showVolumeContents}", "#{!r.dynamic}", titleLinkParams));
            valueComponent.add(createActionLink(context, valueBinding, null, null, "#{CaseFileDialog.openFromDocumentList}", "#{r.dynamic}", titleLinkParams));
            createAndAddColumn(context, richList, fieldTitle, sortValue, false, valueComponent.toArray(new UIComponent[valueComponent.size()]));
        }
    }

    private QName getLabelBoolean(QName propQname) {
        return QName.createQName(propQname.toString() + WMUIProperty.AFTER_LABEL_BOOLEAN);
    }

    public UIRichList getRichList() {
        return null;
    }

    // TODO refactor these into an util?
    private static UIColumn createAndAddColumn(FacesContext context, UIRichList richList, String title, String sortLinkValue, boolean disableCsvExport,
            UIComponent... valueComponent) {
        Application application = context.getApplication();
        UIColumn column = (UIColumn) application.createComponent(ColumnTag.COMPONENT_TYPE);
        if (sortLinkValue != null) {
            UISortLink sortLink = (UISortLink) application.createComponent("org.alfresco.faces.SortLink");
            UIComponentTagUtils.setStringProperty(context, sortLink, "styleClass", "header");
            sortLink.setValue(sortLinkValue);
            sortLink.setLabel(title);
            ComponentUtil.addFacet(column, "header", sortLink);
        } else {
            HtmlOutputText headerText = (HtmlOutputText) application.createComponent(HtmlOutputText.COMPONENT_TYPE);
            UIComponentTagUtils.setStringProperty(context, headerText, "styleClass", "header");
            headerText.setValue(title);
            ComponentUtil.addFacet(column, "header", headerText);
        }
        if (disableCsvExport) {
            UIParameter param = (UIParameter) application.createComponent("javax.faces.Parameter");
            param.setValue("false");
            ComponentUtil.addFacet(column, "csvExport", param);
        }
        ComponentUtil.addChildren(column, valueComponent);
        ComponentUtil.addChildren(richList, column);
        return column;
    }

    private static UIComponent createActionLink(FacesContext context, String valueBinding, String actionBinding, String action, String actionListenerBinding,
            String renderedBinding, Map<String, String> params) {
        return createActionLink(context, valueBinding, actionBinding, action, actionListenerBinding, renderedBinding, params, true);
    }

    private static UIComponent createActionLink(FacesContext context, String valueBinding, String actionBinding, String action, String actionListenerBinding,
            String renderedBinding, Map<String, String> params, boolean condence) {
        Application application = context.getApplication();
        UIActionLink link = (UIActionLink) application.createComponent(UIActions.COMPONENT_ACTIONLINK);
        link.setRendererType(UIActions.RENDERER_ACTIONLINK);
        if (actionBinding != null) {
            link.setAction(application.createMethodBinding(actionBinding, null));
        }
        if (action != null) {
            UIComponentTagUtils.setActionProperty(context, link, actionBinding);
        }
        link.setActionListener(application.createMethodBinding(actionListenerBinding, new Class[] { javax.faces.event.ActionEvent.class }));
        if (renderedBinding != null) {
            UIComponentTagUtils.setValueBinding(context, link, "rendered", renderedBinding);
        }
        UIComponentTagUtils.setStringProperty(context, link, "styleClass", "tooltip" + (condence ? "condence20-" : ""));
        UIComponentTagUtils.setValueProperty(context, link, valueBinding);
        for (Entry<String, String> entry : params.entrySet()) {
            ComponentUtil.addChildren(link, ComponentUtil.createUIParam(entry.getKey(), entry.getValue(), application));
        }
        return link;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String cancel() {
        volumes = null;
        return super.cancel();
    }

    @Override
    public Object getActionsContext() {
        return null; // since we are using actions, but not action context, we don't need instance of NavigationBean that is used in the overloadable method
    }

    public String getInitialSortColumn() {
        return null;
    }

    // START: getters / setters

    public List<VolumeOrCaseFile> getVolumes() {
        return volumes;
    }

    protected void clearRichList() {
        if (richList != null) {
            richList.setValue(null);
        }
    }

    public void setPanel(UIPanel panel) {
        this.panel = panel;
    }

    public UIPanel getPanel() {
        if (panel == null) {
            panel = new UIPanel();
        }
        return panel;
    }

}

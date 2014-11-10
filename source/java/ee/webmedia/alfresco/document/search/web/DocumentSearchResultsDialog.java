package ee.webmedia.alfresco.document.search.web;

import static ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty.getLabelBoolean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getVisitedDocumentsBean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.UIParameter;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
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
import ee.webmedia.alfresco.common.propertysheet.customchildrencontainer.CustomChildrenContainer;
import ee.webmedia.alfresco.common.propertysheet.customchildrencontainer.CustomChildrenCreator;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.service.UnmodifiableFieldDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;
import ee.webmedia.alfresco.simdhs.CSVExporter;
import ee.webmedia.alfresco.simdhs.DataReader;
import ee.webmedia.alfresco.simdhs.RichListDataReader;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;

public class DocumentSearchResultsDialog extends BaseDocumentListDialog {
    public static final String PROPERTIES_COL_PREFIX = "properties;";
    public static final String CONVERTED_PROPS_COL_PREFIX = "convertedPropsMap;";
    public static final String UNIT_STRUC_COL_PREFIX = "unitStrucPropsConvertedMap;";
    public static final String HIERARCHICAL_KEYWORDS_COL = "hierarchicalKeywords";
    public static final String VOLUME_LABEL_COL = "volumeLabel";
    public static final String SERIES_LABEL_COL = "seriesLabel";
    public static final String FUNCTION_LABEL_COL = "functionLabel";
    public static final String CASE_LABEL_COL = "caseLabel";
    public static final String SEND_INFO_RESOLUTION_COL = "sendInfoResolution";
    public static final String SEND_INFO_SEND_DATE_TIME_COL = "sendInfoSendDateTime";
    public static final String SEND_INFO_RECIPIENT_COL = "sendInfoRecipient";
    public static final String SEND_MODE_COL = "sendMode";
    public static final String DOCUMENT_TYPE_NAME_COL = "documentTypeName";
    public static final String WORKFLOW_STATE_COL = "workflowStatus";
    public static final String SENDER_COL = "sender";
    public static final String DOC_NAME_COL = "docName";
    public static final String DOC_CREATED_DATE_COL = "createdDateStr";
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentSearchResultsDialog.class);
    public static final String BEAN_NAME = "DocumentSearchResultsDialog";

    private static Map<QName/* FieldDefinition prop name */, Pair<String /* property name */, String /* translation key */>> CUSTOM_COLUMNS = new HashMap<>();
    static {
        CUSTOM_COLUMNS.put(DocumentSearchModel.Props.DOCUMENT_TYPE, new Pair<>(DOCUMENT_TYPE_NAME_COL, "document_docType"));
        CUSTOM_COLUMNS.put(DocumentSearchModel.Props.SEND_MODE, new Pair<>(SEND_MODE_COL, "document_send_mode"));
        CUSTOM_COLUMNS.put(DocumentSearchModel.Props.SEND_INFO_RECIPIENT, new Pair<>(SEND_INFO_RECIPIENT_COL, "document_search_export_recipient"));
        CUSTOM_COLUMNS.put(DocumentSearchModel.Props.SEND_INFO_SEND_DATE_TIME, new Pair<>(SEND_INFO_SEND_DATE_TIME_COL, "document_search_send_info_time"));
        CUSTOM_COLUMNS.put(DocumentSearchModel.Props.SEND_INFO_RESOLUTION, new Pair<>(SEND_INFO_RESOLUTION_COL, "document_search_send_info_resolution"));
        CUSTOM_COLUMNS.put(DocumentSearchModel.Props.DOCUMENT_CREATED, new Pair<>(DOC_CREATED_DATE_COL, "document_search_document_created"));
    }

    protected Node searchFilter;
    @SuppressWarnings("unused")
    private UIRichList richList;

    public String setup(Node filter) {
        searchFilter = filter;
        resetLimit(true);
        doInitialSearch();
        getVisitedDocumentsBean().clearVisitedDocuments();
        return "documentSearchResultsDialog";
    }

    @Override
    protected void limitChangedEvent() {
        doInitialSearch();
        getVisitedDocumentsBean().clearVisitedDocuments();
    }

    @Override
    public void restored() {
        getVisitedDocumentsBean().resetVisitedDocuments(documentProvider);
    }

    @SuppressWarnings("unchecked")
    protected void doInitialSearch() {
        try {
            DocumentSearchService documentSearchService = getDocumentSearchService();
            List<NodeRef> documents = setLimited(documentSearchService.queryDocuments(searchFilter, getLimit()));
            documentProvider = new DocumentListDataProvider(documents);
            documentProvider.orderInitial(true, DocumentCommonModel.Props.REG_DATE_TIME);
            if (log.isDebugEnabled()) {
                log.debug("Found " + documents.size() + " document(s) during initial search. Limit: " + getLimit());
            }
        } catch (BooleanQuery.TooManyClauses e) {
            Map<QName, Serializable> filterProps = RepoUtil.getNotEmptyProperties(RepoUtil.toQNameProperties(searchFilter.getProperties()));
            log.error("Document search failed: "
                    + e.getMessage()
                    + "\n  searchFilter="
                    + WmNode.toString(filterProps, Repository
                            .getServiceRegistry(FacesContext.getCurrentInstance()).getNamespaceService()));
            setLimitedEmpty();
            documentProvider = new DocumentListDataProvider(new ArrayList<NodeRef>());
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "document_search_toomanyclauses");
        } catch (Hits.TooLongQueryException e) {
            Map<QName, Serializable> filterProps = RepoUtil.getNotEmptyProperties(RepoUtil.toQNameProperties(searchFilter.getProperties()));
            log.error("Document search failed: "
                    + e.getMessage()
                    + "\n  searchFilter="
                    + WmNode.toString(filterProps, Repository
                            .getServiceRegistry(FacesContext.getCurrentInstance()).getNamespaceService()));
            setLimitedEmpty();
            documentProvider = new DocumentListDataProvider(new ArrayList<NodeRef>());
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "document_search_toolongquery");
        }
    }

    @Override
    public String getListTitle() {
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "document_search_results");
    }

    /** @param event */
    public void exportAsCsv(ActionEvent event) {
        DataReader dataReader = new RichListDataReader();
        CSVExporter exporter = new CSVExporter(dataReader);
        exporter.export("documentList");

        // hack for incorrect view id in the next request
        JspStateManagerImpl.ignoreCurrentViewSequenceHack();
    }

    // TODO we need the richList instance
    /**
     * @param richList - partially preconfigured RichList from jsp
     */
    @Override
    public void setRichList(UIRichList richList) {
        this.richList = richList;
        if (!richList.getChildren().isEmpty()) {
            return;
        }
        FacesContext context = FacesContext.getCurrentInstance();
        Map<String, Object> props = searchFilter.getProperties();
        List<UnmodifiableFieldDefinition> searchableFields = getDocumentAdminService().getSearchableDocumentFieldDefinitions();
        final Map<String, String> titleLinkParams = new HashMap<String, String>(2);
        titleLinkParams.put("nodeRef", "#{r.node.nodeRef}");
        titleLinkParams.put("caseNodeRef", "#{r.node.nodeRef}");
        List<String> outputTextOverrides = Arrays.asList(SEND_INFO_RESOLUTION_COL);

        for (Entry<QName, Pair<String, String>> col : CUSTOM_COLUMNS.entrySet()) {
            if (Boolean.TRUE.equals(props.get(getLabelBoolean(col.getKey())))) {
                String sortLinkValue = col.getValue().getFirst();
                String valueBinding = "#{r." + sortLinkValue + "}";
                UIComponent valueComponent = null;
                if (outputTextOverrides.contains(sortLinkValue)) {
                    valueComponent = createOutput(context, sortLinkValue, valueBinding);
                } else {
                    valueComponent = createActionLink(context, valueBinding, "#{DocumentDialog.action}", null, "#{DocumentDialog.open}", null, titleLinkParams);
                }

                createAndAddColumn(context, richList, MessageUtil.getMessage(col.getValue().getSecond()), sortLinkValue, false, valueComponent);
            }
        }
        Set<String> keys = props.keySet();
        for (UnmodifiableFieldDefinition fieldDefinition : searchableFields) {
            QName primaryQName = fieldDefinition.getQName();
            if (primaryQName.equals(RepoUtil.createTransientProp("caseLabelEditable"))) {
                primaryQName = DocumentCommonModel.Props.CASE;
            }
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
            boolean isStructUnit = fieldDefinition.getFieldTypeEnum().equals(FieldType.STRUCT_UNIT);
            String sortValue = null;
            String primaryQNamePrefixString = primaryQName.toPrefixString(getNamespaceService());
            if (primaryQName.equals(DocumentCommonModel.Props.CASE)) {
                valueBinding = "#{r.caseLabel}";
                sortValue = CASE_LABEL_COL;
            } else if (primaryQName.equals(DocumentCommonModel.Props.FUNCTION)) {
                valueBinding = "#{r.functionLabel}";
                sortValue = FUNCTION_LABEL_COL;
            } else if (primaryQName.equals(DocumentCommonModel.Props.SERIES)) {
                valueBinding = "#{r.seriesLabel}";
                sortValue = SERIES_LABEL_COL;
            } else if (primaryQName.equals(DocumentCommonModel.Props.VOLUME)) {
                valueBinding = "#{r.volumeLabel}";
                sortValue = VOLUME_LABEL_COL;
            } else if (primaryQName.equals(DocumentDynamicModel.Props.FIRST_KEYWORD_LEVEL)) {
                if (Boolean.TRUE.equals(props.get(getLabelBoolean(DocumentDynamicModel.Props.THESAURUS).toString()))) {
                    valueBinding = "#{r.hierarchicalKeywords}";
                    fieldTitle = MessageUtil.getMessage("thesaurus_keywords");
                    sortValue = HIERARCHICAL_KEYWORDS_COL;
                } else {
                    continue;
                }
            } else if (isStructUnit) {
                valueBinding = "#{r.unitStrucPropsConvertedMap['" + primaryQNamePrefixString + "']}";
                sortValue = UNIT_STRUC_COL_PREFIX + primaryQNamePrefixString;
            } else {
                valueBinding = "#{r.convertedPropsMap['" + primaryQNamePrefixString + "']}";
                String fieldType = fieldDefinition.getFieldType();
                if (!FieldType.DATE.name().equals(fieldType) && !FieldType.DOUBLE.name().equals(fieldType) && !FieldType.LONG.name().equals(fieldType)) {
                    sortValue = CONVERTED_PROPS_COL_PREFIX + primaryQNamePrefixString;
                } else {
                    sortValue = PROPERTIES_COL_PREFIX + primaryQNamePrefixString;
                }
            }
            List<UIComponent> valueComponent = new ArrayList<UIComponent>();
            if (primaryQName.equals(DocumentCommonModel.Props.DOC_NAME)) {
                valueComponent.add(createActionLink(context, valueBinding, "#{DocumentDialog.action}", null, "#{DocumentDialog.open}", "#{r.cssStyleClass ne 'case'}",
                        titleLinkParams));
                valueComponent.add(createActionLink(context, valueBinding, null, "dialog:documentListDialog", "#{DocumentListDialog.setup}", "#{r.cssStyleClass == 'case'}",
                        titleLinkParams));
            } else if (primaryQName.equals(DocumentCommonModel.Props.VOLUME)) {
                final Map<String, String> volumeLinkParams = new HashMap<String, String>(1);
                volumeLinkParams.put("volumeNodeRef", "#{r.properties['" + primaryQNamePrefixString + "']}");
                valueComponent.add(createActionLink(context, valueBinding, null, null, "#{VolumeListDialog.showVolumeContents}", null, volumeLinkParams));
            } else {
                valueComponent.add(createActionLink(context, valueBinding, "#{DocumentDialog.action}", null, "#{DocumentDialog.open}", null,
                        titleLinkParams, !isStructUnit));
            }
            createAndAddColumn(context, richList, fieldTitle, sortValue, false,
                    valueComponent.toArray(new UIComponent[valueComponent.size()]));
        }
        createAndAddColumn(context, richList, MessageUtil.getMessage("document_allFiles"), null, true, createFileColumnContent(context));
    }

    @Override
    public UIRichList getRichList() {
        return null;
    }

    @Override
    public void clean() {
        super.clean();
        searchFilter = null;
        richList = null;
    }

    // TODO refactor these into an util?
    private static UIColumn createAndAddColumn(FacesContext context, UIRichList richList, String title, String sortLinkValue, boolean disableCsvExport,
            UIComponent... valueComponent) {
        Application application = context.getApplication();
        UIColumn column = (UIColumn) application.createComponent(ColumnTag.COMPONENT_TYPE);
        UIComponentTagUtils.setValueBinding(context, column, "styleClass", "#{r.cssStyleClass}");
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

    private static UIOutput createOutput(FacesContext context, String id, String valueBinding) {
        UIOutput output = ComponentUtil.createUnescapedOutputText(context, SEND_INFO_RESOLUTION_COL);
        UIComponentTagUtils.setValueProperty(context, output, valueBinding);
        UIComponentTagUtils.setStringProperty(context, output, "styleClass", "tooltip condence20-");

        return output;
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

    private static UIComponent createFileColumnContent(FacesContext context) {
        Application application = context.getApplication();
        CustomChildrenContainer fileContainer = (CustomChildrenContainer) application.createComponent(CustomChildrenContainer.class.getCanonicalName());
        fileContainer.setCustomChildCreator("#{DocumentSearchResultsDialog.customChildGenerator}");
        fileContainer.setParametersValueBinding("#{r}");
        return fileContainer;
    }

    public CustomChildrenCreator getCustomChildGenerator() {
        return ComponentUtil.getDocumentRowFileGenerator(FacesContext.getCurrentInstance().getApplication(), 5);
    }
}

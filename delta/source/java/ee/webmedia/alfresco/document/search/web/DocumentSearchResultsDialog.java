package ee.webmedia.alfresco.document.search.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSendOutService;
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
import javax.faces.component.UIParameter;
import javax.faces.component.html.HtmlGraphicImage;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.namespace.QName;
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
import ee.webmedia.alfresco.classificator.enums.SendMode;
import ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.sendout.model.SendInfo;
import ee.webmedia.alfresco.document.web.BaseDocumentListDialog;
import ee.webmedia.alfresco.privilege.web.DocPermissionEvaluator;
import ee.webmedia.alfresco.simdhs.CSVExporter;
import ee.webmedia.alfresco.simdhs.DataReader;
import ee.webmedia.alfresco.simdhs.RichListDataReader;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * @author Alar Kvell
 */
public class DocumentSearchResultsDialog extends BaseDocumentListDialog {
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentSearchResultsDialog.class);
    public static final String BEAN_NAME = "DocumentSearchResultsDialog";

    private static final List<String> EP_EXPORT_SEND_MODES = Arrays.asList(SendMode.MAIL.getValueName(), SendMode.REGISTERED_MAIL.getValueName());

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
        getVisitedDocumentsBean().resetVisitedDocuments(documents);
    }

    protected void doInitialSearch() {
        try {
            DocumentSearchService documentSearchService = getDocumentSearchService();
            documents = setLimited(documentSearchService.searchDocuments(searchFilter, getLimit()));
        } catch (BooleanQuery.TooManyClauses e) {
            Map<QName, Serializable> filterProps = RepoUtil.getNotEmptyProperties(RepoUtil.toQNameProperties(searchFilter.getProperties()));
            // filterProps.remove(DocumentSearchModel.Props.OUTPUT);
            log.error("Document search failed: "
                    + e.getMessage()
                    + "\n  searchFilter="
                    + WmNode.toString(filterProps, Repository
                            .getServiceRegistry(FacesContext.getCurrentInstance()).getNamespaceService())); // stack trace is logged in the service
            documents = setLimitedEmpty();
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "document_search_toomanyclauses");
        } catch (Hits.TooLongQueryException e) {
            Map<QName, Serializable> filterProps = RepoUtil.getNotEmptyProperties(RepoUtil.toQNameProperties(searchFilter.getProperties()));
            // filterProps.remove(DocumentSearchModel.Props.OUTPUT);
            log.error("Document search failed: "
                    + e.getMessage()
                    + "\n  searchFilter="
                    + WmNode.toString(filterProps, Repository
                            .getServiceRegistry(FacesContext.getCurrentInstance()).getNamespaceService())); // stack trace is logged in the service
            documents = setLimitedEmpty();
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

        // Erko hack for incorrect view id in the next request
        JspStateManagerImpl.ignoreCurrentViewSequenceHack();
    }

    /** @param event */
    public void exportEstonianPost(ActionEvent event) {
        CSVExporter exporter = new CSVExporter(new EstonianPostExportDataReader());
        exporter.setOrderInfo(0, false);
        exporter.export("documentList");

        // Erko hack for incorrect view id in the next request
        JspStateManagerImpl.ignoreCurrentViewSequenceHack();
    }

    private class EstonianPostExportDataReader implements DataReader {
        @Override
        public List<String> getHeaderRow(UIRichList list, FacesContext fc) {
            return Arrays.asList(MessageUtil.getMessage("document_send_mode"),
                    MessageUtil.getMessage("document_regNumber"),
                    MessageUtil.getMessage("document_search_export_recipient"));
        }

        @Override
        public List<List<String>> getDataRows(UIRichList list, FacesContext fc) {
            List<List<String>> data = new ArrayList<List<String>>();
            while (list.isDataAvailable()) {
                Document document = (Document) list.nextRow();
                List<SendInfo> sendInfos = getSendOutService().getDocumentSendInfos(document.getNodeRef());
                for (SendInfo sendInfo : sendInfos) {
                    if (EP_EXPORT_SEND_MODES.contains(sendInfo.getSendMode().toString())) {
                        data.add(Arrays.asList(sendInfo.getSendMode().toString(), document.getRegNumber(), sendInfo.getRecipient().toString()));
                    }
                }
            }
            return data;
        }
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
        List<FieldDefinition> searchableFields = getDocumentAdminService().getSearchableFieldDefinitions();
        QName dokLiikBoolean = getLabelBoolean(DocumentSearchModel.Props.DOCUMENT_TYPE);
        QName sendModeBoolean = getLabelBoolean(DocumentSearchModel.Props.SEND_MODE);
        final Map<String, String> titleLinkParams = new HashMap<String, String>(2);
        titleLinkParams.put("nodeRef", "#{r.node.nodeRef}");
        titleLinkParams.put("caseNodeRef", "#{r.node.nodeRef}");
        if (Boolean.TRUE.equals(props.get(dokLiikBoolean.toString()))) {
            UIComponent valueComponent = createActionLink(context, "#{r.documentTypeName}", "#{DocumentDialog.action}", null, "#{DocumentDialog.open}", null,
                    titleLinkParams);
            createAndAddColumn(context, richList, MessageUtil.getMessage("document_docType"), "documentTypeName", false, valueComponent);
        }
        if (Boolean.TRUE.equals(props.get(sendModeBoolean.toString()))) {
            UIComponent valueComponent = createActionLink(context, "#{r.sendMode}", "#{DocumentDialog.action}", null, "#{DocumentDialog.open}", null,
                    titleLinkParams);
            createAndAddColumn(context, richList, MessageUtil.getMessage("document_send_mode"), "sendMode", false, valueComponent);
        }
        Set<String> keys = props.keySet();
        for (FieldDefinition fieldDefinition : searchableFields) {
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
            if (primaryQName.equals(DocumentCommonModel.Props.CASE)) {
                valueBinding = "#{r.caseLabel}";
            } else if (primaryQName.equals(DocumentCommonModel.Props.FUNCTION)) {
                valueBinding = "#{r.functionLabel}";
            } else if (primaryQName.equals(DocumentCommonModel.Props.SERIES)) {
                valueBinding = "#{r.seriesLabel}";
            } else if (primaryQName.equals(DocumentCommonModel.Props.VOLUME)) {
                valueBinding = "#{r.volumeLabel}";
            } else if (isStructUnit) {
                valueBinding = "#{r.unitStrucPropsConvertedMap['" + primaryQName.toPrefixString(getNamespaceService()) + "']}";
            } else {
                valueBinding = "#{r.convertedPropsMap['" + primaryQName.toPrefixString(getNamespaceService()) + "']}";
            }
            List<UIComponent> valueComponent = new ArrayList<UIComponent>();
            if (primaryQName.equals(DocumentCommonModel.Props.DOC_NAME)) {
                valueComponent.add(createActionLink(context, valueBinding, "#{DocumentDialog.action}", null, "#{DocumentDialog.open}", "#{r.cssStyleClass ne 'case'}",
                        titleLinkParams));
                valueComponent.add(createActionLink(context, valueBinding, null, "dialog:documentListDialog", "#{DocumentListDialog.setup}", "#{r.cssStyleClass == 'case'}",
                        titleLinkParams));
            } else if (primaryQName.equals(DocumentCommonModel.Props.VOLUME)) {
                final Map<String, String> volumeLinkParams = new HashMap<String, String>(1);
                volumeLinkParams.put("volumeNodeRef", "#{r.properties['" + primaryQName.toPrefixString(getNamespaceService()) + "']}");
                valueComponent.add(createActionLink(context, valueBinding, null, null, "#{VolumeListDialog.showVolumeContents}", null, volumeLinkParams));
            } else {
                valueComponent.add(createActionLink(context, valueBinding, "#{DocumentDialog.action}", null, "#{DocumentDialog.open}", null,
                        titleLinkParams, !isStructUnit));
            }
            createAndAddColumn(context, richList, fieldTitle, primaryQName.getLocalName(), false, valueComponent.toArray(new UIComponent[valueComponent.size()]));
        }
        createAndAddColumn(context, richList, MessageUtil.getMessage("document_allFiles"), null, true, createFileColumnContent(context));
    }

    private QName getLabelBoolean(QName propQname) {
        return QName.createQName(propQname.toString() + WMUIProperty.AFTER_LABEL_BOOLEAN);
    }

    @Override
    public UIRichList getRichList() {
        return null;
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

    private static UIComponent[] createFileColumnContent(FacesContext context) {
        List<UIComponent> list = new ArrayList<UIComponent>();
        for (int i = 0; i < 5; i++) {
            Application application = context.getApplication();
            {
                UIComponent eval1 = application.createComponent(DocPermissionEvaluator.class.getCanonicalName());
                UIComponentTagUtils.setValueProperty(context, eval1, "#{r.files[" + i + "].node}");
                UIComponentTagUtils.setStringProperty(context, eval1, "allow", "viewDocumentFiles");
                UIActionLink link = (UIActionLink) application.createComponent(UIActions.COMPONENT_ACTIONLINK);
                link.setRendererType(UIActions.RENDERER_ACTIONLINK);
                UIComponentTagUtils.setValueProperty(context, link, "#{r.files[" + i + "].name}");
                UIComponentTagUtils.setStringProperty(context, link, "href", "#{r.files[" + i + "].downloadUrl}");
                UIComponentTagUtils.setStringProperty(context, link, "target", "_blank");
                UIComponentTagUtils.setBooleanProperty(context, link, "showLink", "false");
                UIComponentTagUtils.setStringProperty(context, link, "image", "/images/icons/#{r.files[" + i + "].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}");
                UIComponentTagUtils.setStringProperty(context, link, "styleClass", "inlineAction webdav-readOnly");
                ComponentUtil.addChildren(eval1, link);
                list.add(eval1);
            }

            {
                UIComponent eval1 = application.createComponent(DocPermissionEvaluator.class.getCanonicalName());
                UIComponentTagUtils.setValueProperty(context, eval1, "#{r.files[" + i + "].node}");
                UIComponentTagUtils.setStringProperty(context, eval1, "deny", "viewDocumentFiles");
                HtmlGraphicImage image = (HtmlGraphicImage) application.createComponent(HtmlGraphicImage.COMPONENT_TYPE);
                UIComponentTagUtils.setValueProperty(context, image, "/images/icons/#{r.files[" + i + "].digiDocContainer ? 'ddoc_sign_small.gif' : 'attachment.gif'}");
                UIComponentTagUtils.setStringProperty(context, image, "alt", "#{r.files[" + i + "].name}");
                UIComponentTagUtils.setStringProperty(context, image, "title", "#{r.files[" + i + "].name}");
                UIComponentTagUtils.setStringProperty(context, image, "rendered", "#{r.files[" + i + "] != null}");
                ComponentUtil.addChildren(eval1, image);
                list.add(eval1);
            }
        }
        return list.toArray(new UIComponent[list.size()]);
    }
}

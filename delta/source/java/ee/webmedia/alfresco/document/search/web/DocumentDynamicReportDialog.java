package ee.webmedia.alfresco.document.search.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentConfigService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.classificator.enums.TemplateReportOutputType;
import ee.webmedia.alfresco.classificator.enums.TemplateReportType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.docconfig.generator.fieldtype.DateGenerator;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.model.DocumentReportModel;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchFilterService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

/**
 * @author Riina Tens
 */
public class DocumentDynamicReportDialog extends DocumentDynamicSearchDialog {

    private static final long serialVersionUID = 1L;
    private static final Log LOG = LogFactory.getLog(DocumentDynamicReportDialog.class);

    private List<SelectItem> reportTemplates;

    @Override
    protected DocumentSearchFilterService getFilterService() {
        if (filterService == null) {
            filterService = BeanHelper.getDocumentReportFilterService();
        }
        return filterService;
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        reportTemplates = BeanHelper.getDocumentTemplateService().getReportTemplates(TemplateReportType.DOCUMENTS_REPORT);
        reportTemplates.add(0, new SelectItem("", MessageUtil.getMessage("select_default_label")));
    }

    @Override
    protected void loadConfig() {
        config = getDocumentConfigService().getReportConfig();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (isValidFilter()) {
            try {
                BeanHelper.getReportService().createReportResult(filter, TemplateReportType.DOCUMENTS_REPORT, DocumentReportModel.Assocs.FILTER);
                MessageUtil.addInfoMessage("report_created_success");
            } catch (UnableToPerformException e) {
                MessageUtil.addErrorMessage(e.getMessageKey());
            }
        }
        isFinished = false;
        return null;
    }

    private boolean isValidFilter() {
        Map<String, Object> filterProps = filter.getProperties();
        boolean result = true;
        if (StringUtils.isBlank((String) filterProps.get(DocumentReportModel.Props.REPORT_TEMPLATE))) {
            MessageUtil.addErrorMessage("report_error_template_not_selected");
            result = false;
        }
        if ((filterProps.get(DocumentSearchModel.Props.DOCUMENT_CREATED) == null || filterProps.get(DocumentSearchModel.Props.DOCUMENT_CREATED_END_DATE) == null)
                && (filterProps.get(DocumentCommonModel.Props.REG_DATE_TIME) == null || filterProps.get(DateGenerator.getEndDateQName(DocumentCommonModel.Props.REG_DATE_TIME)) == null)) {
            MessageUtil.addErrorMessage("report_error_created_or_registered_period_empty");
            result = false;
        }
        return result;
    }

    public List<SelectItem> getReportTemplates(FacesContext context, UIInput selectComponent) {
        return reportTemplates;
    }

    @Override
    protected Node getNewFilter() {
        long start = System.currentTimeMillis();
        try {
            Map<QName, Serializable> data = getMandatoryProps();
            TransientNode transientNode = new TransientNode(getFilterType(), null, data);
            Map<String, Object> filterProps = transientNode.getProperties();
            List<FieldDefinition> searchableFields = BeanHelper.getDocumentAdminService().getSearchableFieldDefinitions();
            for (FieldDefinition fieldDefinition : searchableFields) {
                PropertyDefinition def = getDocumentConfigService().getPropertyDefinition(transientNode, fieldDefinition.getQName());
                if (def.isMultiValued()) {
                    filterProps.put(fieldDefinition.getQName().toString(), new ArrayList<Object>());
                }
            }
            filterProps.put(DocumentReportModel.Props.REPORT_OUTPUT_TYPE.toString(), TemplateReportOutputType.DOCS_ONLY.toString());
            return transientNode;
        } finally {
            LOG.info("New report filter generation: " + (System.currentTimeMillis() - start) + "ms");
        }
    }

    @Override
    public String getManageSavedBlockTitle() {
        return MessageUtil.getMessage("document_report_saved_manage");
    }

    @Override
    public String getSavedFilterSelectTitle() {
        return MessageUtil.getMessage("document_report_saved");
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage("document_report_execute_report");
    }

    @Override
    public String getFilterPanelTitle() {
        return MessageUtil.getMessage("document_report");
    }

    @Override
    public QName getFilterType() {
        return DocumentReportModel.Types.FILTER;
    }

    @Override
    public boolean isReportSearch() {
        return true;
    }

    @Override
    public boolean isShowManageSavedDialog() {
        return BeanHelper.getUserService().isAdministrator();
    }

    @Override
    protected boolean isPrivateFilter() {
        return false;
    }

}
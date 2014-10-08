<<<<<<< HEAD
package ee.webmedia.alfresco.volume.search.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentConfigService;
import static ee.webmedia.alfresco.document.search.web.DocumentDynamicSearchDialog.setFilterDefaultValues;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.classificator.enums.TemplateReportOutputType;
import ee.webmedia.alfresco.classificator.enums.TemplateReportType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.volume.search.model.VolumeReportModel;
import ee.webmedia.alfresco.volume.search.service.VolumeSearchFilterService;

/**
 * @author Keit tehvan
 */
public class VolumeDynamicReportDialog extends VolumeDynamicSearchDialog {

    private static final long serialVersionUID = 1L;
    private static final Log LOG = LogFactory.getLog(VolumeDynamicReportDialog.class);

    private List<SelectItem> reportTemplates;

    @Override
    protected VolumeSearchFilterService getFilterService() {
        if (filterService == null) {
            filterService = BeanHelper.getVolumeReportFilterService();
        }
        return filterService;
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        reportTemplates = BeanHelper.getDocumentTemplateService().getReportTemplates(TemplateReportType.VOLUMES_REPORT);
        reportTemplates.add(0, new SelectItem("", MessageUtil.getMessage("select_default_label")));
    }

    @Override
    protected void loadConfig() {
        config = getDocumentConfigService().getVolumeSearchFilterConfig(false);
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (isValidFilter()) {
            try {
                BeanHelper.getReportService().createReportResult(filter, TemplateReportType.VOLUMES_REPORT, VolumeReportModel.Assocs.FILTER);
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
        if (StringUtils.isBlank((String) filterProps.get(VolumeReportModel.Props.REPORT_TEMPLATE))) {
            MessageUtil.addErrorMessage("report_error_template_not_selected");
            result = false;
        }
        boolean areSomeFieldsFilled = false;
        for (Entry<String, Object> entry : filterProps.entrySet()) {
            if (entry.getKey().equals(VolumeReportModel.Props.REPORT_TEMPLATE)) {
                continue;
            }
            Object value = entry.getValue();
            if (value != null) {
                if (value instanceof String && StringUtils.isBlank((String) value)) {
                    continue;
                }
                areSomeFieldsFilled = true;
                break;
            }
        }
        if (!areSomeFieldsFilled) {
            MessageUtil.addErrorMessage("report_error_missing_input");
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
            setFilterDefaultValues(transientNode, BeanHelper.getDocumentAdminService().getSearchableVolumeFieldDefinitions(), null);
            filterProps.put(VolumeReportModel.Props.REPORT_OUTPUT_TYPE.toString(), TemplateReportOutputType.DOCS_ONLY.toString());
            return transientNode;
        } finally {
            LOG.info("New report filter generation: " + (System.currentTimeMillis() - start) + "ms");
        }
    }

    @Override
    public String getManageSavedBlockTitle() {
        return MessageUtil.getMessage("volume_report_saved_manage");
    }

    @Override
    public String getSavedFilterSelectTitle() {
        return MessageUtil.getMessage("volume_report_saved");
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage("volume_report_execute_report");
    }

    @Override
    public String getFilterPanelTitle() {
        return MessageUtil.getMessage("volume_report");
    }

    @Override
    public QName getFilterType() {
        return VolumeReportModel.Types.FILTER;
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
=======
package ee.webmedia.alfresco.volume.search.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentConfigService;
import static ee.webmedia.alfresco.document.search.web.DocumentDynamicSearchDialog.setFilterDefaultValues;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.classificator.enums.TemplateReportOutputType;
import ee.webmedia.alfresco.classificator.enums.TemplateReportType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.volume.search.model.VolumeReportModel;
import ee.webmedia.alfresco.volume.search.service.VolumeSearchFilterService;

public class VolumeDynamicReportDialog extends VolumeDynamicSearchDialog {

    private static final long serialVersionUID = 1L;
    private static final Log LOG = LogFactory.getLog(VolumeDynamicReportDialog.class);

    private List<SelectItem> reportTemplates;

    @Override
    protected VolumeSearchFilterService getFilterService() {
        if (filterService == null) {
            filterService = BeanHelper.getVolumeReportFilterService();
        }
        return filterService;
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        reportTemplates = BeanHelper.getDocumentTemplateService().getReportTemplates(TemplateReportType.VOLUMES_REPORT);
        reportTemplates.add(0, new SelectItem("", MessageUtil.getMessage("select_default_label")));
    }

    @Override
    protected void loadConfig() {
        config = getDocumentConfigService().getVolumeSearchFilterConfig(false);
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (isValidFilter()) {
            try {
                BeanHelper.getReportService().createReportResult(filter, TemplateReportType.VOLUMES_REPORT, VolumeReportModel.Assocs.FILTER);
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
        if (StringUtils.isBlank((String) filterProps.get(VolumeReportModel.Props.REPORT_TEMPLATE))) {
            MessageUtil.addErrorMessage("report_error_template_not_selected");
            result = false;
        }
        boolean areSomeFieldsFilled = false;
        for (Entry<String, Object> entry : filterProps.entrySet()) {
            if (entry.getKey().equals(VolumeReportModel.Props.REPORT_TEMPLATE)) {
                continue;
            }
            Object value = entry.getValue();
            if (value != null) {
                if (value instanceof String && StringUtils.isBlank((String) value)) {
                    continue;
                }
                areSomeFieldsFilled = true;
                break;
            }
        }
        if (!areSomeFieldsFilled) {
            MessageUtil.addErrorMessage("report_error_missing_input");
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
            setFilterDefaultValues(transientNode, BeanHelper.getDocumentAdminService().getSearchableVolumeFieldDefinitions(), null);
            filterProps.put(VolumeReportModel.Props.REPORT_OUTPUT_TYPE.toString(), TemplateReportOutputType.DOCS_ONLY.toString());
            return transientNode;
        } finally {
            LOG.info("New report filter generation: " + (System.currentTimeMillis() - start) + "ms");
        }
    }

    @Override
    public String getManageSavedBlockTitle() {
        return MessageUtil.getMessage("volume_report_saved_manage");
    }

    @Override
    public String getSavedFilterSelectTitle() {
        return MessageUtil.getMessage("volume_report_saved");
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage("volume_report_execute_report");
    }

    @Override
    public String getFilterPanelTitle() {
        return MessageUtil.getMessage("volume_report");
    }

    @Override
    public QName getFilterType() {
        return VolumeReportModel.Types.FILTER;
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
>>>>>>> develop-5.1

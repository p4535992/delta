package ee.webmedia.alfresco.volume.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getCaseListDialog;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentListDialog;
import static ee.webmedia.alfresco.common.web.BeanHelper.getLogService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * Form backing bean for Volumes list
 */
public class VolumeListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    private transient SeriesService seriesService;
    private transient VolumeService volumeService;
    private Series parent;

    public static final String BEAN_NAME = "VolumeListDialog";
    public static final String DIALOG_NAME = "volumeListDialog";

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        resetFields();
        return outcome;
    }

    @Override
    public String cancel() {
        resetFields();
        return super.cancel();
    }

    public void showAll(ActionEvent event) {
        showAll(new NodeRef(ActionUtil.getParam(event, "seriesNodeRef")));
    }

    public void showAll(NodeRef nodeRef) {
        parent = getSeriesService().getSeriesByNodeRef(nodeRef.toString());
        getLogService().addLogEntry(LogEntry.create(LogObject.SERIES, getUserService(), nodeRef, "applog_space_open", parent.getSeriesIdentifier(), parent.getTitle()));
    }

    public List<Volume> getEntries() {
        final List<Volume> volumes = getVolumeService().getAllVolumesBySeries(parent.getNode().getNodeRef());
        return volumes;
    }

    public Series getParent() {
        return parent;
    }

    @Override
    public Object getActionsContext() {
        return parent.getNode();
    }

    public void showVolumeContents(ActionEvent event) {
        NodeRef volumeRef = new NodeRef(ActionUtil.getParam(event, "volumeNodeRef"));
        Volume volume = getVolumeService().getVolumeByNodeRef(volumeRef);
        boolean isVolumeTypeCase = volume.getVolumeType().equals(VolumeType.CASE_FILE.name());
        if (!volume.isContainsCases() && !isVolumeTypeCase) {
            getDocumentListDialog().init(volumeRef);
        } else if (volume.isContainsCases() && !isVolumeTypeCase) {
            getCaseListDialog().init(volumeRef);
        } else {
            throw new RuntimeException("Not implemented");
        }
    }

    // END: jsf actions/accessors

    private void resetFields() {
        parent = null;

    }

    // START: getters / setters
    public void setVolumeService(VolumeService volumeService) {
        this.volumeService = volumeService;
    }

    protected VolumeService getVolumeService() {
        if (volumeService == null) {
            volumeService = (VolumeService) FacesContextUtils.getRequiredWebApplicationContext(//
                    FacesContext.getCurrentInstance()).getBean(VolumeService.BEAN_NAME);
        }
        return volumeService;
    }

    public void setSeriesService(SeriesService seriesService) {
        this.seriesService = seriesService;
    }

    protected SeriesService getSeriesService() {
        if (seriesService == null) {
            seriesService = (SeriesService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(SeriesService.BEAN_NAME);
        }
        return seriesService;
    }
    // END: getters / setters

}

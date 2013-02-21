package ee.webmedia.alfresco.volume.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getCaseDocumentListDialog;
import static ee.webmedia.alfresco.common.web.BeanHelper.getCaseFileDialog;
import static ee.webmedia.alfresco.common.web.BeanHelper.getLogService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getVolumeService;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.model.VolumeOrCaseFile;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * Form backing bean for Volumes list
 * 
 * @author Ats Uiboupin
 */
public class VolumeListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    private Series parent;

    public static final String BEAN_NAME = "VolumeListDialog";

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
        parent = BeanHelper.getSeriesService().getSeriesByNodeRef(nodeRef.toString());
        getLogService().addLogEntry(LogEntry.create(LogObject.SERIES, getUserService(), nodeRef, "applog_space_open", parent.getSeriesIdentifier(), parent.getTitle()));
    }

    public List<Volume> getEntries() {
        VolumeService volumeService = getVolumeService();
        final List<Volume> volumes = volumeService.getAllVolumesBySeries(parent.getNode().getNodeRef());

        // Only check for validFrom value. Alternative is volumeMark or unknown value, which is already covered by VolumeService
        String defaultVolumeSortingField = volumeService.getDefaultVolumeSortingField();
        if (VolumeModel.Props.VALID_FROM.getLocalName().equals(defaultVolumeSortingField)) {
            Collections.sort(volumes, new Comparator<VolumeOrCaseFile>() {

                @Override
                public int compare(VolumeOrCaseFile o1, VolumeOrCaseFile o2) {
                    // This field is mandatory
                    return o2.getValidFrom().compareTo(o1.getValidFrom());
                }
            });
        }

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
        String volumeType = volume.getVolumeType();
        boolean isStaticVolumeType = VolumeType.ANNUAL_FILE.name().equals(volumeType) || VolumeType.SUBJECT_FILE.name().equals(volumeType);
        if (isStaticVolumeType) {
            getCaseDocumentListDialog().init(volumeRef);
        } else {
            getCaseFileDialog().open(volumeRef, false);
        }
    }

    // END: jsf actions/accessors

    private void resetFields() {
        parent = null;

    }

}

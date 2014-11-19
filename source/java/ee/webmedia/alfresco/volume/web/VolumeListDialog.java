package ee.webmedia.alfresco.volume.web;

<<<<<<< HEAD
import static ee.webmedia.alfresco.common.web.BeanHelper.getCaseDocumentListDialog;
import static ee.webmedia.alfresco.common.web.BeanHelper.getCaseFileDialog;
import static ee.webmedia.alfresco.common.web.BeanHelper.getLogService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getVolumeService;

import java.util.Collections;
import java.util.Comparator;
=======
import static ee.webmedia.alfresco.common.web.BeanHelper.getCaseListDialog;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentListDialog;
import static ee.webmedia.alfresco.common.web.BeanHelper.getLogService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
<<<<<<< HEAD

import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.model.VolumeOrCaseFile;
=======
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.volume.model.Volume;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * Form backing bean for Volumes list
<<<<<<< HEAD
 * 
 * @author Ats Uiboupin
 */
public class VolumeListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    private Series parent;

    public static final String BEAN_NAME = "VolumeListDialog";
=======
 */
public class VolumeListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    private transient SeriesService seriesService;
    private transient VolumeService volumeService;
    private Series parent;

    public static final String BEAN_NAME = "VolumeListDialog";
    public static final String DIALOG_NAME = "volumeListDialog";
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

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
<<<<<<< HEAD
        parent = BeanHelper.getSeriesService().getSeriesByNodeRef(nodeRef.toString());
=======
        parent = getSeriesService().getSeriesByNodeRef(nodeRef.toString());
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        getLogService().addLogEntry(LogEntry.create(LogObject.SERIES, getUserService(), nodeRef, "applog_space_open", parent.getSeriesIdentifier(), parent.getTitle()));
    }

    public List<Volume> getEntries() {
<<<<<<< HEAD
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

=======
        final List<Volume> volumes = getVolumeService().getAllVolumesBySeries(parent.getNode().getNodeRef());
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
        String volumeType = volume.getVolumeType();
        boolean isStaticVolumeType = VolumeType.ANNUAL_FILE.name().equals(volumeType) || VolumeType.SUBJECT_FILE.name().equals(volumeType);
        if (isStaticVolumeType) {
            getCaseDocumentListDialog().init(volumeRef);
        } else {
            getCaseFileDialog().open(volumeRef, false);
=======
        boolean isVolumeTypeCase = volume.getVolumeType().equals(VolumeType.CASE_FILE.name());
        if (!volume.isContainsCases() && !isVolumeTypeCase) {
            getDocumentListDialog().init(volumeRef);
        } else if (volume.isContainsCases() && !isVolumeTypeCase) {
            getCaseListDialog().init(volumeRef);
        } else {
            throw new RuntimeException("Not implemented");
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        }
    }

    // END: jsf actions/accessors

    private void resetFields() {
        parent = null;

    }

<<<<<<< HEAD
=======
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

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}

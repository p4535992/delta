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

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.series.model.UnmodifiableSeries;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.volume.model.UnmodifiableVolume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * Form backing bean for Volumes list
 */
public class VolumeListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    private UnmodifiableSeries parent;
    private List<UnmodifiableVolume> volumes;

    public static final String BEAN_NAME = "VolumeListDialog";
    public static final String DIALOG_NAME = "volumeListDialog";

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        clean();
        return outcome;
    }

    @Override
    public String cancel() {
        clean();
        return super.cancel();
    }

    public void showAll(ActionEvent event) {
        showAll(new NodeRef(ActionUtil.getParam(event, "seriesNodeRef")));
    }

    @Override
    public void restored() {
        volumes = null;
        getEntries();
    }

    public void showAll(NodeRef nodeRef) {
        clean();
        parent = BeanHelper.getSeriesService().getUnmodifiableSeries(nodeRef, null);
        getLogService().addLogEntry(LogEntry.create(LogObject.SERIES, getUserService(), nodeRef, "applog_space_open", parent.getSeriesIdentifier(), parent.getTitle()));
    }

    public List<UnmodifiableVolume> getEntries() {
        if (volumes == null) {
            VolumeService volumeService = getVolumeService();
            volumes = volumeService.getAllVolumesBySeries(getParent().getSeriesRef());

            // Only check for validFrom value. Alternative is volumeMark or unknown value, which is already covered by VolumeService
            String defaultVolumeSortingField = BeanHelper.getApplicationConstantsBean().getDefaultVolumeSortingField();
            if (VolumeModel.Props.VALID_FROM.getLocalName().equals(defaultVolumeSortingField)) {
                Collections.sort(volumes, new Comparator<UnmodifiableVolume>() {

                    @Override
                    public int compare(UnmodifiableVolume o1, UnmodifiableVolume o2) {
                        // This field is mandatory
                        return o2.getValidFrom().compareTo(o1.getValidFrom());
                    }
                });
            }
        }
        return volumes;
    }

    public UnmodifiableSeries getParent() {
        if (parent == null) {
            NodeRef nodeRef = BeanHelper.getMenuBean().getLinkNodeRef();
            ChildAssociationRef ref = BeanHelper.getNodeService().getPrimaryParent(nodeRef);
            NodeRef seriesRef = ref.getChildRef();
            parent = BeanHelper.getSeriesService().getUnmodifiableSeries(seriesRef, null);
        }
        return parent;
    }

    public void showVolumeContents(ActionEvent event) {
        NodeRef volumeRef = new NodeRef(ActionUtil.getParam(event, "volumeNodeRef"));
        UnmodifiableVolume volume = getVolumeService().getUnmodifiableVolume(volumeRef, null);
        String volumeType = volume.getVolumeType();
        boolean isStaticVolumeType = VolumeType.ANNUAL_FILE.name().equals(volumeType) || VolumeType.SUBJECT_FILE.name().equals(volumeType);
        if (isStaticVolumeType) {
            getCaseDocumentListDialog().init(volumeRef);
        } else {
            getCaseFileDialog().open(volumeRef, false);
        }
    }

    @Override
    public void clean() {
        parent = null;
        volumes = null;
    }

}

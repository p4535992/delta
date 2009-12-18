package ee.webmedia.alfresco.volume.web;

import java.util.Collections;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItem;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.propertysheet.relateddropdown.RelatedDropdown;
import ee.webmedia.alfresco.common.propertysheet.relateddropdown.RelatedDropdownGenerator;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.service.VolumeService;

public class VolumeListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    private transient SeriesService seriesService;
    private transient VolumeService volumeService;
    private Series parent;

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
        parent = getSeriesService().getSeriesByNoderef((ActionUtil.getParam(event, "seriesNodeRef")));
    }

    public List<Volume> getEntries() {
        final List<Volume> volumes = getVolumeService().getAllVolumesBySeries(parent.getNode().getNodeRef());
        Collections.sort(volumes);
        return volumes;
    }

    public Series getParent() {
        return parent;
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

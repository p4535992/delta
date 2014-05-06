package ee.webmedia.alfresco.series.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;

import java.util.Collection;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.privilege.web.PrivilegesHandler;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * {@link PrivilegesHandler} for nodes of type {@link SeriesModel.Types#SERIES}
 */
public class SeriesTypePrivilegesHandler extends PrivilegesHandler {
    private static final long serialVersionUID = 1L;

    protected SeriesTypePrivilegesHandler() {
        super(SeriesModel.Types.SERIES, getDocumentCaseFilePrivs());
    }

    @Override
    public boolean isSubmitWhenCheckboxUnchecked() {
        return false;
    }

    @Override
    public String getConfirmMessage() {
        String key = checkboxValue ? "series_manage_permissions_confirm_docsVisibleChanged" : "series_manage_permissions_confirm_docsNotVisibleChanged";
        return MessageUtil.getMessageAndEscapeJS(key);
    }

    @Override
    protected boolean initCheckboxValue() {
        return BeanHelper.getSeriesService().getSeriesByNodeRef(state.getManageableRef()).isDocumentsVisibleForUsersWithoutAccess();
    }

    @Override
    public boolean isPermissionColumnDisabled(Privilege privilege) {
        if (Privilege.VIEW_CASE_FILE.equals(privilege) || Privilege.EDIT_CASE_FILE.equals(privilege)) {
            NodeRef seriesRef = state.getManageableRef();
            @SuppressWarnings("unchecked")
            Collection<String> volumeType = (Collection<String>) getNodeService().getProperty(seriesRef, SeriesModel.Props.VOL_TYPE);
            return !volumeType.contains(VolumeType.CASE_FILE.name());
        }
        return false;
    }

    @Override
    public void save() {
        if (checkboxValueBeforeSave != checkboxValue) {
            SeriesService seriesService = BeanHelper.getSeriesService();
            Series series = seriesService.getSeriesByNodeRef(state.getManageableRef());
            series.setDocumentsVisibleForUsersWithoutAccess(checkboxValue);
            seriesService.saveOrUpdate(series);
            BeanHelper.getSeriesDetailsDialog().updateInMemoryDocsVisibilityProperty(checkboxValue);
        }
    }
}
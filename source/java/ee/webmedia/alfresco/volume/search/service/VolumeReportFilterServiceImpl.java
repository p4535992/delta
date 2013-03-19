package ee.webmedia.alfresco.volume.search.service;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.volume.search.model.VolumeReportModel;

/**
 * @author Keit Tehvan
 */
public class VolumeReportFilterServiceImpl extends VolumeSearchFilterServiceImpl implements VolumeReportFilterService {

    @Override
    protected QName getFilterNodeType() {
        return VolumeReportModel.Types.FILTER;
    }

    @Override
    protected QName getFilterContainerAspect() {
        return VolumeReportModel.Aspects.VOLUME_REPORT_FILTERS_CONTAINER;
    }

    @Override
    protected QName getContainerToFilterAssoc() {
        return VolumeReportModel.Assocs.FILTERS;
    }

    @Override
    protected NodeRef getRoot() {
        return generalService.getNodeRef(VolumeReportModel.Repo.FILTERS_SPACE);
    }

}

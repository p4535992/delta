package ee.webmedia.alfresco.volume.search.service;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.filter.service.AbstractFilterServiceImpl;
import ee.webmedia.alfresco.volume.search.model.VolumeSearchModel;

public class VolumeSearchFilterServiceImpl extends AbstractFilterServiceImpl implements VolumeSearchFilterService {

    @Override
    public QName getFilterNameProperty() {
        return VolumeSearchModel.Props.NAME;
    }

    @Override
    protected QName getFilterNodeType() {
        return VolumeSearchModel.Types.FILTER;
    }

    @Override
    protected QName getContainerToFilterAssoc() {
        return VolumeSearchModel.Assocs.FILTER;
    }

    @Override
    protected QName getFilterContainerAspect() {
        return VolumeSearchModel.Aspects.VOLUME_SEARCH_FILTERS_CONTAINER;
    }

    @Override
    protected NodeRef getRoot() {
        return generalService.getNodeRef(VolumeSearchModel.Repo.FILTERS_SPACE);
    }

}

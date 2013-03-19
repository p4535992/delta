package ee.webmedia.alfresco.workflow.search.service;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.filter.service.AbstractFilterServiceImpl;
import ee.webmedia.alfresco.workflow.search.model.CompoundWorkflowSearchModel;

/**
 * @author Keit tehvan
 */
public class CompoundWorkflowSearchFilterServiceImpl extends AbstractFilterServiceImpl implements CompoundWorkflowSearchFilterService {

    @Override
    public QName getFilterNameProperty() {
        return CompoundWorkflowSearchModel.Props.NAME;
    }

    @Override
    protected QName getFilterNodeType() {
        return CompoundWorkflowSearchModel.Types.FILTER;
    }

    @Override
    protected QName getFilterContainerAspect() {
        return CompoundWorkflowSearchModel.Aspects.CW_SEARCH_FILTERS_CONTAINER;
    }

    @Override
    protected QName getContainerToFilterAssoc() {
        return CompoundWorkflowSearchModel.Assocs.FILTER;
    }

    @Override
    protected NodeRef getRoot() {
        return generalService.getNodeRef(CompoundWorkflowSearchModel.Repo.FILTERS_SPACE);
    }

}

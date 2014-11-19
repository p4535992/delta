package ee.webmedia.alfresco.workflow.search.service;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.filter.service.AbstractFilterServiceImpl;
import ee.webmedia.alfresco.workflow.search.model.TaskSearchModel;

<<<<<<< HEAD
/**
 * @author Erko Hansar
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public class TaskSearchFilterServiceImpl extends AbstractFilterServiceImpl implements TaskSearchFilterService {

    @Override
    public QName getFilterNameProperty() {
        return TaskSearchModel.Props.NAME;
    }

    @Override
    protected QName getFilterNodeType() {
        return TaskSearchModel.Types.FILTER;
    }

    @Override
    protected QName getFilterContainerAspect() {
        return TaskSearchModel.Aspects.TASK_SEARCH_FILTERS_CONTAINER;
    }

    @Override
    protected QName getContainerToFilterAssoc() {
        return TaskSearchModel.Assocs.FILTER;
    }

    @Override
    protected NodeRef getRoot() {
        return generalService.getNodeRef(TaskSearchModel.Repo.FILTERS_SPACE);
    }

}

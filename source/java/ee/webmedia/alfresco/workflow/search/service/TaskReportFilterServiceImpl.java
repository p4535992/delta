package ee.webmedia.alfresco.workflow.search.service;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.workflow.search.model.TaskReportModel;

public class TaskReportFilterServiceImpl extends TaskSearchFilterServiceImpl implements TaskReportFilterService {

    @Override
    protected QName getFilterNodeType() {
        return TaskReportModel.Types.FILTER;
    }

    @Override
    protected QName getFilterContainerAspect() {
        return TaskReportModel.Aspects.TASK_REPORT_FILTERS_CONTAINER;
    }

    @Override
    protected QName getContainerToFilterAssoc() {
        return TaskReportModel.Assocs.FILTERS;
    }

    @Override
    protected NodeRef getRoot() {
        return generalService.getNodeRef(TaskReportModel.Repo.FILTERS_SPACE);
    }

}

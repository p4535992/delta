<<<<<<< HEAD
package ee.webmedia.alfresco.document.search.service;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.search.model.DocumentReportModel;

/**
 * @author Riina Tens
 */
public class DocumentReportFilterServiceImpl extends DocumentSearchFilterServiceImpl implements DocumentReportFilterService {

    @Override
    protected QName getFilterNodeType() {
        return DocumentReportModel.Types.FILTER;
    }

    @Override
    protected QName getFilterContainerAspect() {
        return DocumentReportModel.Aspects.DOCUMENT_REPORT_FILTERS_CONTAINER;
    }

    @Override
    protected QName getContainerToFilterAssoc() {
        return DocumentReportModel.Assocs.FILTERS;
    }

    @Override
    protected NodeRef getRoot() {
        return generalService.getNodeRef(DocumentReportModel.Repo.FILTERS_SPACE);
    }

}
=======
package ee.webmedia.alfresco.document.search.service;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.search.model.DocumentReportModel;

public class DocumentReportFilterServiceImpl extends DocumentSearchFilterServiceImpl implements DocumentReportFilterService {

    @Override
    protected QName getFilterNodeType() {
        return DocumentReportModel.Types.FILTER;
    }

    @Override
    protected QName getFilterContainerAspect() {
        return DocumentReportModel.Aspects.DOCUMENT_REPORT_FILTERS_CONTAINER;
    }

    @Override
    protected QName getContainerToFilterAssoc() {
        return DocumentReportModel.Assocs.FILTERS;
    }

    @Override
    protected NodeRef getRoot() {
        return generalService.getNodeRef(DocumentReportModel.Repo.FILTERS_SPACE);
    }

}
>>>>>>> develop-5.1

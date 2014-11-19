package ee.webmedia.alfresco.document.search.service;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.filter.service.AbstractFilterServiceImpl;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public class DocumentSearchFilterServiceImpl extends AbstractFilterServiceImpl implements DocumentSearchFilterService {

    @Override
    public QName getFilterNameProperty() {
        return DocumentSearchModel.Props.NAME;
    }

    @Override
    protected QName getFilterNodeType() {
        return DocumentSearchModel.Types.FILTER;
    }

    @Override
    protected QName getContainerToFilterAssoc() {
        return DocumentSearchModel.Assocs.FILTER;
    }

    @Override
    protected QName getFilterContainerAspect() {
        return DocumentSearchModel.Aspects.DOCUMENT_SEARCH_FILTERS_CONTAINER;
    }

    @Override
    protected NodeRef getRoot() {
        return generalService.getNodeRef(DocumentSearchModel.Repo.FILTERS_SPACE);
    }

}

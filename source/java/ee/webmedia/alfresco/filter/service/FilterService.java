<<<<<<< HEAD
package ee.webmedia.alfresco.filter.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.filter.model.FilterVO;

/**
 * Base interface for filtering services
 * 
 * @author Ats Uiboupin
 */
public interface FilterService {

    Node getFilter(NodeRef filter);

    List<FilterVO> getFilters();

    Node createOrSaveFilter(Node filter, boolean isPrivate);

    Node createFilter(Node filter, boolean isPrivate);

    void deleteFilter(NodeRef filter);

    QName getFilterNameProperty();

}
=======
package ee.webmedia.alfresco.filter.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.filter.model.FilterVO;

/**
 * Base interface for filtering services
 */
public interface FilterService {

    Node getFilter(NodeRef filter);

    List<FilterVO> getFilters();

    Node createOrSaveFilter(Node filter, boolean isPrivate);

    Node createFilter(Node filter, boolean isPrivate);

    void deleteFilter(NodeRef filter);

    QName getFilterNameProperty();

}
>>>>>>> develop-5.1

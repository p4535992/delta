package ee.webmedia.alfresco.document.search.service;

import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

/**
 * @author Alar Kvell
 */
public interface DocumentSearchFilterService {

    String BEAN_NAME = "DocumentSearchFilterService";

    Node createOrSaveFilter(Node filter);

    Node createFilter(Node filter);

    Node getFilter(NodeRef filter);

    Map<NodeRef, String> getFilters();

    void deleteFilter(NodeRef filter);

}

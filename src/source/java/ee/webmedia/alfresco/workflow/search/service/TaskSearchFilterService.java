package ee.webmedia.alfresco.workflow.search.service;

import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

/**
 * @author Erko Hansar
 */
public interface TaskSearchFilterService {

    String BEAN_NAME = "TaskSearchFilterService";

    Node createOrSaveFilter(Node filter);

    Node createFilter(Node filter);

    Node getFilter(NodeRef filter);

    Map<NodeRef, String> getFilters();

    void deleteFilter(NodeRef filter);

}

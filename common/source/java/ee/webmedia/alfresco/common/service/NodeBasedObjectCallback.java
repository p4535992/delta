package ee.webmedia.alfresco.common.service;

import org.alfresco.web.bean.repository.Node;

public interface NodeBasedObjectCallback<T extends Object> {

    T create(Node node);

}

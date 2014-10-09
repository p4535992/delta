package ee.webmedia.alfresco.common.service;

import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

public interface CreateNodeCallback<T extends Node> {

    T create(NodeRef nodeRef, Map<String, Object> properties, QName typeQname);

}

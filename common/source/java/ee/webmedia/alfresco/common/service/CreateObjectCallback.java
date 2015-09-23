package ee.webmedia.alfresco.common.service;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

public interface CreateObjectCallback<T> {

    T create(NodeRef nodeRef, Map<QName, Serializable> properties);

}
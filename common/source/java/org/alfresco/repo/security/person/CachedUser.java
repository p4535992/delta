package org.alfresco.repo.security.person;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.utils.RepoUtil;

public class CachedUser implements Serializable {

    private static final long serialVersionUID = 1L;
    private final Node node;
    private final Map<QName, Serializable> props;

    public CachedUser(Node node) {
        this.node = node;
        props = RepoUtil.toQNameProperties(node.getProperties());
    }

    public Node getNode() {
        return node;
    }

    public Map<QName, Serializable> getProps() {
        return props;
    }
}

package ee.webmedia.alfresco.common.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * Base class for Java VO's that represent nodes in repository
 * 
 * @author Ats Uiboupin
 */
public class NodeBaseVO implements Serializable {
    private static final long serialVersionUID = 1L;

    protected WmNode node;

    public WmNode getNode() {
        return node;
    }

    public NodeRef getNodeRef() {
        return node != null ? node.getNodeRef() : null;
    }

    protected boolean getPropBoolean(QName propName) {
        Boolean prop = getProp(propName);
        if (prop == null) {
            return false;
        }
        return prop;
    }

    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getProp(QName propName) {
        return (T) getNode().getProperties().get(propName);
    }

    @SuppressWarnings("unchecked")
    protected <T extends List<? extends Serializable>> T getPropList(QName propName) {
        return (T) getNode().getProperties().get(propName);
    }

    public void setProp(QName propName, Serializable propValue) {
        getNode().getProperties().put(propName.toString(), propValue);
    }

    protected void setPropList(QName propName, List<? extends Serializable> propValue) {
        if (propValue == null) {
            propValue = new ArrayList<Serializable>(0);
        }
        getNode().getProperties().put(propName.toString(), propValue);
    }

    protected static <E extends Enum<E>> E getEnumFromValue(Class<E> enumType, String value) {
        return StringUtils.isBlank(value) ? null : Enum.valueOf(enumType, value);
    }

    protected static <E extends Enum<E>> String getValueFromEnum(E value) {
        return value == null ? null : value.name();
    }

    public boolean isSaved() {
        return !isUnsaved();
    }

    public boolean isUnsaved() {
        return RepoUtil.isUnsaved(node);
    }

    @Override
    public NodeBaseVO clone() {
        try {
            NodeBaseVO copy = (NodeBaseVO) super.clone();
            copy.node = node.clone();
            return copy;
        } catch (CloneNotSupportedException e) {
            // shouldn't happen, because BaseObject is clonable and children are also subtypes of BaseObject
            throw new RuntimeException(e);
        }
    }
}

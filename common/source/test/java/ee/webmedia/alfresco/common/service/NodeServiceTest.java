package ee.webmedia.alfresco.common.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.BaseAlfrescoSpringTest;
import org.apache.commons.lang.ObjectUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.utils.RepoUtil;

public class NodeServiceTest extends BaseAlfrescoSpringTest {

    public void testSetAndGetDynamicProperty() {
        NodeRef nodeRef = createNode();
        List<Serializable> testValues = createTestValues(nodeRef);
        QName propName = RepoUtil.createTransientProp("foo");
        for (Serializable setValue : testValues) {
            nodeService.setProperty(nodeRef, propName, setValue);
            Serializable getValue = nodeService.getProperty(nodeRef, propName);
            if (!ObjectUtils.equals(setValue, getValue)) {
                Assert.fail("nodeService.setProperty=" + WmNode.toStringWithClass(setValue) + "\nnodeService.getProperty=" + WmNode.toStringWithClass(getValue));
            }
        }
    }

    public void testSetAndGetDynamicProperties() {
        NodeRef nodeRef = createNode();
        List<Serializable> testValues = createTestValues(nodeRef);
        Map<QName, Serializable> setProps = new HashMap<QName, Serializable>();
        int i = 11;
        for (Serializable value : testValues) {
            setProps.put(RepoUtil.createTransientProp("foo" + (i++)), value);
        }
        nodeService.setProperties(nodeRef, setProps);
        Map<QName, Serializable> getProps = nodeService.getProperties(nodeRef);
        getProps.remove(ContentModel.PROP_NAME);
        getProps.remove(ContentModel.PROP_NODE_DBID);
        getProps.remove(ContentModel.PROP_NODE_UUID);
        getProps.remove(ContentModel.PROP_STORE_IDENTIFIER);
        getProps.remove(ContentModel.PROP_STORE_PROTOCOL);
        if (!ObjectUtils.equals(setProps, getProps)) {
            Assert.fail("nodeService.setProperties=" + WmNode.toString(setProps, BeanHelper.getNamespaceService()) + "\nnodeService.getProperties="
                    + WmNode.toString(getProps, BeanHelper.getNamespaceService()));
        }
    }

    private NodeRef createNode() {
        return nodeService.createNode(rootNodeRef, ContentModel.ASSOC_CHILDREN, ContentModel.ASSOC_CHILDREN, ContentModel.TYPE_BASE).getChildRef();
    }

    private List<Serializable> createTestValues(NodeRef nodeRef) {
        List<Serializable> testValues = new ArrayList<Serializable>();

        // NB! Don't test Integer, it is converted to Long in DB
        // - when property is not defined in data model, data type is not known, and DB only has Long column, not Integer column

        addTestElements(testValues, "1", "2", "3");
        addTestElements(testValues, "", "", "");
        addTestElements(testValues, 1L, 2L, 3L);
        addTestElements(testValues, 1f, 2f, 3f);
        addTestElements(testValues, 1d, 2d, 3d);
        addTestElements(testValues, null, null, null);
        addTestElements(testValues, true, false, true);
        addTestElements(testValues, new Date(System.currentTimeMillis() - 100000L), new Date(System.currentTimeMillis() + 100000L), new Date(System.currentTimeMillis() + 200000L));
        addTestElements(testValues, nodeRef, rootNodeRef, nodeRef);
        addTestElements(testValues, ContentModel.PROP_NAME, ContentModel.PROP_CREATOR, ContentModel.PROP_MODIFIER);
        testValues.add(new ArrayList<Serializable>());

        ArrayList<Serializable> listOfDifferentTypeValues = new ArrayList<Serializable>();
        for (Serializable value : testValues) {
            if (value instanceof Collection) {
                @SuppressWarnings("unchecked")
                Collection<? extends Serializable> list = (Collection<? extends Serializable>) value;
                listOfDifferentTypeValues.addAll(list);
            }
        }
        testValues.add(listOfDifferentTypeValues);

        ArrayList<Serializable> listOfLists = new ArrayList<Serializable>();
        for (Serializable value : testValues) {
            listOfLists.add(value);
        }
        testValues.add(listOfLists);

        return testValues;
    }

    private void addTestElements(List<Serializable> result, Serializable... values) {
        ArrayList<Serializable> list = new ArrayList<Serializable>(Arrays.asList(values));
        result.add(list);

        if (values.length >= 3) {
            list = new ArrayList<Serializable>(list.subList(0, 2));
            result.add(list);
        }

        if (values.length >= 2) {
            list = new ArrayList<Serializable>(list.subList(0, 1));
            result.add(list);
        }

        if (values.length >= 1) {
            result.add(values[0]);
        }
    }

}

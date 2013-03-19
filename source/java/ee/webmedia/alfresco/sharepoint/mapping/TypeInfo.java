package ee.webmedia.alfresco.sharepoint.mapping;

import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.ArrayUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.TreeNode;

public class TypeInfo {

    private final QName qname;

    private final DocumentTypeVersion docVer;

    private final Map<QName, PropertyDefinition> propDefs;

    private final TreeNode<QName> childAssocTypeQNameTree;

    private final QName[] hierarchy;

    public TypeInfo(QName qname, Map<QName, PropertyDefinition> propDefs) {
        this(qname, propDefs, null, null, null);
    }

    public TypeInfo(QName qname, Map<QName, PropertyDefinition> propDefs, DocumentTypeVersion docVer, TreeNode<QName> childAssocTypeQNameTree,
                    QName[] hierarchy) {
        this.qname = qname;
        this.docVer = docVer;
        this.propDefs = propDefs;
        this.childAssocTypeQNameTree = childAssocTypeQNameTree;
        this.hierarchy = hierarchy;
    }

    TypeInfo(QName qname, TypeInfo parentInfo) {
        TreeNode<QName> currentTreeNode = null;

        for (TreeNode<QName> treeNode : parentInfo.getChildAssocTypeQNameTree().getChildren()) {
            if (treeNode.getData().equals(qname)) {
                currentTreeNode = treeNode;
                break;
            }
        }

        if (currentTreeNode == null) {
            throw new RuntimeException("Child node type " + qname + " not found for parent node type "
                    + parentInfo.getQName());
        }

        this.qname = qname;
        docVer = null;
        propDefs = parentInfo.propDefs;
        childAssocTypeQNameTree = currentTreeNode;
        hierarchy = (QName[]) ArrayUtils.add(parentInfo.getHierarchy(), qname);
    }

    public PropertyDefinition requireProperty(String property) {
        for (Entry<QName, PropertyDefinition> entry : propDefs.entrySet()) {
            if (entry.getKey().getLocalName().equals(property)) {
                return entry.getValue();
            }
        }

        if (DocumentCommonModel.Types.DOCUMENT.equals(qname)) {
            FieldDefinition fieldDefinition = BeanHelper.getDocumentAdminService().getFieldDefinition(property);
            if (fieldDefinition != null) {
                return BeanHelper.getDocumentConfigService().createPropertyDefinition(fieldDefinition);
            }
        }

        throw new RuntimeException("Could not found property definition for " + property + " under " + qname + " (info: " + propDefs.keySet() + ")");
    }

    public String getName() {
        return qname.getLocalName();
    }

    public QName getQName() {
        return qname;
    }

    public DocumentTypeVersion getDocVer() {
        return docVer;
    }

    public TreeNode<QName> getChildAssocTypeQNameTree() {
        return childAssocTypeQNameTree;
    }

    public QName[] getHierarchy() {
        return hierarchy;
    }
}

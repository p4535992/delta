package ee.webmedia.alfresco.docconfig.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.DynamicType;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docadmin.service.UnmodifiableFieldDefinition;
import ee.webmedia.alfresco.docconfig.generator.FieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.FieldGroupGenerator;
import ee.webmedia.alfresco.utils.TreeNode;

public interface DocumentConfigService {

    String BEAN_NAME = "DocumentConfigService";

    void registerFieldGeneratorByType(FieldGenerator fieldGenerator, FieldType... fieldTypes);

    void registerFieldGeneratorById(FieldGenerator fieldGenerator, String... originalFieldIds);

    void registerFieldGroupGenerator(FieldGroupGenerator fieldGroupGenerator, String... systematicGroupNames);

    DocumentConfig getConfig(Node documentDynamicNode);

    List<String> getSaveListenerBeanNames(Node documentDynamicNode);

    DocumentConfig getSearchConfig();

    DocumentConfig getDocLocationConfig();

    Pair<DocumentType, DocumentTypeVersion> getDocumentTypeAndVersion(Node documentDynamicNode, boolean cloneResult);

    void setUserContactProps(Map<QName, Serializable> props, String userName, String fieldId);

    void setUserContactProps(Map<QName, Serializable> props, String userName, String fieldId, Class<? extends DynamicType> typeClass);

    void setUserContactProps(Map<QName, Serializable> props, String userName, PropertyDefinition propDef, Field field);

    DynamicPropertyDefinition getPropertyDefinition(Node node, QName property);

    /**
     * Get property definition and field pairs that are declared for a document.
     * A pair always has non-null propertyDefinition; but field may be null, if it is a special hidden field.
     *
     * @param documentDynamicNode
     * @return
     */
    Map<String, Pair<DynamicPropertyDefinition, Field>> getPropertyDefinitions(Node documentDynamicNode);

    Map<String, Pair<DynamicPropertyDefinition, Field>> getPropertyDefinitions(PropDefCacheKey cacheKey);

    /**
     * Set default property values on given node and all it's child and grand-child nodes.
     *
     * @param node node, on which default values are set. Can be document node or it's child (or grand-child) node.
     * @param childAssocTypeQNameHierarchy if node is document node, then this should be {@code null}; if node is document's child or grand-child node, then this should be its
     *            childAssocTypeQName hierarchy.
     * @param forceOverwrite if {@code false} then existing values are not overwritten; if {@code true}, then existing values are overwritten with a default value, if the default
     *            value exists.
     * @param docVer version that matches document node; if {@code null} then version is loaded automatically
     */
    void setDefaultPropertyValues(Node node, QName[] childAssocTypeQNameHierarchy, boolean forceOverwrite, boolean reallySetDefaultValues, DocumentTypeVersion docVer);

    /**
     * Set default property values only on specific fields on given node and all it's child and grand-child nodes.
     *
     * @see #setDefaultPropertyValues(Node, QName[], boolean, DocumentTypeVersion)
     */
    void setDefaultPropertyValues(Node node, QName[] childAssocTypeQNameHierarchy, boolean forceOverwrite, boolean reallySetDefaultValues, List<Field> fields);

    void registerMultiValuedOverrideInSystematicGroup(String... originalFieldIds);

    void registerMultiValuedOverrideBySystematicGroupName(String systematicGroupName, Set<String> originalFieldIds);

    void registerHiddenFieldDependency(String hiddenFieldId, String fieldIdAndOriginalFieldId);

    Set<String> getHiddenPropFieldIds(Collection<Field> originalFields);

    void registerChildAssocTypeQNameHierarchy(String systematicGroupName, QName childAssocTypeQName,
            Map<QName[], Set<String> /* originalFieldIds */> additionalChildAssocTypeQNameHierarchy);

    TreeNode<QName> getChildAssocTypeQNameTree(DocumentTypeVersion docVer);

    TreeNode<QName> getChildAssocTypeQNameTree(Node documentDynamicNode);

    DynamicPropertyDefinition createPropertyDefinition(Field field);

    /**
     * NB! before this method can be called, it must be guaranteed that this fieldId is actually used in DocumentDynamicModel!
     *
     * @param fieldId
     * @return
     */
    DynamicPropertyDefinition getPropertyDefinitionById(String fieldId);

    PropertyDefinition getStaticOrDynamicPropertyDefinition(QName propName);

    DocumentConfig getReportConfig();

    DocumentConfig getVolumeSearchFilterConfig(boolean withCheckboxes);

    DocumentConfig getAssocObjectSearchConfig(String additionalStateHolderKey, String renderAssocObjectFieldValueBinding);

    DocumentConfig getEventPlanVolumeSearchFilterConfig();

    boolean isRegDateFilterInAssociationsSearch();

    ItemConfigVO generateFieldGroupReadonlyItem(FieldGroup fieldGroup);

    void removeFrompPopertyDefinitionForSearchCache(String fieldId);

    void removeFromChildAssocTypeQNameTreeCache(Pair<String, Integer> typeAndVersion);

    void removeFromPropertyDefinitionCache(PropDefCacheKey key);

    PropertyDefinition createPropertyDefinition(UnmodifiableFieldDefinition fieldDefinition);

}

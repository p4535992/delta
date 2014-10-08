package ee.webmedia.alfresco.docconfig.service;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;

/**
 * Property definition that isn't globally the same, but some of it's behavior depends on the {@link DocumentTypeVersion}.
 */
public interface DynamicPropertyDefinition extends PropertyDefinition {

    /**
     * If {@code null} or empty, then this property exists on the document node itself, not on child (or grand-child) nodes. If non-empty, then this property does not exist on the
     * document node itself, but on child (or grand-child) nodes that are accessible by the assocTypeQName hierarchy. <br/>
     * <br/>
     * <code>nodeService.getChildAssocs(nodeRef, assocTypeQName, RegexQNamePattern.MATCH_ALL).get(assocIndex);
     * node.allChildAssociationsByAssocType[assocTypeQName][assocIndex]</code>
     */
    QName[] getChildAssocTypeQNameHierarchy();

    Boolean getMultiValuedOverride();

    QName getDataTypeQName();

    FieldType getFieldType();

}

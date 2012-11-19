package ee.webmedia.alfresco.sharepoint.mapping;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentConfigService;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.web.DocAdminUtil;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docconfig.service.PropDefCacheKey;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.sharepoint.SharepointMapping;
import ee.webmedia.alfresco.utils.TreeNode;

public class TypeInfoContext {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(TypeInfoContext.class);

    private final Map<String, TypeInfo> typeInfos = new HashMap<String, TypeInfo>();

    private final GeneralService generalService = BeanHelper.getGeneralService();

    private final NamespaceService namespaceService = BeanHelper.getNamespaceService();

    private final DictionaryService dictionaryService = BeanHelper.getDictionaryService();

    private final Stack<String> toStack = new Stack<String>();

    public TypeInfo begin(String prefix, String to) {
        final TypeInfo current;

        if (typeInfos.containsKey(to)) {
            current = typeInfos.get(to);
        } else if (toStack.isEmpty()) {
            current = createTypeInfo(prefix, to, null);
            typeInfos.put(to, current);
        } else {
            TypeInfo prev = typeInfos.get(toStack.peek());
            current = createTypeInfo(prefix, to, prev);
            typeInfos.put(to, current);
        }

        if (current == null) {
            throw new RuntimeException("Could not resolve type info for target [" + prefix + ":" + to + "]");
        }

        toStack.push(to);
        return current;
    }

    public void end() {
        toStack.pop();
    }

    private TypeInfo createTypeInfo(String prefix, String name, TypeInfo parentInfo) {

        QName qname = QName.createQName(prefix, name, namespaceService);

        Map<QName, PropertyDefinition> propDefs = new HashMap<QName, PropertyDefinition>();
        DocumentTypeVersion latestDocTypeVersion;
        TreeNode<QName> childAssocTypeQNameTree;
        QName[] hierarchy;

        if (parentInfo == null) {

            if (!SharepointMapping.PREFIX_DEFAULT.equals(prefix)) {
                Collection<QName> aspects = generalService.getDefaultAspects(qname);
                if (SharepointMapping.PREFIX_DOCCOM.equals(prefix)) {
                    aspects.add(DocumentCommonModel.Aspects.COMMON);
                }
                aspects.add(qname);

                for (QName aspect : aspects) {
                    propDefs.putAll(dictionaryService.getPropertyDefs(aspect));
                }

                return new TypeInfo(qname, propDefs);
            }

            DocumentType docType = getDocumentAdminService().getDocumentType(name, DocumentAdminService.DOC_TYPE_WITH_OUT_GRAND_CHILDREN_EXEPT_LATEST_DOCTYPE_VER);
            Assert.notNull(docType, "Document type doesn't exist: " + name);

            latestDocTypeVersion = docType.getLatestDocumentTypeVersion();
            childAssocTypeQNameTree = getDocumentConfigService().getChildAssocTypeQNameTree(latestDocTypeVersion);
            hierarchy = new QName[0];

            PropDefCacheKey cacheKey = DocAdminUtil.getPropDefCacheKey(DocumentType.class, latestDocTypeVersion);
            LOG.info("Retrieving property definitions for document type: " + cacheKey.getDynamicTypeId() + "#" + cacheKey.getVersion());

            Map<String, Pair<DynamicPropertyDefinition, Field>> propertyDefinitions = getDocumentConfigService().getPropertyDefinitions(cacheKey);
            if (propertyDefinitions == null) {
                throw new RuntimeException("Property definitions were not returned for: " + cacheKey);
            }

            for (Pair<DynamicPropertyDefinition, Field> pair : propertyDefinitions.values()) {
                propDefs.put(pair.getFirst().getName(), pair.getFirst());
            }

            return new TypeInfo(qname, propDefs, latestDocTypeVersion, childAssocTypeQNameTree, hierarchy);
        }

        return new TypeInfo(qname, parentInfo);
    }
}
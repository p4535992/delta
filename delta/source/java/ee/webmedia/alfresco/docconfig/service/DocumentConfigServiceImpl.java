package ee.webmedia.alfresco.docconfig.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.base.BaseObject.ChildrenList;
import ee.webmedia.alfresco.classificator.constant.FieldChangeableIf;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO.ConfigItemType;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docadmin.service.MetadataItem;
import ee.webmedia.alfresco.docadmin.service.SeparatorLine;
import ee.webmedia.alfresco.docconfig.generator.FieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.FieldGroupGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
import ee.webmedia.alfresco.docconfig.generator.PropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.SaveListener;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.web.MetadataBlock;

/**
 * @author Alar Kvell
 */
public class DocumentConfigServiceImpl implements DocumentConfigService {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DocumentConfigServiceImpl.class);

    private DocumentAdminService documentAdminService;
    private DictionaryService dictionaryService;
    private NamespaceService namespaceService;
    private NodeService nodeService;
    private GeneralService generalService;

    private final Map<FieldType, FieldGenerator> fieldGenerators = new HashMap<FieldType, FieldGenerator>();
    private final Map<QName, FieldGenerator> fieldIdGenerators = new HashMap<QName, FieldGenerator>();

    // TODO in the future caching

    @Override
    public void registerFieldGeneratorByType(FieldGenerator fieldGenerator, FieldType... fieldTypes) {
        Assert.notNull(fieldGenerator, "fieldGenerator");
        for (FieldType fieldType : fieldTypes) {
            Assert.notNull(fieldType, "fieldType");
            Assert.isTrue(!fieldGenerators.containsKey(fieldType), "FieldGenerator with fieldType already registered: " + fieldType);
            fieldGenerators.put(fieldType, fieldGenerator);
        }
    }

    @Override
    public void registerFieldGeneratorById(FieldGenerator fieldGenerator, QName... fieldIds) {
        Assert.notNull(fieldGenerator, "fieldGenerator");
        for (QName fieldId : fieldIds) {
            Assert.notNull(fieldId, "fieldId");
            Assert.isTrue(!fieldIdGenerators.containsKey(fieldId), "FieldGenerator with fieldId already registered: " + fieldId);
            fieldIdGenerators.put(fieldId, fieldGenerator);
        }
    }

    @Override
    public DocumentConfig getConfig(Node documentDynamicNode) {
        Assert.isTrue(DocumentDynamicModel.Types.DOCUMENT_DYNAMIC.equals(documentDynamicNode.getType()));
        String docTypeId = (String) documentDynamicNode.getProperties().get(DocumentDynamicModel.Props.DOCUMENT_TYPE_ID);
        int docTypeVersionNr = (Integer) documentDynamicNode.getProperties().get(DocumentDynamicModel.Props.DOCUMENT_TYPE_VERSION_NR);
        return getConfig(docTypeId, docTypeVersionNr);
    }

    private DocumentConfig getConfig(String docTypeId, int docTypeVersionNr) {
        DocumentType docType = documentAdminService.getDocumentType(docTypeId);
        if (docType == null) {
            throw new RuntimeException("documentType with documentTypeId=" + docTypeId + " not found");
        }
        DocumentTypeVersion docVersion = null;
        List<? extends DocumentTypeVersion> versions = docType.getDocumentTypeVersions().getList();
        for (DocumentTypeVersion version : versions) {
            if (docTypeVersionNr == version.getVersionNr()) {
                docVersion = version;
                break;
            }
        }
        if (docVersion == null) {
            throw new RuntimeException("documentTypeVersion with versionNr=" + docTypeVersionNr + " not found under documentType=" + docType.toString());
        }

        WMPropertySheetConfigElement propSheet = new WMPropertySheetConfigElement();
        Map<String, PropertySheetStateHolder> stateHolders = new HashMap<String, PropertySheetStateHolder>();
        List<String> saveListenerBeanNames = new ArrayList<String>();
        DocumentConfig config = new DocumentConfig(propSheet, stateHolders, saveListenerBeanNames);

        int separatorCount = 0;
        for (MetadataItem metadataItem : docVersion.getMetadata().getList()) {
            if (metadataItem instanceof Field) {
                Field field = (Field) metadataItem;
                processField(config, field);

            } else if (metadataItem instanceof SeparatorLine) {
                processSeparatorLine(config, separatorCount++);

            } else if (metadataItem instanceof FieldGroup) {
                FieldGroup fieldGroup = (FieldGroup) metadataItem;
                processFieldGroup(config, fieldGroup);

            } else {
                throw new RuntimeException("Unsupported metadataItem class=" + metadataItem.getClass() + " under documentTypeVersion=" + docVersion.toString());
            }
        }

        DocumentConfig unmodifiableConfig = new DocumentConfig(config.getPropertySheetConfigElement(), Collections.unmodifiableMap(config.getStateHolders()),
                Collections.unmodifiableList(config.getSaveListenerBeanNames()));
        LOG.info("Returning " + unmodifiableConfig);
        return unmodifiableConfig;
    }

    private static class GeneratorResultsImpl implements GeneratorResults {

        private final ItemConfigVO pregeneratedItem;
        private boolean preGeneratedItemAdded = false;
        private final DocumentConfig config;

        public GeneratorResultsImpl(ItemConfigVO pregeneratedItem, DocumentConfig config) {
            this.pregeneratedItem = pregeneratedItem;
            this.config = config;
        }

        @Override
        public ItemConfigVO getAndAddPreGeneratedItem() {
            Assert.notNull(pregeneratedItem, "Calling this method is not allowed");
            Assert.isTrue(!preGeneratedItemAdded, "This method may be called only once");
            preGeneratedItemAdded = true;
            return pregeneratedItem;
        }

        @Override
        public ItemConfigVO generateAndAddViewModeText(String name, String label) {
            ItemConfigVO viewModeTextItem = new ItemConfigVO(name);
            viewModeTextItem.setConfigItemType(ConfigItemType.PROPERTY);
            viewModeTextItem.setShowInEditMode(false);
            viewModeTextItem.setDisplayLabel(label);
            WMPropertySheetConfigElement propSheet = config.getPropertySheetConfigElement();
            Assert.isTrue(!propSheet.getItems().containsKey(viewModeTextItem.getName()), "PropertySheetItem with name already exists: " + viewModeTextItem.getName());
            propSheet.addItem(viewModeTextItem);
            return viewModeTextItem;
        }

        @Override
        public void addStateHolder(String key, PropertySheetStateHolder stateHolder) {
            Assert.notNull(key, "key");
            Map<String, PropertySheetStateHolder> stateHolders = config.getStateHolders();
            Assert.isTrue(!stateHolders.containsKey(key));
            stateHolders.put(key, stateHolder);
        }

    }

    private void processSeparatorLine(DocumentConfig config, int separatorCount) {
        String name = "_separator" + separatorCount;
        String componentGenerator = "SeparatorGenerator";

        ItemConfigVO item = new ItemConfigVO(name);
        item.setConfigItemType(ConfigItemType.SEPARATOR);
        item.setIgnoreIfMissing(false);
        item.setComponentGenerator(componentGenerator);
        WMPropertySheetConfigElement propSheet = config.getPropertySheetConfigElement();
        Assert.isTrue(!propSheet.getItems().containsKey(item.getName()), "PropertySheetItem with name already exists: " + item.getName());
        propSheet.addItem(item);
    }

    private void processFieldGroup(DocumentConfig config, FieldGroup fieldGroup) {
        ChildrenList<Field> fields = fieldGroup.getFields();
        Set<Class<? extends FieldGenerator>> executedFieldGenerators = new HashSet<Class<? extends FieldGenerator>>();
        for (Field field : fields) {
            boolean fieldAdded = processField(config, field);

            if (!fieldAdded) {
                continue;
            }
            FieldGenerator fieldIdGenerator = fieldIdGenerators.get(field.getFieldId());
            if (fieldIdGenerator != null && fieldIdGenerator instanceof FieldGroupGenerator && !executedFieldGenerators.contains(fieldIdGenerator.getClass())) {
                try {
                    ((FieldGroupGenerator) fieldIdGenerator).generateFieldGroup(fieldGroup, new GeneratorResultsImpl(null, config));
                } catch (Exception e) {
                    throw new RuntimeException("Error running generator for fieldGroup=" + fieldGroup.toString() + ": " + e.getMessage(), e);
                }
                executedFieldGenerators.add(fieldIdGenerator.getClass());
            }
        }
    }

    /** References {@link MetadataBlock#isFieldChangeableIfWorkingDoc()} */
    private boolean processField(DocumentConfig config, Field field) {
        String name = field.getFieldId().toPrefixString(namespaceService);
        ItemConfigVO item = new ItemConfigVO(name);
        item.setConfigItemType(ConfigItemType.PROPERTY);
        item.setIgnoreIfMissing(false);
        item.setDisplayLabel(field.getName());
        item.setDisplayLabelId(null);
        item.setConverter(null);
        item.setComponentGenerator(null); // Must be set by implementation
        item.setReadOnly(false);
        item.setShowInViewMode(true);
        item.setShowInEditMode(true);
        item.setForcedMandatory(field.isMandatory());
        FieldChangeableIf changeableIf = field.getChangeableIfEnum();
        if (changeableIf != null) {
            switch (changeableIf) {
            case ALWAYS_CHANGEABLE:
                break;
            case ALWAYS_NOT_CHANGEABLE:
                item.setReadOnly(true);
                break;
            case CHANGEABLE_IF_WORKING_DOC:
                item.setReadOnlyIf("#{!DocumentDynamicDialog.meta.fieldChangeableIfWorkingDoc}"); // move this to PropertySheetStateBean ?
                break;
            }
        }

        /*
         * 1) Run "by fieldType" generator
         * 2) Run "by id" generator if it exists; give the same preGeneratedItem to it that was given to (1), so (1) may have modified it
         * Both can call generateAndAddViewModeText method and multiple times, all textItems will be added in the order called; item name duplicate checks are NOT performed.
         * Both can call addStateHolder method and multiple times, all textItems will be added in the order called; duplicate key will throw exception.
         * ViewModeTextItems and StateHolders will be added regardless if getAndAddPreGeneratedItem is or is not called.
         * 3) If this field belongs to a fieldGroup and this field was added, then run generateFieldGroup on "by id" generator if it supports it; this method will be run only once
         * per fieldGroup+generator
         * .
         * If 1 and 2 call getAndAddPreGeneratedItem method:
         * a) NOT EXIST - warn and return
         * b) FALSE, NOT EXIST - do not add preGeneratedItem
         * c) FALSE, FALSE - do not add preGeneratedItem
         * d) FALSE, TRUE - add
         * e) TRUE, NOT EXIST - add
         * f) TRUE, FALSE - do not add
         * g) TRUE, TRUE - add
         * If (2) does not exist, then add item if (1) called getAndAddPreGeneratedItem method.
         * If (2) exists, then add item if (2) called getAndAddPreGeneratedItem method, regardless what (1) called.
         */

        // 1) Run "by fieldType" generator
        FieldGenerator fieldGeneratorByType = fieldGenerators.get(field.getFieldTypeEnum());
        if (fieldGeneratorByType == null) {
            LOG.warn("Unsupported field type, ignoring: " + field.toString());
            return false;
        }
        GeneratorResultsImpl generatorResults = new GeneratorResultsImpl(item, config);
        try {
            fieldGeneratorByType.generateField(field, generatorResults);
        } catch (Exception e) {
            throw new RuntimeException("Error running generator for field type, field=" + field.toString() + ": " + e.getMessage(), e);
        }
        boolean preGeneratedItemAdded = generatorResults.preGeneratedItemAdded;

        // 2) Run "by id" generator if it exists
        FieldGenerator fieldGeneratorById = fieldIdGenerators.get(field.getFieldId());
        if (fieldGeneratorById != null) {
            generatorResults = new GeneratorResultsImpl(item, config);
            try {
                fieldGeneratorById.generateField(field, generatorResults);
            } catch (Exception e) {
                throw new RuntimeException("Error running generator for field id, field=" + field.toString() + ": " + e.getMessage(), e);
            }
            preGeneratedItemAdded = generatorResults.preGeneratedItemAdded;
        }

        if (!preGeneratedItemAdded) {
            return false;
        }

        WMPropertySheetConfigElement propSheet = config.getPropertySheetConfigElement();
        Assert.isTrue(!propSheet.getItems().containsKey(item.getName()), "PropertySheetItem with name already exists: " + item.getName());
        propSheet.addItem(item);

        addSaveListener(config, fieldGeneratorByType);
        addSaveListener(config, fieldGeneratorById);

        return true;
    }

    private void addSaveListener(DocumentConfig config, FieldGenerator fieldGenerator) {
        if (fieldGenerator instanceof SaveListener) {
            SaveListener saveListener = (SaveListener) fieldGenerator;
            List<String> saveListenerBeanNames = config.getSaveListenerBeanNames();
            String beanName = saveListener.getBeanName();
            if (!saveListenerBeanNames.contains(beanName)) {
                saveListenerBeanNames.add(beanName);
            }
        }
    }

    // START: setters
    public void setDocumentAdminService(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }
    // END: setters

}

package ee.webmedia.alfresco.classificator.service;

import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.io.Serializable;
import java.io.Writer;
import java.text.RuleBasedCollator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO9075;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import com.thoughtworks.xstream.XStream;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.classificator.model.Classificator;
import ee.webmedia.alfresco.classificator.model.ClassificatorExportVO;
import ee.webmedia.alfresco.classificator.model.ClassificatorExportVO.ClassificatorValueState;
import ee.webmedia.alfresco.classificator.model.ClassificatorModel;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.utils.ComparableTransformer;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;

public class ClassificatorServiceImpl implements ClassificatorService {

    private static final Log log = LogFactory.getLog(ClassificatorServiceImpl.class);

    public static BeanPropertyMapper<Classificator> classificatorBeanPropertyMapper;
    private static BeanPropertyMapper<ClassificatorValue> classificatorValueBeanPropertyMapper;
    private static final Comparator<ClassificatorValue> CLASSIFICATOR_VALUES_ALFABETIC_ORDER_COMPARATOR;
    private static final Set<QName> CLASSIFICATOR_VALUE_TYPE_QNAME = new HashSet<>();
    private DocumentSearchService documentSearchService;
    private BulkLoadNodeService bulkLoadNodeService;
    static {
        classificatorBeanPropertyMapper = BeanPropertyMapper.newInstance(Classificator.class);
        classificatorValueBeanPropertyMapper = BeanPropertyMapper.newInstance(ClassificatorValue.class);

        RuleBasedCollator et_EECollator = (RuleBasedCollator) AppConstants.getNewCollatorInstance();
        @SuppressWarnings("unchecked")
        Comparator<ClassificatorValue> tmp = new TransformingComparator(new ComparableTransformer<ClassificatorValue>() {
            @Override
            public Comparable<?> tr(ClassificatorValue c) {
                return c.getValueName() == null ? "" : c.getValueName();
            }
        }, et_EECollator);
        CLASSIFICATOR_VALUES_ALFABETIC_ORDER_COMPARATOR = tmp;
        CLASSIFICATOR_VALUE_TYPE_QNAME.add(ClassificatorModel.Types.CLASSIFICATOR_VALUE);
    }

    private GeneralService generalService;
    private NodeService nodeService;
    private SimpleCache<String, Classificator> classificatorsCache;
    private static NodeRef classificatorRoot;

    @Override
    public List<Classificator> getAllClassificators() {

        Map<String, NodeRef> namesAndNodeRefs = bulkLoadNodeService.loadChildElementsNodeRefs(getClassificatorRoot(), ClassificatorModel.Props.CLASSIFICATOR_NAME,
                ClassificatorModel.Types.CLASSIFICATOR);
        List<Classificator> allClassificators = new ArrayList<>();
        for (Entry<String, NodeRef> classif : namesAndNodeRefs.entrySet()) {
            allClassificators.add(getClassificator(classif.getKey(), classif.getValue()));
        }
        if (log.isDebugEnabled()) {
            log.debug("Classificators found: " + allClassificators);
        }
        return allClassificators;
    }

    private List<ClassificatorExportVO> getAllClassificatorsToExport() {
        final List<Classificator> allClassificators = getAllClassificators();
        final List<ClassificatorExportVO> exportClassificators = new ArrayList<>(allClassificators.size());
        for (Classificator classificator : allClassificators) {
            final List<ClassificatorValue> allClassificatorValues = classificator.getValues();
            final ClassificatorExportVO clExport = new ClassificatorExportVO(classificator, allClassificatorValues);
            exportClassificators.add(clExport);
        }
        return exportClassificators;
    }

    @Override
    public void exportClassificators(Writer writer) {
        final List<ClassificatorExportVO> allClassificatorsToExport = getAllClassificatorsToExport();
        XStream xstream = new XStream();
        xstream.processAnnotations(ClassificatorExportVO.class);
        xstream.processAnnotations(ClassificatorValue.class);
        xstream.toXML(allClassificatorsToExport, writer);
    }

    @Override
    public void importClassificators(Collection<ClassificatorExportVO> changedClassificators) {
        for (ClassificatorExportVO classificatorExportVO : changedClassificators) {
            for (Entry<String, ClassificatorValueState> entry : classificatorExportVO.getValuesByName().entrySet()) {
                final ClassificatorValueState classificatorValueState = entry.getValue();
                if (classificatorValueState.isChanged()) {
                    final ClassificatorValue previousValue = classificatorValueState.getPreviousValue();
                    if (previousValue != null) {
                        removeClassificatorValue(classificatorExportVO, previousValue);
                    }
                    final ClassificatorValue newValue = classificatorValueState.getNewValue();
                    if (newValue != null) {
                        addClassificatorValue(classificatorExportVO, newValue);
                    } else {
                        log.debug("removed classificatorValue '" + previousValue.getValueName() //
                                + "' from classificator '" + classificatorExportVO.getName() + "'");
                    }
                }
            }

            List<QName> propsQNames = classificatorExportVO.getChangedProperties();
            StringBuilder sb = new StringBuilder();
            if (propsQNames.contains(ClassificatorModel.Props.DESCRIPTION)) {
                String newPropertyValue = classificatorExportVO.getDescription();
                nodeService.setProperty(classificatorExportVO.getNodeRef(), ClassificatorModel.Props.DESCRIPTION, newPropertyValue);
                sb.append("new description: " + newPropertyValue);
            }
            if (propsQNames.contains(ClassificatorModel.Props.DELETE_ENABLED)) {
                boolean newPropertyValue = classificatorExportVO.isDeleteEnabled();
                nodeService.setProperty(classificatorExportVO.getNodeRef(), ClassificatorModel.Props.DELETE_ENABLED, newPropertyValue);
                sb.append("new deleteEnabled: " + newPropertyValue);
            }
            if (log.isDebugEnabled()) {
                log.debug("Node (" + classificatorExportVO.getNodeRef() + ") changed: " + sb);
            }
        }
    }

    @Override
    public Node getNewUnsavedClassificator() {
        return generalService.createNewUnSaved(ClassificatorModel.Types.CLASSIFICATOR, new HashMap<QName, Serializable>());
    }

    @Override
    public Classificator getClassificatorByName(String name) {
        return getClassificator(name);
    }

    @Override
    public List<ClassificatorValue> getAllClassificatorValues(String classificator) {
        return getClassificator(classificator).getValues();
    }

    private Classificator getClassificator(String name) {
        return getClassificator(name, null);
    }

    private Classificator getClassificator(String name, NodeRef nodeRef) {
        Classificator classif = classificatorsCache.get(name);
        if (classif == null) {
            if (nodeRef != null) {
                classif = createClassificator(nodeRef);
            } else {
                List<ChildAssociationRef> children = nodeService.getChildAssocs(getClassificatorRoot(), ClassificatorModel.Associations.CLASSIFICATOR, getAssocName(name));
                if (children != null && children.size() == 1) {
                    nodeRef = children.get(0).getChildRef();
                    classif = createClassificator(nodeRef);
                } else {
                    throw new RuntimeException("Unknown classificator: " + name);
                }
            }
        }
        return classif;
    }

    public Classificator createClassificator(NodeRef nodeRef) {
        Classificator classif = new Classificator();
        classif.setNodeRef(nodeRef);
        classificatorBeanPropertyMapper.toObject(nodeService.getProperties(nodeRef), classif);
        classif.addClassificatorValues(loadAllClassificatorValuesFromDB(nodeRef));
        addToCache(classif);
        return classif;
    }

    private QName getAssocName(String classificatorName) {
        return QName.createQName(ClassificatorModel.URI, ISO9075.encode(classificatorName));
    }

    @Override
    public List<ClassificatorValue> loadAllClassificatorValuesFromDB(final NodeRef classificatorRef) {
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(classificatorRef, CLASSIFICATOR_VALUE_TYPE_QNAME);
        List<ClassificatorValue> classificatorValues = new ArrayList<>(childRefs.size());
        for (ChildAssociationRef childRef : childRefs) {
            ClassificatorValue clv = new ClassificatorValue();
            clv.setNodeRef(childRef.getChildRef());
            classificatorValueBeanPropertyMapper.toObject(nodeService.getProperties(childRef.getChildRef()), clv);
            classificatorValues.add(clv);
        }
        if (log.isTraceEnabled()) {
            log.trace("Classificator values found: " + classificatorValues);
        }
        return classificatorValues;
    }

    @Override
    public String getClassificatorValuesValueData(String classificatorName, String classificatorValueName) {
        if (StringUtils.isBlank(classificatorValueName) || StringUtils.isBlank(classificatorName)) {
            return null;
        }
        Classificator c = getClassificator(classificatorName);
        List<ClassificatorValue> values = c.getValues();
        for (ClassificatorValue classificatorValue : values) {
            if (classificatorValueName.equals(classificatorValue.getValueName())) {
                return classificatorValue.getValueData();
            }
        }
        return null;
    }

    @Override
    public List<ClassificatorValue> getActiveClassificatorValues(Classificator classificator) {
        List<ClassificatorValue> allClassificatorValues = classificator.getValues();
        List<ClassificatorValue> activeClassificatorValues = new ArrayList<>(allClassificatorValues.size());
        for (ClassificatorValue classificatorValue : allClassificatorValues) {
            if (classificatorValue.isActive()) {
                activeClassificatorValues.add(classificatorValue);
            }
        }
        return activeClassificatorValues;
    }

    @Override
    public final void addToClassificatorsCache(Classificator cl) {
        addToCache(cl);
    }

    private void addToCache(Classificator cl) {
        classificatorsCache.remove(cl.getName());
        classificatorsCache.put(cl.getName(), cl);
    }

    @Override
    public void removeClassificatorValue(Classificator classificator, ClassificatorValue classificatorValue) {
        NodeRef classifRef = classificator.getNodeRef();
        NodeRef valueNodeRef = classificatorValue.getNodeRef();
        nodeService.removeChild(classifRef, valueNodeRef);
        removeClassificatorValueFromCache(classificator, valueNodeRef);
        if (log.isDebugEnabled()) {
            log.debug("Node (" + valueNodeRef + ") removed: " + classificatorValue);
        }
    }

    @Override
    public void removeClassificatorValueByNodeRef(Classificator classificator, String ref) {
        NodeRef nodeRef = new NodeRef(ref);
        removeClassificatorValueByNodeRef(classificator, nodeRef);
    }

    @Override
    public void removeClassificatorValueByNodeRef(Classificator classificator, NodeRef nodeRef) {
        nodeService.removeChild(classificator.getNodeRef(), nodeRef);
        removeClassificatorValueFromCache(classificator, nodeRef);
        if (log.isDebugEnabled()) {
            log.debug("Node (" + nodeRef + ") removed.");
        }
    }

    private void removeClassificatorValueFromCache(Classificator classificator, NodeRef nodeRef) {
        classificator.removeClassificatorValue(nodeRef);
        addToCache(classificator);
    }

    @Override
    public boolean isClassificatorUsed(String classificatorName) {
        // TODO DLSeadist maybe need to cache the result - field using classificator will alwais remain using that classificator even if it is changed
        // (new field is created under new DocumentTypeVersion)
        boolean used = documentSearchService.isMatch(
                joinQueryPartsAnd(
                        joinQueryPartsOr(
                                generateTypeQuery(DocumentAdminModel.Types.FIELD)
                                , generateTypeQuery(DocumentAdminModel.Types.FIELD_DEFINITION)
                        )
                        , generateStringExactQuery(classificatorName, DocumentAdminModel.Props.CLASSIFICATOR))
                );
        return used;
    }

    @Override
    public void deleteClassificator(Classificator classificator) {
        String name = classificator.getName();
        Assert.isTrue(!isClassificatorUsed(name), "Can't delete - classificator is used: " + classificator);
        nodeService.deleteNode(classificator.getNodeRef());
        classificatorsCache.remove(name);
    }

    @Override
    public void addClassificatorValue(Classificator classificator, ClassificatorValue classificatorValue) {
        NodeRef classificatorRef = classificator.getNodeRef();
        NodeRef valueRef = nodeService.createNode(classificatorRef, ClassificatorModel.Associations.CLASSIFICATOR_VALUE,
                ClassificatorModel.Associations.CLASSIFICATOR_VALUE,
                ClassificatorModel.Types.CLASSIFICATOR_VALUE,
                classificatorValueBeanPropertyMapper.toProperties(classificatorValue)).getChildRef();
        classificatorValue.setNodeRef(valueRef);
        classificator.addClassificatorValue(classificatorValue);
        addToCache(classificator);
        if (log.isDebugEnabled()) {
            log.debug("Node (" + valueRef + ") added: " + classificatorValue);
        }
    }

    @Override
    public void addNewClassificators(List<ClassificatorExportVO> classificatorsToAdd) {
        for (ClassificatorExportVO newClassificator : classificatorsToAdd) {
            NodeRef classificatorRef = nodeService.createNode(getClassificatorRoot(),
                    ClassificatorModel.Associations.CLASSIFICATOR,
                    getAssocName(newClassificator.getName()),
                    ClassificatorModel.Types.CLASSIFICATOR,
                    classificatorBeanPropertyMapper.toProperties(newClassificator)).getChildRef();

            List<ClassificatorValue> classificatorValues = newClassificator.getClassificatorValues();
            newClassificator.setNodeRef(classificatorRef);
            addToCache(newClassificator.toClassificator());
            for (ClassificatorValue classificatorValue : classificatorValues) {
                addClassificatorValue(newClassificator, classificatorValue);
            }
        }
    }

    @Override
    public void updateClassificatorValues(Classificator classificator, Node classifNode, Map<String, ClassificatorValue> originalValues
            , List<ClassificatorValue> classificatorValues, List<ClassificatorValue> addedClassificatorValues) {
        Map<String, Object> properties = classifNode.getProperties();
        classificatorBeanPropertyMapper.toObject(RepoUtil.toQNameProperties(properties), classificator);
        NodeRef classificatorRef = classificator.getNodeRef();
        reOrderClassificatorValues(classificator, classificatorValues);
        nodeService.setProperties(classificatorRef, classificatorBeanPropertyMapper.toProperties(classificator));
        for (ClassificatorValue mod : classificatorValues) {
            ClassificatorValue orig = originalValues.get(mod.getNodeRef().toString());
            if (orig == null) {
                continue;
            }

            if (!orig.equals(mod)) {
                if (log.isDebugEnabled()) {
                    log.debug("Updating the classificator value with nodeRef = " + mod.getNodeRef());
                }
                removeClassificatorValue(classificator, orig);
                addClassificatorValue(classificator, mod);
            }
        }
        // save the added new value
        if (addedClassificatorValues != null && !addedClassificatorValues.isEmpty()) {
            for (ClassificatorValue add : addedClassificatorValues) {
                addClassificatorValue(classificator, add);
                if (log.isDebugEnabled()) {
                    log.debug("New classificator value (" + add.getValueName() + ") saved.");
                }
            }
        }
    }

    @Override
    public boolean hasClassificatorValueName(String classificatorName, String classificatorValueName) {
        if (StringUtils.isBlank(classificatorValueName) || StringUtils.isBlank(classificatorName)) {
            return false;
        }
        List<ClassificatorValue> classificatorValues = getClassificator(classificatorName).getValues();
        for (ClassificatorValue classificatorValue : classificatorValues) {
            if (classificatorValueName.equals(classificatorValue.getValueName())) {
                return true;
            }
        }
        return false;
    }

    public static Boolean reOrderClassificatorValues(Classificator classificator, List<ClassificatorValue> classificatorValues) {
        Boolean alfabeticOrder = classificator.getAlfabeticOrder();
        if (alfabeticOrder == null) {
            alfabeticOrder = false;
            classificator.setAlfabeticOrder(false);
        }
        if (alfabeticOrder) {
            Collections.sort(classificatorValues, CLASSIFICATOR_VALUES_ALFABETIC_ORDER_COMPARATOR);
        } else {
            Collections.sort(classificatorValues);
        }
        for (int i = 0; i < classificatorValues.size(); i++) {
            ClassificatorValue classificatorValue = classificatorValues.get(i);
            classificatorValue.setOrder(i + 1);
        }
        return alfabeticOrder;
    }

    // START: getters / setters
    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    public void setClassificatorsCache(SimpleCache<String, Classificator> classificatorsCache) {
        this.classificatorsCache = classificatorsCache;
    }

    public void setBulkLoadNodeService(BulkLoadNodeService bulkLoadNodeService) {
        this.bulkLoadNodeService = bulkLoadNodeService;
    }

    private NodeRef getClassificatorRoot() {
        if (classificatorRoot == null) {
            classificatorRoot = generalService.getNodeRef(ClassificatorModel.Repo.CLASSIFICATORS_SPACE);
        }
        return classificatorRoot;
    }

    // END: getters / setters

    @Override
    public String saveClassificatorNode(Node classificatorNode) {
        Map<QName, Serializable> propsMap = RepoUtil.toQNameProperties(classificatorNode.getProperties());
        String newName = (String) propsMap.get(ClassificatorModel.Props.CLASSIFICATOR_NAME);
        validateNewClassifName(newName);
        NodeRef newClassifRef = nodeService.createNode(getClassificatorRoot(),
                ClassificatorModel.Associations.CLASSIFICATOR,
                getAssocName(newName),
                ClassificatorModel.Types.CLASSIFICATOR,
                propsMap).getChildRef();
        Classificator cl = new Classificator();
        cl.setNodeRef(newClassifRef);
        classificatorBeanPropertyMapper.toObject(nodeService.getProperties(newClassifRef), cl);
        addToCache(cl);
        return cl.getName();
    }

    private void validateNewClassifName(String newName) {
        if (StringUtils.isBlank(newName)) {
            throw new UnableToPerformException("classificators_classificator_name_isBlank");
        }
        if (!newName.matches("[A-Za-z]*")) {
            throw new UnableToPerformException("classificators_classificator_name_wrong");
        }
        if (classificatorsCache.contains(newName)) {
            throw new UnableToPerformException("classificators_classificator_name_exists");
        }
    }

    @Override
    public List<Classificator> search(String searchCriteria) {
        List<Classificator> filteredValues = new ArrayList<>();
        Pattern pattern = Pattern.compile(searchCriteria, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher("");
        for (Classificator classif : getAllClassificators()) {
            String name = classif.getName();
            String description = classif.getDescription();
            if (matcher.reset(name).find() || description != null && matcher.reset(description).find()) {
                filteredValues.add(classif);
            }
        }
        return filteredValues;
    }

    @Override
    public List<ClassificatorValue> searchValues(String searchCriteria, String classifName) {
        List<ClassificatorValue> filteredValues = new ArrayList<>();
        List<ClassificatorValue> allValues = getClassificator(classifName).getValues();
        Pattern pattern = Pattern.compile(searchCriteria, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher("");
        for (ClassificatorValue value : allValues) {
            String valueName = value.getValueName();
            if (valueName != null && matcher.reset(valueName).find()) {
                filteredValues.add(value);
            }
        }
        return filteredValues;
    }
}

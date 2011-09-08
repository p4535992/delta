package ee.webmedia.alfresco.classificator.service;

import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyExactQuery;
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
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
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
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
    private DocumentSearchService documentSearchService;
    static {
        classificatorBeanPropertyMapper = BeanPropertyMapper.newInstance(Classificator.class);
        classificatorValueBeanPropertyMapper = BeanPropertyMapper.newInstance(ClassificatorValue.class);

        RuleBasedCollator et_EECollator = (RuleBasedCollator) AppConstants.DEFAULT_COLLATOR;
        @SuppressWarnings("unchecked")
        Comparator<ClassificatorValue> tmp = new TransformingComparator(new ComparableTransformer<ClassificatorValue>() {
            @Override
            public Comparable<?> tr(ClassificatorValue c) {
                return c.getValueName() == null ? "" : c.getValueName();
            }
        }, et_EECollator);
        CLASSIFICATOR_VALUES_ALFABETIC_ORDER_COMPARATOR = tmp;
    }

    private GeneralService generalService;
    private NodeService nodeService;
    private String classificatorsPath;

    @Override
    public List<Classificator> getAllClassificators() {
        NodeRef root = generalService.getNodeRef(classificatorsPath);
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(root);
        List<Classificator> classificators = new ArrayList<Classificator>(childRefs.size());
        for (ChildAssociationRef childRef : childRefs) {
            Classificator cl = new Classificator();
            cl.setNodeRef(childRef.getChildRef());
            classificatorBeanPropertyMapper.toObject(nodeService.getProperties(childRef.getChildRef()), cl);
            classificators.add(cl);
        }
        if (log.isDebugEnabled()) {
            log.debug("Classificators found: " + classificators);
        }
        return classificators;
    }

    private List<ClassificatorExportVO> getAllClassificatorsToExport() {
        final List<Classificator> allClassificators = getAllClassificators();
        final List<ClassificatorExportVO> exportClassificators = new ArrayList<ClassificatorExportVO>(allClassificators.size());
        for (Classificator classificator : allClassificators) {
            if (classificator.isAddRemoveValues()) {
                final List<ClassificatorValue> allClassificatorValues = getAllClassificatorValues(classificator);
                final ClassificatorExportVO clExport = new ClassificatorExportVO(classificator, allClassificatorValues);
                exportClassificators.add(clExport);
            }
        }
        return exportClassificators;
    }

    @Override
    public void exportClassificators(Writer writer) {
        final List<ClassificatorExportVO> allClassificatorsToExport = getAllClassificatorsToExport();
        // final List<Classificator> allClassificatorsToExport = getAllClassificatorsToExport();
        XStream xstream = new XStream();
        xstream.processAnnotations(ClassificatorExportVO.class);
        // xstream.processAnnotations(Classificator.class);
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
    public List<Classificator> getClassificatorsByNodeRefs(List<NodeRef> classifRefs) {
        List<Classificator> classificators = new ArrayList<Classificator>(classifRefs.size());
        for (NodeRef classifRef : classifRefs) {
            classificators.add(getClassificatorByNodeRef(classifRef));
        }
        return classificators;
    }

    @Override
    public Classificator getClassificatorByNodeRef(NodeRef nodeRef) {
        Classificator cl = new Classificator();
        cl.setNodeRef(nodeRef);
        classificatorBeanPropertyMapper.toObject(nodeService.getProperties(nodeRef), cl);
        return cl;
    }

    @Override
    public Classificator getClassificatorByName(String name) {
        String xpath = getClassificatorPathByName(name);
        NodeRef classifNodeRef1 = generalService.getNodeRef(xpath);
        NodeRef classifNodeRef = classifNodeRef1;
        Assert.notNull(classifNodeRef, "Unknown classificator '" + name + "'");
        return getClassificatorByNodeRef(classifNodeRef);
    }

    private String getClassificatorPathByName(String classificatorName) {
        return classificatorsPath + "/" + getAssocName(classificatorName);
    }

    private QName getAssocName(String classificatorName) {
        return QName.createQName(ClassificatorModel.URI, ISO9075.encode(classificatorName));
    }

    @Override
    public List<ClassificatorValue> getAllClassificatorValues(Classificator classificator) {
        return getAllClassificatorValues(classificator.getNodeRef());
    }

    private List<ClassificatorValue> getClassificatorValuesByNodeRefs(List<NodeRef> clValueNodeRefs) {
        List<ClassificatorValue> classificatorValues = new ArrayList<ClassificatorValue>(clValueNodeRefs.size());
        for (NodeRef valueRef : clValueNodeRefs) {
            ClassificatorValue clv = new ClassificatorValue();
            clv.setNodeRef(valueRef);
            classificatorValueBeanPropertyMapper.toObject(nodeService.getProperties(valueRef), clv);
            classificatorValues.add(clv);
        }
        return classificatorValues;
    }

    private List<ClassificatorValue> getAllClassificatorValues(final NodeRef classificatorRef) {
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(classificatorRef, ClassificatorModel.Associations.CLASSIFICATOR_VALUE,
                    RegexQNamePattern.MATCH_ALL);
        List<ClassificatorValue> classificatorValues = new ArrayList<ClassificatorValue>(childRefs.size());
        for (ChildAssociationRef childRef : childRefs) {
            ClassificatorValue clv = new ClassificatorValue();
            clv.setNodeRef(childRef.getChildRef());
            classificatorValueBeanPropertyMapper.toObject(nodeService.getProperties(childRef.getChildRef()), clv);
            classificatorValues.add(clv);
        }
        if (log.isDebugEnabled()) {
            log.debug("Classificator values found: " + classificatorValues);
        }
        return classificatorValues;
    }

    @Override
    public List<ClassificatorValue> getActiveClassificatorValues(Classificator classificator) {
        List<ClassificatorValue> allClassificatorValues = getAllClassificatorValues(classificator);
        List<ClassificatorValue> activeClassificatorValues = new ArrayList<ClassificatorValue>(allClassificatorValues.size());
        for (ClassificatorValue classificatorValue : allClassificatorValues) {
            if (classificatorValue.isActive()) {
                activeClassificatorValues.add(classificatorValue);
            }
        }
        return activeClassificatorValues;
    }

    @Override
    public void removeClassificatorValue(Classificator classificator, ClassificatorValue classificatorValue) {
        nodeService.removeChild(classificator.getNodeRef(), classificatorValue.getNodeRef());
        if (log.isDebugEnabled()) {
            log.debug("Node (" + classificatorValue.getNodeRef() + ") removed: " + classificatorValue);
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
        if (log.isDebugEnabled()) {
            log.debug("Node (" + nodeRef + ") removed.");
        }
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
                        , generatePropertyExactQuery(DocumentAdminModel.Props.CLASSIFICATOR, classificatorName, false))
                );
        return used;
    }

    @Override
    public void deleteClassificator(Classificator classificator) {
        Assert.isTrue(!isClassificatorUsed(classificator.getName()), "Can't delete - classificator is used: " + classificator);
        nodeService.deleteNode(classificator.getNodeRef());
    }

    @Override
    public void addClassificatorValue(Classificator classificator, ClassificatorValue classificatorValue) {
        ChildAssociationRef assoc = nodeService.createNode(classificator.getNodeRef(), ClassificatorModel.Associations.CLASSIFICATOR_VALUE,
                ClassificatorModel.Associations.CLASSIFICATOR_VALUE,
                ClassificatorModel.Types.CLASSIFICATOR_VALUE,
                classificatorValueBeanPropertyMapper.toProperties(classificatorValue));
        if (log.isDebugEnabled()) {
            log.debug("Node (" + assoc.getChildRef() + ") added: " + classificatorValue);
        }
    }

    @Override
    public void addNewClassificators(List<ClassificatorExportVO> classificatorsToAdd) {
        for (ClassificatorExportVO newClassificator : classificatorsToAdd) {
            NodeRef classificatorRef = nodeService.createNode(getClassificatorRoot(),
                    ClassificatorModel.Associations.CLASSIFICATOR,
                    ClassificatorModel.Associations.CLASSIFICATOR,
                    ClassificatorModel.Types.CLASSIFICATOR,
                    classificatorBeanPropertyMapper.toProperties(newClassificator)).getChildRef();

            List<ClassificatorValue> classificatorValues = newClassificator.getClassificatorValues();
            newClassificator.setNodeRef(classificatorRef);
            for (ClassificatorValue classificatorValue : classificatorValues) {
                addClassificatorValue(newClassificator, classificatorValue);
            }
        }
    }

    @Override
    public void updateClassificatorValues(Classificator classificator, Node classifNode, Map<String, ClassificatorValue> originalValues
            , List<ClassificatorValue> classificatorValues, List<ClassificatorValue> addedClassificators) {
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
        if (addedClassificators != null && !addedClassificators.isEmpty()) {
            for (ClassificatorValue add : addedClassificators) {
                addClassificatorValue(classificator, add);
                if (log.isDebugEnabled()) {
                    log.debug("New classificator value (" + add.getValueName() + ") saved.");
                }
            }
        }
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

    public void setClassificatorsPath(String classificatorsPath) {
        this.classificatorsPath = classificatorsPath;
    }

    // END: getters / setters
    private NodeRef getClassificatorRoot() {
        return generalService.getNodeRef(ClassificatorModel.Repo.CLASSIFICATORS_SPACE);
    }

    @Override
    public void saveClassificatorNode(Node classificatorNode) {
        Map<QName, Serializable> propsMap = RepoUtil.toQNameProperties(classificatorNode.getProperties());
        String newName = (String) propsMap.get(ClassificatorModel.Props.CLASSIFICATOR_NAME);
        validateNewClassifName(newName);
        nodeService.createNode(getClassificatorRoot(),
                ClassificatorModel.Associations.CLASSIFICATOR,
                getAssocName(newName),
                ClassificatorModel.Types.CLASSIFICATOR,
                    propsMap).getChildRef();
    }

    private void validateNewClassifName(String newName) {
        if (StringUtils.isBlank(newName)) {
            throw new UnableToPerformException("classificators_classificator_name_isBlank");
        }
        if (!newName.matches("[A-Za-z]*")) {
            throw new UnableToPerformException("classificators_classificator_name_wrong");
        }
        List<Classificator> classificators = getAllClassificators();
        for (Classificator classificator : classificators) {
            if (StringUtils.equalsIgnoreCase(newName, classificator.getName())) {
                throw new UnableToPerformException("classificators_classificator_name_exists");
            }
        }
    }

    @Override
    public List<Classificator> search(String searchCriteria) {
        return getClassificatorsByNodeRefs(BeanHelper.getDocumentSearchService().simpleSearch(searchCriteria, null, ClassificatorModel.Types.CLASSIFICATOR,
                ClassificatorModel.Props.CLASSIFICATOR_NAME, ClassificatorModel.Props.DESCRIPTION));
    }

    @Override
    public List<ClassificatorValue> searchValues(String searchCriteria, NodeRef classifNodeRef) {
        return getClassificatorValuesByNodeRefs(BeanHelper.getDocumentSearchService().simpleSearch(searchCriteria, classifNodeRef, ClassificatorModel.Types.CLASSIFICATOR_VALUE,
                ClassificatorModel.Props.CL_VALUE_NAME));
    }
}

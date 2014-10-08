package ee.webmedia.alfresco.classificator.service;

import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.model.Classificator;
import ee.webmedia.alfresco.classificator.model.ClassificatorExportVO;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;

/**
 * Service for searching and modifying classificators from the model.
 */
public interface ClassificatorService {

    public static final String BEAN_NAME = "ClassificatorService";

    /**
     * Returns all the defined classificators.
     * 
     * @return list of found Classificator objects
     */
    List<Classificator> getAllClassificators();

    /**
     * Returns the classificator referenced by nodeRef.
     * 
     * @param nodeRef
     * @return
     */
    Classificator getClassificatorByNodeRef(NodeRef nodeRef);

    /**
     * Returns the classificator referenced by its name.
     * 
     * @param name
     * @return
     */
    Classificator getClassificatorByName(String name);

    /**
     * @param classificator
     * @return list of all values for the given classificator
     */
    List<ClassificatorValue> getAllClassificatorValues(Classificator classificator);

    List<ClassificatorValue> getAllClassificatorValues(String classificator);

    /**
     * Removes the selected classificator value from the classificator.
     * 
     * @param classificator
     * @param classificatorValue
     */
    void removeClassificatorValue(Classificator classificator, ClassificatorValue classificatorValue);

    /**
     * Removes the selected classificator value from the classificator by reference.
     * 
     * @param classificator
     * @param nodeRef
     */
    void removeClassificatorValueByNodeRef(Classificator classificator, String nodeRef);

    /**
     * Removes the selected classificator value from the classificator by reference.
     * 
     * @param classificator
     * @param nodeRef
     */
    void removeClassificatorValueByNodeRef(Classificator classificator, NodeRef nodeRef);

    /**
     * Adds a new classificator value to the classificator.
     */
    void addClassificatorValue(Classificator classificator, ClassificatorValue classificatorValue);

    /**
     * @see {@link ClassificatorService#getAllClassificatorValues(Classificator)}
     * @param classificator
     * @return list of values for the given classificator that are active
     */
    List<ClassificatorValue> getActiveClassificatorValues(Classificator classificator);

    void exportClassificators(Writer writer);

    void importClassificators(Collection<ClassificatorExportVO> changedClassificators);

    void updateClassificatorValues(Classificator classificator, Node classificatorNode, Map<String, ClassificatorValue> originalValues
            , List<ClassificatorValue> classificatorValues, List<ClassificatorValue> addedClassificators);

    boolean isClassificatorUsed(String classificatorName);

    void deleteClassificator(Classificator selectedClassificator);

    Node getNewUnsavedClassificator();

    NodeRef saveClassificatorNode(Node classificatorNode);

    void addNewClassificators(List<ClassificatorExportVO> classificatorsToAdd);

    List<Classificator> search(String searchCriteria);

    List<ClassificatorValue> searchValues(String searchCriteria, NodeRef classifNodeRef);

    List<Classificator> getClassificatorsByNodeRefs(List<NodeRef> classifRefs);

    String getClassificatorValuesValueData(String classificatorName, String classificatorValueName);

    boolean hasClassificatorValueName(String classificatorName, String classificatorValueName);
}

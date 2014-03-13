package ee.webmedia.alfresco.classificator.model;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * Subclass of Classificator for exporting and importing classificator values
 */
@XStreamAlias("classificator")
public class ClassificatorExportVO extends Classificator {

    private static final long serialVersionUID = 1L;
    private static ClassificatorValueComparator classificatorValueComparator;

    @XStreamAlias("classificatorValues")
    private final List<ClassificatorValue> classificatorValues;
    @XStreamOmitField
    private List<ClassificatorValue> previousClassificatorValues;

    @XStreamOmitField
    private Boolean valueChanged;
    @XStreamOmitField
    private ImportStatus valueChangeStatus;

    public ClassificatorExportVO(String classificatorName, List<ClassificatorValue> allClassificatorValues) {
        setAddRemoveValues(true);
        setName(classificatorName);
        classificatorValues = allClassificatorValues;
    }

    public ClassificatorExportVO(Classificator classificator, List<ClassificatorValue> allClassificatorValues) {
        this(classificator.getName(), allClassificatorValues);
        Assert.isTrue(classificator.isAddRemoveValues(), "Classificators, that have no changable values should not be exported: " + classificator);
    }

    public List<ClassificatorValue> getClassificatorValues() {
        return classificatorValues;
    }

    public List<ClassificatorValue> getPreviousClassificatorValues() {
        return previousClassificatorValues;
    }

    public void setPreviousClassificatorValues(List<ClassificatorValue> previousClassificatorValues) {
        this.previousClassificatorValues = previousClassificatorValues;
        valueChanged = null;
    }

    public ImportStatus getStatus() {
        if (valueChangeStatus == null) {
            isValuesChanged();
        }
        return valueChangeStatus;
    }

    public Map<String /* classificatorValueName */, ClassificatorValueState> getValuesByName() {
        Map<String /* classificatorValueName */, ClassificatorValueState> res = new HashMap<String, ClassificatorValueState>(previousClassificatorValues.size());
        for (ClassificatorValue value : previousClassificatorValues) {
            res.put(value.getValueName(), new ClassificatorValueState(value));
        }
        for (ClassificatorValue value : classificatorValues) {
            ClassificatorValueState classifValue = res.get(value.getValueName());
            if (classifValue == null) {
                classifValue = new ClassificatorValueState(null);
                res.put(value.getValueName(), classifValue);
                value.setReadOnly(false);
            } else {
                value.setReadOnly(classifValue.getPreviousValue().isReadOnly());
            }
            classifValue.setNewValue(value);
        }
        return res;
    }

    public boolean isValuesChanged() {
        if (valueChanged == null) {
            Assert.notNull(classificatorValues, "classificatorValues must not be null when importing classificator");
            if (previousClassificatorValues == null) {
                valueChanged = true;
                valueChangeStatus = ImportStatus.ADD;
                return valueChanged;
            }
            if (previousClassificatorValues.size() != classificatorValues.size()) {
                valueChanged = true;
                valueChangeStatus = previousClassificatorValues.size() > classificatorValues.size()
                        ? ImportStatus.REMOVE_VALUES
                        : ImportStatus.ADD_VALUES;
                return valueChanged;
            }
            final HashMap<String, ClassificatorValue> currentValues = new HashMap<String, ClassificatorValue>(classificatorValues.size());
            for (ClassificatorValue value : classificatorValues) {
                currentValues.put(value.getValueName(), value);
            }
            final HashMap<String, ClassificatorValue> previousValues = new HashMap<String, ClassificatorValue>(previousClassificatorValues.size());
            for (ClassificatorValue value : previousClassificatorValues) {
                previousValues.put(value.getValueName(), value);
            }
            final HashSet<String> currentValueNames = new HashSet<String>(currentValues.keySet());
            final HashSet<String> allValueNames = new HashSet<String>(previousValues.keySet());
            allValueNames.addAll(currentValueNames);
            if (allValueNames.size() != classificatorValues.size()) {
                valueChanged = true;
                valueChangeStatus = ImportStatus.ADD_REMOVE_VALUES;
                return valueChanged;
            }

            // no new valueNames, checking if value contents are equal
            for (String clValueName : allValueNames) {
                final int compare = getClassificatorValueComparator().compare(currentValues.get(clValueName), previousValues.get(clValueName));
                if (compare != 0) {
                    valueChanged = true;
                    valueChangeStatus = ImportStatus.CHANGE_VALUE;
                    return valueChanged;
                }
            }
            valueChangeStatus = ImportStatus.NOT_CHANGED;
            valueChanged = false;
        }
        return valueChanged;
    }

    private ClassificatorValueComparator getClassificatorValueComparator() {
        if (classificatorValueComparator == null) {
            classificatorValueComparator = new ClassificatorValueComparator();
        }
        return classificatorValueComparator;
    }

    public enum ImportStatus {
        ADD("classificators_import_status_addClassificator")
        , REMOVE_VALUES("classificators_import_status_removeValues")
        , ADD_VALUES("classificators_import_status_addValues")
        , ADD_REMOVE_VALUES("classificators_import_status_addRemoveValues")
        , CHANGE_VALUE("classificators_import_status_changeValue")
        , NOT_CHANGED("classificators_import_status_notChanged");
        private String translation;

        private ImportStatus(String translation) {
            this.translation = translation;
        }

        @Override
        public String toString() {
            return translation;
        }
    }

    class ClassificatorValueComparator implements Comparator<ClassificatorValue> {
        @Override
        public int compare(ClassificatorValue o1, ClassificatorValue o) {
            // comparing by name and readOnly is left out as readOnly must not be changed and should not compare values with different names
            if (o1 == o) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            }
            if (o == null || o1.getClass() != o.getClass()) {
                return 1;
            }
            if (o1.getOrder() < o.getOrder()) {
                return -1;
            } else if (o1.getOrder() > o.getOrder()) {
                return 1;
            } else if (o1.isActive() != o.isActive()) {
                return 1;
            } else if (o1.isByDefault() != o.isByDefault()) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    public class ClassificatorValueState {
        private final ClassificatorValue previousValue;
        private ClassificatorValue newValue;
        private Boolean changed;

        public ClassificatorValueState(ClassificatorValue previousValue) {
            this.previousValue = previousValue;
        }

        public void setNewValue(ClassificatorValue newValue) {
            this.newValue = newValue;
        }

        public ClassificatorValue getPreviousValue() {
            return previousValue;
        }

        public ClassificatorValue getNewValue() {
            return newValue;
        }

        public boolean isChanged() {
            if (changed == null) {
                changed = getClassificatorValueComparator().compare(newValue, previousValue) != 0;
            }
            return changed;
        }
    }
}

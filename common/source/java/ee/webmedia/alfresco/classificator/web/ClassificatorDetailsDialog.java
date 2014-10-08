package ee.webmedia.alfresco.classificator.web;

import static ee.webmedia.alfresco.classificator.web.ClassificatorUtil.getClassificatorReorderHelper;
import static ee.webmedia.alfresco.common.web.BeanHelper.getClassificatorService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.GUID;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.model.Classificator;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorServiceImpl;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.web.ListReorderHelper;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;

public class ClassificatorDetailsDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ClassificatorDetailsDialog.class);
    private static final String ADD_VALUE_ACTION_GROUP = "browse_add_classificator_values";
    private static final String ADD_REMOVE_VALUE_ACTION_GROUP = "browse_add_delete_classificator_values";

    public static final String BEAN_NAME = "ClassificatorDetailsDialog";

    private transient UIRichList richList;

    private Map<String, ClassificatorValue> originalValues;
    private Map<String, ClassificatorValue> originalValuesToSort;
    private List<ClassificatorValue> classificatorValues;
    private List<ClassificatorValue> addedClassificatorValues;
    private Classificator selectedClassificator;
    private Node classificatorNode;
    private String searchCriteria = "";
<<<<<<< HEAD
    private boolean savedClassificator;
=======
>>>>>>> develop-5.1

    @Override
    public void init(Map<String, String> parameters) {
        super.init(parameters);
    }

    /**
     * Used in JSP pages.
     */
    public List<ClassificatorValue> getClassificatorValues() {
        return classificatorValues;
    }

    public Node getClassificatorNode() {
        if (selectedClassificator != null && classificatorNode == null) {
            classificatorNode = new Node(selectedClassificator.getNodeRef());
        } else if (selectedClassificator == null && classificatorNode == null) {
            classificatorNode = BeanHelper.getClassificatorService().getNewUnsavedClassificator();
        }
        return classificatorNode;
    }

    /**
     * Used in JSP pages.
     */
    public UIRichList getRichList() {
        return richList;
    }

    /**
     * Used in JSP pages.
     */
    public void setRichList(UIRichList richList) {
        this.richList = richList;
    }

    /**
     * Used in JSP pages.
     */
    public boolean isAddRemoveValuesAllowed() {
        if (selectedClassificator != null) {
            return selectedClassificator.isAddRemoveValues();
        }
        return false;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (RepoUtil.isUnsaved(classificatorNode)) {
            NodeRef classificatorRef = BeanHelper.getClassificatorService().saveClassificatorNode(classificatorNode);
            MessageUtil.addInfoMessage("save_success");
            reloadSavedData(classificatorRef);
            return null;
        }
        String validationMessage;
        final Set<String> messages = new HashSet<String>(3);
        int defaultCheckBox = 0;

        Set<String> distinctValues = new HashSet<String>();
        Set<String> duplicatedValues = new HashSet<String>();
        for (ClassificatorValue classificatorValue : classificatorValues) {
            if (classificatorValue.isByDefault()) {
                defaultCheckBox++;
            }
            if ((validationMessage = classificatorValue.validate()) != null) {
                messages.add(validationMessage);
            }

            // check for duplicated values
            String valueName = StringUtils.trim(classificatorValue.getValueName());
            if (distinctValues.contains(valueName)) {
                duplicatedValues.add(valueName);
            } else {
                distinctValues.add(valueName);
            }
            originalValuesToSort.put(classificatorValue.getNodeRef().toString(), classificatorValue);
        }

        boolean hasErrors = false;
        if (duplicatedValues.size() > 0) {
            MessageUtil.addErrorMessage(context, "classificator_duplicated_value", StringUtils.join(duplicatedValues, ", "));
            hasErrors = true;
        }

        if (defaultCheckBox > 1) {
            messages.add("classificator_value_validation_bydefault");
        }
        if (hasErrors || messages.size() > 0) {
            for (String message : messages) {
                MessageUtil.addErrorMessage(message);
            }
        } else {
            ListReorderHelper.reorder(originalValuesToSort.values(), getClassificatorReorderHelper());
            for (ClassificatorValue classificatorValue : classificatorValues) {
                String classificatorRef = classificatorValue.getNodeRef().toString();
                if (originalValuesToSort.containsKey(classificatorRef)) {
                    classificatorValue.setOrder(originalValuesToSort.get(classificatorRef).getOrder());
                }
            }
            BeanHelper.getClassificatorService().updateClassificatorValues(selectedClassificator, classificatorNode, originalValues,
                    new ArrayList<ClassificatorValue>(originalValuesToSort.values()), addedClassificatorValues);
            reloadSavedData(selectedClassificator.getNodeRef());
            MessageUtil.addInfoMessage("save_success");
        }
        return null;
    }

    private void reloadSavedData(NodeRef classificatorRef) {
        resetData();
        selectedClassificator = BeanHelper.getClassificatorService().getClassificatorByNodeRef(classificatorRef);
        classificatorNode = new Node(classificatorRef);
        loadClassificatorValues();
    }

    @Override
    public String cancel() {
        resetData();
        return super.cancel();
    }

    @Override
    public String getContainerTitle() {
        if (selectedClassificator != null) {
            return selectedClassificator.getName();
        }
        return MessageUtil.getMessage("classificators_create");
    }

    /** used by delete action to do actual deleting (after user has confirmed deleting in DeleteDialog) */
<<<<<<< HEAD
    public String deleteClassificator(@SuppressWarnings("unused") ActionEvent event) {
        getClassificatorService().deleteClassificator(selectedClassificator);
        return getCloseOutcome(2);
=======
    public void deleteClassificator(@SuppressWarnings("unused") ActionEvent event) {
        getClassificatorService().deleteClassificator(selectedClassificator);
>>>>>>> develop-5.1
    }

    /**
     * JSP event handler.
     * Called before displaying classificator value list.
     * 
     * @param event
     */
    public void select(ActionEvent event) {
        resetData();
        if (ActionUtil.hasParam(event, "nodeRef")) {
            NodeRef classificatorRef = ActionUtil.getParam(event, "nodeRef", NodeRef.class);
            selectedClassificator = BeanHelper.getClassificatorService().getClassificatorByNodeRef(classificatorRef);
            classificatorNode = new Node(classificatorRef);
            loadClassificatorValues();
        } else {
            classificatorNode = BeanHelper.getClassificatorService().getNewUnsavedClassificator();
            classificatorValues = new ArrayList<ClassificatorValue>();
        }
    }

    /**
     * JSP event handler.
     * Removes the selected value and updates the UI model.
     * 
     * @param event
     */
    public void removeValue(ActionEvent event) {
        String ref = ActionUtil.getParam(event, "nodeRef");

        ClassificatorValue classificatorValue = getClassificatorValueByNodeRef(ref);
        classificatorValues.remove(classificatorValue);
        if (originalValues.containsKey(ref)) {
            originalValues.remove(ref);
            originalValuesToSort.remove(ref);
            BeanHelper.getClassificatorService().removeClassificatorValueByNodeRef(selectedClassificator, ref);
        } else {
            addedClassificatorValues.remove(classificatorValue);
        }
        MessageUtil.addInfoMessage("classificator_value_remove_success");
        if (log.isDebugEnabled()) {
            log.debug("Classificator value with nodeRef = " + ref + " deleted.");
        }
        reOrderIfNeeded();
    }

    private void reOrderIfNeeded() {
        ClassificatorServiceImpl.classificatorBeanPropertyMapper.toObject(RepoUtil.toQNameProperties(classificatorNode.getProperties()), selectedClassificator);
        ClassificatorServiceImpl.reOrderClassificatorValues(selectedClassificator, classificatorValues);
    }

    /**
     * JSP event handler.
     * Adds new value to the model and temporary list before saving.
     * 
     * @param event
     */
    public void addNewValue(ActionEvent event) {
        ClassificatorValue addedClassificatorValue = new ClassificatorValue();
        addedClassificatorValue.setActive(true);
        addedClassificatorValue.setOrder(getMaxClassificatorValueOrder() + 1);
        // set the temporary random unique ID to be used in the UI form
        addedClassificatorValue.setNodeRef(new NodeRef(addedClassificatorValue.hashCode() + "", event.hashCode() + "", GUID.generate()));
        classificatorValues.add(addedClassificatorValue);
        addedClassificatorValues.add(addedClassificatorValue);
    }

    private void clearRichList() {
        if (getRichList() != null) {
            getRichList().setValue(null);
        }
    }

    protected void resetData() {
        originalValues = null;
        originalValuesToSort = null;
        classificatorValues = null;
        selectedClassificator = null;
        addedClassificatorValues = null;
        richList = null;
        classificatorNode = null;
    }

    private void loadClassificatorValues() {
        if (selectedClassificator != null) {
            classificatorValues = BeanHelper.getClassificatorService().getAllClassificatorValues(selectedClassificator);
            originalValues = new TreeMap<String, ClassificatorValue>();
            originalValuesToSort = new TreeMap<String, ClassificatorValue>();
            for (ClassificatorValue cv : classificatorValues) {
                originalValues.put(cv.getNodeRef().toString(), new ClassificatorValue(cv));
                originalValuesToSort.put(cv.getNodeRef().toString(), new ClassificatorValue(cv));
            }
            addedClassificatorValues = new ArrayList<ClassificatorValue>();
            getClassificatorReorderHelper().markBaseState(classificatorValues);
        }
    }

    public void search() {
        if (StringUtils.isNotBlank(getSearchCriteria())) {
            clearRichList();
            classificatorValues = BeanHelper.getClassificatorService().searchValues(getSearchCriteria(), selectedClassificator.getNodeRef());
            getClassificatorReorderHelper().markBaseState(classificatorValues);
        } else {
            MessageUtil.addInfoMessage("classificators_error_emptySearchField");
        }
    }

    public void showAll() {
        clearRichList();
        setSearchCriteria("");
        classificatorValues = BeanHelper.getClassificatorService().getAllClassificatorValues(selectedClassificator);
    }

    private int getMaxClassificatorValueOrder() {
        int maxOrder = 0;
        for (ClassificatorValue cv : classificatorValues) {
            maxOrder = Math.max(maxOrder, cv.getOrder());
        }
        return maxOrder;
    }

    private ClassificatorValue getClassificatorValueByNodeRef(String ref) {
        Assert.notNull(ref);
        NodeRef nodeRef = new NodeRef(ref);
        for (ClassificatorValue cv : classificatorValues) {
            if (nodeRef.equals(cv.getNodeRef())) {
                return cv;
            }
        }
        return null;
    }

    @Override
    public Object getActionsContext() {
        return selectedClassificator;
    }

    public boolean isClassificatorNodeSet() {
        return classificatorNode != null;
    }

    public String getSearchCriteria() {
        return searchCriteria;
    }

    public void setSearchCriteria(String searchCriteria) {
        this.searchCriteria = searchCriteria;
    }

    public void setClassificatorNode(Node classificatorNode) {
        this.classificatorNode = classificatorNode;
    }

    public boolean isSavedClassificator() {
        return RepoUtil.isSaved(classificatorNode);
    }

    public boolean isUnsavedClassificator() {
        return !isSavedClassificator();
    }
}

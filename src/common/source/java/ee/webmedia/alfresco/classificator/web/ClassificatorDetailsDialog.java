package ee.webmedia.alfresco.classificator.web;

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
import org.alfresco.web.app.Application;
import org.alfresco.web.app.context.IContextListener;
import org.alfresco.web.app.context.UIContextService;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.model.Classificator;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.utils.ActionUtil;

public class ClassificatorDetailsDialog extends BaseDialogBean implements IContextListener {

    private static final long serialVersionUID = 1L;
    
    private static final Log log = LogFactory.getLog(ClassificatorDetailsDialog.class);
    private static final String ADD_VALUE_ACTION_GROUP = "browse_classificator_values";
    
    private transient ClassificatorService classificatorService;
    private transient UIRichList richList;
    
    private List<ClassificatorValue> classificatorValues;
    private Map<String, ClassificatorValue> originalValues;
    private Classificator selectedClassificator;
    private List<ClassificatorValue> addedClassificators;

    public ClassificatorDetailsDialog() {
        UIContextService.getInstance(FacesContext.getCurrentInstance()).registerBean(this);
    }

    public void setClassificatorService(ClassificatorService classificatorService) {
        this.classificatorService = classificatorService;
    }

    protected ClassificatorService getClassificatorService() {
        if (classificatorService == null) {
            classificatorService = (ClassificatorService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(ClassificatorService.BEAN_NAME);
        }
        return classificatorService;
    }
    
    /**
     * Used in JSP pages.
     */
    public List<ClassificatorValue> getClassificatorValues() {
        return classificatorValues;
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
        String validationMessage;
        final Set<String> messages = new HashSet<String>(3);
        int defaultCheckBox = 0; 
        for (ClassificatorValue classificatorValue : classificatorValues) {
            if (classificatorValue.isByDefault()) {
                defaultCheckBox++;
            }
            if ((validationMessage = classificatorValue.validate()) != null) {
                messages.add(validationMessage);
            }
        }
        if (defaultCheckBox > 1) {
            messages.add("classificator_value_validation_bydefault");
        }
        if (messages.size() > 0) {
            for (String message : messages) {
                Utils.addErrorMessage(Application.getMessage(context, message));
            }
            outcome = null;
            super.isFinished = false;
        } else {
            updateClassificatorValues();
            resetData();
        }
        return outcome;
    }
    
    @Override
    public String cancel() {
        resetData();
        return super.cancel();
    }
    
    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }
    
    @Override
    public String getContainerTitle() {
        if (selectedClassificator != null) {
            return selectedClassificator.getName();
        }
        return super.getContainerTitle();
    }
    
    @Override
    public String getActionsConfigId() {
        if (selectedClassificator != null && selectedClassificator.isAddRemoveValues()) {
            return ADD_VALUE_ACTION_GROUP;
        }
        return null;
    }
    
    /**
     * JSP event handler.
     * Called before displaying classificator value list. 
     * @param event
     */
    public void select(ActionEvent event) {
        selectedClassificator = getClassificatoByNodeRef(ActionUtil.getParam(event, "nodeRef"));
        loadClassificatorValues();
    }
    
    /**
     * JSP event handler.
     * Removes the selected value and updates the UI model.
     * @param event
     */
    public void removeValue(ActionEvent event) {
        String ref = ActionUtil.getParam(event, "nodeRef");
        originalValues.remove(ref);
        classificatorValues.remove(getClassificatorValueByNodeRef(ref));
        getClassificatorService().removeClassificatorValueByNodeRef(selectedClassificator, ref);
        if (log.isDebugEnabled()) {
            log.debug("Classificator value with nodeRef = " + ref + " deleted.");
        }
    }
    
    /**
     * JSP event handler. 
     * Adds new value to the model and temporary list before saving. 
     * @param event
     */
    public void addNewValue(ActionEvent event) {
        ClassificatorValue addedClassificatorValue = new ClassificatorValue();
        addedClassificatorValue.setActive(true);
        addedClassificatorValue.setOrder(getMaxClassificatorValueOrder() + 1);
        // set the temporary random unique ID to be used in the UI form 
        addedClassificatorValue.setNodeRef(new NodeRef(addedClassificatorValue.hashCode() + "", event.hashCode() + "", GUID.generate()));
        classificatorValues.add(addedClassificatorValue);
        addedClassificators.add(addedClassificatorValue);
    }

    public void contextUpdated() {
        clearRichList();
    }

    public void areaChanged() {
        clearRichList();
    }

    public void spaceChanged() {
        clearRichList();
    }

    private void clearRichList() {
        if (getRichList() != null) {
            getRichList().setValue(null);
        }
    }
    
    private void updateClassificatorValues() {
        for (ClassificatorValue mod : classificatorValues) {
            ClassificatorValue orig = originalValues.get(mod.getNodeRef().toString());
            if (orig == null) {
                continue;
            }
            
            if (!orig.equals(mod)) {
                if (log.isDebugEnabled()) {
                    log.debug("Updating the classificator value with nodeRef = " + mod.getNodeRef());
                }
                getClassificatorService().removeClassificatorValue(selectedClassificator, orig);
                getClassificatorService().addClassificatorValue(selectedClassificator, mod);
            }
        }
        //save the added new value
        if (addedClassificators != null && addedClassificators.size() > 0) {
            for (ClassificatorValue add : addedClassificators) {
                getClassificatorService().addClassificatorValue(selectedClassificator, add);
                if (log.isDebugEnabled()) {
                    log.debug("New classificator value (" + add.getValueName() + ") saved.");
                }
            }
            addedClassificators = null;
        }
    }

    private void resetData() {
        originalValues = null;
        classificatorValues = null;
        selectedClassificator = null;
        addedClassificators = null;
        richList = null;
    }

    private void loadClassificatorValues() {
        if (selectedClassificator != null) {
            classificatorValues = getClassificatorService().getAllClassificatorValues(selectedClassificator);
            originalValues = new TreeMap<String, ClassificatorValue>();
            for (ClassificatorValue cv : classificatorValues) {
                originalValues.put(cv.getNodeRef().toString(), new ClassificatorValue(cv));
            }
            addedClassificators = new ArrayList<ClassificatorValue>();
        }
    }

    private int getMaxClassificatorValueOrder() {
        int maxOrder = 0;
        for (ClassificatorValue cv : classificatorValues) {
            maxOrder = Math.max(maxOrder, cv.getOrder());
        }
        return maxOrder;
    }
    
    private Classificator getClassificatoByNodeRef(String ref) {
        Assert.notNull(ref);
        return getClassificatorService().getClassificatorByNodeRef(ref);
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
        return null;
    }
}

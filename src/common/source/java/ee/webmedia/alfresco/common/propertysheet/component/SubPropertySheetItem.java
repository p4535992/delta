/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package ee.webmedia.alfresco.common.propertysheet.component;

import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomAttributeNames.STYLE_CLASS;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.component.UIParameter;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;
import javax.faces.event.FacesEvent;

import org.alfresco.config.Config;
import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.IComponentGenerator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.config.ActionsConfigElement;
import org.alfresco.web.config.ActionsConfigElement.ActionDefinition;
import org.alfresco.web.config.ActionsConfigElement.ActionGroup;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIPanel;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.myfaces.shared_impl.renderkit.JSFAttr;
import org.apache.myfaces.shared_impl.renderkit.html.HTML;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.propertysheet.generator.CustomAttributes;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.SessionContext;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Component to represent a child propertySheet as a PropertySheetItem within a parent property sheet
 * 
 * @author Ats Uiboupin
 */
public class SubPropertySheetItem extends PropertySheetItem implements CustomAttributes {
    public static final String SUB_PROPERTY_SHEET_ITEM = SubPropertySheetItem.class.getCanonicalName();
    public static final String PARAM_ASSOC_INDEX = "aIndex";
    /** noderef to property sheet node where that actionLink is located */
    public static final String PARAM_CURRENT_PROP_SHEET_NODE = "currentNodeRef";
    /** noderef to property sheet node that surrounds that actionLink is located */
    public static final String PARAM_PARENT_PROP_SHEET_NODE = "sourceNodeRef";
    public static final String SUB_PROP_SHEET_ID_PREFIX = "subPropSheet_";

    private static final String ACTION_ADD_SUFFIX = "_add";
    private static final String ACTION_REMOVE_SUFFIX = "_remove";
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(SubPropertySheetItem.class);
    /**
     * Determines which kind of associations to show on this subPropertySheets <br>
     * currently supported only "children"<br>
     */
    private final static String ATTR_ASSOC_BRAND = "assocBrand";
    private final static String ATTR_ASSOC_NAME = "assocName";
    private final static String ATTR_ACTIONS_GROUP_ID = "actionsGroupId";
    private final static String ATTR_TITLE_LABEL_ID = "titleLabelId";

    private Map<String, String> customAttributes;
    private QName assocTypeQName;
    private Node parentPropSheetNode;
    private NamespaceService namespaceService;
    private NodeService nodeService;
    private GeneralService generalService;
    private SessionContext sessionContext;
    private DictionaryService dictionaryService;
    private NodeAssocBrand associationBrand;
    private Integer subPropSheetCounter;
    private int nrOfChildNodes;

    @Override
    protected String getIncorrectParentMsg() {
        return "The property component must be nested within a property sheet component";
    }

    @Override
    protected void generateItem(FacesContext context, UIPropertySheet outerPropSheet) throws IOException {
        parentPropSheetNode = outerPropSheet.getNode();
        final List<Node> childNodes;
        associationBrand = NodeAssocBrand.get(getCustomAttributes().get(ATTR_ASSOC_BRAND));
        if (NodeAssocBrand.CHILDREN.equals(associationBrand)) {
            childNodes = parentPropSheetNode.getAllChildAssociations(getAssocTypeQName());
        } else {
            throw new RuntimeException("fetching related nodes for association with associationBrand='" + associationBrand + "' is unimplemented");
        }
        // put parent parentPropSheetNode (parentPropSheetNode of the parent property sheet) into sessionContext, so it could be used by add/remove actions
        getSessionContext().put(parentPropSheetNode.getNodeRefAsString(), parentPropSheetNode);
        subPropSheetCounter = null;
        if (childNodes == null || childNodes.size() == 0) {
            createSubPropertySheetWrapper(outerPropSheet, null, context);
            return;
        }
        subPropSheetCounter = 0;
        nrOfChildNodes = childNodes.size();

        for (Node subPropSheetNode : childNodes) {
            createSubPropertySheetWrapper(outerPropSheet, subPropSheetNode, context);
            subPropSheetCounter++;
        }
    }

    @Override
    public void broadcast(FacesEvent event) throws AbortProcessingException {
        if (event instanceof AddRemoveEvent) {
            AddRemoveEvent addRemoveEvent = (AddRemoveEvent) event;
            SubPropertySheetItem subPropSheet = (SubPropertySheetItem) addRemoveEvent.getSource();
            UIPropertySheet propSheet = (UIPropertySheet) subPropSheet.getParent();
            propSheet = ComponentUtil.getAncestorComponent(propSheet, UIPropertySheet.class, true);
            log.debug("addRemoveEvent" + addRemoveEvent);
            propSheet.getChildren().clear();
            propSheet.getClientValidations().clear();

        } else {
            super.broadcast(event);
        }
    }

    @Override
    public Map<String, String> getCustomAttributes() {
        if (customAttributes == null) {
            customAttributes = new HashMap<String, String>(0);
        }
        return customAttributes;
    }

    @Override
    public void setCustomAttributes(Map<String, String> propertySheetItemAttributes) {
        this.customAttributes = propertySheetItemAttributes;
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        if (!isRendered()) {
            if (log.isTraceEnabled()) {
                log.trace("End encoding component " + getId() + " since rendered attribute is set to false ");
            }
            return;
        }
        super.encodeBegin(context);
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement(HTML.TR_ELEM, this);
        writer.writeAttribute("class", "subPropertySheetTR", null);
        writer.startElement(HTML.TD_ELEM, this);
        writer.writeAttribute("colspan", 3, null);
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException {
        super.encodeEnd(context);
        ResponseWriter writer = context.getResponseWriter();
        writer.endElement(HTML.TD_ELEM);
        writer.endElement(HTML.TR_ELEM);
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] state = new Object[2];
        state[0] = super.saveState(context);
        state[1] = getCustomAttributes();
        return state;
    }

    @Override
    public void restoreState(FacesContext context, Object stateObj) {
        Object[] state = (Object[]) stateObj;
        super.restoreState(context, state[0]);
        @SuppressWarnings("unchecked")
        Map<String, String> customAttributes = (Map<String, String>) state[1];
        setCustomAttributes(customAttributes);
    }

    public QName getAssocTypeQName() {
        if (assocTypeQName == null) {
            assocTypeQName = resoveToQName(getCustomAttributes().get(ATTR_ASSOC_NAME));
        }
        return assocTypeQName;
    }

    private HtmlPanelGroup buildActionGroup(FacesContext context, final String actionGroupId, Node subPropSheetNode) {
        Application application = context.getApplication();
        HtmlPanelGroup actionsWrapper = (HtmlPanelGroup) application.createComponent(ComponentConstants.JAVAX_FACES_PANELGROUP);
        FacesHelper.setupComponentId(context, actionsWrapper, "actionsWrapper");
        @SuppressWarnings("unchecked")
        final Map<String, Object> attr = actionsWrapper.getAttributes();
        attr.put(STYLE_CLASS, "subPropSheetActions");
        @SuppressWarnings("unchecked")
        final List<UIComponent> wrapperChildren = actionsWrapper.getChildren();
        Config config = org.alfresco.web.app.Application.getConfigService(context).getGlobalConfig();
        final ActionsConfigElement actionsConfig = (ActionsConfigElement)
                config.getConfigElement(ActionsConfigElement.CONFIG_ELEMENT_ID);
        final ActionGroup actionGroup = actionsConfig.getActionGroup(actionGroupId);
        final String nodeKeyInSessionContext = parentPropSheetNode.getNodeRefAsString();
        // needed if no actions to be shown)
        for (String actionId : actionGroup) {
            ActionDefinition actionDef = actionsConfig.getActionDefinition(actionId);
            if (isActionDisabled(actionDef, actionGroupId)) {
                continue;
            }
            UIActionLink link = (UIActionLink) application.createComponent(UIActions.COMPONENT_ACTIONLINK);
            link.setRendererType(UIActions.RENDERER_ACTIONLINK);
            link.setImage(actionDef.Image);
            FacesHelper.setupComponentId(context, link, actionId);
            String lblMessage = actionDef.LabelMsg != null ? MessageUtil.getMessage(context, actionDef.LabelMsg) : actionDef.Label;
            link.setValue(lblMessage);
            link.setActionListener(application.createMethodBinding(actionDef.ActionListener, new Class[] { javax.faces.event.ActionEvent.class }));
            String tooltip = actionDef.TooltipMsg != null ? MessageUtil.getMessage(context, actionDef.TooltipMsg) : actionDef.Tooltip;
            link.setTooltip(tooltip);

            final AddRemoveActionListener listener = new AddRemoveActionListener();
            link.addActionListener(listener);
            link.setShowLink(actionDef.ShowLink);
            @SuppressWarnings("unchecked")
            List<UIComponent> parameters = link.getChildren();

            UIParameter parentPropSheetParam = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
            parentPropSheetParam.setName(PARAM_PARENT_PROP_SHEET_NODE);
            parentPropSheetParam.setValue(nodeKeyInSessionContext);

            UIParameter assocIndexParam = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
            assocIndexParam.setName(PARAM_ASSOC_INDEX);
            assocIndexParam.setValue(subPropSheetCounter);

            parameters.add(assocIndexParam);
            parameters.add(parentPropSheetParam);
            if (subPropSheetNode != null) { // subPropSheetNode == null if there are no subPropertySheets, but we want to still show "addNew" button
                UIParameter subPropSheetParam = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
                subPropSheetParam.setName(PARAM_CURRENT_PROP_SHEET_NODE);
                subPropSheetParam.setValue(subPropSheetNode.getNodeRef().toString());
                parameters.add(subPropSheetParam);
            }

            wrapperChildren.add(link);
        }

        return actionsWrapper;
    }

    private boolean isActionDisabled(ActionDefinition actionDef, String actionGroupId) {
        if (actionDef.Evaluator != null && !actionDef.Evaluator.evaluate(parentPropSheetNode)) {
            return true;
        }
        final String actionId = actionDef.getId();
        final boolean isRemoveAction = actionId.startsWith(actionGroupId) && actionId.endsWith(ACTION_REMOVE_SUFFIX)
                && actionId.length() == actionGroupId.length() + ACTION_REMOVE_SUFFIX.length();
        if (isRemoveAction && !isDeleteEnabled()) {
            return true; // can't remove mandatory association
        }
        final boolean isAddAction = actionId.startsWith(actionGroupId) && actionId.endsWith(ACTION_ADD_SUFFIX)
                && actionId.length() == actionGroupId.length() + ACTION_ADD_SUFFIX.length();
        if (isAddAction && !isAddEnabled()) {
            return true; // can't remove mandatory association
        }

        return false;
    }

    private boolean isDeleteEnabled() {
        return !isAssocMandatory() || subPropSheetCounter != 0;
    }

    private boolean isAddEnabled() {
        return (nrOfChildNodes == 0) || (subPropSheetCounter + 1 == nrOfChildNodes);
    }

    private boolean isAssocMandatory() {
        DictionaryService dictionaryService = getDictionaryService();
        final AssociationDefinition association2 = dictionaryService.getAssociation(getAssocTypeQName());
        return association2.isTargetMandatory();
    }

    private QName resoveToQName(String expression) {
        if ("*".equals(expression)) {
            return null;
        }
        return QName.resolveToQName(getNamespaceService(), expression);
    }

    private UIComponent createSubPropertySheetWrapper(UIPropertySheet outerPropSheet, Node subPropSheetNode, FacesContext context) {
        final UINamingContainer uiNamingContainer = new UINamingContainer();
        FacesHelper.setupComponentId(context, uiNamingContainer, "uiNamingContainer" + subPropSheetCounter);

        final String titleLabelId = getCustomAttributes().get(ATTR_TITLE_LABEL_ID);
        final StringBuilder debugBuffer = new StringBuilder();
        final UIPanel parentUIPanel = ComponentUtil.getAncestorComponent(this, UIPanel.class, true, debugBuffer);

        final String parentUIPanelLabel = parentUIPanel.getLabel();
        log.debug("debugBuffer:\n" + debugBuffer);

        UIPanel subPropSheetWrapper = new UIPanel();
        FacesHelper.setupComponentId(context, subPropSheetWrapper, "subPropSheetWrapper");
        subPropSheetWrapper.setLabel(MessageUtil.getMessage(context, titleLabelId, parentUIPanelLabel, subPropSheetCounter != null ? subPropSheetCounter + 1
                : null));

        final String actionGroupId = getCustomAttributes().get(ATTR_ACTIONS_GROUP_ID);
        @SuppressWarnings("unchecked")
        final List<UIComponent> wrapperChildren = subPropSheetWrapper.getChildren();

        if (outerPropSheet.inEditMode()) {
            final HtmlPanelGroup subPropSheetActionsWrapper = buildActionGroup(context, actionGroupId, subPropSheetNode);
            wrapperChildren.add(subPropSheetActionsWrapper);
        }

        if (subPropSheetNode != null) {
            wrapperChildren.add(createSubPropertySheet(outerPropSheet, subPropSheetNode, context));
        }
        @SuppressWarnings("unchecked")
        final List<UIComponent> namingContainerChildren = uiNamingContainer.getChildren();
        namingContainerChildren.add(subPropSheetWrapper);

        @SuppressWarnings("unchecked")
        final List<UIComponent> subPropSheetChildren = getChildren();
        subPropSheetChildren.add(uiNamingContainer);

        // @SuppressWarnings("unchecked")
        // final Map<String, UIComponent> facets = subPropSheetWrapper.getFacets();
        // final String actionsClientId = subPropSheetActionsWrapper.getClientId(context);
        // facets.put(actionsClientId, subPropSheetActionsWrapper);

        return uiNamingContainer;
    }

    private UIPropertySheet createSubPropertySheet(UIPropertySheet outerPropSheet, Node subPropSheetNode, FacesContext context) {
        int assocIndex = subPropSheetCounter;
        final WMUIPropertySheet childProperySheet = new WMUIPropertySheet();// TODO: PropertySheetGridTag.getcomponent()
        FacesHelper.setupComponentId(context, childProperySheet, SUB_PROP_SHEET_ID_PREFIX + assocIndex);
        childProperySheet.setNode(subPropSheetNode);
        // copy defaults from parent propertySheet
        childProperySheet.setMode(outerPropSheet.getMode());
        childProperySheet.setParent(this);
        childProperySheet.setReadOnly(outerPropSheet.isReadOnly());
        childProperySheet.setRendererType(outerPropSheet.getRendererType());
        childProperySheet.setTransient(outerPropSheet.isTransient());
        childProperySheet.setValidationEnabled(outerPropSheet.isValidationEnabled());
        childProperySheet.setAssociationIndex(assocIndex);
        childProperySheet.setAssociationBrand(associationBrand);
        childProperySheet.setVar(outerPropSheet.getVar() + getPropSheetVarSuffix(assocIndex));
        // defaults can be overridden from config element
        @SuppressWarnings("unchecked")
        final Map<String, Object> attr = childProperySheet.getAttributes();
        attr.put(STYLE_CLASS, "subPropSheet");
        attr.put("externalConfig", true);
        attr.put("labelStyleClass", "propertiesLabel");
        for (Entry<String, String> entry : getCustomAttributes().entrySet()) {
            final String key = entry.getKey();
            Object value = entry.getValue();
            if (JSFAttr.COLUMNS_ATTR.equalsIgnoreCase(key)) { // columns attribute must be cast to integer
                value = Integer.parseInt((String) value);
            }
            attr.put(key, value);
        }
        return childProperySheet;
    }

    private String getPropSheetVarSuffix(int assocIndex) {
        if (NodeAssocBrand.CHILDREN.equals(associationBrand)) {
            return ".allChildAssociationsByAssocType[\"" + getAssocTypeQName().toString() + "\"][" + assocIndex + "]";
        }
        throw new RuntimeException("Creating propertySheetVar for subPropertySheet associated to parent with associationBrand='" //
                + associationBrand + "' is unimplemented");
    }

    protected SessionContext getSessionContext() {
        if (sessionContext == null) {
            sessionContext = (SessionContext) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(SessionContext.BEAN_NAME);
        }
        return sessionContext;
    }

    protected GeneralService getGeneralService() {
        if (generalService == null) {
            generalService = (GeneralService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(GeneralService.BEAN_NAME);
        }
        return generalService;
    }

    protected NamespaceService getNamespaceService() {
        if (namespaceService == null) {
            namespaceService = (NamespaceService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean("NamespaceService");
        }
        return namespaceService;
    }

    protected DictionaryService getDictionaryService() {
        if (dictionaryService == null) {
            dictionaryService = (DictionaryService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean("DictionaryService");
        }
        return dictionaryService;
    }

    protected NodeService getNodeService() {
        if (nodeService == null) {
            nodeService = (NodeService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean("NodeService");
        }
        return nodeService;
    }

    /**
     * Let subclasses override it
     * 
     * @param context FacesContext
     * @param generatorName The name of the component generator to retrieve
     * @return component generated and optionally changed as well
     * @author Ats Uiboupin
     */
    protected IComponentGenerator getComponentGenerator(FacesContext context, String componentGeneratorName) {
        return FacesHelper.getComponentGenerator(context, componentGeneratorName);
    }

    public static class AddRemoveEvent extends ActionEvent {
        private static final long serialVersionUID = 1L;

        public AddRemoveEvent(UIComponent uiComponent) {
            super(uiComponent);
        }

    }

    public static class AddRemoveActionListener implements ActionListener, Serializable {
        private static final long serialVersionUID = 1L;

        public AddRemoveActionListener() {
            log.debug("new AddRemoveActionListener()");
        }

        @Override
        public void processAction(ActionEvent actionEvent) throws AbortProcessingException {
            log.debug("processAction: " + actionEvent);
            final UIComponent eventSource = actionEvent.getComponent();
            if (eventSource instanceof UIActionLink) {
                UIActionLink eventSourceLink = (UIActionLink) eventSource;
                final String linkId = eventSourceLink.getId();
                if (linkId != null) {
                    if (!linkId.endsWith(ACTION_ADD_SUFFIX) && !linkId.endsWith(ACTION_REMOVE_SUFFIX)) {
                        return;
                    }
                    final PropertySheetItem subPropSheet = ComponentUtil.getAncestorComponent(eventSourceLink, PropertySheetItem.class, true);
                    if (subPropSheet != null && null != ComponentUtil.getAncestorComponent(subPropSheet, UIPropertySheet.class, true)) {
                        eventSourceLink.queueEvent(new AddRemoveEvent(subPropSheet));
                    }
                }
            }
        }
    }

}

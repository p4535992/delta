/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
*/
package javax.faces.component;

import java.util.Arrays;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.MethodBinding;
import javax.faces.el.ValueBinding;
import javax.faces.event.*;

import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.evaluator.ActionInstanceEvaluator;
import org.alfresco.web.ui.repo.component.evaluator.PermissionEvaluator;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.listener.StatisticsPhaseListenerLogColumn;
import ee.webmedia.alfresco.common.listener.StatisticsPhaseListener;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * see Javadoc of <a href="http://java.sun.com/j2ee/javaserverfaces/1.1_01/docs/api/index.html">JSF Specification</a>
 *
 * @author Manfred Geiler (latest modification by $Author: weber $)
 * @version $Revision: 501845 $ $Date: 2007-01-31 14:54:23 +0100 (Mi, 31 Jän 2007) $
 */
public class UICommand
        extends UIComponentBase
        implements ActionSource
{
    private MethodBinding _action = null;
    private MethodBinding _actionListener = null;
    private static List<String> disableValidationActions = Arrays.asList("#{AddFileDialog.start}", "#{DocumentDynamicDialog.addFile}", "#{DocumentDynamicDialog.addInactiveFile}",
            "#{DocumentQuickSearchResultsDialog.setup}", "#{DocumentDialog.searchDocsAndCases}", "#{DocumentDialog.search.setup}", "#{MenuBean.closeBreadcrumbItem}");

    public void setAction(MethodBinding action)
    {
        _action = action;
    }

    public MethodBinding getAction()
    {
        return _action;
    }

    public void setActionListener(MethodBinding actionListener)
    {
        _actionListener = actionListener;
    }

    public MethodBinding getActionListener()
    {
        return _actionListener;
    }

    public void addActionListener(ActionListener listener)
    {
        addFacesListener(listener);
    }

    public ActionListener[] getActionListeners()
    {
        return (ActionListener[])getFacesListeners(ActionListener.class);
    }

    public void removeActionListener(ActionListener listener)
    {
        removeFacesListener(listener);
    }

    public void broadcast(FacesEvent event)
            throws AbortProcessingException
    {
        super.broadcast(event);

        if (event instanceof ActionEvent)
        {
            FacesContext context = getFacesContext();

            MethodBinding actionListenerBinding = getActionListener();
            if (actionListenerBinding != null)
            {
                long startTime = System.nanoTime();
                try
                {
                    verifyActionAllowed((ActionEvent) event);
                    actionListenerBinding.invoke(context, new Object[] {event});
                }
                catch (EvaluationException e)
                {
                    Throwable cause = e.getCause();
                    if (cause != null && cause instanceof AbortProcessingException)
                    {
                        throw (AbortProcessingException)cause;
                    }
                    else
                    {
                        throw e;
                    }
                }
                finally
                {
                    long duration = (System.nanoTime() - startTime) / 1000000L;
                    StatisticsPhaseListener.add(StatisticsPhaseListenerLogColumn.ACTION_LISTENER, duration + "," + actionListenerBinding.getExpressionString());
                }
            }

            ActionListener defaultActionListener
                    = context.getApplication().getActionListener();
            if (defaultActionListener != null)
            {
                defaultActionListener.processAction((ActionEvent)event);
            }
        }
    }

    public void queueEvent(FacesEvent event)
    {
        if (event != null && this == event.getSource() && event instanceof ActionEvent)
        {
            if (isImmediate())
            {
                event.setPhaseId(PhaseId.APPLY_REQUEST_VALUES);
            }
            else
            {
                event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            }
            if(getActionListener() != null){
                disableRequestValidationIfNeeded(getActionListener().getExpressionString());
            } else if (StringUtils.startsWith(getId(), MenuBean.SHORTCUT_MENU_ITEM_PREFIX)) {
                Utils.setRequestValidationDisabled(FacesContext.getCurrentInstance());
            }
        }
        super.queueEvent(event);
    }


    private void disableRequestValidationIfNeeded(String actionExpr) {
        if(disableValidationActions.contains(actionExpr)){
            Utils.setRequestValidationDisabled(FacesContext.getCurrentInstance());
        }
        
    }
    
    private void verifyActionAllowed(ActionEvent event) {
        UIComponent component = event.getComponent();
        if (component != null) {
            if (!component.isRendered()) {
                handleActionNotAllowedError();
            }
            verifyComponentActionAllowed(component, BeanHelper.getNodeService());
        }
    }

    private void verifyComponentActionAllowed(UIComponent component, NodeService nodeService) {
        UIComponent parent = component.getParent();
        if (parent == null) {
            return;
        }
        if (parent instanceof ActionInstanceEvaluator) {
            ActionInstanceEvaluator actionInstanceEvaluator = (ActionInstanceEvaluator) parent;
            UIActions actionsComponent = ComponentUtil.findParentWithClass(actionInstanceEvaluator, UIActions.class);
            if (actionsComponent != null) {
                final Object context = reloadContext(actionsComponent.getContext(), nodeService);
                if (!actionInstanceEvaluator.evaluate(context)) {
                    handleActionNotAllowedError();
                }
            }
        } else if (parent instanceof PermissionEvaluator) {
            PermissionEvaluator permissionEvaluator = (PermissionEvaluator) parent;
            Object context = reloadContext(permissionEvaluator.getValue(), nodeService);
            if (!permissionEvaluator.evaluate(context)) {
                handleActionNotAllowedError();
            }
        } else if (!parent.isRendered()) {
            handleActionNotAllowedError();
        }
        verifyComponentActionAllowed(parent, nodeService);
    }
    
    private Object reloadContext(Object context, NodeService nodeService) {
        if (context instanceof Node && RepoUtil.isSaved((Node) context) && nodeService.exists(((Node) context).getNodeRef())) {
            // reload saved node
            Node node = (Node) context;
            context = (context instanceof WmNode) ? new WmNode(node.getNodeRef(), node.getType()) : new Node(node.getNodeRef());
        }
        return context;
    }

    private void handleActionNotAllowedError() {
        MessageUtil.addErrorMessage("action_error_action_not_allowed");
        throw new AbortProcessingException();
    }


    //------------------ GENERATED CODE BEGIN (do not modify!) --------------------

    public static final String COMPONENT_TYPE = "javax.faces.Command";
    public static final String COMPONENT_FAMILY = "javax.faces.Command";
    private static final String DEFAULT_RENDERER_TYPE = "javax.faces.Button";
    private static final boolean DEFAULT_IMMEDIATE = false;

    private Boolean _immediate = null;
    private Object _value = null;

    public UICommand()
    {
        setRendererType(DEFAULT_RENDERER_TYPE);
    }

    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }

    public void setImmediate(boolean immediate)
    {
        _immediate = Boolean.valueOf(immediate);
    }

    public boolean isImmediate()
    {
        if (_immediate != null) return _immediate.booleanValue();
        ValueBinding vb = getValueBinding("immediate");
        Boolean v = vb != null ? (Boolean)vb.getValue(getFacesContext()) : null;
        return v != null ? v.booleanValue() : DEFAULT_IMMEDIATE;
    }

    public void setValue(Object value)
    {
        _value = value;
    }

    public Object getValue()
    {
        if (_value != null) return _value;
        ValueBinding vb = getValueBinding("value");
        return vb != null ? vb.getValue(getFacesContext()) : null;
    }



    public Object saveState(FacesContext context)
    {
        Object values[] = new Object[5];
        values[0] = super.saveState(context);
        values[1] = saveAttachedState(context, _action);
        values[2] = saveAttachedState(context, _actionListener);
        values[3] = _immediate;
        values[4] = _value;
        return values;
    }

    public void restoreState(FacesContext context, Object state)
    {
        Object values[] = (Object[])state;
        super.restoreState(context, values[0]);
        _action = (MethodBinding)restoreAttachedState(context, values[1]);
        _actionListener = (MethodBinding)restoreAttachedState(context, values[2]);
        _immediate = (Boolean)values[3];
        _value = values[4];
    }
    //------------------ GENERATED CODE END ---------------------------------------
}

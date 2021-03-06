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
package org.alfresco.web.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.config.ConfigElement;
import org.alfresco.config.ConfigException;
import org.alfresco.config.element.ConfigElementAdapter;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.web.action.ActionEvaluator;

import ee.webmedia.alfresco.common.evaluator.EvaluatorSharedResource;

/**
 * Action config element.
 *
 * @author Kevin Roast
 */
public class ActionsConfigElement extends ConfigElementAdapter
{
    public static final String CONFIG_ELEMENT_ID = "actions";

    private final Map<String, ActionDefinition> actionDefs = new HashMap<String, ActionDefinition>(32, 1.0f);
    private Map<String, ActionGroup> actionGroups = new HashMap<String, ActionGroup>(16, 1.0f);

    /**
     * Default constructor
     */
    public ActionsConfigElement()
    {
        super(CONFIG_ELEMENT_ID);
    }

    /**
     * @param name
     */
    public ActionsConfigElement(String name)
    {
        super(name);
    }

    /**
     * @see org.alfresco.config.element.ConfigElementAdapter#getChildren()
     */
    @Override
    public List<ConfigElement> getChildren()
    {
        throw new ConfigException("Reading the Actions config via the generic interfaces is not supported");
    }

    /**
     * @see org.alfresco.config.element.ConfigElementAdapter#combine(org.alfresco.config.ConfigElement)
     */
    @Override
    public ConfigElement combine(ConfigElement configElement)
    {
        ActionsConfigElement newElement = (ActionsConfigElement) configElement;
        ActionsConfigElement combinedElement = new ActionsConfigElement();

        // add the existing action definitions
        combinedElement.actionDefs.putAll(actionDefs);

        // overwrite any existing action definitions i.e. don't combine
        combinedElement.actionDefs.putAll(newElement.actionDefs);

        // add the existing action groups
        Map<String, ActionGroup> combinedActionGroups = new HashMap<String, ActionGroup>(actionGroups.size());
        try
        {
            for (ActionGroup group : actionGroups.values())
            {
                combinedActionGroups.put(group.getId(), (ActionGroup) group.clone());
            }
        } catch (CloneNotSupportedException e)
        {
            throw new AlfrescoRuntimeException("clone() required on ActionGroup class.", e);
        }
        combinedElement.actionGroups = combinedActionGroups;

        // any new action groups with the same name must be combined
        for (ActionGroup newGroup : newElement.actionGroups.values())
        {
            if (combinedElement.actionGroups.containsKey(newGroup.getId()))
            {
                // there is already a group with this id, combine it with the new one
                ActionGroup combinedGroup = combinedElement.actionGroups.get(newGroup.getId());
                if (newGroup.ShowLink != combinedGroup.ShowLink)
                {
                    combinedGroup.ShowLink = newGroup.ShowLink;
                }
                if (newGroup.Style != null)
                {
                    combinedGroup.Style = newGroup.Style;
                }
                if (newGroup.StyleClass != null)
                {
                    combinedGroup.StyleClass = newGroup.StyleClass;
                }

                // add all the actions from the new group to the combined one
                for (String actionRef : newGroup.getAllActions())
                {
                    combinedGroup.addAction(actionRef);
                }

                // add all the hidden actions from the new group to the combined one
                for (String actionRef : newGroup.getHiddenActions())
                {
                    combinedGroup.hideAction(actionRef);
                }
            }
            else
            {
                // it's a new group so just add it
                combinedElement.actionGroups.put(newGroup.getId(), newGroup);
            }
        }

        return combinedElement;
    }

    /* package */void addActionDefinition(ActionDefinition actionDef)
    {
        actionDefs.put(actionDef.getId(), actionDef);
    }

    public ActionDefinition getActionDefinition(String id)
    {
        return actionDefs.get(id);
    }

    /* package */void addActionGroup(ActionGroup group)
    {
        actionGroups.put(group.getId(), group);
    }

    public ActionGroup getActionGroup(String id)
    {
        return actionGroups.get(id);
    }

    /**
     * Simple class representing the definition of a UI action.
     *
     * @author Kevin Roast
     */
    public static class ActionDefinition
    {
        public ActionDefinition(String id)
        {
            if (id == null || id.length() == 0)
            {
                throw new IllegalArgumentException("ActionDefinition ID is mandatory.");
            }
            this.id = id;
        }

        public String getId()
        {
            return id;
        }

        public void addAllowPermission(String permission)
        {
            if (permissionAllow == null)
            {
                permissionAllow = new ArrayList<String>(2);
            }
            permissionAllow.add(permission);
        }

        public void addDenyPermission(String permission)
        {
            if (permissionDeny == null)
            {
                permissionDeny = new ArrayList<String>(1);
            }
            permissionDeny.add(permission);
        }

        public List<String> getAllowPermissions()
        {
            return permissionAllow;
        }

        public List<String> getDenyPermissions()
        {
            return permissionDeny;
        }

        public void addParam(String name, String value)
        {
            if (params == null)
            {
                params = new HashMap<String, String>(1, 1.0f);
            }
            params.put(name, value);
        }

        public Map<String, String> getParams()
        {
            return params;
        }

        String id;
        private List<String> permissionAllow = null;
        private List<String> permissionDeny = null;
        private Map<String, String> params = null;

        public ActionEvaluator Evaluator = null;
        public String Label;
        public String LabelMsg;
        public String Tooltip;
        public String TooltipMsg;
        public boolean ShowLink = true;
        public String Style;
        public String StyleClass;
        public String Image;
        public String ActionListener;
        public String Action;
        public String Href;
        public String Target;
        public String Script;
        public String Onclick;
    }

    /**
     * Simple class representing a group of UI actions.
     *
     * @author Kevin Roast
     */
    public static class ActionGroup implements Iterable<String>, Cloneable
    {
        public ActionGroup(String id)
        {
            if (id == null || id.length() == 0)
            {
                throw new IllegalArgumentException("ActionGroup ID is mandatory.");
            }
            this.id = id;
        }

        @Override
        protected Object clone() throws CloneNotSupportedException
        {
            ActionGroup clone = new ActionGroup(id);
            clone.actions = (Set<String>) ((LinkedHashSet) actions).clone();
            clone.hiddenActions = (Set<String>) ((HashSet) hiddenActions).clone();
            clone.ShowLink = ShowLink;
            clone.Style = Style;
            clone.StyleClass = StyleClass;
            clone.sharedResources = sharedResources;
            return clone;
        }

        public String getId()
        {
            return id;
        }

        /**
         * @return Iterator over the visible ActionDefinition IDs referenced by this group
         */
        @Override
        public Iterator<String> iterator()
        {
            // create a list of the visible actions and return it's iterator
            ArrayList<String> visibleActions = new ArrayList<String>(actions.size());
            for (String actionId : actions)
            {
                if (hiddenActions.contains(actionId) == false)
                {
                    visibleActions.add(actionId);
                }
            }

            visibleActions.trimToSize();

            return visibleActions.iterator();
        }

        /* package */void addAction(String actionId)
        {
            actions.add(actionId);
        }

        /* package */void hideAction(String actionId)
        {
            hiddenActions.add(actionId);
        }

        /* package */Set<String> getAllActions()
        {
            return actions;
        }

        /* package */Set<String> getHiddenActions()
        {
            return hiddenActions;
        }

        void setSharedResources(Class<EvaluatorSharedResource<Serializable>> value) {
            sharedResources = value;
        }

        public boolean isSharedResources() {
            return sharedResources != null;
        }

        public Class<EvaluatorSharedResource<Serializable>> getSharedResources() {
            return sharedResources;
        }

        private final String id;

        /**
         * the action definitions, we use a Linked HashSet to ensure we do not have more
         * than one action with the same Id and that the insertion order is preserved
         */
        private Set<String> actions = new LinkedHashSet<String>(16, 1.0f);

        /** the actions that have been hidden */
        private Set<String> hiddenActions = new HashSet<String>(4, 1.0f);
        private Class<EvaluatorSharedResource<Serializable>> sharedResources;

        public boolean ShowLink;
        public String Style;
        public String StyleClass;
    }
}

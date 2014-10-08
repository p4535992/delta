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
package org.alfresco.web.ui.repo.component.evaluator;

import java.util.StringTokenizer;

import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.component.evaluator.BaseEvaluator;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.series.model.SeriesModel;

/**
 * Evaulator for returning whether a Node is Allowed/Denied a list of permissions
 * 
 * @author Kevin Roast
 */
public class PermissionEvaluator extends BaseEvaluator
{
   /**
    * Evaluate against the component attributes. Return true to allow the inner
    * components to render, false to hide them during rendering.
    * 
    * IN: Value - either a Node (preferred) or NodeRef to test
    *     Allow - Permission(s) (comma separated) to test against
    *     Deny  - Permission(s) (comma separated) to test against
    * 
    * @return true to allow rendering of child components, false otherwise
    */
   public boolean evaluate()
   {
      boolean result = true;
      
      // TODO: implement Deny permissions checking (as required...)
      
      try
      {
         Object obj = getValue();
         result = evaluate(result, obj);
      }
      catch (Exception err)
      {
         handleEvaluationError(err);
         return false;
      }
      
      return result;
   }
   
    public boolean evaluate(Object obj) {
        try {
            return evaluate(true, obj);
        } catch (Exception err) {
            handleEvaluationError(err);
            return false;
        }
    }

    public void handleEvaluationError(Exception err) {
        // return default value on error
         s_logger.warn("Error during PermissionEvaluator evaluation: " + err.getMessage(), err);
    }

    public boolean evaluate(boolean result, Object obj) {
        Privilege[] permissionsToCheck = getAllowPermissions();
        Privilege[] denyPermissions = getDenyPermissions();
        Assert.isTrue(permissionsToCheck.length == 0 || denyPermissions.length == 0, "Allow and deny permission checks are mutually exclusive, cannot use both in same evaluation");
        boolean revert = permissionsToCheck.length == 0;
        if (revert) {
            permissionsToCheck = denyPermissions;
        }
        if (permissionsToCheck.length > 0) {
            if (obj instanceof Node)
            {
                // used the cached permissions checks in the Node instance
                // this means multiple calls to evaluators don't need to keep calling services
                // and permissions on a Node shouldn't realistically change over the life of an instance
                boolean hasPermission = ((Node) obj).hasPermission(permissionsToCheck);
                result = revert ? !hasPermission : hasPermission;
            }
            else if (obj instanceof NodeRef)
            {
                NodeRef nodeRef = (NodeRef) obj;
                boolean hasPermission = BeanHelper.getPrivilegeService().hasPermission(nodeRef, AuthenticationUtil.getRunAsUser(), permissionsToCheck);
                result = revert ? !hasPermission : hasPermission;
                if (!result && !revert && ArrayUtils.contains(permissionsToCheck, Privilege.VIEW_DOCUMENT_META_DATA)) {
                    NodeRef seriesRef = BeanHelper.getGeneralService().getAncestorNodeRefWithType(nodeRef, SeriesModel.Types.SERIES);
                    result = seriesRef != null && !Boolean.FALSE.equals(BeanHelper.getNodeService().getProperty(seriesRef, SeriesModel.Props.DOCUMENTS_VISIBLE_FOR_USERS_WITHOUT_ACCESS));
                }
            }

            return result;
        }
        return result;
    }

    /**
     * @see javax.faces.component.StateHolder#restoreState(javax.faces.context.FacesContext, java.lang.Object)
     */
    @Override
    public void restoreState(FacesContext context, Object state)
    {
        Object values[] = (Object[]) state;
        // standard component attributes are restored by the super class
        super.restoreState(context, values[0]);
        allow = (String) values[1];
        deny = (String) values[2];
    }

    /**
     * @see javax.faces.component.StateHolder#saveState(javax.faces.context.FacesContext)
     */
    @Override
    public Object saveState(FacesContext context)
    {
        Object values[] = new Object[3];
        // standard component attributes are saved by the super class
        values[0] = super.saveState(context);
        values[1] = allow;
        values[2] = deny;
        return (values);
    }

    /**
     * @return the array of Allow permissions
     */
    private Privilege[] getAllowPermissions()
    {
        Privilege[] allowPermissions;

        String allow = getAllow();
        if (allow != null)
        {
            if (allow.indexOf(',') == -1)
            {
                // simple case - one permission
                allowPermissions = new Privilege[1];
                allowPermissions[0] = Privilege.getPrivilegeByName(allow);
            }
            else
            {
                // complex case - multiple permissions
                StringTokenizer t = new StringTokenizer(allow, ",");
                allowPermissions = new Privilege[t.countTokens()];
                for (int i = 0; i < allowPermissions.length; i++)
                {
                    allowPermissions[i] = Privilege.getPrivilegeByName(t.nextToken());
                }
            }
        }
        else
        {
            allowPermissions = new Privilege[0];
        }

        return allowPermissions;
    }

    /**
     * @return the array of Allow permissions
     */
    private Privilege[] getDenyPermissions()
    {
        Privilege[] denyPermissions;

        String deny = getDeny();
        if (deny != null)
        {
            if (deny.indexOf(',') == -1)
            {
                // simple case - one permission
                denyPermissions = new Privilege[1];
                denyPermissions[0] = Privilege.getPrivilegeByName(deny);
            }
            else
            {
                // complex case - multiple permissions
                StringTokenizer t = new StringTokenizer(deny, ",");
                denyPermissions = new Privilege[t.countTokens()];
                for (int i = 0; i < denyPermissions.length; i++)
                {
                    denyPermissions[i] = Privilege.getPrivilegeByName(t.nextToken());
                }
            }
        }
        else
        {
            denyPermissions = new Privilege[0];
        }

        return denyPermissions;
    }

    
    /**
     * Get the allow permissions to match value against
     * 
     * @return the allow permissions to match value against
     */
    public String getAllow()
    {
       ValueBinding vb = getValueBinding("allow");
       if (vb != null)
       {
          this.allow = (String)vb.getValue(getFacesContext());
       }
       
       return this.allow;
    }
    
    /**
     * Set the allow permissions to match value against
     * 
     * @param allow     allow permissions to match value against
     */
    public void setAllow(String allow)
    {
       this.allow = allow;
    }
    
    /**
     * Get the deny permissions to match value against
     * 
     * @return the deny permissions to match value against
     */
    public String getDeny()
    {
       ValueBinding vb = getValueBinding("deny");
       if (vb != null)
       {
          this.deny = (String)vb.getValue(getFacesContext());
       }
       
       return this.deny;
    }
    
    /**
     * Set the deny permissions to match value against
     * 
     * @param deny     deny permissions to match value against
     */
    public void setDeny(String deny)
    {
       this.deny = deny;
    }
    
    
    /** the deny permissions to match value against */
    private String deny = null;
    
    /** the allow permissions to match value against */
    private String allow = null;
}

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
package org.alfresco.web.bean.groups;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.MessageUtil;

public class CreateGroupDialog extends BaseDialogBean
{
   private static final long serialVersionUID = -8074475974375860695L;
   
   protected String parentGroup;
   protected String parentGroupName;
   protected String name;
   
   /** The AuthorityService to be used by the bean */
   transient private AuthorityService authService;
   
   private static final String MSG_ERR_EXISTS = "groups_err_exists";
   private static final String MSG_ERR_NAME = "groups_err_group_name";
   private static final String MSG_ROOT_GROUPS = "root_groups";
   private static final String MSG_BUTTON_NEW_GROUP = "new_group";

   
   // ------------------------------------------------------------------------------
   // Dialog implementation
   
   @Override
   public void init(Map<String, String> parameters)
   {
      super.init(parameters);

      // retrieve parameters
      this.parentGroup = parameters.get(GroupsDialog.PARAM_GROUP);
      this.parentGroupName = parameters.get(GroupsDialog.PARAM_GROUP_NAME);
      
      // reset variables
      this.name = null;
   }

   @Override
   protected String finishImpl(FacesContext context, String outcome) throws Exception
   {
       // create new Group using Authentication Service
       authService = getAuthService();
       String newAuthorityName = authService.getName(AuthorityType.GROUP, name);
       for (String authorityName : authService.getAllAuthorities(AuthorityType.GROUP)) {
           if (StringUtils.equalsIgnoreCase(newAuthorityName, authorityName)
                   || StringUtils.equalsIgnoreCase(name, authService.getAuthorityDisplayName(authorityName))) {
               MessageUtil.addErrorMessage(MSG_ERR_EXISTS);
               isFinished = false;
               return null;
           }
       }
      String groupName = this.getAuthService().getName(AuthorityType.GROUP, this.name);
         this.getAuthService().createAuthority(AuthorityType.GROUP, this.name);
         if (StringUtils.isNotBlank(this.parentGroup))
         {
             this.getAuthService().addAuthority(this.parentGroup, groupName);
         }
         MessageUtil.addInfoMessage("save_success");
      return outcome;
   }

   @Override
   public String getFinishButtonLabel()
   {
      return MessageUtil.getMessage(MSG_BUTTON_NEW_GROUP);
   }

   @Override
   public String getContainerSubTitle()
   {
      String subtitle = null;

      if (this.parentGroupName != null)
      {
         subtitle = this.parentGroupName;
      }
      else
      {
         subtitle = MessageUtil.getMessage(MSG_ROOT_GROUPS);
      }

      return subtitle;
   }
   
   
   // ------------------------------------------------------------------------------
   // Bean property getters and setters
   
   public String getName()
   {
      return this.name;
   }

   public void setName(String name)
   {
       this.name = StringUtils.strip(name);
   }
   
   public void setAuthService(AuthorityService authService)
   {
      this.authService = authService;
   }
   
   /**
    * @return the authService
    */
   protected AuthorityService getAuthService()
   {
      if (authService == null)
      {
         authService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getAuthorityService();
      }
      return authService;
   }
   
   
   // ------------------------------------------------------------------------------
   // Helpers

   public void validateGroupName(FacesContext context, UIComponent component, Object value) throws ValidatorException
   {
      String name = (String) value;
      
      if (name.indexOf('"') != -1 || name.indexOf('\\') != -1)
      {
          String err = MessageFormat.format(MessageUtil.getMessage(MSG_ERR_NAME), 
                  new Object[] { "\", \\" });
         throw new ValidatorException(new FacesMessage(err));
      }
   }
}

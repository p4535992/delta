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
package org.alfresco.web.bean.forums;

import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.model.ForumModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.spaces.DeleteSpaceDialog;

/**
 * Bean implementation for the "Delete Topic" dialog
 * 
 * @author gavinc
 */
public class DeleteTopicDialog extends DeleteSpaceDialog
{
   private static final long serialVersionUID = 548182341698381545L;
   
   protected boolean reDisplayTopics;

   // ------------------------------------------------------------------------------
   // Dialog implementation
   
   @Override
   public void init(Map<String, String> parameters)
   {
      super.init(parameters);
      
      // reset the reDisplayTopics flag
      this.reDisplayTopics = false;
   }

   @Override
   protected String finishImpl(FacesContext context, String outcome) throws Exception
   {
      // find out what the parent type of the node being deleted 
      Node node = this.browseBean.getActionSpace();
      if(node == null) {
          node = this.navigator.getCurrentNode();
      }
      ChildAssociationRef assoc = this.getNodeService().getPrimaryParent(node.getNodeRef());
      if (assoc != null)
      {
         NodeRef parent = assoc.getParentRef();
         QName parentType = this.getNodeService().getType(parent);
         if (parentType.equals(ForumModel.TYPE_FORUM))
         {
            this.reDisplayTopics = true;
         }
      }

      return super.finishImpl(context, outcome);
   }

   @Override
   protected String doPostCommitProcessing(FacesContext context, String outcome)
   {
      outcome = super.doPostCommitProcessing(context, outcome);
      navigator.setCurrentNodeId(((ForumsBean) FacesHelper.getManagedBean(context, "ForumsBean")).getForumId()); // Set context back to forum
      return AlfrescoNavigationHandler.getMultipleCloseOutcome(2);
   }
   
   @Override
    public String getFinishButtonLabel() {
       return Application.getMessage(FacesContext.getCurrentInstance(), "delete");
    }
   
   /**
    * Returns the message bundle id of the confirmation message to display to 
    * the user before deleting the topic.
    * 
    * @return The message bundle id
    */
   @Override
   protected String getConfirmMessageId()
   {
      return "delete_topic_confirm";
   }
}

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
package org.alfresco.web.bean.content;

import java.text.MessageFormat;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.ml.MultilingualContentService;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.notification.model.NotificationModel;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * Bean implementation for the "Delete Content" dialog
 *
 * @author gavinc
 */
public class DeleteContentDialog extends BaseDialogBean
{
   private static final long serialVersionUID = 4199496011879649213L;

   transient private MultilingualContentService multilingualContentService;

   private static final Log logger = LogFactory.getLog(DeleteContentDialog.class);
   
   private static final String MSG_DELETE = "delete";

   // ------------------------------------------------------------------------------
   // Dialog implementation
   
   @Override
    public String getFinishButtonLabel() {
        return Application.getMessage(FacesContext.getCurrentInstance(), MSG_DELETE);
    }

   @Override
   protected String finishImpl(FacesContext context, String outcome)
         throws Exception
   {
      // get the content to delete
      Node node = this.browseBean.getDocument();
      if (node != null)
      {
         if(ContentModel.TYPE_MULTILINGUAL_CONTAINER.equals(node.getType()))
         {
             if (logger.isDebugEnabled())
                 logger.debug("Trying to delete multilingual container: " + node.getId() + " and its translations" );

             // delete the mlContainer and its translations
             getMultilingualContentService().deleteTranslationContainer(node.getNodeRef());
         }
         else
         {
             if (logger.isDebugEnabled())
                 logger.debug("Trying to delete content node: " + node.getId());

             // delete the node
             this.getNodeService().deleteNode(node.getNodeRef());
         }

      }
      else
      {
         logger.warn("WARNING: delete called without a current Document!");
      }

      return outcome;
   }

   @Override
   protected String doPostCommitProcessing(FacesContext context, String outcome)
   {
      // clear action context
      this.browseBean.setDocument(null);

      // setting the outcome will show the browse view again
      return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME +
             AlfrescoNavigationHandler.OUTCOME_SEPARATOR + "browse";
   }

   @Override
   protected String getErrorMessageId()
   {
      return "error_delete_file";
   }

   @Override
   public boolean getFinishButtonDisabled()
   {
      return false;
   }

   // ------------------------------------------------------------------------------
   // Bean Getters and Setters

   /**
    * Returns the confirmation to display to the user before deleting the content.
    *
    * @return The formatted message to display
    */
   public String getConfirmMessage()
   {
      String fileConfirmMsg = null;

      Node document = this.browseBean.getDocument();
      String documentName = document.getName();

      if(document.getType().equals(ContentModel.TYPE_MULTILINGUAL_CONTAINER))
      {
          fileConfirmMsg = Application.getMessage(FacesContext.getCurrentInstance(),
              "delete_ml_container_confirm");
      }
      else if(document.hasAspect(ContentModel.ASPECT_MULTILINGUAL_EMPTY_TRANSLATION))
      {
          fileConfirmMsg = Application.getMessage(FacesContext.getCurrentInstance(),
              "delete_empty_translation_confirm");
      }
      else if(document.hasAspect(ContentModel.ASPECT_MULTILINGUAL_DOCUMENT))
      {
          fileConfirmMsg = Application.getMessage(FacesContext.getCurrentInstance(),
              "delete_translation_confirm");
      }
      else if (document.getType().equals(WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION)) {
          fileConfirmMsg = MessageUtil.getMessage("delete_compound_workflow_confirm");
          documentName = (String)document.getProperties().get(WorkflowCommonModel.Props.NAME);
      }
      else if (document.getType().equals(NotificationModel.Types.GENERAL_NOTIFICATION)) {
          // FIXME: kaarel - see küll õige koht ei tohiks olla.. pigem DeleteFileDialog'is
          fileConfirmMsg = MessageUtil.getMessage("notification_delete_notification_confirm");
          // FIXME: kaarel - mis on tähtsa teate kustutamisel pistmist töövoogudega?
          documentName = (String)document.getProperties().get(WorkflowCommonModel.Props.NAME);
      }
      else
      {
          String strHasMultipleParents = this.parameters.get("hasMultipleParents");
          if (strHasMultipleParents != null && "true".equals(strHasMultipleParents))
          {
             fileConfirmMsg = Application.getMessage(FacesContext.getCurrentInstance(),
                "delete_file_multiple_parents_confirm");
          }
          else
          {
             fileConfirmMsg = Application.getMessage(FacesContext.getCurrentInstance(),
                 "delete_file_confirm");
          }
      }

      return MessageFormat.format(fileConfirmMsg, new Object[] {documentName});
   }
   
   @Override
    public String getContainerTitle() {
       String title = null;
       Node document = this.browseBean.getDocument();
       
       if (document.getType().equals(NotificationModel.Types.GENERAL_NOTIFICATION)) {
           title = MessageUtil.getMessage("notification_delete_notification");
       }
       else if (document.getType().equals(WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION)) {
           title = MessageUtil.getMessage("delete_compound_workflow");
       }
       
        return title;
    }
   

   /**
   * @param multilingualContentService the Multilingual Content Service to set
   */
   public void setMultilingualContentService(MultilingualContentService multilingualContentService)
   {
       this.multilingualContentService = multilingualContentService;
   }
   
   protected MultilingualContentService getMultilingualContentService()
   {
      if (multilingualContentService == null)
      {
         multilingualContentService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getMultilingualContentService();
      }
      return multilingualContentService;
   }

}

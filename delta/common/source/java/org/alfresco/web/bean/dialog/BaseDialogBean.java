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
package org.alfresco.web.bean.dialog;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.context.UIContextService;
import org.alfresco.web.bean.BrowseBean;
import org.alfresco.web.bean.NavigationBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.config.DialogsConfigElement.DialogButtonConfig;
import org.alfresco.web.ui.common.ReportedException;
import org.alfresco.web.ui.common.Utils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.MessageData;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformMultiReasonException;

/**
 * Base class for all dialog beans providing common functionality
 * 
 * @author gavinc
 */
public abstract class BaseDialogBean implements IDialogBean, Serializable
{
   private static final long serialVersionUID = 1L;
   protected Map<String, String> parameters;
   protected boolean isFinished = false;
   
   // services common to most dialogs
   protected BrowseBean browseBean;
   protected NavigationBean navigator;
   
   transient private TransactionService transactionService;
   transient private NodeService nodeService;
   transient private FileFolderService fileFolderService;
   transient private SearchService searchService;
   transient private DictionaryService dictionaryService;
   transient private NamespaceService namespaceService;

   private Map<String, Object> customAttributes = new HashMap<String, Object>();

   public Object getCustomAttribute(String key) {
       return customAttributes.get(key);
   }

   public void addCustomAttribute(String key, Object value) {
       customAttributes.put(key, value);
   }

   private void clearCustomAttributes() {
       customAttributes = new HashMap<String, Object>();
   }
   
   public void init(Map<String, String> parameters)
   {
      // tell any beans to update themselves so the UI gets refreshed
      UIContextService.getInstance(FacesContext.getCurrentInstance()).notifyBeans();
      
      // store the parameters, create empty map if necessary
      this.parameters = parameters;
      
      if (this.parameters == null)
      {
         this.parameters = Collections.<String, String>emptyMap();
      }
      
      // reset the isFinished flag
      isFinished = false;
   }
   
   public void restored()
   {
      // do nothing by default, subclasses can override if necessary
   }
   
   public String cancel()
   {
      // remove container variable
      FacesContext.getCurrentInstance().getExternalContext().getSessionMap().remove(
                AlfrescoNavigationHandler.EXTERNAL_CONTAINER_SESSION);
      clearCustomAttributes();
      return getDefaultCancelOutcome();
   }
   
   public String finish()
   {
      final FacesContext context = FacesContext.getCurrentInstance();
      final String defaultOutcome = getDefaultFinishOutcome();
      String outcome = null;
      
      // check the isFinished flag to stop the finish button
      // being pressed multiple times
      if (isFinished == false)
      {
         isFinished = true;
      
         RetryingTransactionHelper txnHelper = Repository.getRetryingTransactionHelper(context);
         RetryingTransactionCallback<String> callback = new RetryingTransactionCallback<String>()
         {
            public String execute() throws Throwable
            {
                // call the actual implementation
                return finishImpl(context, defaultOutcome);
            }
         };
         try
         {
            // Execute
            outcome = txnHelper.doInTransaction(callback, false, true);
            
            // allow any subclasses to perform post commit processing 
            // i.e. resetting state or setting status messages
            outcome = doPostCommitProcessing(context, outcome);
            
            // remove container variable
            context.getExternalContext().getSessionMap().remove(
                    AlfrescoNavigationHandler.EXTERNAL_CONTAINER_SESSION);
            clearCustomAttributes();

            if (outcome == null) {
                isFinished = false;
            }
         } catch (UnableToPerformException e) {
             outcome = handleException(e);
         } catch (UnableToPerformMultiReasonException e) {
             outcome = handleException(e);
         }
         catch (Throwable e)
         {
            outcome = handleException(e);
         }
      }
      else
      {
         Utils.addErrorMessage(Application.getMessage(context, "error_wizard_completed_already"));
      }
      
      return outcome;
   }

    public String handleException(Throwable e) {
        String outcome;
        // reset the flag so we can re-attempt the operation
        isFinished = false;
        Throwable cause = e.getCause();
        if(cause!=null && (e instanceof UnableToPerformException || e instanceof UnableToPerformMultiReasonException)) {
            e = cause;
        }
        outcome = getErrorOutcome(e);
        if (e instanceof UnableToPerformException) {
            MessageUtil.addStatusMessage((MessageData) e);
        } else if (e instanceof UnableToPerformMultiReasonException) {
            MessageUtil.addStatusMessages(FacesContext.getCurrentInstance(), ((UnableToPerformMultiReasonException) e).getMessageDataWrapper());
        } else 
        if (outcome == null && e instanceof ReportedException == false)
        {
            Utils.addErrorMessage(formatErrorMessage(e), e);
        }
        ReportedException.throwIfNecessary(e);
        return outcome;
    }
   
   public boolean isFinished()
   {
      return isFinished;
   }
   
   public List<DialogButtonConfig> getAdditionalButtons()
   {
      // none by default, subclasses can override if necessary
      
      return null;
   }

   public String getCancelButtonLabel()
   {
      return Application.getMessage(FacesContext.getCurrentInstance(), "back_button");
   }

   public String getFinishButtonLabel()
   {
      return Application.getMessage(FacesContext.getCurrentInstance(), "ok");
   }
   
   public boolean getFinishButtonDisabled()
   {
      return true;
   }

   @Override
    public boolean isFinishButtonVisible(boolean dialogConfOKButtonVisible) {
        return dialogConfOKButtonVisible;
    }

   public String getContainerTitle()
   {
      // nothing by default, subclasses can override if necessary
      
      return null;
   }
   
   public String getContainerSubTitle()
   {
      // nothing by default, subclasses can override if necessary
      
      return null;
   }
   
   public String getContainerDescription()
   {
      // nothing by default, subclasses can override if necessary
      
      return null;
   }
   
   public Object getActionsContext()
   {
      // dialog implementations can override this method to return the
      // appropriate object for their use case
      
      return null;
   }

   public String getActionsConfigId()
   {
      // nothing by default, subclasses can override if necessary
      
      return null;
   }

   public String getMoreActionsConfigId()
   {
      // nothing by default, subclasses can override if necessary
      
      return null;
   }

   /**
    * @param browseBean The BrowseBean to set.
    */
   public void setBrowseBean(BrowseBean browseBean)
   {
      this.browseBean = browseBean;
   }
   
   /**
    * @param navigator The NavigationBean to set.
    */
   public void setNavigator(NavigationBean navigator)
   {
      this.navigator = navigator;
   }
   
   protected TransactionService getTransactionService()
   {
      if (transactionService == null)
      {
         transactionService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getTransactionService();
      }
      return transactionService;
   }
   
   /**
    * @param nodeService The nodeService to set.
    */
   public void setNodeService(NodeService nodeService)
   {
      this.nodeService = nodeService;
   }
   
   protected NodeService getNodeService()
   {
      if (nodeService == null)
      {
         nodeService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getNodeService();
      }
      return nodeService;
   }
   
   /**
    * @param fileFolderService used to manipulate folder/folder model nodes
    */
   public void setFileFolderService(FileFolderService fileFolderService)
   {
      this.fileFolderService = fileFolderService;
   }
   
   protected FileFolderService getFileFolderService()
   {
      if (fileFolderService == null)
      {
         fileFolderService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getFileFolderService();
      }
      return fileFolderService;
   }

   /**
    * @param searchService the service used to find nodes
    */
   public void setSearchService(SearchService searchService)
   {
      this.searchService = searchService;
   }
   
   protected SearchService getSearchService()
   {
      if (searchService == null)
      {
         searchService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getSearchService();
      }
      return searchService;
   }
   
   /**
    * Sets the dictionary service
    * 
    * @param dictionaryService  the dictionary service
    */
   public void setDictionaryService(DictionaryService dictionaryService)
   {
      this.dictionaryService = dictionaryService;
   }
   
   protected DictionaryService getDictionaryService()
   {
      if (dictionaryService == null)
      {
         dictionaryService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getDictionaryService();
      }
      return dictionaryService;
   }
   
   /**
    * @param namespaceService The NamespaceService
    */
   public void setNamespaceService(NamespaceService namespaceService)
   {
      this.namespaceService = namespaceService;
   }
   
   protected NamespaceService getNamespaceService()
   {
      if (namespaceService == null)
      {
         namespaceService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getNamespaceService();
      }
      return namespaceService;
   }
   
   /**
    * Returns the default cancel outcome
    * 
    * @return Default close outcome, dialog:close by default
    */
   protected String getDefaultCancelOutcome()
   {
      return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
   }

   public static String getCloseOutcome(Integer dialogsToClose) {
       if (dialogsToClose == null || dialogsToClose == 0) {
           return null;
       }
       if (dialogsToClose == 1) {
           return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
       } else if (dialogsToClose > 1) {
           return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME+"[" + dialogsToClose + "]";
       } else {
           throw new IllegalArgumentException("Can't close " + dialogsToClose + " dialogs");
       }
   }
   
   /**
    * Returns the default finish outcome
    * 
    * @return Default finish outcome, dialog:close by default
    */
   protected String getDefaultFinishOutcome()
   {
      return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
   }
   
   /**
    * Performs the actual processing for the wizard.
    * NOTE: This method is called within the context of a transaction
    * so no transaction handling is required
    * 
    * @param context FacesContext
    * @param outcome The default outcome
    * @return The outcome
    */
   protected abstract String finishImpl(FacesContext context, String outcome)
      throws Throwable;

   /**
    * Performs any post commit processing subclasses may want to provide
    * 
    * @param context FacesContext
    * @param outcome The default outcome
    * @return The outcome
    */
   protected String doPostCommitProcessing(FacesContext context, String outcome)
   {
      // do nothing by default, subclasses can override if necessary
      
      return outcome;
   }
   
   /**
    * The default message id to use in error messages
    * 
    * @return The error message lookup id
    */
   protected String getErrorMessageId()
   {
      return Repository.ERROR_GENERIC;
   }
   
   /**
    * The outcome to return if the given exception occurs
    * 
    * @param exception The exception that got thrown
    * @return The error outcome, null by default
    */
   protected String getErrorOutcome(Throwable exception)
   {
      return null;
   }
   
   /**
    * Returns a formatted exception string for the given exception
    * 
    * @param exception The exception that got thrown
    * @return The formatted message
    */
   protected String formatErrorMessage(Throwable exception)
   {
      return MessageFormat.format(Application.getMessage(
            FacesContext.getCurrentInstance(), getErrorMessageId()), 
            exception.getMessage());
   }

   public static void validatePermission(Node documentNode, String permission) {
       validatePermission(documentNode, null, permission);
   }

   public static void validatePermission(Node documentNode, String errMsg, String permission) {
       if (errMsg == null) {
           errMsg = "action_failed_missingPermission";
       }
       if (!documentNode.hasPermission(permission)) {
           throw new UnableToPerformException(errMsg, new MessageDataImpl("permission_" + permission));
       }
   }

   public static void validatePermission(NodeRef documentNodeRef, String permission) {
       if (!hasPermission(documentNodeRef, permission)) {
           UnableToPerformException e = new UnableToPerformException("action_failed_missingPermission_" + permission, new MessageDataImpl("permission_" + permission));
           e.setFallbackMessage(new MessageDataImpl("action_failed_missingPermission", new MessageDataImpl("permission_" + permission)));
           throw e;
       }
   }

   public static boolean hasPermission(NodeRef nodeRef, String permission) {
       return AccessStatus.ALLOWED == BeanHelper.getPermissionService().hasPermission(nodeRef, permission);
   }

}

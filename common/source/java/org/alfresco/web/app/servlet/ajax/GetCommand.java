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
package org.alfresco.web.app.servlet.ajax;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.ValueBinding;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.web.bean.repository.Repository;

/**
 * Command that executes the given value binding expression.
 * <p>
 * This command is intended to be used for calling existing managed 
 * bean methods. The result of the value binding is added to
 * the response as is i.e. by calling toString(). 
 * The content type of the response is always text/html.
 * 
 * @author gavinc
 */
public class GetCommand extends BaseAjaxCommand
{
   @Override
   public void execute(final FacesContext facesContext, String expression,
         HttpServletRequest request, final HttpServletResponse response)
         throws ServletException, IOException
   {
      // create the JSF binding expression
      String bindingExpr = makeBindingExpression(expression);
      
      if (logger.isDebugEnabled())
         logger.debug("Retrieving value from value binding: " + bindingExpr);
      
      try
      {
         // create the value binding
         final ValueBinding binding = facesContext.getApplication().
               createValueBinding(bindingExpr);
         
         if (binding != null)
         {
            // setup the transaction
            RetryingTransactionHelper txnHelper = Repository.getRetryingTransactionHelper(FacesContext.getCurrentInstance());
            RetryingTransactionCallback<Object> callback = new RetryingTransactionCallback<Object>()
            {
               @Override
               public Object execute() throws Throwable
               {
                  // get the value from the value binding
                  Object value = binding.getValue(facesContext);
                  if (value != null)
                  {
                     response.getWriter().write(value.toString());
                  }
                  return null;
               }
            };
            txnHelper.doInTransaction(callback, true);
         }
      }
      catch (Throwable err)
      {
         if (err instanceof EvaluationException)
         {
            final Throwable cause = ((EvaluationException)err).getCause();
            if (cause != null)
            {
               err = cause;
            }
         }
         else if (err instanceof InvocationTargetException)
         {
            final Throwable cause = ((InvocationTargetException)err).getCause();
            if (cause != null)
            {
               err = cause;
            }
         }

         logger.error("Failed to retrieve value " + expression + ": " + err.getMessage(), err);
         throw new AlfrescoRuntimeException("Failed to retrieve value " + expression + ": " + err.getMessage(), err);
      }
   }
}

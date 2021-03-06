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

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.web.action.ActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.component.evaluator.BaseEvaluator;

import ee.webmedia.alfresco.common.evaluator.EvaluatorSharedResource;
import ee.webmedia.alfresco.common.evaluator.SharedResourceEvaluator;
import ee.webmedia.alfresco.common.model.NodeBaseVO;

/**
 * Evaluator for executing an ActionEvaluator instance.
 * 
 * @author Kevin Roast
 */
public class ActionInstanceEvaluator extends BaseEvaluator
{
   private static final String EVALUATOR_CACHE = "_alf_evaluator_cache";
   
   private EvaluatorSharedResource<Serializable> evaluatorSharedResource;

   public void setSharedResource(EvaluatorSharedResource<Serializable> evaluatorSharedResource) {
       this.evaluatorSharedResource = evaluatorSharedResource;
   }
   
   /**
    * Evaluate by executing the specified action instance evaluator.
    * 
    * @return true to allow rendering of child components, false otherwise
    */
   public boolean evaluate()
   {
      boolean result = false;
      
      try
      {
          // TODO: always try to cache evaluator result because evaluators are always called twice.
          // Once for upper titlebar and second time for footer-titlebar where all the buttons
          // are dubbed. Evaluators are called regardless if the footer-titlebar is visible or not.
          final Object obj = getValue();
          if (obj instanceof Node)
          {
              String cacheKeyPrefix = ((Node) obj).getNodeRef().toString();
              result = evaluateCachedResult((Node) obj, cacheKeyPrefix);
          }
          else if (obj instanceof NodeBaseVO && ((NodeBaseVO) obj).getNodeRef() != null) {
              String cacheKeyPrefix = ((NodeBaseVO) obj).getNodeRef().toString();
              result = evaluateCachedResult((NodeBaseVO) obj, cacheKeyPrefix);
          }
          else
          {
              result = getEvaluator().evaluate(obj);
          }
      }
      catch (Exception err)
      {
         handleException(err);
      }
      
      return result;
   }
   
    public boolean evaluate(final Object obj) {
        try {
            ActionEvaluator evaluator2 = getEvaluator();
            return obj instanceof Node ? evaluator2.evaluate((Node) obj) : evaluator2.evaluate(obj);
        } catch (Exception err) {
            handleException(err);
        }
        return false;
    }  

    private void handleException(Exception err) {
        // return default value on error and report meaningful error
         StringBuilder builder = new StringBuilder("Error during ActionInstanceEvaluator evaluation of ");
         builder.append(this.getEvaluator()).append(": ");
         String msg = err.getMessage();
         if (msg != null)
         {
            builder.append(msg);
         }
         else
         {
            StringWriter strWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(strWriter);
            err.printStackTrace(writer);
            builder.append(strWriter.toString());
         }
         
         s_logger.warn(builder.toString());
    }
   
   /**
    * To reduce invocations of a particular evaluator for a particular node
    * save a cache of evaluator result for a node against the current request.
    * Since the same evaluator may get reused several times for multiple actions, but
    * in effect execute against the same node instance, this can significantly reduce
    * the number of invocations required for a particular evaluator. 
    * 
    * @param node Node to evaluate against
    * 
    * @return evaluator result
    */
    private boolean evaluateCachedResult(Serializable obj, String cacheKeyPrefix)
    {
        Boolean result;

        ActionEvaluator evaluator = getEvaluator();
        String cacheKey = cacheKeyPrefix + '_' + evaluator.getClass().getName();
        Map<String, Boolean> cache = getEvaluatorResultCache();
        result = cache.get(cacheKey);
        if (result == null)
        {
            if (evaluator instanceof SharedResourceEvaluator && evaluatorSharedResource != null) {
                SharedResourceEvaluator sharedEvaluator = (SharedResourceEvaluator) evaluator;
                evaluatorSharedResource.setObject(obj);
                sharedEvaluator.setSharedResource(evaluatorSharedResource);
                result = sharedEvaluator.evaluate();
            } else if (obj instanceof Node) {
                result = evaluator.evaluate((Node) obj);
            } else {
                result = evaluator.evaluate(obj);
            }
            cache.put(cacheKey, result);
        }
        return result;
    }

   /**
    * @return the evaluator result cache - tied to the current request
    */
   private Map<String, Boolean> getEvaluatorResultCache()
   {
      FacesContext fc = FacesContext.getCurrentInstance();
      Map<String, Boolean> cache = (Map<String, Boolean>)fc.getExternalContext().getRequestMap().get(EVALUATOR_CACHE);
      if (cache == null)
      {
         cache = new HashMap<String, Boolean>(64, 1.0f);
         fc.getExternalContext().getRequestMap().put(EVALUATOR_CACHE, cache);
      }
      return cache;
   }
   
   /**
    * @see javax.faces.component.StateHolder#restoreState(javax.faces.context.FacesContext, java.lang.Object)
    */
   public void restoreState(FacesContext context, Object state)
   {
      Object values[] = (Object[])state;
      // standard component attributes are restored by the super class
      super.restoreState(context, values[0]);
      this.evaluator = (ActionEvaluator)values[1];
      this.evaluatorClassName = (String)values[2];
   }
   
   /**
    * @see javax.faces.component.StateHolder#saveState(javax.faces.context.FacesContext)
    */
   public Object saveState(FacesContext context)
   {
      return new Object[] {
         // standard component attributes are saved by the super class
         super.saveState(context),
         this.evaluator,
         this.evaluatorClassName 
      };
   }
   
   /** 
    * @return the ActionEvaluator to execute
    */
   public ActionEvaluator getEvaluator()
   {
      if (this.evaluator == null)
      {
         Object objEvaluator;
         try
         {
            Class clazz = Class.forName(getEvaluatorClassName());
            objEvaluator = clazz.newInstance();
         }
         catch (Throwable err)
         {
            throw new AlfrescoRuntimeException("Unable to construct action evaluator: " + getEvaluatorClassName());
         }
         if (objEvaluator instanceof ActionEvaluator == false)
         {
            throw new AlfrescoRuntimeException("Must implement ActionEvaluator interface: " + getEvaluatorClassName());
         }
         this.evaluator = (ActionEvaluator)objEvaluator;
      }
      return this.evaluator;
   }
   
   /**
    * @param evaluator     The ActionEvaluator to execute
    */
   public void setEvaluator(ActionEvaluator evaluator)
   {
      this.evaluator = evaluator;
   }
   
   /**
    * @return the evaluatorClassName
    */
   public String getEvaluatorClassName()
   {
      ValueBinding vb = getValueBinding("evaluatorClassName");
      if (vb != null)
      {
         this.evaluatorClassName = (String)vb.getValue(getFacesContext());
      }
      return this.evaluatorClassName;
   }

   /**
    * @param evaluatorClassName the evaluatorClassName to set
    */
   public void setEvaluatorClassName(String evaluatorClassName)
   {
      this.evaluatorClassName = evaluatorClassName;
   }
   
   private ActionEvaluator evaluator;
   private String evaluatorClassName;
}

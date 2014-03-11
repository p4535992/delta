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
package org.apache.myfaces.application;

import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.NavigationHandler;
import javax.faces.component.ActionSource;
import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.MethodBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;

import ee.webmedia.alfresco.common.listener.StatisticsPhaseListenerLogColumn;
import ee.webmedia.alfresco.common.listener.StatisticsPhaseListener;


/**
 * @author Manfred Geiler (latest modification by $Author: grantsmith $)
 * @author Anton Koinov
 * @version $Revision: 472618 $ $Date: 2006-11-08 21:06:54 +0100 (Mi, 08 Nov 2006) $
 */
public class ActionListenerImpl
    implements ActionListener
{
    public void processAction(ActionEvent actionEvent) throws AbortProcessingException
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Application application = facesContext.getApplication();

        ActionSource actionSource = (ActionSource)actionEvent.getComponent();
        MethodBinding methodBinding = actionSource.getAction();

        String fromAction;
        String outcome;
        if (methodBinding == null)
        {
            fromAction = null;
            outcome = null;
        }
        else
        {
            fromAction = methodBinding.getExpressionString();
            long startTime = System.currentTimeMillis();
            try
            {
                outcome = (String) methodBinding.invoke(facesContext, null);
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
                    throw new FacesException("Error calling action method of component with id " + actionEvent.getComponent().getClientId(facesContext), e);
                }
            }
            catch (RuntimeException e)
            {
                throw new FacesException("Error calling action method of component with id " + actionEvent.getComponent().getClientId(facesContext), e);
            }
            finally
            {
                if (fromAction != null) {
                    long duration = System.currentTimeMillis() - startTime;
                    StatisticsPhaseListener.add(StatisticsPhaseListenerLogColumn.ACTION, duration + "," + methodBinding.getExpressionString());
                }
            }
        }

        long startTime = System.currentTimeMillis();
        NavigationHandler navigationHandler = application.getNavigationHandler();
        navigationHandler.handleNavigation(facesContext,
                                           fromAction,
                                           outcome);
        long duration = System.currentTimeMillis() - startTime;
        StatisticsPhaseListener.add(StatisticsPhaseListenerLogColumn.OUTCOME, duration + "," + outcome);
		//Render Response if needed
		facesContext.renderResponse();

    }
}

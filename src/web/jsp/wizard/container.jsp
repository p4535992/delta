<%--
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 
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
 * http://www.alfresco.com/legal/licensing
--%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>
<%@ page import="org.alfresco.web.app.Application" %>
<%@ page import="org.alfresco.web.ui.common.PanelGenerator" %>

<r:page title="<%=Application.getWizardManager().getTitle() %>"
        doctypeRootElement="html"
        doctypePublic="-//W3C//DTD HTML 4.01 Transitional//EN"
        doctypeSystem="http://www.w3c.org/TR/html4/loose.dtd">
  <f:view>
   
   <%-- load a bundle of properties with I18N strings --%>
   <r:loadBundle var="msg"/>

   <h:form acceptcharset="UTF-8" id="wizard">

      <%-- Title bar --%>
      <%@ include file="../parts/titlebar.jsp"%>

      <%-- Main area --%>
      <a:panel id="container">

      <%-- Shelf --%> 
      <%@ include file="../parts/shelf.jsp"%> 

      <%-- Work Area --%>
      <a:panel id="content">

      <%-- Breadcrumb --%>
      <%@ include	file="../parts/breadcrumb.jsp"%>

      <%-- Status and Actions --%> <%-- Status and Actions inner contents table --%>
      <%-- Generally this consists of an icon, textual summary and actions for the current object --%>
      <a:panel id="titlebar">
         <a:panel id="titlebar-wizard-buttons-panel">
            <h:commandButton id="cancel-button" styleClass="wizardButton" value="#{WizardManager.cancelButtonLabel}" action="#{WizardManager.cancel}"
               immediate="true" />
            <f:verbatim>&nbsp;</f:verbatim>
            <h:commandButton id="back-button" styleClass="wizardButton" value="#{WizardManager.backButtonLabel}" action="#{WizardManager.back}"
               disabled="#{WizardManager.backButtonDisabled}" />
            <f:verbatim>&nbsp;</f:verbatim>
            <h:commandButton id="next-button" styleClass="wizardButton" value="#{WizardManager.nextButtonLabel}" action="#{WizardManager.next}"
               disabled="#{WizardManager.nextButtonDisabled}" />
            <f:verbatim>&nbsp;</f:verbatim>
            <h:commandButton id="finish-button" styleClass="wizardButton" value="#{WizardManager.finishButtonLabel}" action="#{WizardManager.finish}"
               disabled="#{WizardManager.finishButtonDisabled}" />
         </a:panel>
         
         <f:verbatim>
         <h2 class="title-icon">
         </f:verbatim>
            <h:graphicImage id="wizard-logo" url="#{WizardManager.icon}" alt="" />
            <h:outputText value="#{WizardManager.title}" />
         <f:verbatim>
         </h2>
         </f:verbatim>
         <%--
         <a:panel id="description">
            <p><h:outputText value="#{WizardManager.subTitle}" /></p>
            <p><h:outputText value="#{WizardManager.description}" /></p>
         </a:panel>
          --%>

         <a:panel id="actions">
            <f:verbatim><span></f:verbatim>
               <h:outputText value="#{msg.steps}:" />
            <f:verbatim></span></f:verbatim>
            <a:modeList itemSpacing="3" iconColumnWidth="2" styleClass="wizard-steps" selectedStyleClass="current" value="#{WizardManager.currentStepAsString}"
            disabled="true">
               <a:listItems value="#{WizardManager.stepItems}" />
            </a:modeList>
         </a:panel>
      </a:panel>

      <%-- Details --%>
      <a:errors message="#{WizardManager.errorMessage}" styleClass="message" errorClass="error-message" infoClass="info-message" />
      <a:panel id="wizard-column-wrapper" styleClass="column-wrapper">
         <a:panel id="wizard-step-body" label="#{WizardManager.stepTitle}" styleClass="column panel-90">
            <f:verbatim>
            <p class="wizard-step-description">
            </f:verbatim>
               <h:outputText value="#{WizardManager.stepDescription}" />
            <f:verbatim>
            </p>
            </f:verbatim>

            <f:subview id="wizard-body">
               <jsp:include page="<%=Application.getWizardManager().getPage() %>" />
            </f:subview>

            <f:verbatim>
            <p class="wizard-step-instructions">
            </f:verbatim>
               <h:outputText value="#{WizardManager.stepInstructions}" />
            <f:verbatim>
            </p>
            </f:verbatim>
         </a:panel>
         
      </a:panel>
      </a:panel>
      <f:verbatim><div class="clear"></div></f:verbatim>
      </a:panel>
      <%@ include file="../parts/footer.jsp"%>
      </h:form>
  </f:view>
</r:page>

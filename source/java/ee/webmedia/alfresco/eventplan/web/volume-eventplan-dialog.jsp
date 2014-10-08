<<<<<<< HEAD
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ page import="ee.webmedia.alfresco.utils.MessageUtil"%>


<a:panel id="eventplan-volume-panel" label="#{msg.eventplan_volume_plan_title}" styleClass="panel-100 edit-mode">
   <r:propertySheetGrid id="eventplan-volume-props" value="#{VolumeEventPlanDialog.predefinedPlan.node}" columns="1"
      mode="#{VolumeEventPlanDialog.inEditMode ? 'edit' : 'view'}" externalConfig="true" labelStyleClass="propertiesLabel no-wrap" />
</a:panel>

<a:panel id="eventplan-volume-data-panel" label="#{msg.eventplan_volume_data_title}" styleClass="panel-100 edit-mode">
   <r:propertySheetGrid id="eventplan-volume-data-props" value="#{VolumeEventPlanDialog.plan.node}" columns="1"
      mode="#{VolumeEventPlanDialog.inEditMode ? 'edit' : 'view'}" externalConfig="true" labelStyleClass="propertiesLabel no-wrap"
      configArea="volumeEventPlan" var="node2" />
</a:panel>
=======
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ page import="ee.webmedia.alfresco.utils.MessageUtil"%>


<a:panel id="eventplan-volume-panel" label="#{msg.eventplan_volume_plan_title}" styleClass="panel-100 edit-mode">
   <r:propertySheetGrid id="eventplan-volume-props" value="#{VolumeEventPlanDialog.predefinedPlan.node}" columns="1"
      mode="#{VolumeEventPlanDialog.inEditMode ? 'edit' : 'view'}" externalConfig="true" labelStyleClass="propertiesLabel no-wrap" />
</a:panel>

<a:panel id="eventplan-volume-data-panel" label="#{msg.eventplan_volume_data_title}" styleClass="panel-100 edit-mode">
   <r:propertySheetGrid id="eventplan-volume-data-props" value="#{VolumeEventPlanDialog.plan.node}" columns="1"
      mode="#{VolumeEventPlanDialog.inEditMode ? 'edit' : 'view'}" externalConfig="true" labelStyleClass="propertiesLabel no-wrap"
      configArea="volumeEventPlan" var="node2" />
</a:panel>
>>>>>>> develop-5.1

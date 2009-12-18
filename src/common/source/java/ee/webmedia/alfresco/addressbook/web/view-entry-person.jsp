<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a"%>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r"%>

<f:verbatim>
<script type="text/javascript">

window.onload = pageLoaded;

function pageLoaded()
{
   document.getElementById("wizard:finish-button").disabled = true;
   document.getElementById("wizard:next-button").disabled = false;
}
</script>

</f:verbatim>
<a:panel id="props-panel" styleClass="mainSubTitle" label="#{msg.addressbook_private_person_data}">
   <r:propertySheetGrid id="node-props" value="#{DialogManager.bean.currentNode}" columns="1" externalConfig="true" mode="view" />
</a:panel>

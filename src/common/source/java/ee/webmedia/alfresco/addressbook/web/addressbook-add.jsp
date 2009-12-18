<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
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

<r:propertySheetGrid id="node-props" value="#{WizardManager.bean.entry}" columns="1" externalConfig="true" mode="edit" />

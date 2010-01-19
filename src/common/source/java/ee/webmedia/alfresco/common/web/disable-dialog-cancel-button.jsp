<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<f:verbatim>
   <script type="text/javascript">
      $jQ(document).ready(function () {
         var cancelButton = $jQ('#' + escapeId4JQ('dialog:cancel-button'));
         cancelButton.remove();
      });      
   </script>
</f:verbatim>
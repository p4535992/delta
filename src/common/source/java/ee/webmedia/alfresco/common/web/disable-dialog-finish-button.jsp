<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ page buffer="64kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<f:verbatim>
   <script type="text/javascript">
      $jQ(document).ready(function(){
         var finishButton = $jQ('#' + escapeId4JQ('dialog:finish-button'));
         finishButton.remove();
      });
   </script>
</f:verbatim>

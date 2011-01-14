<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<f:verbatim>
   <script type="text/javascript">
      $jQ(document).ready(function(){
         disableAndRemoveButton('dialog:finish-button');
         disableAndRemoveButton('dialog:finish-button-2');
      });
   </script>
</f:verbatim>

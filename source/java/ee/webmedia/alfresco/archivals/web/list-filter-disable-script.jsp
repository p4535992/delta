<%@ page buffer="32kb" contentType="text/html;charset=UTF-8"%>
<%@ page isELIgnored="false"%>

<script type="text/javascript">
	$jQ(document).ready(function(){
	   disableArchiveVolumeSearchBtn();		   
   		$jQ(".volumeArchiveFilterInput").keyup(function(){
   		   disableArchiveVolumeSearchBtn();
   		});
   		$jQ(".volumeArchiveFilterInput").change(function(){
   		   disableArchiveVolumeSearchBtn();
   		});
   		$jQ(".volumeArchiveFilterInput .date").keyup(function(){
   		   disableArchiveVolumeSearchBtn();
   		});
   		$jQ(".volumeArchiveFilterInput .date").change(function(){
   		   disableArchiveVolumeSearchBtn();
   		});	   		
	});
	function disableArchiveVolumeSearchBtn(){
	   var isFilterFieldFilled = false;		   
	   $jQ(".volumeArchiveFilterInput").each(function(){
	      var inputValue = $jQ(this).val();
	      isFilterFieldFilled = isFilterFieldFilled || !isEmptyValue(inputValue);
	   });
	   if(!isFilterFieldFilled) {
		   $jQ(".volumeArchiveFilterInput .date").each(function(){
		      var inputValue = $jQ(this).val();
		      isFilterFieldFilled = isFilterFieldFilled || !isEmptyValue(inputValue);
		   });		   
	   }
   	  $jQ(".volumeArchiveFilterPanelSearch")[0].disabled = !isFilterFieldFilled;
	}
</script>
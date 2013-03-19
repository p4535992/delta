/*
 * NB! This file is supposed to be included only in test-environment.
 * You can add here functionality for assertions, but
 * DO NOT ADD HERE FUNCTIONALITY, THAT IS CRITICAL TO CORREC FUNCTIONING OF APPLICATION
 */

function outlineDates() {
   var confusingDates = $jQ(".beginDate.endDate");
   if (confusingDates.length > 0) {
      $jQ.log("found " + confusingDates.length + " confusing dates:");
      confusingDates.each(function(index, element) {
         $jQ.log(element);
         element.focus();
      });
      alert(confusingDates.length + " kuupäevalise välja puhul, mis peaksid olema kas perioodi algus või lõppkuupäev ei suudeta tuvastada, kumb see tegelikult olema peaks");
   }
   var beginDates = $jQ(".beginDate");
   beginDates.css("border-color", "green");
   beginDates.css("border-width", "3px");
   var endDates = $jQ(".endDate");
   endDates.css("border-color", "blue");
   endDates.css("border-width", "3px");
   var endDates = $jQ(".date");
   endDates.each(function(index, element) {
      var dateElem = $jQ(this);
      if (!dateElem.hasClass('beginDate') && !dateElem.hasClass('endDate')) {
         dateElem.css("border-color", "red");
         dateElem.css("border-width", "3px");
      }
   });
}
function validateDatesOnRow() {
   $jQ(".beginDate").each(function (index, element) {
      // Get the date
      var elem = $jQ(element);
      var row = elem.closest("tr");
      if (row == null) {
         $jQ.log("beginDate not in row");
         $jQ.log(elem);
         alert("antud juhu alguskuupäev ei asu tabeli reas, nagu eeldatud");
         return;
      }
   });
}
$jQ(document).ready(function() {
//   outlineDates(); // unncomment this to test beginDate endDate functionality
   validateDatesOnRow();
});


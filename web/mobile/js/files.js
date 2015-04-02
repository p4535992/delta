function deleteFile($deleteButton) {
   var deleteUrl = $deleteButton.attr("data-delete-url");
   if(deleteUrl) {
      deleteUploadedFile($deleteButton, deleteUrl);
      return;
   }
   var fileRef = $deleteButton.attr("data-file-ref");
   var taskRef = $deleteButton.attr("data-task-ref")
   var count = $deleteButton.closest("tbody").siblings("span").length;
   var prefix = "inProgressTasks['"+taskRef+"'].files["+count+"]";
   var deletedFile = $("<input name="+prefix+".deleted type='hidden' value='true'/>");
   var deletedFileRef = $("<input name="+prefix+".nodeRef type='hidden' value='"+fileRef+"'/>");
   var deletedFiles = $("<span />");
   deletedFiles.append(deletedFile);
   deletedFiles.append(deletedFileRef);
   $deleteButton.closest("table").append(deletedFiles);
   $deleteButton.closest("tr").fadeOut();
}
function deleteUploadedFile($deleteButton, deleteUrl) {
   if(!deleteUrl) {
      $deleteButton.closest("tr").remove();
      return;
   }
   $deleteButton.prop('disabled', true);
   $deleteButton.hide();
   $deleteButton.parent().spin("tiny", "#ff9000");
   $deleteButton.parent().find(".spinner").css({"position" : "relative"});
   $.ajax({
      type: 'POST',
      url: deleteUrl,
      dataType: 'json',
      mode: 'queue',
      limitConcurrentUploads: 1,
      success: function (responseText) {
         $deleteButton.closest("tr").fadeOut();
      },
      error: function() {
         $deleteButton.parent().find(".spinner").remove();
         $deleteButton.prop('disabled', false);
         $deleteButton.show();
      }
   });
}
function createDeleteButton(actions, fileDeleteUrl) {
   var deleteButton = $("<a class='remove'/>");
   deleteButton.on("click", {deleteUrl:fileDeleteUrl}, handleDeleteFile);
   actions.append(deleteButton);
}
function handleDeleteFile(event) {
   deleteUploadedFile($(this), event.data.deleteUrl);
}
function addFileUpload(taskRefId, taskRef, url) {
   // allow files to be dragged and dropped in task area but nowhere else on the screen
   $(document).bind('drop dragover', function (e) {
      e.preventDefault();
   });
   
   $('#fileupload-' + taskRefId).fileupload({
       url: url,
       dataType: 'json',
       mode: 'queue',
       limitConcurrentUploads: 1,
       dropZone: $('#task-' + taskRefId),
       beforeSend: function(xhr) {
          xhr.setRequestHeader('X-TaskRef', taskRef);
       }
   }).on('fileuploadadd', function(e, data) {
      data.context = $('<tr class="fileRow" />').appendTo('#taskFiles-' + taskRefId + ' tbody');
      $.each(data.files, function(index, file) {
         var td = $('<td/>');
         var progressBar = $('<div class="file-upload-progress"/>');
         td.append(progressBar);
         td.append($('<span/>').text(file.name));
         data.context.append(td);
         progressBar.css('height', td.css('height'));
         data.context.append($('<td class="actions"/>'));
      });
   }).on('fileuploaddone', function(e, data) {
      if(data.result == null) {
         data.context.addClass('invalid');
         createDeleteButton(data.context.find(".actions"));
      }
      $.each(data.result.files, function (index, file) {
         if(file.deleteUrl) {
            createDeleteButton(data.context.find(".actions"), file.deleteUrl);
         } else if(file.error) {
            data.context.addClass('invalid');
            createDeleteButton(data.context.find(".actions"));
         }
      });
   }).on('fileuploadfail', function (e, data) {
      data.context.addClass('invalid');
      createDeleteButton(data.context.find(".actions"));
   }).on('fileuploadprogress', function(e, data) {
      var progress = parseInt(data.loaded / data.total * 100, 10);
      var rowWidth = data.context.closest(".fileRow").css('width').replace('px','');
      var row = data.context.find(".file-upload-progress");
      var widthAttr = Math.floor(rowWidth * (progress / 100));
      row.css({'width': widthAttr + 'px'});
      if(progress == 100) {
         row.fadeOut();
      }
   });
}
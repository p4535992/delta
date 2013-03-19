var _uploads = [];

function handle_upload_helper(fileInputElement,
                              uploadId,
                              callback,
                              contextPath,
                              actionUrl,
                              params)
{
  // When file upload has begun, user sees an hourglass cursor
  $jQ(".submit-protection-layer").show().focus();
  $jQ.ajaxDestroy(); // do not allow any new AJAX requests to start
  $jQ(fileInputElement).before('Faili laetakse üles, palun oodake...');

  var id = fileInputElement.getAttribute("name");
  var d = fileInputElement.ownerDocument;
  var w = d.defaultView || d.parentWindow;
  var iframe = d.createElement("iframe");
  iframe.style.display = "none";
  iframe.name = id + "upload_frame";
  iframe.id = iframe.name;
  d.body.appendChild(iframe);

  // makes it possible to target the frame properly in ie.
  w.frames[iframe.name].name = iframe.name;

  _uploads[uploadId] = { path: fileInputElement.value, callback: callback };

  var form = d.createElement("form");
  d.body.appendChild(form);
  form.id = id + "_upload_form";
  form.name = form.id;
  form.style.display = "none";
  form.method = "post";
  form.encoding = "multipart/form-data";
  form.enctype = "multipart/form-data";
  form.target = iframe.name;
  actionUrl = actionUrl || "/uploadFileServlet";
  form.action = contextPath + actionUrl;
  form.appendChild(fileInputElement);

  var id = d.createElement("input");
  id.type = "hidden";
  form.appendChild(id);
  id.name = "upload-id";
  id.value = uploadId;

  for (var i in params)
  {
    var p = d.createElement("input");
    p.type = "hidden";
    form.appendChild(p);
    id.name = i;
    id.value = params[i];
  }

  var rp = d.createElement("input");
  rp.type = "hidden";
  form.appendChild(rp);
  rp.name = "return-page";
  if (w != window)
  {
    w.upload_complete_helper = window.upload_complete_helper;
  }

  rp.value = "javascript:window.parent.upload_complete_helper('" + uploadId + 
    "',{error: '${_UPLOAD_ERROR}', fileTypeImage: '${_FILE_TYPE_IMAGE}'})";

  form.submit();
}

function upload_complete_helper(id, args)
{
  var submitButton = $jQ("#" + escapeId4JQ("dialog:confirmFileSelectionButton"));
  if (submitButton.length) {
    $jQ('#alfFileInput_upload_form').remove();
    submitButton.click();
  } else {
    var upload = _uploads[id];
    upload.callback(id, 
                  upload.path, 
                  upload.path.replace(/.*[\/\\]([^\/\\]+)/, "$1"),
                  args.fileTypeImage,
                  args.error != "${_UPLOAD_ERROR}" ? args.error : null);
  }
}

/*
Dialog used to create new application resource.
 */
EVO.ide.dialog.resource = function () { 
    var self = {};
    
    var payload = '<form>' +
                      '<label for="filename">File Name:</label><input type="text" id="filename" name="filename" />' +
                      '<label for="filetype">File Type:</label>' +
                      '<select id="filetype" name="filetype" style="width:160px;">' +
                        '<option id="type-html" value="html">HTML</option>' +
                        '<option id="type-css" value="css">CSS</option>' +
                        '<option id="type-js" value="js">Javascript</option>' +
                      '</select>' +
                    '</form><br>';
    
    var dialog = new YAHOO.widget.Dialog("new-resource-dialog",  { 
      width : "300px", 
    	fixedcenter : true, 
    	visible : false, 
    	draggable: false,
    	underlay: "shadow",
    	hideaftersubmit: true,
      constraintoviewport : true, 
      modal: true,
    	buttons : [ { text:"Create", handler:handleSubmit, isDefault:true }, 
    	            { text:"Cancel", handler:handleCancel } ] 
    });

    // adds a listener to the dialogs enter key to handle submit
    var enterListener = new YAHOO.util.KeyListener(document, {
            keys : [13]
        }, {
            fn:handleSubmit,
            scope:dialog,
            correctScope:true
    });
    dialog.cfg.queueProperty("keylisteners", enterListener);
	
    dialog.setHeader("&nbsp;");

    function validateResource(resource, type) {
        if (resource.indexOf(type, resource.length - type.length) === -1) {
            resource = resource + '.' + type;
        }
        return resource;
    }
	
    // various event handlers for Dialog 
    function handleSubmit() { 

        // get the file name and validate 
        var resource = validateResource(this.getData().filename, this.getData().filetype[0]);

        if (self.isController) {
          var controllerName = resource.split('.')[0];
          var code = "/*\n" +
          " * File: " + resource + "\n" +
          " *\n" +
          " * Access public action at /" + EVO.app.name + "/" + controllerName + "\n" +
          " *\n" +
          " */\n" +
          "function " + controllerName + "() {\n\n" +
          "    /* private functions go here */ \n\n" +
          "    return {\n\n" +
          "        /* public action that maps to /" + EVO.app.name + "/" + controllerName + "/hello */\n" +
          "        hello: function(request) {\n" +
          "            return {\n" +
          "                status: 200,\n" +
          "                headers: { \"Content-Type\": \"text/plain\" },\n" +
          "                body: [\"Hello World!\"]\n" +
          "            };\n" +
          "        }\n\n    " +
          "};\n" +
          "}";
          evo.createResource(EVO.app.name, 'server-side', resource, code);
        } else if (self.isPartial) {
        	evo.createResource(EVO.app.name, "partials", resource);
        } else if (self.isLib) {
        	evo.createResource(EVO.app.name, "lib", resource);
        } else {
          // create the new resource on the server
          evo.createResource(EVO.app.name, this.getData().filetype[0], resource);
        }
      	this.hide();
    }; 
    
    function handleCancel() { 
        this.cancel(); 
    };
     
    dialog.render(document.body);
    
    self.show = function (type) {
        dialog.setBody(payload);

        if (type === null) {
          type = 'html';
        }
        
        if (type === 'partials') {
        	type = 'html';
        	this.isPartial = true;
        } else {
        	this.isPartial = false;
        }

        if (type === "server-side") {
          type = 'js';
          this.isController = true;
        } else {
          this.isController = false;
        }
        
        if (type === 'lib') {
            type = 'js';
            this.isLib = true;
        } else {
        	this.isLib = false;
        }

        this.ctype = type;

        // set this type as selected
        $('#type-' + type).attr('selected', "true");
        $('#filename').attr('value', 'untitled.' + type);
        dialog.show();
    }; 
     
    return self; 
};
/* create an instance of the dialog and store it in the global namepsace */
YAHOO.util.Event.addListener(window, "load", function() { EVO.app.dialog.resource = EVO.ide.dialog.resource(); });

EVO.ide.dialog.confirm = function () {
  var my = {};

  var dialog = new YAHOO.widget.SimpleDialog("evo-dialog-confirm", { 
    width: "300px", 
	  fixedcenter: true, 
	  visible: false, 
	  draggable: false, 
	  underlay: "shadow",
	  close: true, 
	  constraintoviewport: true, 
    modal: true
	});
	
	dialog.setHeader("Confirm Delete");
  dialog.setBody("Are you sure you want to delete this item?");
  dialog.cfg.setProperty("icon", YAHOO.widget.SimpleDialog.ICON_WARN);
	
	var handleYes = function() { 
  	dialog.hide(); 
  }; 
  	 
  var handleNo = function() { 
  	dialog.hide(); 
  };
  
	dialog.render(document.body);
	
	return dialog;
};
YAHOO.util.Event.addListener(window, "load", function() { EVO.app.dialog.confirm = EVO.ide.dialog.confirm(); });

/*
 * Upload dialog
 */
EVO.ide.dialog.upload = function () { 
    var self = {};
    var bodyImage = '<form id="imageUpload" enctype="multipart/form-data"><input type="file" name="fileToUpload" id="fileToUpload" /></form><br><br>';
    
    var dialog = new YAHOO.widget.Dialog("dialog2",  { 
      width : "300px", 
    	fixedcenter : true, 
    	visible : false, 
    	draggable: false,
    	underlay: "shadow",
    	hideaftersubmit: true,
      constraintoviewport : true, 
      modal: true,
    	buttons : [ { text:"Create", handler:handleSubmit, isDefault:true }, 
    	            { text:"Cancel", handler:handleCancel } ] 
	  });
	
	  dialog.setHeader("New Image");

    function handleSubmit() { 
        var data = document.getElementById('fileToUpload').files[0];
        var reader = new FileReader();
        reader.onloadend = (function(data) {
            return function(evt) {
              evo.createResource(
                EVO.app.name, 
                EVO.app.dialog.upload.ctype, 
                data.name, 
                evt.target.result.split(",")[1]
              );
            };
          })(data);
        reader.readAsDataURL(data);
      	this.hide();
    }; 
    
    function handleCancel() { 
        this.cancel(); 
    };
     
    dialog.render(document.body);
    
    self.show = function (type) {
        dialog.setBody(bodyImage);
        this.ctype = type;
        dialog.show();
    }; 
     
    return self; 
};
/* create an instance of the dialog and store it in the global namepsace */
YAHOO.util.Event.addListener(window, "load", function() { EVO.app.dialog.upload = EVO.ide.dialog.upload(); });

EVO.ide.dialog.about = function () { 
    var self = {};
    var bodyImage = '<center><img src="/img/evo/evo-logo-tiny.png"><br><span>Developer Preview</span><br><span>Build 0327</span><br><br><span>Copyright &copy; 2012 FullScale Labs, LLC.</span><br><span>All rights reserved.</span></center>';
    
    var dialog = new YAHOO.widget.Dialog("evo-dialog-about",  { 
      width : "300px", 
      fixedcenter : true, 
      visible : false, 
      draggable: false,
      underlay: "shadow",
      hideaftersubmit: true,
      constraintoviewport : true, 
      modal: true,
      close: true
    });
    
    dialog.setHeader("&nbsp;");
    dialog.render(document.body);
    
    self.show = function() {
        dialog.setBody(bodyImage);
        dialog.show();
    }; 
     
    return self; 
};
/* create an instance of the dialog and store it in the global namepsace */
YAHOO.util.Event.addListener(window, "load", function() { EVO.app.dialog.about = EVO.ide.dialog.about(); });

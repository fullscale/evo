var canon = require("pilot/canon");
var event = require("pilot/event");

/* edit modes */
var HtmlMode = require("ace/mode/html").Mode;
var CssMode = require("ace/mode/css").Mode;
var JavaScriptMode = require("ace/mode/javascript").Mode;

/* keyboard bindings */
var vim = require("ace/keyboard/keybinding/vim").Vim;
var emacs = require("ace/keyboard/keybinding/emacs").Emacs;
      
EVO.ide.editor.save = function(tab) {

	var idx = tabView.getTabIndex(tab);
	
	// no tab was provided, use the current active tab
	if (idx === null) {
		idx = tabView.get('activeIndex');
	}

	if (idx !== null) {
		var tab = tabView.getTab(idx);
		var idx = tab.get('postData');
		//var el = tab.get('contentEl');
		//var textArea = el.getElementsByTagName('div')[0];
		//var idx = textArea.getAttribute('id');
		var path = idx.split(':');
	  
		var app = path[0];
		var dir = path[1];
		var id = path[2];
		var src = Editors[idx].getSession().getValue();

		var mimetype = 'text/plain';

		if (dir === 'html' || dir === 'partials') {
			mimetype = 'text/html';
		} else if (dir === 'css') {
			mimetype = 'text/css';
		} else if (dir === 'js' || dir === 'lib') {
			mimetype = 'application/javascript';
		} else if (dir === 'img') {
			var suffix = resource.split('.')[1];
			mimetype = 'image/' + suffix;
		} else if (dir == 'server-side') {
			mimetype = 'application/javascript';
		}

    	jQuery.ajax({
      		type: "PUT",
      		url: '/evo/apps/' + app + '/' + dir + '/' + id,
      		contentType: "application/json",
      		data: JSON.stringify({"mime":mimetype, "code":src}),
      		dataType:"json",
      		processData: false,
      		success:function(data){
        		if (data.status === "ok") {
                    var status = tab.getElementsByClassName('modified')[0];
                    if (status !== undefined) {
                        YAHOO.util.Dom.replaceClass(status, 'modified', 'close');
                    }
                } else {
                    alert("There was a problem saving this file.");
                }
            },
            error:function (xhr, ajaxOptions, thrownError){
                alert("An Error Occurred");
            }    
        });
    } 
};

EVO.ide.editor.saveAll = function() {
    tabView.get('tabs').forEach(function(tab) { EVO.ide.editor.save(tab); });
};
                    
canon.addCommand({
    name: "save",
    bindKey: {
        win: "Ctrl-S",
        mac: "Command-S",
        sender: "editor"
    },
    exec: EVO.ide.editor.save
});

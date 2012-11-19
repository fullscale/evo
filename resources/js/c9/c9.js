/* Extend jQuery with functions for PUT and DELETE requests. */
function _ajax_request(url, data, callback, type, method) {
    if (jQuery.isFunction(data)) {
        callback = data;
        data = {};
    }
    return jQuery.ajax({
        type: method,
        url: url,
        data: data,
        success: callback,
        dataType: type
        });
}

jQuery.extend({
    put: function(url, data, callback, type) {
        return _ajax_request(url, data, callback, type, 'PUT');
    },
    'delete': function(url, data, callback, type) {
        return _ajax_request(url, data, callback, type, 'DELETE');
    }
});

var cloud9 = function() {}

cloud9.prototype.showError = function(where, msg) {
    var errdiv = $('<div class="error">' + msg + '</div>');
    where.append(errdiv);
    errdiv.fadeOut(5000, function() { $(this).remove(); });
}

cloud9.prototype.deleteResource = function(app, dir, name, conf) {
    var url = '/cloud9/apps/' + app + '/' + dir + '/' + name
    $.delete(url, conf, function(data) {
        if (data.status == "error") {
            console.error("Deleting resource (%s) failed.", url)
        }
    });  
}

cloud9.prototype.renameResource = function(app, dir, oldName, newName) {
    var url = '/cloud9/apps/' + app + '/' + dir + '/_rename'
    $.ajax({
        type: 'PUT',
        url: url,
        processData: false,
        contentType: "application/json",
        dataType: "json",
        data: JSON.stringify({from:oldName,to:newName}),
        success: function(data) {
            if (data.status == "failed") {
                console.error("Renaming resource (%s) failed.", oldName)
            }
        } 
    });
}

cloud9.prototype.createResource = function(app, dir, name, code) {
	url = '/cloud9/apps/' + app + '/' + dir + '/' + name;
  
    $.ajax({
        type: 'POST',
        url: url,
        processData: false,
        contentType: "text/plain",
        data: code, 
        success: function(data) {
            if (data.status == "error") {
                c9.showError($('#applications'), data.response);
            } else {
                // update the file tree
                var node = C9.ide.navigator.tree().getNodeByProperty('label', dir);
                var tempNode = new YAHOO.widget.TextNode(name, node, false);
                tempNode.label = name;
                tempNode.type = dir;
                tempNode.isLeaf = true;
                tempNode.labelStyle = "icon-" + dir;
                node.refresh();
                tempNode.focus();
          
                /*node.collapse()
                node.dynamicLoadComplete = false 
                node.tree.removeChildren(node);
                node.expand()*/
          
                /* temp hack -- need to create a "tabview" object (replicated from navigator.js )*/
                var id = app + '-' + dir + '-' + name;
          
                if (dir !== "images") {
                    var thisTab = new YAHOO.widget.Tab({
                        label: name + '<span class="close">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>',
                        content: '<div style="min-height:96%;width:99.8%;" id=\"' + id  + '\">' + code + '</div>',
                        active: true,
                        postData: id
                    });
			
                    /* add the new tab */
                    tabView.addTab(thisTab);
			    
                    /* add this editor instance to the global map */
                    Editors[id] = ace.edit(id);
                    Editors[id].setTheme("ace/theme/eclipse");

                    /* by default, use soft tabs (spaces) and set size to 4 */
                    Editors[id].getSession().setUseSoftTabs(true);
                    Editors[id].getSession().setTabSize(4);
          
                    /* detect and set the proper mode for the resource */
                    if (dir === "html") {
                        Editors[id].getSession().setMode(new HtmlMode());
                    } else if (dir === "css") {
                        Editors[id].getSession().setMode(new CssMode());
                    } else if (dir === "js" || dir === "controllers") {
                        Editors[id].getSession().setMode(new JavaScriptMode());
                    }

                    /* wire up event handler to fire when document is changed */
                    Editors[id].getSession().doc.on('change', function(){
                        var idx = tabView.get('activeIndex');
                        if (idx !== null) {
                            var tab = tabView.getTab(idx).getElementsByClassName('modified')[0];
                            if (tab === undefined) {
                                tab = tabView.getTab(idx).getElementsByClassName('close')[0];
                                YAHOO.util.Dom.replaceClass(tab, 'close', 'modified');
                            }
                        } 
                    });

                } else {
                    thisTab = new YAHOO.widget.Tab({
                        label: name + '<span class="close">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>',
                        content: '<div style="height:1500px;width:99.8%" id=\"' + id  + '\"><img src="/' + app +'/' + dir + '/' + name + '" /></div>',
                        active: true,
                        postData: id
                    });
	
                    /* add the new tab */
                    tabView.addTab(thisTab);
				    
                    /* regsiters the tab as "open" */
                    Editors[id] = true;
                }
                /* add the close handler */
                YAHOO.util.Event.on(thisTab.getElementsByClassName('close')[0], 'click', handleClose, thisTab);
            }
        }
    });	
}

/* temp hack -- need to create a "tabview" object (replicated from navigator.js )*/
function handleClose(e, tab) { 
    YAHOO.util.Event.preventDefault(e); 

    /* need to remove the tab id from the Editor array */
    var el = tab.get('contentEl');
    var textArea = el.getElementsByTagName('div')[0];

    /* if this tab had an editor (may have been an image) */
    if (typeof textArea !== "undefined") {
        var id = textArea.getAttribute('id');

        // get the status of this tab
        var status = tab.getElementsByTagName('span')[0];

        // the document has unsaved changes
        if (YAHOO.util.Dom.hasClass(status, 'modified')) {
            var saveFirstCallbacks = [{
                // save then close this tab
                text: "Yes", 
                handler: function() {
                    C9.app.dialog.confirm.hide();
                    C9.ide.editor.save(tab);
                    delete Editors[id];
                    tabView.removeTab(tab);
                }, 
                isDefault: true 
            },{ 
                // close tab without saving
                text: "No", 
                handler: function() { 
                    C9.app.dialog.confirm.hide();
                    delete Editors[id];
                    tabView.removeTab(tab);
                }
            }];

            // setup the dialog text and register the callbacks
            C9.app.dialog.confirm.setHeader("Confirm Close");
            C9.app.dialog.confirm.setBody("Would you like to save your changes first?");
            C9.app.dialog.confirm.cfg.setProperty("buttons", saveFirstCallbacks);
            C9.app.dialog.confirm.show();
        } else {
            // no unsaved changes, safe to close this tab
            delete Editors[id];
            tabView.removeTab(tab);
        }
    } else {
        // tab contained an image, safe to close
        tabView.removeTab(tab); 
    }
};

cloud9.prototype.deleteCollection = function(url) {
    components = url.split('/');
    id = components[components.length -1];
    $.delete(url, {},
      function(data) {
          if (data.status == "ok") {
              $('#col-'+id).hide();
          } else {
              alert("Sorry, an error occurred");
          }
    }, "json");
}

cloud9.prototype.deleteApplication = function(url) {
    components = url.split('/');
    id = components[components.length -1];
    $.delete(url, {},
      function(data) {
          if (data.status == "ok") {
              $('#col-'+id).hide();
          } else {
              alert("Sorry, an error occurred");
          }
    }, "json");
}

cloud9.prototype.deleteType = function(url) {
    components = url.split('/');
    id = components[components.length -1];
    
    $.delete(url, {},
      function(data) {
          if (data.status == "ok") {
              $('#type-'+id).hide();
          } else {
              alert("Error");
          }
    }, "json");
}

cloud9.prototype.deleteDocument = function(collection, type, docid) {
    $.ajax({
        type: "DELETE",
        url: "/cloud9/content/" + collection +"/"+ type +"/"+ docid, 
        dataType: "json",
        success: function(msg){
            $('#'+docid).hide("fast");
        },
        error:function (xhr, ajaxOptions, thrownError){
            alert("An Error Occurred")
        }
    });
}

cloud9.prototype.deleteUser = function(userid){
    console.log("Delete: " + userid)
    $.ajax({
        type: "DELETE",
        url: "/cloud9/user/" + userid,
        dataType: "json",
        success: function(response){
            if (response.status == "ok") {
                $('#c9-user-table-row-'+userid).hide();
            } else {
                console.error(response.msg);
            }
        }
    });
}

cloud9.prototype.addDocument = function(url, func) {
    var values = {};
    
    /* check for form values that are numbers */
    $('#contentForm input[type != submit]').each(function() {
        var parts = (this.name).split('^')
        var name = parts[0]
        var mtype = parts[1]
        
        switch(mtype) {
            case "string":
                values[name] = $(this).val()
                break
            case "integer":
                values[name] = parseFloat($(this).val())
                break
            case "float":
                values[name] = parseFloat($(this).val())
                break
            case "date":
                values[name] = $(this).val()
                break
            default:
                alert('Error')
        }
    });

    $.ajax({
        url: url,
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify(values),
        type: 'post',
        success: function(data) {
        	if (data.status === 'ok') {
        		location.reload();
        	} else {
        		c9.showError($('#newdoc'), data.response);
        	}
        }
    });
}

cloud9.prototype.loadAndSlide = function(container, data, offset, options) {
    var defaults = {
        queue: true,
        duration: 250
    };

    var options = $.extend(defaults, options);

    $(container).html(data);
    // TODO: make userTableContainer configurable
    $('.userTableContainer').animate({left: offset}, options);    
}

cloud9.prototype.slide = function(offset, options) {
    var defaults = {
        queue: true,
        duration: 250
    };

    var options = $.extend(defaults, options);
    // TODO: make userTableContainer configurable
    $('.userTableContainer').animate({left: offset}, options);
}

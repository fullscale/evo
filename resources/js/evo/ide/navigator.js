/**
 * The ContentTree module controls the creation of the
 * project tree. It provides animation when expanding and
 * collapsing tree nodes. It also caches its items so that
 * on each operation it does not have to callback to the server
 * for a list of valid components.
 * 
 * @module ContentTree
 * @requires dom, event, layout
 */
EVO.ide.navigator = function () {
        
    var tree = "";
    var currentIconMode = "";
        
      DDTreeNode = function(id, sGroup, config) {
      DDTreeNode.superclass.constructor.call(this, id, sGroup, config);
    };

    YAHOO.extend(DDTreeNode, YAHOO.util.DDProxy, {
        endDrag: function(e) {},
        onDragDrop: function(e, id) {}
    });

    function changeIconMode() {
        var newVal = parseInt(this.value);
        if (newVal != currentIconMode) {
            currentIconMode = newVal;
        }
        buildTree();
    }

    function loadContentTypes(node, fnLoadComplete)  {
        var nodeLabel = encodeURI(node.label);
		var appName = node.appName;
        var sUrl = "/evo/apps/" + nodeLabel;
        var htmlUrl = "/evo/apps/" + appName + "/html";

        var callback = {
            success: function(oResponse) {
                var x = YAHOO.lang.JSON.parse(oResponse.responseText);
                for (var i = 0; i < x.length; i++) {
                	if (x[i] !== 'html') {
                		var tempNode = new YAHOO.widget.TextNode(x[i], node, false);
                		tempNode.label = x[i];
                		tempNode.type = nodeLabel;
					    tempNode.appName = appName;
					    tempNode.setDynamicLoad(loadResources, currentIconMode);
                	}
                }

                var loadHtml = {
                    	success: function(oResponse) {
                            var x = YAHOO.lang.JSON.parse(oResponse.responseText);
                            x.sort();
                            for (var i = 0; i < x.length; i++) {
                                var tempNode = new YAHOO.widget.TextNode(x[i], node, false);
                                tempNode.label = x[i];
                                tempNode.type = 'html';
                                tempNode.isLeaf = true;
                                tempNode.editable = true;
                                tempNode.labelStyle = "icon-html";
                            }
                    		oResponse.argument.fnLoadComplete();
                    	},
                    	failure: function(oResponse) {
                    		oResponse.argument.fnLoadComplete();
                    	},
                        argument: {
                            "node": node,
                            "fnLoadComplete": fnLoadComplete
                        },
                    	timeout: 7000
                    };
                YAHOO.util.Connect.asyncRequest('GET', htmlUrl, loadHtml);
            },
            failure: function(oResponse) {
                oResponse.argument.fnLoadComplete();
            },
            argument: {
                "node": node,
                "fnLoadComplete": fnLoadComplete
            },
            timeout: 7000
        };
        
        YAHOO.util.Connect.asyncRequest('GET', sUrl, callback);
    }
    
    function loadResources(node, fnLoadComplete)  {
        var resource = encodeURI(node.label);
        var appName = node.appName;
        var sUrl = "/evo/apps/" + appName + "/" + resource;

        var callback = {
            success: function(oResponse) {
                var x = YAHOO.lang.JSON.parse(oResponse.responseText);
                x.sort();
                for (var i = 0; i < x.length; i++) {
                    var tempNode = new YAHOO.widget.TextNode(x[i], node, false);
                    tempNode.label = x[i];
                    tempNode.type = resource;
                    tempNode.isLeaf = true;
                    tempNode.editable = true;
                    tempNode.labelStyle = "icon-" + resource;
                }
                oResponse.argument.fnLoadComplete();    
            },
            failure: function(oResponse) {
                oResponse.argument.fnLoadComplete();
            },
            argument: {
                "node": node,
                "fnLoadComplete": fnLoadComplete
            },
            timeout: 7000
        };
        YAHOO.util.Connect.asyncRequest('GET', sUrl, callback);
    }

    function buildTree(appName) {
    	/* create a new tree: */
    	tree = new YAHOO.widget.TreeView("evo-ide-navigator");

    	/* set tree expand/collapse animation */
    	tree.setExpandAnim(YAHOO.widget.TVAnim.FADE_IN);
    	tree.setCollapseAnim(YAHOO.widget.TVAnim.FADE_OUT);
           
    	/* get root node for tree: */
    	var root = new YAHOO.widget.TextNode(appName, tree.getRoot(), false);
	    root.appName = appName;
	    root.setDynamicLoad(loadContentTypes, currentIconMode);
           
	    /* render tree with these toplevel nodes; all descendants of these node
	     * will be generated as needed by the dynamoader.
	     */
	    tree.draw();

	    /* editorSaveEvent is fired when user presses enter on inline editor. This 
	     * function handles persisting that change on the back-end.
	     */
	    tree.subscribe("editorSaveEvent", function(oArgs) {
	    	var newName = oArgs.newValue;
	    	var oldName = oArgs.oldValue;
	    	var appName = oArgs.node.parent.appName;
	    	var dirName = oArgs.node.parent.label;
	    	
	    	if (dirName === appName) {
	    		dirName = 'html';
	    	}
	    	evo.renameResource(appName, dirName, oldName, newName);

	    	var id = appName + '-' + dirName + '-' + oldName;
	    	var newId = appName + '-' + dirName + '-' + newName;

	    	/* if this file is tied to an open editor then we need to
	    	 * also rename the associated tab name and id */
	    	if (Editors[id]) {
	    		var tabs = tabView.get('tabs');
	    		var i = tabs.length;

	    		while (i--) {
	    			if (tabs[i].get('postData') === id) {
	    				var thisTab = tabs[i];
	    				// This file is currently open in a tab
	    				thisTab.set('label', newName + '<span class="close">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>');
	    				thisTab.set('postData', newId);

	    				var el = thisTab.get('contentEl');
	    				var textArea = el.getElementsByTagName('div')[0];

	    				/* if this tab had an editor (may have been an image) */
	    				if (typeof textArea !== "undefined") {
	    					textArea.setAttribute('id', newId);
	    				}

	    				Editors[newId] = Editors[id];
	    				delete Editors[id];

	    				// rewire the close event based on the new id
	    				YAHOO.util.Event.on(thisTab.getElementsByClassName('close')[0], 'click', handleClose, thisTab);

	    				break;
	    			}
	    		}
	    	}
	    });

	    tree.singleNodeHighlight = true;

	    /* creates a new tab for this resource */
	    tree.subscribe("clickEvent", function(oArgs) { 
	    	var app = oArgs.node.parent.appName;
			var dir = oArgs.node.parent.label;
			if (app === dir) {
				dir = 'html';
			}
			var resource = oArgs.node.label;
			var id = app + ':' + dir + ':' + resource;

			oArgs.node.highlight();
			
			if (Editors[id] || oArgs.node.isLeaf == false) {
				/* tab is already open, need to set as active tab */
				var tabs = tabView.get('tabs');
				var i = tabs.length;

				while (i--) {
					if (tabs[i].get('postData') === id) {
						var idx = tabView.getTabIndex(tabs[i]);
						tabView.selectTab(idx);
						break;
					}
				}
			} else {
				var url = "/evo/apps/" + app + "/" + dir + "/" + resource;
				var callback = {
					success: function(oResponse) {
						var code = oResponse.responseText;
						var thisTab; 

						if (dir !== "img") {
							thisTab = new YAHOO.widget.Tab({
								label: resource + '<span class="close">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>',
								content: '<div style="min-height:96%;width:99.8%" id=\"' + id  + '\"></div>',
								active: true,
								postData: id
							});
							/* add the new tab */
							tabView.addTab(thisTab);
						      
							/* add this editor instance to the global map */
							Editors[id] = ace.edit(id);
							Editors[id].setTheme("ace/theme/evo");

	  						/* by default, use soft tabs (spaces) and set size to 4 */
	  						Editors[id].getSession().setUseSoftTabs(true);
	  						Editors[id].getSession().setTabSize(4);
	                  
	  						/* detect and set the proper mode for the resource */
	  						if (dir === "html" || dir === 'partials') {
	  							Editors[id].getSession().setMode(new HtmlMode());
	  						} else if (dir === "css") {
	  							Editors[id].getSession().setMode(new CssMode());
	  						} else if (dir === "js" || dir === "server-side" || dir === 'lib') {
	  							Editors[id].getSession().setMode(new JavaScriptMode());
	  						} 
	  						Editors[id].getSession().setValue(code);
	
	  						/* wire up event handler to fire when document is changed */
	  						Editors[id].getSession().doc.on('change', function() {
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
			            		label: resource + '<span class="close">&nbsp;&nbsp;&nbsp;&nbsp;</span>',
			            		content: '<div style="height:1500px;width:99.8%" id=\"' + id  + '\"><img src="/' + app +'/' + dir + '/' + resource + '" /></div>',
			            		active: true,
			            		postData: id
			            	});
			            	/* add the new tab */
			            	tabView.addTab(thisTab);
	  						    
			            	/*  regsiters the tab as "open" */
			            	Editors[id] = true;
						}
							    
			            /* add the close handler */
						YAHOO.util.Event.on(thisTab.getElementsByClassName('close')[0], 'click', handleClose, thisTab);
					},
			        failure: function(oResponse) {
			        	alert("Unable to retrieve document.");
			        },
			        timeout: 7000
				};
				YAHOO.util.Connect.asyncRequest('GET', url, callback);
			}
	    });
	    return root;
    };
    
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
                        EVO.app.dialog.confirm.hide();
                        EVO.ide.editor.save(tab);
                        delete Editors[id];
                        tabView.removeTab(tab);
                    }, 
                    isDefault: true 
                },{ 
                    // close tab without saving
                    text: "No", 
                    handler: function() { 
                        EVO.app.dialog.confirm.hide();
                        delete Editors[id];
                        tabView.removeTab(tab);
                    }
                }];

                // setup the dialog text and register the callbacks
                EVO.app.dialog.confirm.setHeader("Confirm Close");
                EVO.app.dialog.confirm.setBody("Would you like to save your changes first?");
                EVO.app.dialog.confirm.cfg.setProperty("buttons", saveFirstCallbacks);
                EVO.app.dialog.confirm.show();
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
    
    var oCurrentTextNode = null;
    var contains = function(a, obj) {
    	var i = a.length;
    	while (i--) {
    		if (a[i] === obj) {
    			return true;
    		}
    	}
    	return false;
    };
    
    /*
     trigger that displays the resource level context menu.
    */
    function onTriggerFileContextMenu(p_oEvent) { 
    	var oTarget = this.contextEventTarget; 

    	/*
    	  Get the TextNode instance that that triggered the
    	  display of the ContextMenu instance.
    	*/ 
    	oCurrentTextNode = EVO.ide.navigator.tree().getNodeByElement(oTarget); 
    	
    	if (!oCurrentTextNode || !oCurrentTextNode.isLeaf) { 
    		// Cancel the display of the ContextMenu instance.     	 
    		this.cancel();
    	} 
    }
    
    /*
      trigger that displays the directory level context menu.
    */
    function onTriggerDirContextMenu(p_oEvent) { 
    	var oTarget = this.contextEventTarget; 

    	/*
    	  Get the TextNode instance that triggered the
    	  display of the ContextMenu instance.
    	*/ 
    	oCurrentTextNode = EVO.ide.navigator.tree().getNodeByElement(oTarget); 
    	
    	if (!oCurrentTextNode || !contains(['css', 'html', 'img', 'js', 'server-side', 'partials', 'lib'], oCurrentTextNode.label)) { 
    		// Cancel the display of the ContextMenu instance.     	 
    		this.cancel();
    	} 
    }
    
    /*
      trigger that displays the project level context menu.
    */
    function onTriggerProjContextMenu(p_oEvent) { 
    	var oTarget = this.contextEventTarget; 
    	/*
    	  Get the TextNode instance that that triggered the
    	  display of the ContextMenu instance.
    	*/ 
    	oCurrentTextNode = EVO.ide.navigator.tree().getNodeByElement(oTarget); 
    	
    	if (!oCurrentTextNode || !oCurrentTextNode.parent.isRoot()) {
    	  // Cancel the display of the ContextMenu instance.     	 
    	  this.cancel();
    	} 
    }
    
    function performDeletion() { 
    	var dir = oCurrentTextNode.parent.label;
    	var resource = oCurrentTextNode.label;
    	var id = EVO.app.name + '-' + dir + '-' + resource;
      
    	evo.deleteResource(EVO.app.name, dir, resource);
    	
    	tree.removeNode(oCurrentTextNode); 
    	tree.draw(); 

    	// clean-up any open tabs or allocated Editor instance
    	var tabs = tabView.get('tabs');
    	var i = tabs.length;

    	while (i--) {
    		if (tabs[i].get('postData') === id) {
    			tabView.removeTab(tabs[i]);
    			delete Editors[id];
    			break;
    		}
    	}
    	EVO.app.dialog.confirm.hide();
    }
    
    function deleteNode() { 
      var deleteNodeCallbacks = [{ 
    	  text: "Yes", 
    	  handler: performDeletion, 
    	  isDefault: true 
      },{ 
    	  text: "No", 
    	  handler: function() { 
    		  EVO.app.dialog.confirm.hide(); 
    	  }
      }];
      EVO.app.dialog.confirm.cfg.setProperty("buttons", deleteNodeCallbacks);
      EVO.app.dialog.confirm.show();
    }
    
    function addNode(type, args, ctype) {
    	var type = oCurrentTextNode.label;
        
    	if (type === "img") {
    		EVO.app.dialog.upload.show(type);
    	} else if (type in {html:true, css:true, js:true, 'server-side':true, partials:true, lib:true}) {
    		EVO.app.dialog.resource.show(type); 
    	} else if (ctype in {html:true, css:true, js:true, 'server-side':true, partials:true, lib:true}) {
    		EVO.app.dialog.resource.show(ctype);
    	} else if (ctype === "img") {
    		EVO.app.dialog.upload.show(ctype);
    	}
    }

    function renameNode() {
    	// shows an inline editor
    	// callback is tree.subscribe("editorSaveEvent", function(oArgs)...
    	oCurrentTextNode.editNode();
    }

    function collapseTree() {
    	tree.collapseAll();
    }

    return {
        /*
          Builds the tree (navigator) along with supportinging context menus.
        */
        init: function(appName) {
            YAHOO.util.Event.on(["mode0", "mode1"], "click", changeIconMode);
            var el = document.getElementById("mode1");
            if (el && el.checked) {
                currentIconMode = parseInt(el.value);
            } else {
                currentIconMode = 0;
            }
            buildTree(appName).toggle();
            
            /*
            	Instantiates the context menu when right clicking on a resource (file).
            */ 
            var fileContextMenu = new YAHOO.widget.ContextMenu("evo-file-context-menu", { 
            	trigger: "evo-ide-navigator", 
            	lazyload: true, 
            	itemdata: [{ 
            		text: "Rename...", 
            		onclick: { 
            			fn: renameNode 
            		}
            	},{ 
            		text: "Delete File", 
            		onclick: { 
            			fn: deleteNode 
            		}
            	}] 
            });
            
            /*
            	Instantiates the context menu when right clicking on a content-type (dir).
            */
            var dirContextMenu = new YAHOO.widget.ContextMenu("evo-dir-context-menu", { 
            	trigger: "evo-ide-navigator", 
            	lazyload: true, 
            	itemdata: [{ 
            		text: "New File", 
            		onclick: { 
            			fn: addNode 
            		}
            	}] 
            });
            
            /*
            	Instantiates the context menu when right clicking on the project.
            */
            var projContextMenu = new YAHOO.widget.ContextMenu("evo-proj-context-menu", { 
            	trigger: "evo-ide-navigator", 
            	lazyload: true, 
            	itemdata: [{ 
            		text: "New",
            		helptext: "Ctrl + N",
            		onclick: { 
            			fn: addNode 
            		}
            	},{ text: "Collapse All", 
            		onclick: {
            			fn: collapseTree 
            		}
            	},[{
                    text: "Upload Image",
 		    	   	onclick: { 
 		    	   		fn: addNode, 
 		    	   		obj:"img" 
 		    	   	}
                }]] 
            });

            /*
              Submenu data items for project level context menu.
            */
            var projSubMenu = [{
            	id: "New",  
        		itemdata: [
    		       { 
    		    	   text: "HTML File", 
    		    	   onclick: { 
    		    		   fn: addNode, 
    		    		   obj:"html" 
    		    		}
    		       },{ 
    		    	   text: "HTML Partial", 
    		    	   onclick: {
    		    		   fn:addNode, 
    		    		   obj:'partials'
    		    	   }
    		       },{ 
    		    	   text: "CSS File", 
    		    	   onclick: { 
    		    		   fn: addNode, 
    		    		   obj:"css" 
    		    		}
    		       },{ 
    		    	   text: "Javascript File", 
    		    	   onclick: { 
    		    		   fn: addNode, 
    		    		   obj:"js" 
    		    	   }
    		       },{ 
    		    	   text: "Javascript Library", 
    		    	   onclick: {
    		    		   fn:addNode, 
    		    		   obj:'lib'
    		    		}
    		       },{ 
    		    	   text: "Server-Side Controller", 
    		    	   onclick: {
    		    		   fn:addNode, 
    		    		   obj:'server-side'
    		    		}
    		       }
    		   ]
            }];
            
            /*
              attaches submenu to project context menu.
            */
            projContextMenu.subscribe("beforeRender", function () {
              if (this.getRoot() == this) { 	 
            	  this.getItem(0).cfg.setProperty("submenu", projSubMenu[0]);
            	}
            });
            
            /*
              Subscribe to the "contextmenu" event for the element(s)
              specified as the "trigger" for each ContextMenu instance.
            */ 	 
            fileContextMenu.subscribe("triggerContextMenu", onTriggerFileContextMenu);
            dirContextMenu.subscribe("triggerContextMenu", onTriggerDirContextMenu);
            projContextMenu.subscribe("triggerContextMenu", onTriggerProjContextMenu);
        },
        tree: function() {
        	return tree; 
        }
    };
}();


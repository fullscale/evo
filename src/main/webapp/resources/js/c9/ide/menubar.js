/*
 * Initialize and render the MenuBar when the page's DOM is ready 
 * to be scripted.
 */

YAHOO.util.Event.onDOMReady(function () {

    function showResourceDialog(type, args, ctype) {
        C9.app.dialog.resource.show(ctype);  
    }

    function showUploadDialog(type, args, ctype) {
        C9.app.dialog.upload.show(ctype);  
    }

    function showAboutDialog() {
        C9.app.dialog.about.show();
    }

    // returns active editor
    var getCurrentEditor = function() {
        var idx = tabView.get('activeIndex');
        if (idx !== null) {
            var tab = tabView.getTab(idx);
            var el = tab.get('contentEl');
            var textArea = el.getElementsByTagName('div')[0];
            var idx = textArea.getAttribute('id');
            return Editors[idx];
        } else {
            return null;
        }
    }

    // returns current font size of active editor
    var getFontSize = function() {
        var idx = tabView.get('activeIndex');
        if (idx !== null) {
            var tab = tabView.getTab(idx);
            var el = tab.get('contentEl');
            var textArea = el.getElementsByTagName('div')[0];
            var currentSize = textArea.style.fontSize;
            var size = 12;

            if (currentSize !== "") {
                size = currentSize.slice(0,currentSize.length - 2);
                size = parseInt(size);
            }
            return size;
        } else {
            return null;
        }
    }

    // increase current font size
    var increaseFontSize = function() {
        var editor = getCurrentEditor();
        var currentSize = getFontSize();
        if (currentSize !== null) {
            editor.setFontSize((currentSize + 1) + 'px');
        }
    }

    // descreases current font size
    var decreaseFontSize = function() {
        var editor = getCurrentEditor();
        var currentSize = getFontSize();
        if (currentSize !== null) {
            editor.setFontSize((currentSize - 1) + 'px');
        }
    }

    // adds a listener to increase editor font size
    var newResourceListener = new YAHOO.util.KeyListener(document, {
            keys : [78],
            ctrl: true
        }, {
            fn:showResourceDialog,
            obj: "html"
    });
    newResourceListener.enable();

    // adds a listener to increase editor font size
    var increaseFontListener = new YAHOO.util.KeyListener(document, {
            keys : [187],
            ctrl: true
        }, {
            fn:increaseFontSize
    });
    increaseFontListener.enable();

    // adds a listener to decrease editor font
    var decreaseFontListener = new YAHOO.util.KeyListener(document, {
            keys : [189],
            ctrl: true
        }, {
            fn:decreaseFontSize
    });
    decreaseFontListener.enable();

    var enablePrintMargin = function() {
        var editor = getCurrentEditor();
        if (editor !== null) {
            editor.renderer.setShowPrintMargin(true);
        }
    }

    var disablePrintMargin = function() {
        var editor = getCurrentEditor();
        if (editor !== null) {
            editor.renderer.setShowPrintMargin(false);
        }  
    }

    var enableGutter = function() {
        var editor = getCurrentEditor();
        if (editor !== null) {
            editor.renderer.setShowGutter(true);
        }    
    }

    var disableGutter = function() {
        var editor = getCurrentEditor();
        if (editor !== null) {
            editor.renderer.setShowGutter(false);
        } 
    }

    var previewProject = function() {
        window.open("/" + C9.app.name);    
    }

    var exportProject = function() {
        window.open("/v1/apps/" + C9.app.name);    
    }

    var viewDevGuide = function() {
        window.open("http://www.cloud9search.com/docs.html");
    }

    var viewApiDocs = function() {
        window.open("http://www.cloud9search.com/jsdocs/C9.api.html");
    }

    // adds a listener to show gutter
    var showGutterListener = new YAHOO.util.KeyListener(document, {
            keys : [71],
            ctrl: true
        }, {
            fn:enableGutter
    });
    showGutterListener.enable();

    // adds a listener to hide gutter
    var hideGutterListener = new YAHOO.util.KeyListener(document, {
            keys : [72],
            ctrl: true
        }, {
            fn:disableGutter
    });
    hideGutterListener.enable();

    // adds a listener to preview project in new tab
    var previewListener = new YAHOO.util.KeyListener(document, {
            keys : [79],
            ctrl: true
        }, {
            fn:previewProject
    });
    previewListener.enable();

    var setKeyBinding = function(type, args, mode) {
        var editor = getCurrentEditor();
        if (editor !== null) {
            editor.setKeyboardHandler(mode);
        }
    }

    var enableSoftTabs = function() {
        var editor = getCurrentEditor();
        if (editor !== null) {
            editor.getSession().setUseSoftTabs(true);
        }
    }

    var disableSoftTabs = function() {
        var editor = getCurrentEditor();
        if (editor !== null) {
            editor.getSession().setUseSoftTabs(false);
        }
    }

    var setTabSize = function(type, args, size) {
        var editor = getCurrentEditor();
        if (editor !== null) {
            editor.getSession().setTabSize(size);
        }        
    }

    var setTheme = function(type, args, theme) {
        var editor = getCurrentEditor();
        if (editor !== null) {
            editor.setTheme("ace/theme/" + theme);
        }  
    }

    var enableSoftWrap = function() {
        var editor = getCurrentEditor();
        if (editor !== null) {
            editor.getSession().setUseWrapMode(true);
            editor.getSession().setWrapLimitRange(80, 80);
            editor.renderer.setPrintMarginColumn(80);
        }         
    }

    var disableSoftWrap = function() {
        var editor = getCurrentEditor();
        if (editor !== null) {
            editor.getSession().setUseWrapMode(false);
            editor.renderer.setPrintMarginColumn(80);
        }         
    }

    var onMenuItemClick = function () {
        alert("Callback for MenuItem: " + this.cfg.getProperty("text"));
    };

    /*
     * Define an array of object literals, each containing 
     * the data necessary to create the items for a MenuBar.
     */
    var menuData = [{ 
        text: "<em id=\"cloud9label\">&nbsp;</em>", 
            submenu: { 
                id: "cloud9", 
                itemdata: [{
                    text: "About Cloud9",
                    onclick: {
                        fn: showAboutDialog
                    }
                }]
            }
        },{
            text: "Project",
            submenu: {
                id: "preview",
                itemdata: [{
                    text: "View in Browser",
                    helptext: "Ctrl + O",
                    onclick: {
                        fn: previewProject
                    }
                },{
                    text: "Export",
                    onclick: {
                        fn: exportProject
                    }
                }]
            }
        },{ 
        text: "File", 
            submenu: {  
                id: "filemenu", 
                itemdata: [[{ 
                    text: "New", 
                    helptext: "Ctrl + N", 
                    keylistener: { 
                        ctrl: true, 
                        keys: 78 
                    },
                    submenu: { 
                        id: "filetypemenu", 
                        itemdata: [{ 
                            text: 'HTML File', 
                            value: 'html', 
                            id: 'newHtml', 
                            onclick: {
                                fn: showResourceDialog, 
                                obj:"html"
                            }
                        },{ 
                            text: 'CSS File', 
                            value: 'css', 
                            id: 'newCss', 
                            onclick: {
                                fn: showResourceDialog, 
                                obj:"css"
                            }
                        },{ 
                            text: 'Javascript File', 
                            value: 'javascript', 
                            id: 'newJavascript', 
                            onclick: {
                                fn: showResourceDialog, 
                                obj:"js"
                            }
                        },{
                            text: 'Javascript Controller',
                            value: 'controllers',
                            id: 'newController',
                            onclick: {
                                fn: showResourceDialog,
                                obj:"controllers"
                            }
                        }]
                    }
                }],[{
                    text: "Upload Image",
                    onclick: {
                        fn: showUploadDialog,
                        obj:"images"
                    }
                }],[{ 
                    text: "Save", 
                    helptext: "Ctrl + S", 
                    onclick: { 
                        fn: C9.ide.editor.save 
                    }
                },{ 
                    text: "Save All",
                    onclick: {
                        fn: C9.ide.editor.saveAll
                    }
                }]]
            }
        },{
            text: "Preferences", 
            submenu: { 
                id: "prefmenu", 
                itemdata: [[{ 
                    text: "Font", 
                    submenu: {
                        id: "fontmenu",
                        itemdata:[{
                            text: "Larger",
                            helptext: "Ctrl +",
                            onclick: {
                                fn: increaseFontSize
                            }
                        },{
                            text: "Smaller",
                            helptext: "Ctrl -",
                            onclick: {
                                fn: decreaseFontSize
                            }
                        }]
                    }
                }],[{
                    text: "Print Margin",
                    submenu: {
                        id: "printmarginmenu",
                        itemdata: [{ 
                            text: "Show",
                            onclick: {
                                fn: enablePrintMargin
                            }
                        },{
                            text: "Hide",
                            onclick: {
                                fn: disablePrintMargin
                            }
                        }]
                    }
                },{
                    text: "Soft Wrap",
                    submenu: {
                        id: "softwrapmenu",
                        itemdata: [{
                            text: "Enable",
                            onclick: {
                                fn: enableSoftWrap
                            }
                        },{
                            text: "Disable",
                            onclick: {
                                fn: disableSoftWrap
                            }
                        }]
                    }
                }],[{
                    text: "Show Gutter",
                    helptext: "Ctrl + G",
                    onclick: {
                        fn: enableGutter
                    }
                },{
                    text: "Hide Gutter",
                    helptext: "Ctrl + H",
                    onclick: {
                        fn: disableGutter
                    }
                }],[{
                    text: "Soft Tabs (Spaces)",
                    submenu: {
                        id: "softtabmenu",
                        itemdata: [{
                            text: "Enable",
                            onclick: {
                                fn: enableSoftTabs
                            }  
                        },{
                            text: "Disable",
                            onclick: {
                                fn: disableSoftTabs
                            }
                        }]
                    }
                },{
                    text: "Tab Size",
                    submenu: {
                        id: "tabsizemenu",
                        itemdata: [{
                            text: "2 Spaces",
                            onclick: {
                                fn: setTabSize,
                                obj: "2"
                            }
                        },{
                            text: "4 Spaces",
                            onclick: {
                                fn: setTabSize,
                                obj: "4"
                            }  
                        },{
                            text: "6 Spaces",
                            onclick: {
                                fn: setTabSize,
                                obj: "6"
                            }
                        },{
                            text: "8 Spaces",
                            onclick: {
                                fn: setTabSize,
                                obj: "8"
                            }
                        }]
                    }
                }],[{
                    text: "Themes",
                    submenu: {
                        id: "thememenu",
                        itemdata: [
                            {text: "Clouds",onclick: {fn: setTheme,obj: "clouds"}},
                            {text: "Clouds Midnight",onclick: {fn: setTheme,obj: "clouds_midnight"}},
                            {text: "Cobalt",onclick: {fn: setTheme,obj: "cobalt"}},
                            {text: "Crimson Editor",onclick: {fn: setTheme,obj: "crimson_editor"}},
                            {text: "Dawn",onclick: {fn: setTheme,obj: "dawn"}},
                            {text: "Eclipse",onclick: {fn: setTheme,obj: "eclipse"}},
                            {text: "Idle Fingers",onclick: {fn: setTheme,obj: "idle_fingers"}},
                            {text: "Kr Theme",onclick: {fn: setTheme,obj: "kr_theme"}},
                            {text: "Merbivore",onclick: {fn: setTheme,obj: "merbivore"}},
                            {text: "Merbivore Soft",onclick: {fn: setTheme,obj: "merbivore_soft"}},
                            {text: "Mono Industrial",onclick: {fn: setTheme,obj: "mono_industrial"}},
                            {text: "Monokai",onclick: {fn: setTheme,obj: "monokai"}},
                            {text: "Pastel on Dark",onclick: {fn: setTheme,obj: "pastel_on_dark"}},
                            {text: "Solarized Dark",onclick: {fn: setTheme,obj: "solarized_dark"}},
                            {text: "Solarized Light",onclick: {fn: setTheme,obj: "solarized_light"}},
                            {text: "Textmate",onclick: {fn: setTheme,obj: "textmate"}},
                            {text: "Twilight",onclick: {fn: setTheme,obj: "twilight"}},
                            {text: "Vibrant Ink",onclick: {fn: setTheme,obj: "vibrant_ink"}}
                        ]
                    }

                },{
                    text: "Key Bindings",
                    submenu: {
                        id: "keybindmenu",
                        itemdata: [{
                            text: "default",
                            onclick: {
                                fn: setKeyBinding,
                                obj: null
                            }
                        },{
                            text: "vim",
                            onclick: {
                                fn: setKeyBinding,
                                obj: vim
                            }
                        },{
                            text: "emacs",
                            onclick: {
                                fn: setKeyBinding,
                                obj: emacs
                            }
                        }]
                    }
                }]]
            }
        },{
            text: "Help",
            submenu: {
                id: "helpmenu",
                itemdata: [{
                    text: "Developer Guide",
                    onclick: {
                        fn: viewDevGuide
                    }
                },{
                    text: "Javascript API",
                    onclick: {
                        fn: viewApiDocs
                    } 
                }]
            }
        }
    ];

    /*
    * Instantiate a MenuBar:  The first argument passed to the 
    * constructor is the id of the element to be created; the 
    * second is an object literal of configuration properties.
    */
    var oMenuBar = new YAHOO.widget.MenuBar("c9-ide-menubar", { 
        lazyload: true, 
        itemdata: menuData 
    });

    /*
     * Since this MenuBar instance is built completely from 
     * script, call the "render" method passing in a node 
     * reference for the DOM element that its should be 
     * appended to.
     */
    oMenuBar.render("c9-ide-menubar-container");
    
    /* Add a "show" event listener for each submenu. */
	function onSubmenuShow() {

		var oIFrame, oElement, nOffsetWidth;

		/* Keep the left-most submenu against the left edge of the browser viewport */
		if (this.id == "cloud9") {
			YAHOO.util.Dom.setX(this.element, 0);
			oIFrame = this.iframe;            
			if (oIFrame) {
				YAHOO.util.Dom.setX(oIFrame, 0);
			}
			this.cfg.setProperty("x", 0, true);
		}

		/*
		 * Need to set the width for submenus of submenus in IE to prevent the mouseout 
		 * event from firing prematurely when the user mouses off of a MenuItem's 
		 * text node.
		 */
		if ((this.id == "filemenu" || this.id == "editmenu") && YAHOO.env.ua.ie) {

			oElement = this.element;
			nOffsetWidth = oElement.offsetWidth;
	
			/*
			 * Measuring the difference of the offsetWidth before and after
			 * setting the "width" style attribute allows us to compute the 
			 * about of padding and borders applied to the element, which in 
			 * turn allows us to set the "width" property correctly.
			 */
			oElement.style.width = nOffsetWidth + "px";
			oElement.style.width = (nOffsetWidth - (oElement.offsetWidth - nOffsetWidth)) + "px";
		}
	}  

  /* Subscribe to the "show" event for each submenu */
  oMenuBar.subscribe("show", onSubmenuShow);

});
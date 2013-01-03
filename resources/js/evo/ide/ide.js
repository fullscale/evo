C9.ide.init = function(app) { 
  C9.app.name = app;
  C9.ide.layout.init();
  C9.ide.navigator.init(app);
  tabView = new YAHOO.widget.TabView('c9-ide-editor');
  Editors = new Object;
  c9 = new cloud9();
  tabView.subscribe('activeTabChange', function(ev){
	  var tabId = ev.newValue.get('postData');
	  Editors[tabId].resize();
  });
};
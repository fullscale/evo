EVO.ide.init = function(app) { 
  EVO.app.name = app;
  EVO.ide.layout.init();
  EVO.ide.navigator.init(app);
  tabView = new YAHOO.widget.TabView('evo-ide-editor');
  Editors = new Object;
  evo = new evoApp();
  tabView.subscribe('activeTabChange', function(ev){
	  var tabId = ev.newValue.get('postData');
	  Editors[tabId].resize();
  });
};

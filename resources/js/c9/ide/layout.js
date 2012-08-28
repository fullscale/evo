/** 
 * @projectDescription test
 * The ApplicationLayout module controls the layout of the pypes interface
 * using the YUI Layout widget. This module defines gutter sizes, scrollable
 * portions, the height/wdith of each setion, and whether or not the section
 * is resizable.
 *
 * @author Eric Gaumer egaumer@gmail.com
 * @version 1.0 
 */
C9.ide.layout = function () {

    Event = YAHOO.util.Event;
        
    return  {
                
        /**
         * Returns a function that will return a number one greater than the previous returned value, starting at n.
         * @alias fooBar
         * @alias FOO.Lib.fooBar
         * @param {Object} n    Number to start with. Default is 1.
         * @return {Function} Returns a function that will return a number one greater than the previous returned value.
         */
        init: function () {
	
            Event.onDOMReady(function() { 
                var layout = new YAHOO.widget.Layout({ 
                    units: [ 
                        { position: 'top', height: 26, body: 'top1', gutter: '0px', scroll: null, zIndex: 2 }, 
                        { position: 'bottom', height: 0, resize: false, body: 'bottom1', gutter: '0px'}, 
                        { position: 'left', width: 300, resize: true, body: 'left1', gutter: '0px', scroll: true }, 
                        { position: 'center', body: 'center1', gutter: '0px'} 
                    ] 
                }); 
                layout.on('render', function() { 
                    layout.getUnitByPosition('left').on('close', function() { 
                        closeLeft(); 
                    }); 
                }); 
                layout.render(); 
                Event.on('tLeft', 'click', function(ev) { 
                    Event.stopEvent(ev); 
                    layout.getUnitByPosition('left').toggle(); 
                });
            }); 
        }
    };

}();
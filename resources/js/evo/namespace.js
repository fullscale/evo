if (typeof EVO == "undefined") { var EVO = {}; }

EVO.namespace = function() {
    var a = arguments, o = null, i, j, d;
    for (i = 0; i < a.length; i = i + 1) {
        d = a[i].split(".");
        o = EVO;
        for (j = 0; j < d.length; j = j + 1) {
            o[d[j]] = o[d[j]] || {};
            o = o[d[j]];
        }
    }
    return o;
};

EVO.namespace("app");
EVO.namespace("ide.dialog");
EVO.namespace("ide.editor");
EVO.namespace("app.dialog");
EVO.namespace("api.query");
EVO.namespace("api.filter");
EVO.namespace("api.search");
EVO.namespace("api.index");
EVO.namespace("api.facet");
EVO.namespace("api.visual");

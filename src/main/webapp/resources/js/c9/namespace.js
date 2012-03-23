if (typeof C9 == "undefined") { var C9 = {}; }

C9.namespace = function() {
    var a = arguments, o = null, i, j, d;
    for (i = 0; i < a.length; i = i + 1) {
        d = a[i].split(".");
        o = C9;
        for (j = 0; j < d.length; j = j + 1) {
            o[d[j]] = o[d[j]] || {};
            o = o[d[j]];
        }
    }
    return o;
};

C9.namespace("app");
C9.namespace("ide.dialog");
C9.namespace("ide.editor");
C9.namespace("app.dialog");
C9.namespace("api.query");
C9.namespace("api.filter");
C9.namespace("api.search");
C9.namespace("api.index");
C9.namespace("api.facet");
C9.namespace("api.visual");
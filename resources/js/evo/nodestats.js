var barChart = function(dom_id) {
  if ('undefined' == typeof dom_id)  {
      dom_id = 'chart';
  }
  
  var data = function(json) {
      this.sum = json.reduce(function(a,b){return a+b;});
      this.data = json.map(function(x) {return [x];});
      return this;
  };

  var label = function(text) {
    this.labelText = text;
    return this;
  };
  
  unitText = ''
  var unit = function(unitTextarg) {
    unitText = unitTextarg;
    return this;
  };

  var draw = function() {
        w = 725,
        h = 25,
        x = pv.Scale.linear(0, this.sum).range(0, w),
        y = pv.Scale.ordinal(pv.range(1)).splitBanded(0, h, 4/5);

    var vis = new pv.Panel()
        .width(w)
        .height(h)
        .bottom(20)
        .left(90)
        .right(20)
        .top(5);

    var bar = vis.add(pv.Panel)
        .data(this.data)
      .add(pv.Bar)
        .data(function(a) {return a})
        .top(function() { return y(this.index)})
        .height(y.range().band)
        .left(pv.Layout.stack())
        .fillStyle(pv.colors("#ff505c", "#bfbfbf"))
        .width(x);
    
    bar.anchor("right");//.add(pv.Label)
        //.visible(function(d) {return d > .2})
        //.textStyle("white")
        //.text(function(d) {return d.toFixed(1) + unitText;}); // label on bar

    bar.anchor("left").add(pv.Label)
        .visible(function() {return !this.parent.index})
        .textMargin(5)
        .textAlign("right")
        .text(this.labelText); // y axis label

    vis.add(pv.Rule)
        .data(x.ticks())
        .left(function(d) {return Math.round(x(d)) - .5})
        .strokeStyle(function(d) {return d ? "rgba(255,255,255,.3)" : "#000"})
      .add(pv.Rule)
        .bottom(0)
        .height(5)
        .strokeStyle("#000")
      .anchor("bottom").add(pv.Label)
        .text(function(d) {return d.toFixed(1) + unitText;}); // x axis label

    vis.root.canvas(dom_id);
    vis.render();
  };
  
  return {
      data: data,
      draw: draw,
      label: label,
      unit: unit
  };
  
};
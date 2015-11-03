"use strict";

var frp = function() {
	var fs = [];
	var register_ = function(f) { fs.push(f); };
	var fire_ = function(data) { for (var i = 0; i < fs.length; i++) fs[i](data); };
	return {
		concat: function(frp_) {
			var frp1 = frp();
			register_(function(data) { frp1.fire(data); });
			frp_.register(function(data) { frp1.fire(data); });
			return frp1;
		}
		, filter: function(f) {
			var frp1 = frp();
			register_(function(data) { if (f(data)) frp1.fire(data); });
			return frp1;
		}
		, fold: function(f, value) {
			var frp1 = frp();
			register_(function(data) {
				value = f(value, data);
				frp1.fire(value);
			});
			return frp1;
		}
		, last: function() {
			var data_;
			register_(function(data) { data_ = data; });
			return function() { return data_; };
		}
		, map: function(f) {
			var frp1 = frp();
			register_(function(data) { frp1.fire(f(data)); });
			return frp1;
		}
		, register: register_
		, resample: function(frp_) {
			var data_;
			register_(function(data) { data_ = data; });
			var frp1 = frp();
			frp_.register(function(data) { frp1.fire(data_); });
			return frp1;
		}
		, fire: fire_
	};
};

var frpanimframe = function() {
	var frp_ = frp();
	var tick = function() {
		frp_.fire(true);
		requestAnimationFrame(tick);
	}
	requestAnimationFrame(tick);
	return frp_;
};

var frptick = function(timeout) {
	var frp_ = frp();
	var tick = function() {
		frp_.fire(true);
		setTimeout(tick, timeout);
	}
	setTimeout(tick, timeout);
	return frp_;
};

var keyboardfrps = {};
var mousemovefrp = frp();
var mousebuttonfrp = frp();

var tokeycode = function(e) { return (!(e.which)) ? e.keyCode : (e.which ? e.which : 0); };

var pressed_ = function(e, down) {
	var frp = keyboardfrps[(!(e.which)) ? e.keyCode : (e.which ? e.which : 0)];
	if (frp) frp.fire(down);
};

document.onkeydown = function(e) { pressed_(e, true); };
document.onkeyup = function(e) { pressed_(e, false); };
document.onmousedown = function(e) { mousebuttonfrp.fire(true); };
document.onmousemove = function(e) {
	var e1 = (!e) ? window.event : e;
	var x;
	var y;
	if (e1.pageX || e1.pageY) {
		x = e1.pageX;
		y = e1.pageY;
	} else if (e1.clientX || e1.clientY) {
		x = e1.clientX + document.body.scrollLeft + document.documentElement.scrollLeft;
		y = e1.clientY + document.body.scrollTop + document.documentElement.scrollTop;
	}
	mousemovefrp.fire({ x: x, y: y });
};
document.onmouseup = function(e) { mousebuttonfrp.fire(false); };

var frpkbpressed = function(keycode) { return keyboardfrps[keycode] = frp(); };
var frpkbleft = frpkbpressed(37).map(function(d) { return d ? -1 : 0; });
var frpkbright = frpkbpressed(39).map(function(d) { return d ? 1 : 0; });
var frpkbup = frpkbpressed(38).map(function(d) { return d ? -1 : 0; });
var frpkbdown = frpkbpressed(40).map(function(d) { return d ? 1 : 0; });
var frpkbarrowx = frpkbleft.concat(frpkbright); // .fold(function(a, b) { return a + b; }, 0).last();
var frpkbarrowy = frpkbup.concat(frpkbdown); // .fold(function(a, b) { return a + b; }, 0).last();
var frpmousebutton = mousebuttonfrp;
var frpmousemove = mousemovefrp;

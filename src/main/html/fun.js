"use strict";

var map = function(fun, ins) {
	var outs = [];
	for (var i = 0; i < ins.length; i++) outs.push(fun(ins[i]));
	return outs;
};

var filter = function(fun, ins) {
	var outs = [];
	for (var i = 0; i < ins.length; i++) {
		var object = ins[i];
		if (fun(object)) outs.push(object);
	}
	return outs;
};

var concat = function(ins) {
	var out = [];
	for (var i = 0; i < ins.length; i++) {
		var child = ins[i];
		for (var j = 0; j < child.length; j++)
			outs.push(child[j]);
	}
	return outs;
};

var fold = function(fun, value, ins) {
	for (var i = 0; i < ins.length; i++)
		value = fun(value, ins[i]);
	return value;
};

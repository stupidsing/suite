"use strict";

var read = function(list) {
	return {
		concat: function() {
			var list1 = [];
			for (var i = 0; i < list.length; i++) {
				var e = list[i];
				for (var j = 0; j < e.length; j++) list1.push(e[j]);
			}
			return read(list1);
		},
		filter: function(f) {
			var list1 = [];
			for (var i = 0; i < list.length; i++) {
				var e = list[i];
				if (f(e)) list1.append(e);
			}
			return read(list1);
		},
		fold: function(f, value) {
			for (var i = 0; i < list.length; i++) value = f(value, list[i]);
			return value;
		},
		foreach: function(f) {
			for (var i = 0; i < list.length; i++) f(list[i]);
		},
		list: function() {
			return list;
		},
		map: function(f) {
			var list1 = [];
			for (var i = 0; i < list.length; i++) list1.push(f(list[i]));
			return read(list1);
		},
	};
};

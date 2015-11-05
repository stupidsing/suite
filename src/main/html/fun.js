"use strict";

var read = list => {
	return {
		concat: () => {
			var list1 = [];
			for (var i = 0; i < list.length; i++) {
				var e = list[i];
				for (var j = 0; j < e.length; j++) list1.push(e[j]);
			}
			return read(list1);
		},
		filter: f => {
			var list1 = [];
			for (var i = 0; i < list.length; i++) {
				var e = list[i];
				if (f(e)) list1.append(e);
			}
			return read(list1);
		},
		fold: (f, value) => {
			for (var i = 0; i < list.length; i++) value = f(value, list[i]);
			return value;
		},
		foreach: f => {
			for (var i = 0; i < list.length; i++) f(list[i]);
		},
		list: () => list,
		map: f => {
			var list1 = [];
			for (var i = 0; i < list.length; i++) list1.push(f(list[i]));
			return read(list1);
		},
	};
};

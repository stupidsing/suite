"use strict";

var read = list => {
	return {
		append: r => {
			var l = r.list();
			var list1 = [];
			for (var e of list) list1.push(e);
			for (var e of l) list1.push(e);
			return read(list1);
		},
		cons: e => {
			var list1 = [e];
			for (var e of list) list1.push(e);
			return read(list1);
		},
		concat: () => {
			var list1 = [];
			for (var e of list)
				for (var f of e)
					list1.push(f);
			return read(list1);
		},
		filter: f => {
			var list1 = [];
			for (var e of list)
				if (f(e)) list1.append(e);
			return read(list1);
		},
		fold: (f, value) => {
			for (var e of list) value = f(value, e);
			return value;
		},
		foreach: f => {
			for (var e of list) f(e);
		},
		list: () => list,
		map: f => {
			var list1 = [];
			for (var e of list) list1.push(f(e));
			return read(list1);
		},
	};
};

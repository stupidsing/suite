"use strict";

let read = list => {
	return {
		append: r => {
			let l = r.list();
			let list1 = [];
			for (let e of list) list1.push(e);
			for (let e of l) list1.push(e);
			return read(list1);
		},
		cons: e => {
			let list1 = [e];
			for (let e of list) list1.push(e);
			return read(list1);
		},
		concat: () => {
			let list1 = [];
			for (let e of list)
				for (let f of e)
					list1.push(f);
			return read(list1);
		},
		filter: f => {
			let list1 = [];
			for (let e of list)
				if (f(e)) list1.append(e);
			return read(list1);
		},
		fold: (f, value) => {
			for (let e of list) value = f(value, e);
			return value;
		},
		foreach: f => {
			for (let e of list) f(e);
		},
		list: () => list,
		map: f => {
			let list1 = [];
			for (let e of list) list1.push(f(e));
			return read(list1);
		},
	};
};

"use strict";

let lens_index = (index, f) => list0 => [...list0.slice(0, index), f(list0[index]), ...list0.slice(index + 1, list0.length),];
let lens_key = (key, f) => object0 => ({ ...object0, [key]: f(object0[key]), });

let lens = f => ({
	apply: f,
	index: index => lens(lens_index(index, f)),
	key: key => lens(lens_key(key, f)),
});

let read = list => {
	return {
		append: r => {
			let l = r.list();
			let list1 = [];
			for (let e of list) list1.push(e);
			for (let e of l) list1.push(e);
			return read(list1);
		},
		concat: () => {
			let list1 = [];
			for (let e of list)
				for (let f of e)
					list1.push(f);
			return read(list1);
		},
		cons: e => {
			let list1 = [e];
			for (let e of list) list1.push(e);
			return read(list1);
		},
		filter: f => {
			let list1 = [];
			for (let e of list)
				f(e) && list1.push(e);
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
		range: (s, e) => {
			let list = [];
			for (let i = s; i < e; i++) list.push(i);
			return read(list);
		},
	};
};

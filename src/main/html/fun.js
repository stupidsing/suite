"use strict";

let lens_ = gp => {
	return {
		apply: f => object0 => {
			let { g: object1, p, } = gp(object0);
			return p(f(object1));
		},
		index: index => lens_(object0 => {
			let { g: object1, p, } = gp(object0);
			return {
				g: object1[index],
				p: value => p([...object1.slice(0, index), value, ...object1.slice(index + 1, object1.length),]),
			};
		}),
		key: key => lens_(object0 => {
			let { g: object1, p, } = gp(object0);
			return {
				g: object1[key],
				p: value => p({ ...object1, [key]: value, }),
			};
		}),
	};
};

let lens = lens_(object => ({ g: object, p: value => value, }));

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

'use strict';

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

let read_ = iter => {
	return {
		append: r => {
			return read_(() => {
				let its = [iter(), r.iter(),];
				return () => {
					let e;
					while (0 < its.length && (e = its[0]()) == null) its.shift();
					return e;
				};
			});
		},
		concat: () => read_(() => {
			let it0 = iter();
			let it1 = null;
			return () => {
				let e;
				while (it1 == null || (e = it1()) == null) {
					let iter1 = it0();
					if (iter1 != null) it1 = iter1.iter(); else return null;
				}
				return e;
			};
		}),
		cons: e => read_(() => {
			let it = iter();
			let i = 0;
			return () => i++ == 0 ? e : it();
		}),
		filter: f => read_(() => {
			let it = iter();
			return () => {
				let e;
				while ((e = it()) != null && !f(e));
				return e;
			};
		}),
		fold: (value, f) => {
			let it = iter(), e;
			while ((e = it()) != null) value = f(value, e);
			return value;
		},
		foreach: f => {
			let it = iter(), e;
			while ((e = it()) != null) f(e);
		},
		list: () => {
			let list = [];
			let it = iter(), e;
			while ((e = it()) != null) list.push(e);
			return list;
		},
		map: f => read_(() => {
			let it = iter();
			return () => {
				let e = it();
				return e != null ? f(e) : null;
			};
		}),
		object: () => {
			let ob = {};
			let it = iter(), e;
			while ((e = it()) != null) ob[e[0]] = e[1];
			return ob;
		},
		range: (s, e) => read_(() => {
			let i = s;
			return () => i < e ? i++ : null;
		}),
		iter,
	};
};

let read = object => read_(() => {
	let list = typeof object == 'string' || Array.isArray(object) ? object : Object.entries(object);
	let i = 0;
	return () => i < list.length ? list[i++] : null;
});

// test
// read([read([0, 1,]), read([2, 3,])]).concat().append(read([4, 5, 6,])).cons(-1).foreach(s => console.log(s))

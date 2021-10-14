'use strict';

loadedmodule = globalThis.render = evalscript('fun.js').then(({ read, }) => {
	let verifyList = (parent, nodes) => {
		let e = parent.lastChild;
		for (let i = nodes.length - 1; 0 <= i; i--)
			while (e != nodes[i])
				e = e.previousSibling;
		return nodes;
	}

	let verifyCud = cud => {
		verifyList(cud.parent.childRef, [null, cud.childRef0, cud.childRef,]);
		return cud;
	};

	let r_cud = (parent, domc0, domcx) => {
		let delete_ = cud => {
			while (cud.childRef0 != cud.childRef) {
				let prev = cud.childRef.previousSibling;
				parent.childRef.removeChild(cud.childRef);
				cud.setTail(prev);
			}
		};

		let insert_ = (cud, c) => {
			let parentRef = parent.childRef;
			let childRef_ = cud.childRef;
			cud.setTail(parentRef.insertBefore(c, childRef_ != null ? childRef_.nextSibling : parentRef.firstChild));
		};

		let cud = verifyCud({
			childRef0: domc0, // exclusive
			childRef: domcx, // inclusive
			create: c => { insert_(cud, c); verifyCud(cud); },
			delete: () => { delete_(cud); verifyCud(cud); },
			get: domc => r_cud(cud, domc.previousSibling, domc),
			parent,
			setTail: c => { cud.childRef = c; verifyCud(cud); },
			update: c => { delete_(cud); insert_(cud, c); verifyCud(cud); },
		});

		return cud;
	};

	let gwm = new WeakMap(); // global weak map

	let getOrAdd = (map0, key) => {
		let map1 = map0.get(key);
		if (map1 == null) map0.set(key, map1 = new Map());
		return map1;
	};

	let gwmget = (key0, key1) => getOrAdd(getOrAdd(gwm, key0), key1);

	let isClear = (vm0, vm1) => vm0 == vm1;

	/*
		a typical "render-difference" function accept 3 parameters:
		vm0 - old view model, null to append DOM elements
		vm1 - new view model, null to remove DOM elements
		cudf - DOM manipulator (create, update, delete)
		The renderer should detect the differences and apply changes using cud.
	*/

	let rdt_attrs = attrs => (vm0, vm1, cudf) => {
		if (vm0 == null)
			for (let [key, value] of Object.entries(attrs))
				cudf.childRef.setAttribute(key, value);
		if (vm1 == null)
			for (let [key, value] of Object.entries(attrs))
				cudf.childRef.removeAttribute(key);
	};

	let rdt_attrsf = attrsf => (vm0, vm1, cudf) => {
		if (isClear(vm0, vm1))
			;
		else if (vm1 != null)
			for (let [key, value] of Object.entries(attrsf(vm1)))
				if (key !== 'value')
					cudf.childRef.setAttribute(key, value);
				else
					cudf.childRef[key] = value;
		else
			for (let [key, value] of Object.entries(attrsf(vm0)))
				if (key !== 'value')
					cudf.childRef.removeAttribute(key);
				else
					delete cudf.childRef[key];
	};

	let rdt_child = childf => (vm0, vm1, cudf) =>
		isClear(vm0, vm1)
		|| childf(vm0, vm1, r_cud(cudf, null, cudf.childRef.lastChild));

	let rdt_forRange = (vmsf, rangef, rd_item) => (vm0, vm1, cudf) => {
		let domc = cudf.childRef;
		let children0 = domc != null ? Array.from(domc.childNodes) : null;

		if (isClear(vm0, vm1))
			;
		else if (vm0 == null) {
			let [s, e] = rangef(vm1), vms1 = vmsf(vm1);
			for (let i1 = s; i1 < e; i1++)
				rd_item(null, vms1[i1], r_cud(cudf, domc.lastChild, domc.lastChild));
		} else if (vm1 == null) {
			let [s, e] = rangef(vm0), vms0 = vmsf(vm0);
			for (let i0 = s; i0 < e; i0++)
				rd_item(vms0[i0], null, cudf.get(children0[i0 - s]));
		} else {
			let [si, ei] = rangef(vm0), vms0 = vmsf(vm0);
			let [sx, ex] = rangef(vm1), vms1 = vmsf(vm1);
			let s_ = si;
			let e_ = ei;

			// remove elements at start and end of range
			while (s_ < e_ && s_ < sx)
				rd_item(vms0[s_++], null, r_cud(cudf, null, domc.firstChild));
			while (s_ < e_ && ex < e_)
				rd_item(vms0[--e_], null, r_cud(cudf, domc.lastChild.previousSibling, domc.lastChild));

			// relocate range if empty
			if (s_ == e_) s_ = e_ = sx;

			// insert elements at start and end of range
			while (sx < s_)
				rd_item(null, vms1[--s_], r_cud(cudf, null, null));
			while (e_ < ex)
				rd_item(null, vms1[e_++], r_cud(cudf, domc.lastChild, domc.lastChild));

			// update elements at common range
			for (let i = Math.max(si, sx); i < Math.min(ei, ex); i++)
				rd_item(vms0[i], vms1[i], cudf.get(domc.childNodes[i - s_]));
		}
	};

	let rdt_listen = (event, cb) => {
		let els = {};
		return (vm0, vm1, cudf) => {
			let domc = cudf.childRef;
			if (isClear(vm0, vm1))
				;
			else {
				if (vm0 != null) {
					domc.removeEventListener(event, els[domc]);
					delete els[domc];
				}
				if (vm1 != null) {
					domc.addEventListener(event, els[domc] = ev => cb(ev, vm1));
				}
			}
		};
	};

	let rdt_style = style => (vm0, vm1, cudf) => {
		if (vm0 == null)
			for (let [key, value] of Object.entries(style))
				cudf.childRef.style[key] = value;
		if (vm1 == null)
			for (let [key, value] of Object.entries(style))
				cudf.childRef.style[key] = null;
	};

	let rdt_stylef = stylef => (vm0, vm1, cudf) => {
		if (isClear(vm0, vm1))
			;
		else if (vm1 != null)
			for (let [key, value] of Object.entries(stylef(vm1)))
				cudf.childRef.style[key] = value;
		else
			for (let [key, value] of Object.entries(stylef(vm0)))
				cudf.childRef.style[key] = null;
	};

	let rd_dom = elementf => (vm0, vm1, cudf) => {
		if (isClear(vm0, vm1))
			;
		else {
			vm0 != null && cudf.delete();
			vm1 != null && cudf.create(elementf(vm1));
		}
	};

	let rd_domDecors = (elementf, decorfs) => (vm0, vm1, cudf) => {
		if (vm0 == null)
			cudf.create(elementf());
		if (isClear(vm0, vm1))
			;
		else
			for (let decorf of decorfs)
				decorf(vm0, vm1, cudf);
		if (vm1 == null)
			cudf.delete();
	};

	let rd_for = (keyf, rd_item) => {
		let key = {};

		return (vm0, vm3, cudf) => {
			if (isClear(vm0, vm3))
				;
			else {
				let parent = cudf.parent;
				let domc = parent.childRef;
				let cm = gwmget(domc, key);
				let vm1 = [];
				let list0;
				let cud;

				if (vm0 != null) {
					list0 = cm.get(vm0);
					list0[0] = cudf.childRef0;
				} else {
					vm0 = [];
					list0 = [cudf.childRef0,];
				}

				vm3 = vm3 != null ? vm3 : [];

				let map1 = new Map();
				let map2 = new Map();
				let map3 = new Map();
				for (let i3 = 0; i3 < vm3.length; i3++)
					map3.set(keyf(vm3[i3]), i3);

				let list1 = [list0[0]];
				let i1 = 0;

				for (let i0 = 0; i0 < vm0.length; i0++) {
					let vm = vm0[i0];
					let key = keyf(vm);

					if (list0[i0] == list0[i0 + 1])
						cud = r_cud(parent, list1[i1], list1[i1]);
					else
						cud = r_cud(parent, list1[i1], list0[i0 + 1]);

					if (!map1.has(key) && map3.has(key))
						map1.set(key, i1);
					else
						rd_item(vm, null, cud);

					vm1.push(vm);
					list1[++i1] = cud.childRef;
				}

				let list2 = [list1[0]];
				let vm2 = vm3;

				for (let i2 = 0; i2 < vm2.length; i2++) {
					let key = keyf(vm2[i2]);
					let i1 = map1.get(key);

					if (!map2.has(key) && i1 != null) { // transplant DOM children
						map2.set(key, i2);
						let child0 = list1[i1];
						let childx = list1[i1 + 1];
						let childRef = list2[i2];

						if (child0 != list2[i2]) {
							let list = [];
							let c = child0;

							while (c != childx)
								list.push(c = c != null ? c.nextSibling : domc.firstChild);

							for (let node of list)
								domc.removeChild(node);

							let before = childRef != null ? childRef.nextSibling : domc.firstChild;

							for (let node of list)
								domc.insertBefore(node, before);
						}

						rd_item(vm1[i1], vm2[i2], cud = r_cud(parent, list2[i2], childx));
						list2[i2 + 1] = list1[i2 + 1] = cud.childRef;
					} else
						list2[i2 + 1] = list2[i2];
				}

				let list3 = [list2[0]];

				for (let i3 = 0; i3 < vm3.length; i3++) {
					let key = keyf(vm3[i3]);
					let i2 = map2.get(key);

					if (i2 != null)
						if (list2[i2] == list2[i2 + 1])
							cud = r_cud(parent, list3[i3], list3[i3]);
						else
							cud = r_cud(parent, list3[i3], list2[i2 + 1]);
					else
						rd_item(null, vm3[i3], cud = r_cud(parent, list3[i3], list3[i3]));
					list3[i3 + 1] = cud.childRef;
				}

				cudf.setTail(list3[vm3.length]);
				cm.delete(vm0);
				cm.set(vm3, verifyList(domc, list3));
			}
		};
	};

	let rd_ifElse = (iff, thenf, elsef) => (vm0, vm1, cudf) => {
		if (isClear(vm0, vm1))
			;
		else {
			let f0 = vm0 != null ? (iff(vm0) ? thenf : elsef) : null;
			let f1 = vm1 != null ? (iff(vm1) ? thenf : elsef) : null;

			if (f0 == f1)
				f0(vm0, vm1, cudf);
			else {
				f0 != null && f0(vm0, null, cudf);
				f1 != null && f1(null, vm1, cudf);
			}
		}
	};

	let rd_list = childrenfs => {
		let key = {};

		if (childrenfs.length == 0)
			return (vm0, vm1, cudf) => {};
		else if (childrenfs.length == 1)
			return childrenfs[0];
		else
			return (vm0, vm1, cudf) => {
				if (isClear(vm0, vm1))
					;
				else {
					let parent = cudf.parent;
					let domc = parent.childRef;
					let cm = gwmget(domc, key);
					let list0 = cm.get(vm0);
					let list1 = [cudf.childRef0,];
					let cud;

					for (let i = 0; i < childrenfs.length; i++) {
						if (vm0 == null || list0[i] == list0[i + 1])
							childrenfs[i](vm0, vm1, cud = r_cud(parent, list1[i], list1[i]));
						else
							childrenfs[i](vm0, vm1, cud = r_cud(parent, list1[i], list0[i + 1]));
						list1[i + 1] = cud.childRef;
					}

					cudf.setTail(list1[childrenfs.length]);
					cm.delete(vm0);
					cm.set(vm1, verifyList(domc, list1));
				}
			};
	};

	let rd_map = (vmf, rdf) => {
		let wm = new WeakMap();

		let apply = vm => {
			let result = wm.get(vm);
			if (result == null) wm.set(vm, result = vmf(vm));
			return result;
		};

		return (vm0, vm1, cudf) => isClear(vm0, vm1) || rdf(
			vm0 != null ? apply(vm0) : null,
			vm1 != null ? apply(vm1) : null,
			cudf);
	};

	let rd_span_component = (initf, deinitf, publicf, privatef, xhtml) => vm => {
		let span = document.createElement('span');
		let cudf_;
		let vm_ = null;
		let view;

		let change = f => {
			let pvm_ = vm_;
			view(pvm_, vm_ = f(pvm_), cudf_);
		};

		let changeAsync = f => {
			let vm0 = vm_;
			f(vm0).then(vm1 => {
				if (vm_ === vm0) {
					view(vm_, vm1, cudf_);
					vm_ = vm1;
				} else
					console.error('race condition in view updates');
			});
		};

		let muts = { change, changeAsync, };

		view = rd.parse(privatef(muts), xhtml);

		return {
			init: cudf => {
				cudf.create(span);
				cudf_ = r_cud({ childRef: span, }, null, span.lastChild);
				change(vm_ => initf(vm));
			},
			deinit: cudf => {
				change(vm_ => deinitf(vm));
				change(vm_ => null);
				cudf.delete();
			},
			...publicf(muts),
		};
	};

	let rd_switch = routes => (vm0, vm1, cudf) => {
		let key0 = vm0?.k;
		let key1 = vm1?.k;
		if (key0 != key1) {
			if (key0 != null) routes[key0](vm0.v, null, cudf);
			if (key1 != null) routes[key1](null, vm1.v, cudf);
		} else if (key0 != null)
			routes[key0](vm0.v, vm1.v, cudf);
	};

	let rdb_tagf = (elementf, decorfs) => {
		let decor = decorf => rdb_tagf(elementf, [...decorfs, decorf,]);
		let attrs = attrs => decor(rdt_attrs(attrs));
		let child = childf => decor(rdt_child(childf));

		return {
			attr: (key, value) => attrs({ [key]: value, }),
			attrs,
			attrsf: attrsf => decor(rdt_attrsf(attrsf)),
			child,
			children: (...childrenfs) => child(rd_list(childrenfs)),
			decor,
			listen: (event, cb) => decor(rdt_listen(event, cb)),
			rd: () => rd_domDecors(elementf, decorfs),
			style: style => decor(rdt_style(style)),
			stylef: stylef => decor(rdt_stylef(stylef)),
			text: () => child(rd_dom(vm => document.createTextNode(vm))),
		};
	};

	let rdb_tag = tag => rdb_tagf(() => document.createElement(tag), []);

	let rdb_vscrollf = (height, rowHeight, rd_item, cbScroll) => {
		let nItemsShown = Math.floor(height / rowHeight) + 1;

		return rdb_tag('div')
			.style({ height: height + 'px', overflow: 'auto', position: 'absolute', })
			.listen('scroll', (ev, vm) => cbScroll(Math.floor(ev.target.scrollTop / rowHeight)))
			.child(rdb_tag('div')
				.stylef(vm => ({
					height: (vm.vms.length - vm.start) * rowHeight + 'px',
					position: 'relative',
					top: vm.start * rowHeight + 'px',
				}))
				.decor(rdt_forRange(
					vm => vm.vms,
					vm => [vm.start, vm.start + nItemsShown],
					rdb_tag('div').style({ height: rowHeight + 'px', }).child(rd_item).rd()))
				.rd()
			);
	};

	let rd_parse = (env, s) => {
		let parseLambda = (v, s) => {
			let e = s.startsWith('{') && s.endsWith('}') ? s : '(' + s + ')';
			return eval(v + ' => ' + e);
		};

		let parseExpr = s =>  s != null ? parseLambda('vm', s) : vm => vm;
		let parseListen = s => parseLambda('(ev, vm)', s);

		let parseTemplate = s => {
			let pos0 = 0, pos1, pos2;
			let f = vm => '';
			while (0 <= (pos1 = s.indexOf('{', pos0)) && 0 <= (pos2 = s.indexOf('}', pos1))) {
				let s0 = s.substring(pos0, pos1);
				let f0 = f;
				let f1 = parseExpr(s.substring(pos1 + 1, pos2).trim());
				f = vm => f0(vm) + s0 + f1(vm);
				pos0 = pos2 + 1;
			}
			{
				let f0 = f;
				f = vm => f0(vm) + s.substring(pos0);
			}
			return f;
		};

		let parseDom = node0 => {
			if (node0.nodeType == Node.COMMENT_NODE) {
				let sf = parseTemplate(node0.nodeValue);
				return rd_dom(vm => document.createComment(sf(vm)));
			} else if (node0.nodeType == Node.ELEMENT_NODE)
				if (node0.localName == 'rd_component') {
					let icf = eval(node0.getAttribute('v'));
					let ics = {};
					return (vm0, vm1, cudf) => {
						if (isClear(vm0, vm1))
							;
						else {
							if (vm0 != null) {
								ics[vm0].deinit(cudf);
								delete ics[vm0];
							}
							if (vm1 != null) {
								(ics[vm1] = icf(vm1)).init(cudf);
							}
						}
					};
				} else if (node0.localName == 'rd_for')
					return rd_map(parseExpr(node0.getAttribute('v')), rd_for(vm => vm, parseDomNodes(node0.childNodes)));
				else if (node0.localName == 'rd_if')
					return rd_ifElse(parseExpr(node0.getAttribute('v')), parseDomNodes(node0.childNodes), (vm0, vm1, cudf) => {});
				else if (node0.localName == 'rd_map')
					return rd_map(parseExpr(node0.getAttribute('v')), parseDomNodes(node0.childNodes));
				else if (node0.localName == 'rd_switch')
					return rd_switch(Object.fromEntries(Array
						.from(node0.childNodes)
						.filter(n => n.nodeType == Node.ELEMENT_NODE)
						.map(n => [n.getAttribute('k'), parseDom(n),])));
				else if (node0.localName == 'rd_view')
					return eval(node0.getAttribute('v'));
				else {
					let name = node0.localName;
					let as = {}, cs = parseDomNodes(node0.childNodes);
					let decors = [];

					for (let attr of node0.attributes)
						if (attr.name.startsWith('rd_on_'))
							decors.push(rd => rd.listen(attr.name.substring(6), parseListen(attr.value)));
						else
							as[attr.name] = parseTemplate(attr.value);

					let rd = rdb_tag(name).attrsf(vm => read(as).map(([k, vf]) => [k, vf(vm)]).object()).child(cs);
					for (let decor of decors) rd = decor(rd);
					return rd.rd();
				}
			else if (node0.nodeType == Node.TEXT_NODE) {
				let sf = parseTemplate(node0.nodeValue);
				return rd_dom(vm => document.createTextNode(sf(vm)));
			} else
				throw 'unknown node type';
		};

		let parseDomNodes = nodes => rd_list(Array.from(nodes).map(parseDom));

		return parseDom(new DOMParser().parseFromString(s, 'text/xml').childNodes[0]);
	};

	let rd = {
		div: () => rdb_tag('div'),
		dom: rd_dom,
		for: rd_for,
		if_: (iff, thenf) => rd_ifElse(iff, thenf, rd_dom(vm => document.createComment('else'))),
		ifElse: rd_ifElse,
		li: () => rdb_tag('li'),
		list: rd_list,
		map: rd_map,
		p: () => rdb_tag('p'),
		parse: rd_parse,
		span: () => rdb_tag('span'),
		span_component: rd_span_component,
		tag: rdb_tag,
		ul: () => rdb_tag('ul'),
		vscrollf: rdb_vscrollf,
	};

	let pvm = null;
	let target = document.getElementById('target');
	let cudf_ = r_cud({ childRef: target, }, null, target.lastChild);

	let renderAgain = (renderer, f) => {
		let ppvm = pvm;
		renderer(ppvm, pvm = f(pvm), cudf_);
	};

	return { rd, renderAgain, };
});

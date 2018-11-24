'use strict';

let verify_ = (parent, s0, sx) => {
	if (s0 != null && s0.parentNode != parent) throw 'fail';
	if (sx != null && sx.parentNode != parent) throw 'fail';
	let e = parent.lastChild;
	while (e != sx) e = e.previousSibling;
	while (e != s0) e = e.previousSibling;
	while (e != null) e = e.previousSibling;
};

let verifyChildren = (parent, children) => {
	if (children != null)
		for (let i = 1; i < children.length; i++)
			verify_(parent, children[i - 1], children[i]);
};

let r_cud = (dom, domc0, domcx) => {
	let verify = cud => {
		verify_(dom, cud.childRef0, cud.childRef);
		return cud;
	};

	let delete_ = cud => {
			while (cud.childRef0 != cud.childRef) {
				let prev = cud.childRef.previousSibling;
				dom.removeChild(cud.childRef);
				cud.childRef = prev;
			}
	};

	let insert_ = (cud, c) => {
		let childRef_ = cud.childRef;
		cud.childRef = dom.insertBefore(c, childRef_ != null ? childRef_.nextSibling : dom.firstChild);
	};

	let cud = verify({
		childRef0: domc0, // exclusive
		childRef: domcx, // inclusive
		create: c => insert_(cud, c),
		delete: () => delete_(cud),
		parentRef: dom,
		update: c => { delete_(cud); insert_(cud, c); },
	});

	return cud;
};

let r_cudChild = (dom, domc) => r_cud(dom, domc.previousSibling, domc);

/*
	a typical "render-difference" function accept 3 parameters:
	vm0 - old view model, null to append DOM elements
	vm1 - new view model, null to remove DOM elements
	cudf - DOM manipulator (create, update, delete)
	The renderer should detect the differences and apply changes using cud.
*/

let rdt_attrs = attrs => (wm, vm0, vm1, cudf) => {
	if (vm0 == null)
		for (let [key, value] of Object.entries(attrs))
			cudf.childRef.setAttribute(key, value);
	if (vm1 == null)
		for (let [key, value] of Object.entries(attrs))
			cudf.childRef.removeAttribute(key);
};

let rdt_attrsf = attrsf => (wm, vm0, vm1, cudf) => {
	if (vm0 == vm1)
		;
	else if (vm1 != null)
		for (let [key, value] of Object.entries(attrsf(vm1)))
			cudf.childRef.setAttribute(key, value);
	else
		for (let [key, value] of Object.entries(attrsf(vm0)))
			cudf.childRef.removeAttribute(key);
};

let rdt_child = childf => (wm, vm0, vm1, cudf) => {
	if (vm0 == vm1)
		;
	else {
		let domc = cudf.childRef;
		childf(vm0, vm1, r_cud(domc, null, domc.lastChild));
	}
};

let rdt_children = childrenfs => (wm, vm0, vm1, cudf) => {
	if (vm0 == vm1)
		;
	else {
		let domc = cudf.childRef;
		let children0 = wm.get(domc);
		let children1 = [null,];

		for (let i = 0; i < childrenfs.length; i++) {
			let cud;
			if (vm0 == null)
				cud = r_cud(domc, domc.lastChild, domc.lastChild);
			else
				cud = r_cud(domc, children1[i], children0[i + 1]);
			childrenfs[i](vm0, vm1, cud);
			children1.push(cud.childRef);
		}

		wm.set(domc, children1);
	}
};

let rdt_eventListener = (event, cb) => (wm, vm0, vm1, cudf) => {
	if (vm0 == vm1)
		;
	else {
		vm0 != null && cudf.childRef.removeEventListener(event, cb);
		vm1 != null && cudf.childRef.addEventListener(event, cb);
	}
};

let rdt_for = (keyf, rd_item) => (wm, vm0, vm1, cudf) => {
	if (vm0 == vm1)
		;
	else {
		let domc0 = cudf.childRef;
		let children0 = wm.get(domc0);
		let children1 = [null,];

		if (vm0 == null)
			for (let i1 = 0; i1 < vm1.length; i1++) {
				let cud = r_cud(domc0, domc0.lastChild, domc0.lastChild);
				rd_item(null, vm1[i1], cud);
				children1.push(cud.childRef);
			}
		else if (vm1 == null)
			for (let i0 = 0; i0 < vm0.length; i0++)
				rd_item(vm0[i0], null, r_cud(domc0, children0[i0], children0[i0 + 1]));
		else {
			let map0 = new Map();
			let map1 = new Map();

			for (let i0 = 0; i0 < vm0.length; i0++)
				map0.set(keyf(vm0[i0]), i0);
			for (let i1 = 0; i1 < vm1.length; i1++)
				map1.set(keyf(vm1[i1]), i1);

			let isSameOrder = vm0.length == vm1.length;

			for (let i1 = 0; i1 < vm1.length; i1++) {
				let i0 = map0.get(keyf(vm1[i1]));
				isSameOrder &= i0 == i1;
			}

			if (isSameOrder)
				for (let i = 0; i < vm1.length; i++) {
					let cud = r_cud(domc0, children1[i], children0[i + 1]);
					rd_item(vm0[i], vm1[i], cud);
					children1.push(cud.childRef);
				}
			else {
				let prevSiblingMap = new Map();

				for (let child of domc0.childNodes)
					prevSiblingMap.set(child, child.previousSibling);

				let domc1 = domc0.cloneNode(false);
				cudf.update(domc1);

				for (let i1 = 0; i1 < vm1.length; i1++) {
					let i0 = map0.get(keyf(vm1[i1]));
					let prev = domc1.lastChild;
					let cud;

					if (i0 != null) {
						let child0 = children0[i0];
						let childx = children0[i0 + 1];
						let list = [];

						while (child0 != childx) {
							list.push(childx);
							childx = prevSiblingMap.get(childx);
						}

						while (0 < list.length)
							domc1.insertBefore(list.pop(), null);

						rd_item(vm0[i0], vm1[i1], cud = r_cud(domc1, prev, domc1.lastChild));
					} else
						rd_item(null, vm1[i1], cud = r_cud(domc1, prev, prev));

					children1.push(cud.childRef);
				}

				for (let i0 = 0; i0 < vm0.length; i0++)
					if (!map1.has(keyf(vm0[i0])))
						rd_item(vm0[i0], null, r_cud(domc0, children0[i0], children0[i0 + 1]));

				domc0 = domc1;
			}
		}

		wm.set(domc0, children1);
	}
};

let rdt_forRange = (vmsf, rangef, rd_item) => (wm, vm0, vm1, cudf) => {
	let domc = cudf.childRef;
	let children0 = domc != null ? Array.from(domc.childNodes) : null;

	if (vm0 == vm1)
		;
	else if (vm0 == null) {
		let [s, e] = rangef(vm1), vms1 = vmsf(vm1);
		for (let i1 = s; i1 < e; i1++)
			rd_item(null, vms1[i1], r_cud(domc, domc.lastChild, domc.lastChild));
	} else if (vm1 == null) {
		let [s, e] = rangef(vm0), vms0 = vmsf(vm0);
		for (let i0 = s; i0 < e; i0++)
			rd_item(vms0[i0], null, r_cud(domc, children0[i0 - 1], children0[i0]));
	} else {
		let [si, ei] = rangef(vm0), vms0 = vmsf(vm0);
		let [sx, ex] = rangef(vm1), vms1 = vmsf(vm1);
		let s_ = si;
		let e_ = ei;

		// remove elements at start and end of range
		while (s_ < e_ && s_ < sx)
			rd_item(vms0[s_++], null, r_cud(domc, null, domc.firstChild));
		while (s_ < e_ && ex < e_)
			rd_item(vms0[--e_], null, r_cud(domc, domc.lastChild.previousSibling, domc.lastChild));

		// relocate range if empty
		if (s_ == e_) s_ = e_ = sx;

		// insert elements at start and end of range
		while (sx < s_)
			rd_item(null, vms1[--s_], r_cud(domc, null, null));
		while (e_ < ex)
			rd_item(null, vms1[e_++], r_cud(domc, domc.lastChild, domc.lastChild));

		// update elements at common range
		for (let i = Math.max(si, sx); i < Math.min(ei, ex); i++)
			rd_item(vms0[i], vms1[i], r_cudChild(domc, domc.childNodes[i - s_]));
	}
};

let rdt_style = style => (wm, vm0, vm1, cudf) => {
	if (vm0 == null)
		for (let [key, value] of Object.entries(style))
			cudf.childRef.style[key] = value;
	if (vm1 == null)
		for (let [key, value] of Object.entries(style))
			cudf.childRef.style[key] = null;
};

let rdt_stylef = stylef => (wm, vm0, vm1, cudf) => {
	if (vm0 == vm1)
		;
	else if (vm1 != null)
		for (let [key, value] of Object.entries(stylef(vm1)))
			cudf.childRef.style[key] = value;
	else
		for (let [key, value] of Object.entries(stylef(vm0)))
			cudf.childRef.style[key] = null;
};

let gwm = new WeakMap();

let rd_dom = elementf => (vm0, vm1, cudf) => {
	if (vm0 == vm1)
		;
	else {
		vm0 != null && cudf.delete();
		vm1 != null && cudf.create(elementf(vm1));
	}
};

let rd_domDecors = (elementf, decorfs) => {
	let wm = new WeakMap(); // map from DOM element to list of children DOM

	return (vm0, vm1, cudf) => {
		if (vm0 == null)
			cudf.create(elementf());
		if (vm0 == vm1)
			;
		else
			for (let decorf of decorfs)
				decorf(wm, vm0, vm1, cudf);
		if (vm1 == null)
			cudf.delete();
	};
};

let rd_ifElse = (iff, thenf, elsef) => (vm0, vm1, cudf) => {
	if (vm0 == vm1)
		;
	else {
		let f0 = vm0 != null && (iff(vm0) ? thenf : elsef);
		let f1 = vm1 != null && (iff(vm1) ? thenf : elsef);

		if (f0 == f1)
			f0(vm0, vm1, cudf);
		else {
			f0 != null && f0(vm0, null, cudf);
			f1 != null && f1(null, vm1, cudf);
		}
	}
};

let rd_list = childrenfs => (vm0, vm1, cudf) => {
	if (vm0 == vm1)
		;
	else {
		let domc = cudf.parentRef;
		let map0 = gwm.get(domc);
		let map1;
		if (map0 != null)
			map1 = map0;
		else
			gwm.set(domc, map1 = new Map());
		let list0 = map1.get(vm0);
		let list1 = [cudf.childRef0,];

		for (let i = 0; i < childrenfs.length; i++) {
			let cud;
			if (vm0 == null)
				cud = cudf;
			else
				cud = r_cud(domc, list1[i], list0[i + 1]);
			childrenfs[i](vm0, vm1, cud);
			list1.push(cud.childRef);
		}

		cudf.childRef = list1[childrenfs.length];
		map1.delete(vm0);
		map1.set(vm1, list1);
	}
};

let rdb_tagf = (elementf, decorfs) => {
	let decor = decorf => rdb_tagf(elementf, [...decorfs, decorf,]);
	let attrs = attrs => decor(rdt_attrs(attrs));
	let children = childrenfs => decor(rdt_children(childrenfs));
	let child = childf => decor(rdt_child(childf));

	return {
		attr: (key, value) => attrs({ [key]: value, }),
		attrs,
		attrsf: attrsf => decor(rdt_attrsf(attrsf)),
		child,
		children: (...childrenfs) => children(childrenfs),
		decor,
		for_: (keyf, rd_item) => decor(rdt_for(keyf, rd_item)),
		listen: (event, cb) => decor(rdt_eventListener(event, cb)),
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
		.listen('scroll', d => cbScroll(Math.floor(d.target.scrollTop / rowHeight)))
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

let rd = {
	div: () => rdb_tag('div'),
	dom: rd_dom,
	if_: (iff, thenf) => rd_ifElse(iff, thenf, rd_dom(vm => document.createComment('else'))),
	ifElse: rd_ifElse,
	li: () => rdb_tag('li'),
	list: rd_list,
	p: () => rdb_tag('p'),
	scope: (key, rdf) => (vm0, vm1, cudf) => rdf(
		vm0 != null ? vm0[key] : null,
		vm1 != null ? vm1[key] : null,
		cudf),
	span: () => rdb_tag('span'),
	tag: rdb_tag,
	ul: () => rdb_tag('ul'),
	vscrollf: rdb_vscrollf,
};

let rd_parseTemplate = s => {
	let pos0 = 0, pos1, pos2;
	let f = vm => '';
	while (0 <= (pos1 = s.indexOf('{', pos0)) && 0 <= (pos2 = s.indexOf('}', pos1))) {
		let s0 = s.substring(pos0, pos1);
		let f0 = f;
		let f1 = eval('vm => (' + s.substring(pos1 + 1, pos2).trim() + ')');
		f = vm => f0(vm) + s0 + f1(vm);
		pos0 = pos2 + 1;
	}
	{
		let f0 = f;
		f = vm => f0(vm) + s.substring(pos0);
	}
	return f;
};

let rd_parseDom = node0 => {
	if (node0.nodeType == Node.COMMENT_NODE) {
		let sf = rd_parseTemplate(node0.nodeValue);
		return rd.dom(vm => document.createComment(sf(vm)));
	} else if (node0.nodeType == Node.ELEMENT_NODE) {
		let name = node0.localName;
		let tag = rd.tag(name);
		let bf = (as, cs) => tag.attrsf(vm => as).children(...cs).rd();
		let as = {}, cs = [], scope;

		for (let attr of node0.attributes)
			as[attr.name] = attr.value;
		for (let child of node0.childNodes)
			cs.push(rd_parseDom(child));

		if (node0.getAttribute('for-span') != null)
			return tag.for_(vm => vm, rd.span().children(...cs).rd()).rd();
		else if ((scope = node0.getAttribute('scope')) != null)
			return rd.scope(scope, bf(as, cs));
		else
			return bf(as, cs);
	} else if (node0.nodeType == Node.TEXT_NODE) {
		let sf = rd_parseTemplate(node0.nodeValue);
		return rd.dom(vm => document.createTextNode(sf(vm)));
	} else {
		console.error('unknown node type', node0);
		return rd.dom(vm => document.createComment('unknown node type' + node0));
	}
};

let rd_parse = s => rd_parseDom(new DOMParser().parseFromString(s, 'text/xml').childNodes[0]);

let pvm = null;

let renderAgain = (renderer, f) => {
	let target = document.getElementById('target');
	let ppvm = pvm;
	renderer(ppvm, pvm = f(pvm), r_cud(target, null, target.lastChild));
};

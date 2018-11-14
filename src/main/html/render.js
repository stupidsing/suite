'use strict';

let next = dom => dom != null ? dom.nextSibling : null;

let c_cud = (dom, domc0, insertBefore) => {
	let cud_ = {
		childCud: child_ => c_cud(cud_.childRef, child_, next(child_)),
		childRef: domc0,
		create: c => dom.insertBefore(cud_.childRef = c, insertBefore),
		delete: () => dom.removeChild(cud_.childRef),
		update: c => { let c0 = cud_.childRef; dom.replaceChild(cud_.childRef = c, c0); },
	};
	return cud_;
};

let r_cud = (dom, domc0, domcx) => {
	let range = { s: domc0, e: domcx, }; // s exclusive, e inclusive

	let deleteRange = () => {
			while (range.s != range.e) {
				let child1 = range.e.previousSibling;
				dom.removeChild(range.e);
				range.e = child1;
			}
	};

	let cud_ = {
		childCud: child_ => c_cud(range.e, child_, next(child_)),
		childRef: domcx,
		create: c => {
			dom.insertBefore(c, next(range.e));
			cud_.childRef = range.e = c;
		},
		delete: () => {
			deleteRange();
			cud_.childRef = null;
		},
		update: c => {
			deleteRange();
			dom.insertBefore(c, next(range.e));
			cud_.childRef = range.e = c;
		},
	};

	return cud_;
};

let wm = new WeakMap();

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
	if (vm0 == vm1)
		;
	else if (vm1 != null)
		for (let [key, value] of Object.entries(attrsf(vm1)))
			cudf.childRef.setAttribute(key, value);
	else
		for (let [key, value] of Object.entries(attrsf(vm0)))
			cudf.childRef.removeAttribute(key);
};

let rdt_children = childrenfs => (vm0, vm1, cudf) => {
	let domc0 = cudf.childRef;
	let children0 = domc0 != null ? Array.from(domc0.childNodes) : null;

	if (vm0 == vm1)
		;
	else
		for (let i = 0; i < childrenfs.length; i++)
			childrenfs[i](vm0, vm1, cudf.childCud(children0 != null ? children0[i] : null));
};

let rdt_eventListener = (event, cb) => rd_cd(
	(vm, cudf) => cudf.childRef.addEventListener(event, cb),
	(vm, cudf) => cudf.childRef.removeEventListener(event, cb));

let rdt_style = style => (vm0, vm1, cudf) => {
	if (vm0 == null)
		for (let [key, value] of Object.entries(style))
			cudf.childRef.style[key] = value;
	if (vm1 == null)
		for (let [key, value] of Object.entries(style))
			cudf.childRef.style[key] = null;
};

let rdt_stylef = stylef => (vm0, vm1, cudf) => {
	if (vm0 == vm1)
		;
	else if (vm1 != null)
		for (let [key, value] of Object.entries(stylef(vm1)))
			cudf.childRef.style[key] = value;
	else
		for (let [key, value] of Object.entries(stylef(vm0)))
			cudf.childRef.style[key] = null;
};

let rd_cd = (cf, df) => (vm0, vm1, cudf) => {
	if (vm0 == vm1)
		;
	else {
		vm0 != null && df(vm0, cudf);
		vm1 != null && cf(vm1, cudf);
	}
};

let rd_dom = elementf => rd_cd(
	(vm, cudf) => cudf.create(elementf(vm)),
	(vm, cudf) => cudf.delete()
);

let rd_domDecors = (elementf, decorfs) => (vm0, vm1, cudf) => {
	if (vm0 == null)
		cudf.create(elementf());
	if (vm0 == vm1)
		;
	else
		for (let decorf of decorfs)
			decorf(vm0, vm1, cudf);
	if (vm1 == null)
		cudf.delete();
};

let rd_for = (keyf, rd_item) => (vm0, vm1, cudf) => {
	if (vm0 == vm1)
		;
	else {
		let domc0 = cudf.childRef;
		let children0 = wm.get(domc0);
		let children1 = [null,];

		if (vm0 == null)
			for (let i1 = 0; i1 < vm1.length; i1++) {
				rd_item(null, vm1[i1], r_cud(domc0, domc0.lastChild, domc0.lastChild));
				children1.push(domc0.lastChild);
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
					rd_item(vm0[i], vm1[i], r_cud(domc0, children0[i], children0[i + 1]));
					children1.push(children0[i + 1]);
				}
			else {
				let prevSiblingMap = new Map();

				for (let child of domc0.childNodes)
					prevSiblingMap.set(child, child.previousSibling);

				let domc1 = domc0.cloneNode(false);
				cudf.update(domc1);

				for (let i1 = 0; i1 < vm1.length; i1++) {
					let i0 = map0.get(keyf(vm1[i1]));

					if (i0 != null) {
						let prev = domc1.lastChild;
						let child0 = children0[i0];
						let childx = children0[i0 + 1];

						while (child0 != childx) {
							domc1.insertBefore(childx, null);
							childx = prevSiblingMap.get(childx);
						}

						rd_item(vm0[i0], vm1[i1], r_cud(domc1, prev, childx));
					} else
						rd_item(null, vm1[i1], r_cud(domc1, domc1.lastChild, domc1.lastChild));

					children1.push(domc1.lastChild);
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

let rd_forRange = (vmsf, rangef, rd_item) => (vm0, vm1, cudf) => {
	let domc0 = cudf.childRef;
	let children0 = domc0 != null ? Array.from(domc0.childNodes) : null;

	if (vm0 == vm1)
		;
	else if (vm0 == null) {
		let [s, e] = rangef(vm1), vms1 = vmsf(vm1);
		for (let i1 = s; i1 < e; i1++)
			rd_item(null, vms1[i1], cudf.childCud(null));
	} else if (vm1 == null) {
		let [s, e] = rangef(vm0), vms0 = vmsf(vm0);
		for (let i0 = s; i0 < e; i0++)
			rd_item(vms0[i0], null, cudf.childCud(children0[i0]));
	} else {
		let [si, ei] = rangef(vm0), vms0 = vmsf(vm0);
		let [sx, ex] = rangef(vm1), vms1 = vmsf(vm1);
		let s_ = si;
		let e_ = ei;

		// remove elements at start and end of range
		while (s_ < e_ && s_ < sx)
			rd_item(vms0[s_++], null, cudf.childCud(domc0.firstChild));
		while (s_ < e_ && ex < e_)
			rd_item(vms0[--e_], null, cudf.childCud(domc0.lastChild));

		// relocate range if empty
		if (s_ == e_) s_ = e_ = sx;

		// insert elements at start and end of range
		while (sx < s_)
			rd_item(null, vms1[--s_], r_cud(domc0, null, domc0.firstChild));
		while (e_ < ex)
			rd_item(null, vms1[e_++], r_cud(domc0, null, null));

		// update elements at common range
		for (let i = Math.max(si, sx); i < Math.min(ei, ex); i++)
			rd_item(vms0[i], vms1[i], cudf.childCud(domc0.childNodes[i - s_]));
	}
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

let rd_tagf = (elementf, decorfs) => {
	let decor = decorf => rd_tagf(elementf, [...decorfs, decorf,]);
	let attrs = attrs => decor(rdt_attrs(attrs));
	let children = childrenfs => decor(rdt_children(childrenfs));
	let child = childf => children([childf]);

	return {
		attr: (key, value) => attrs({ [key]: value, }),
		attrs,
		attrsf: attrsf => decor(rdt_attrsf(attrsf)),
		child,
		children: (...childrenfs) => children(childrenfs),
		decor,
		for_: (keyf, rd_item) => decor(rd_for(keyf, rd_item)),
		listen: (event, cb) => decor(rdt_eventListener(event, cb)),
		rd: () => rd_domDecors(elementf, decorfs),
		style: style => decor(rdt_style(style)),
		stylef: stylef => decor(rdt_stylef(stylef)),
		text: () => child(rd_dom(vm => document.createTextNode(vm))),
	};
};

let rd_tag = tag => rd_tagf(() => document.createElement(tag), []);

let rd_vscrollf = (height, rowHeight, rd_item, cbScroll) => {
	let nItemsShown = Math.floor(height / rowHeight) + 1;

	return rd_tag('div')
		.style({ height: height + 'px', overflow: 'auto', position: 'absolute', })
		.listen('scroll', d => cbScroll(Math.floor(d.target.scrollTop / rowHeight)))
		.child(rd_tag('div')
			.stylef(vm => ({
				height: (vm.vms.length - vm.start) * rowHeight + 'px',
				position: 'relative',
				top: vm.start * rowHeight + 'px',
			}))
			.decor((vm0, vm1, cudf) => rd_forRange(
				vm => vm.vms,
				vm => [vm.start, vm.start + nItemsShown],
				rd_tag('div').style({ height: rowHeight + 'px', }).child(rd_item).rd())
				(vm0, vm1, cudf))
			.rd()
		);
};

let rd = {
	div: () => rd_tag('div'),
	dom: rd_dom,
	if_: (iff, thenf) => rd_ifElse(iff, thenf, rd_dom(vm => document.createComment('else'))),
	ifElse: rd_ifElse,
	li: () => rd_tag('li'),
	p: () => rd_tag('p'),
	scope: (key, rdf) => (vm0, vm1, cudf) => rdf(
		vm0 != null ? vm0[key] : null,
		vm1 != null ? vm1[key] : null,
		cudf),
	span: () => rd_tag('span'),
	tag: rd_tag,
	ul: () => rd_tag('ul'),
	vscrollf: rd_vscrollf,
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
		let tag = rd.tag(node0.localName);
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

declare dl = this;

declare dl-new = [dl,] (
	let `dl` = 0;
	let `dl + 4` = 0;
);

declare dl-next = [e,] (
	`dl + 4`;
);

declare dl-add-prev = [e, ne,] (
	declare prev = `e + 0`;
	let `ne + 0` = prev;
	let `ne + 4` = e;
	let `e + 0` = ne;
	let `prev + 4` = ne;
);

declare dl-add-next = [e, ne,] (
	declare next = `e + 4`;
	let `ne + 0` = e;
	let `ne + 4` = next;
	let `e + 4` = ne;
	let `next + 0` = ne;
);

declare dl-remove = [e,] (
	declare prev = `e + 0`;
	declare next = `e + 4`;
	let `prev + 4` = next;
	let `next + 0` = prev;
);

declare dl = this;

declare dl-new = [dl,] (
	let `dl + 4` = 0;
	let `dl + 8` = 0;
);

declare dl-prev = [e,] (
	`dl + 4`;
);

declare dl-next = [e,] (
	`dl + 8`;
);

declare dl-add-prev = [e, ne,] (
	declare prev = `e + 4`;
	let `ne + 4` = prev;
	let `ne + 8` = e;
	let `e + 4` = ne;
	let `prev + 8` = ne;
);

declare dl-add-next = [e, ne,] (
	declare next = `e + 8`;
	let `ne + 4` = e;
	let `ne + 8` = next;
	let `e + 8` = ne;
	let `next + 4` = ne;
);

declare dl-remove = [e,] (
	declare prev = `e + 4`;
	declare next = `e + 8`;
	let `prev + 8` = next;
	let `next + 4` = prev;
);

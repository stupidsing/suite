declare dl = this;

declare dl-prev = [e,] (
	`e`;
);

declare dl-next = [e,] (
	`e + 4`;
);

declare dl-set-prev = [e, p,] (
	let `e` = p;
);

declare dl-set-next = [e, p,] (
	let `e + 4` = p;
);

declare dl-new = [e,] (
	dl:dl-set-prev [e, 0,];
	dl:dl-set-next [e, 0,];
);

declare dl-insert-prev = [e, ne,] (
	declare prev = dl:dl-prev [e,];
	dl:dl-set-prev [ne, prev,];
	dl:dl-set-next [ne, e,];
	dl:dl-set-prev [e, ne,];
	(prev && dl:dl-set-next [prev, ne,]);
);

declare dl-insert-next = [e, ne,] (
	declare next = dl:dl-next [e,];
	dl:dl-set-prev [ne, e,];
	dl:dl-set-next [ne, next,];
	(next && dl:dl-set-prev [next, ne,]);
	dl:dl-set-next [e, ne,];
);

declare dl-remove = [e,] (
	declare prev = dl:dl-prev [e,];
	declare next = dl:dl-next [e,];
	(prev && dl:dl-set-next [prev, next,]);
	(next && dl:dl-set-prev [next, prev,]);
);

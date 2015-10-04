data dl-node = fix :dl-node struct (
	prev as pointer:(:dl-node),
	next as pointer:(:dl-node),
);

declare dl-prev = function [e as pointer:dl-node,] (
	`e`^prev;
);

declare dl-next = function [e as pointer:dl-node,] (
	`e`^next;
);

declare dl-set-prev = function [e as pointer:dl-node, p,] (
	let (`e`^prev) = p;
);

declare dl-set-next = function [e as pointer:dl-node, p,] (
	let (`e`^next) = p;
);

declare dl-new = function [e,] (
	dl-set-prev [e, null,];
	dl-set-next [e, null,];
);

declare dl-insert-prev = function [e, ne,] (
	declare prev = dl-prev [e,];
	dl-set-prev [ne, prev,];
	dl-set-next [ne, e,];
	dl-set-prev [e, ne,];
	(prev && dl-set-next [prev, ne,]);
);

declare dl-insert-next = function [e, ne,] (
	declare next = dl-next [e,];
	dl-set-prev [ne, e,];
	dl-set-next [ne, next,];
	(next && dl-set-prev [next, ne,]);
	dl-set-next [e, ne,];
);

declare dl-remove = function [e,] (
	declare prev = dl-prev [e,];
	declare next = dl-next [e,];
	(prev && dl-set-next [prev, next,]);
	(next && dl-set-prev [next, prev,]);
);

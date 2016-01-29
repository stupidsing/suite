constant dl-node .t = fix :dl-node struct (
	+prev as pointer::dl-node,
	+next as pointer::dl-node,
	+current as .t,
);

constant dl-node$t = (:t => dl-node :t);

declare dl-prev as _ = function [e as pointer:dl-node$t,] (
	e/*/+prev;
);

declare dl-next as _ = function [e as pointer:dl-node$t,] (
	e/*/+next;
);

declare dl-set-prev as _ = function [e as pointer:dl-node$t, p,] (
	{e/*/+prev} = p;
);

declare dl-set-next as _ = function [e as pointer:dl-node$t, p,] (
	{e/*/+next} = p;
);

declare dl-new as _ = function [e as pointer:dl-node$t,] (
	{e/*/+prev} = null;
	{e/*/+next} = null;
	e;
);

declare dl-insert-prev as _ = function [e as pointer:dl-node$t, ne,] (
	declare prev = dl-prev [e,];
	dl-set-prev [ne, prev,];
	dl-set-next [ne, e,];
	dl-set-prev [e, ne,];
	(prev && dl-set-next [prev, ne,]);
);

declare dl-insert-next as _ = function [e as pointer:dl-node$t, ne,] (
	declare next = dl-next [e,];
	dl-set-prev [ne, e,];
	dl-set-next [ne, next,];
	(next && dl-set-prev [next, ne,]);
	dl-set-next [e, ne,];
);

declare dl-remove as _ = function [e as pointer:dl-node$t,] (
	declare prev = dl-prev [e,];
	declare next = dl-next [e,];
	(prev && dl-set-next [prev, next,]);
	(next && dl-set-prev [next, prev,]);
);

constant dl-node .t = fix :dl-node struct (
| pointer::dl-node +prev
| pointer::dl-node +next
| .t +current
);

constant dl-node$t = (:t => dl-node :t);

declare _ dl-prev = function [pointer:dl-node$t e,] (
	e/*/+prev;
);

declare _ dl-next = function [pointer:dl-node$t e,] (
	e/*/+next;
);

declare _ dl-set-prev = function [pointer:dl-node$t e, p,] (
	{e/*/+prev} = p;
);

declare _ dl-set-next = function [pointer:dl-node$t e, p,] (
	{e/*/+next} = p;
);

declare _ dl-create = function [pointer:dl-node$t e,] (
	{e/*/+prev} = null;
	{e/*/+next} = null;
	e;
);

declare _ dl-insert-prev = function [pointer:dl-node$t e, ne,] (
	declare prev = dl-prev [e,];
	dl-set-prev [ne, prev,];
	dl-set-next [ne, e,];
	dl-set-prev [e, ne,];
	if prev then (dl-set-next [prev, ne,]) else ();
);

declare _ dl-insert-next = function [pointer:dl-node$t e, ne,] (
	declare next = dl-next [e,];
	dl-set-prev [ne, e,];
	dl-set-next [ne, next,];
	if next then (dl-set-prev [next, ne,]) else ();
	dl-set-next [e, ne,];
);

declare _ dl-remove = function [pointer:dl-node$t e,] (
	declare prev = dl-prev [e,];
	declare next = dl-next [e,];
	if prev then (dl-set-next [prev, next,]) else ();
	if next then (dl-set-prev [next, prev,]) else ();
);

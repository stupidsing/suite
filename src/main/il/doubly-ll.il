constant dl-node .t = fix :dl-node struct (
| pointer::dl-node prev
| pointer::dl-node next
| .t current
);

constant dl-node$t = (:t => dl-node :t);

declare _ dl-prev = function [pointer:dl-node$t e,] (
	e/*/prev;
);

declare _ dl-next = function [pointer:dl-node$t e,] (
	e/*/next;
);

declare _ dl-create = function [pointer:dl-node$t e,] (
	{e/*/prev} = null;
	{e/*/next} = null;
	e;
);

declare _ dl-insert-prev = function [pointer:dl-node$t e, pointer:dl-node$t ne,] (
	declare prev = dl-prev [e,];
	{ne/*/prev} = prev;
	{ne/*/next} = e;
	{e/*/prev} = ne;
	if prev then ({prev/*/next} = ne) else ();
);

declare _ dl-insert-next = function [pointer:dl-node$t e, pointer:dl-node$t ne,] (
	declare next = dl-next [e,];
	{ne/*/prev} = e;
	{ne/*/next} = next;
	if next then ({next/*/prev} = ne) else ();
	{e/*/next} = ne;
);

declare _ dl-remove = function [pointer:dl-node$t e,] (
	declare prev = dl-prev [e,];
	declare next = dl-next [e,];
	if prev then ({prev/*/next} = next) else ();
	if next then ({next/*/prev} = prev) else ();
);

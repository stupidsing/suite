constant ll-node .t = fix :ll-node struct (
| +next as pointer::ll-node
| +current as .t
);

constant ll-node$t = (:t => ll-node :t);

declare ll-next as _ = function [pointer:ll-node$t e,] (
	e/*/+next;
);

declare ll-set-next as _ = function [pointer:ll-node$t e, p,] (
	{e/*/+next} = p;
);

declare ll-create as _ = function [pointer:ll-node$t e,] (
	{e/*/+next} = null;
	e;
);

declare ll-insert-next as _ = function [pointer:ll-node$t e, ne,] (
	declare next = ll-next [e,];
	ll-set-next [ne, next,];
	ll-set-next [e, ne,];
);

declare ll-remove-next as _ = function [pointer:ll-node$t e,] (
	ll-set-next [e, ll-next [e,],];
);

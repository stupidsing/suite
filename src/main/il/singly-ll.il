constant ll-node .t = fix :ll-node struct (
| pointer::ll-node +next
| .t +current
);

constant ll-node$t = (:t => ll-node :t);

declare _ ll-next = function [pointer:ll-node$t e,] (
	e/*/+next;
);

declare _ ll-set-next = function [pointer:ll-node$t e, p,] (
	{e/*/+next} = p;
);

declare _ ll-create = function [pointer:ll-node$t e,] (
	{e/*/+next} = null;
	e;
);

declare _ ll-insert-next = function [pointer:ll-node$t e, ne,] (
	declare next = ll-next [e,];
	ll-set-next [ne, next,];
	ll-set-next [e, ne,];
);

declare _ ll-remove-next = function [pointer:ll-node$t e,] (
	ll-set-next [e, ll-next [ll-next [e,],],];
);

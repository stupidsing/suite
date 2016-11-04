constant stack .t = struct (
| int +size
| pointer:(.t * _) +array
);

constant stack$t = (:t => stack :t);

constant stk-is-empty = function [pointer:stack$t stack,] (
	0 < stack/*/+size;
);

constant stk-push = function [pointer:stack$t stack, value,] (
	{stack/*/+array/*/:(stack/*/+size =+ 1)} = value;
);

constant stk-pop = function [pointer:stack$t stack,] (
	if (stack/*/+size != 0) then (
		stack/*/+array/*/:(stack/*/+size += -1);
	) else (no-type 0);
);

constant stk-setup = function [pointer:stack$t stack, array,] (
	{stack/*} = new stack$t (+size = 0, +array = array,);
);

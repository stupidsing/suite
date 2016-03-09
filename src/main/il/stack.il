constant stack .t = struct (
| (pointer:.t * 0) +array
| int +pointer
);

constant stack$t = (:t => stack :t);

declare stk-push = function [pointer:stack$t stack, value,] (
	{stack/*/+array/:(stack/*/+pointer =+ 1)} = value;
);

declare stk-pop = function [pointer:stack$t stack,] (
	if (stack/*/+pointer != 0) then (
		stack/*/+array/:(stack/*/+pointer += -1);
	) else (no-type 0);
);

declare stk-setup = function [pointer:stack$t stack, array, size,] (
	{stack/*/+array} = array;
	{stack/*/+pointer} = size;
);

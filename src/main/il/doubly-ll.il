declare dl$prev = function [e,] (
	e;
);

declare dl$next = function [e,] (
	e + 4;
);

declare dl-prev = function [e,] (
	`invoke dl$prev [e,]`;
);

declare dl-next = function [e,] (
	`invoke dl$next [e,]`;
);

declare dl-set-prev = function [e, p,] (
	let `invoke dl$prev [e,]` = p;
);

declare dl-set-next = function [e, p,] (
	let `invoke dl$next [e,]` = p;
);

declare dl-new = function [e,] (
	invoke dl-set-prev [e, 0,];
	invoke dl-set-next [e, 0,];
);

declare dl-insert-prev = function [e, ne,] (
	declare prev = invoke dl-prev [e,];
	invoke dl-set-prev [ne, prev,];
	invoke dl-set-next [ne, e,];
	invoke dl-set-prev [e, ne,];
	(prev && invoke dl-set-next [prev, ne,]);
);

declare dl-insert-next = function [e, ne,] (
	declare next = invoke dl-next [e,];
	invoke dl-set-prev [ne, e,];
	invoke dl-set-next [ne, next,];
	(next && invoke dl-set-prev [next, ne,]);
	invoke dl-set-next [e, ne,];
);

declare dl-remove = function [e,] (
	declare prev = invoke dl-prev [e,];
	declare next = invoke dl-next [e,];
	(prev && invoke dl-set-next [prev, next,]);
	(next && invoke dl-set-prev [next, prev,]);
);

declare dl$prev = function [e,] (
	e;
);

declare dl$next = function [e,] (
	e + 4;
);

declare dl-prev = function [e,] (
	`dl$prev [e,]`;
);

declare dl-next = function [e,] (
	`dl$next [e,]`;
);

declare dl-set-prev = function [e, p,] (
	let `dl$prev [e,]` = p;
);

declare dl-set-next = function [e, p,] (
	let `dl$next [e,]` = p;
);

declare dl-new = function [e,] (
	dl-set-prev [e, 0,];
	dl-set-next [e, 0,];
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

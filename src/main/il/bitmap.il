declare bm-get = function [bm, index,] (
	declare d = index shr 5;
	declare m = index and 31;
	1 and `bm`:d shr m;
);

declare bm-set = function [bm, index, flag,] (
	declare d = index shr 5;
	declare m = index and 31;
	declare p = & `bm`:d;
	declare b = 1 shl m;
	let `p` = if flag then (`p` or b) else (`p` and +xFFFFFFFF xor b);
);

lc-deterministic .a
	:- member (CUT, FAIL, YES,) .a, !
#
lc-deterministic (.a _)
	:- member (NOT, ONCE, SYSTEM-CALL,) .a, !
#
lc-deterministic (.oper _)
	:- member (EQ, LE, LT, NE,) .oper, !
#
lc-deterministic (AND .a .b)
	:- (lc-deterministic .a; lc-cut-choices .b)
	, lc-deterministic .b
#
lc-deterministic (OR .a .b)
	:- lc-cut-choices .a, lc-deterministic .b
#

lc-cut-choices (AND .a .b)
	:- lc-cut-choices .b
	; lc-cut-choices .a, lc-deterministic .b
#
lc-cut-choices (OR .a .b)
	:- !, lc-cut-choices .a
#
lc-cut-choices CUT #

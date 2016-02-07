-------------------------------------------------------------------------------
-- red-black tree implementation
--
-- adapted from http://rosettacode.org/wiki/Pattern_matching#Prolog
--

rbt-member (_ .n _ _) .v :- rbt-member .n .v #
rbt-member (_ _ .v _) .v #
rbt-member (_ _ _ .n) .v :- rbt-member .n .v #

rbt-get (_ .n0 .pivot _) .v :- rbt-compare .v .pivot, !, rbt-get .n0 .v #
rbt-get (_ _ .pivot .n1) .v :- rbt-compare .pivot .v, !, rbt-get .n1 .v #
rbt-get (_ _ .v _) .v #

rbt-get-list () .l/.l
#
rbt-get-list (_ .n0 .pivot .n1) .l0/.lx
	:- rbt-get-list .n0 .l0/.l1
	, .l1 = (.pivot, .l2)
	, rbt-get-list .n1 .l2/.lx
#

rbt-depth () 0 #
rbt-depth (_ .n0 _ .n1) .depth
	:- rbt-depth .n0 .d0
	, rbt-depth .n1 .d1
	, (.d0 < .d1, .d = .d1; .d = .d0)
	, let .depth (.d + 1)
	, !
#

rbt-union-bind .t0 .t1 .t :- rbt-union0 BIND .t0 .t1 .t #

rbt-union-insert .t0 .t1 .t :- rbt-union0 INSERT .t0 .t1 .t #

rbt-union-replace .t0 .t1 .t :- rbt-union0 REPLACE .t0 .t1 .t #

rbt-union0 _ () .tree .tree #
rbt-union0 .mode (_ .n0 .pivot .n1) .tree0 .treex
	:- rbt-union0 .mode .n0 .tree0 .tree1
	, rbt-add1 .mode .pivot .tree1/.tree2
	, rbt-union0 .mode .n1 .tree2 .treex
#

rbt-insert-list .list .tree0/.treex
	:- list.fold .list/.tree0/.treex .e/.tree0_/.treex_ (rbt-insert .e .tree0_/.treex_)
#

rbt-insert .v .t :- rbt-add1 INSERT .v .t #

rbt-bind .v .t :- rbt-add1 BIND .v .t #

rbt-replace .v .t :- rbt-add1 REPLACE .v .t #

rbt-add1 .mode .v .tree/(BLACK .npn) :- rbt-add0 .mode .v .tree/(_ .npn) #

rbt-add0 _ .v ()/(RED () .v ()) #
rbt-add0 .mode .v (.color .n0 .pivot .n1)/.treex
	:- rbt-compare .v .pivot, !
	, rbt-add0 .mode .v .n0/.newn0
	, rbt-balance (.color .newn0 .pivot .n1)/.treex
	; rbt-compare .pivot .v, !
	, rbt-add0 .mode .v .n1/.newn1
	, rbt-balance (.color .n0 .pivot .newn1)/.treex
	; .mode = BIND
		, .v = .pivot, .color .n0 .v .n1 = .treex
	; .mode = REPLACE
		, .color .n0 .v .n1 = .treex
#

rbt-balance (BLACK .npn)/(RED (BLACK .n0 .p0 .n1) .p1 (BLACK .n2 .p2 .n3))
	:- (
		.npn = (RED .npn1) .p2 .n3
		, (.npn1 = (RED .n0 .p0 .n1) .p1 .n2; .npn1 = .n0 .p0 (RED .n1 .p1 .n2))
		; .npn = .n0 .p0 (RED .npn1)
		, (.npn1 = (RED .n1 .p1 .n2) .p2 .n3; .npn1 = .n1 .p1 (RED .n2 .p2 .n3))
	)
	, !
#
rbt-balance .tree/.tree
#

rbt-compare .k0/_ .k1/_ :- !, .k0 < .k1 #
rbt-compare .v0 .v1 :- .v0 < .v1 #

-------------------------------------------------------------------------------
-- Red-black tree implementation
--
-- adapted from http://rosettacode.org/wiki/Pattern_matching#Prolog
--

rb-member .tree .v :- bound .v, !, rb-get .tree .v #
rb-member (_ .n _ _) .v :- rb-member .n .v #
rb-member (_ _ .v _) .v #
rb-member (_ _ _ .n) .v :- rb-member .n .v #

rb-get (_ .n0 .pivot _) .v :- rb-compare .v .pivot, !, rb-get .n0 .v #
rb-get (_ _ .pivot .n1) .v :- rb-compare .pivot .v, !, rb-get .n1 .v #
rb-get (_ _ .v _) .v #

rb-depth () 0 #
rb-depth (_ .n0 _ .n1) .depth
	:- rb-depth .n0 .d0
	, rb-depth .n1 .d1
	, (.d0 > .d1, .d = .d0; .d = .d1)
	, let .depth (.d + 1)
	, !
#

rb-merge () .tree .tree #
rb-merge (_ .n0 .pivot .n1) .tree0 .treex
	:- rb-merge .n0 .tree0 .tree1
	, rb-add .pivot .tree1/.tree2
	, rb-merge .n1 .tree2 .treex
#

rb-add-list () .tree/.tree #
rb-add-list (.v, .vs) .tree0/.treex
	:- rb-add .v .tree0/.tree1
	, rb-add-list .vs .tree1/.treex
#

rb-add .v .tree/(BLACK .npn) :- rb-add0 .v .tree/(_ .npn) #

-- There is a special replacement case (last line of rb-add0), necessary for
-- functional compiler to replace already-defined variables.
rb-add0 .v ()/(RED () .v ()) #
rb-add0 .v (.color .n0 .pivot .n1)/.treex
	:- rb-compare .v .pivot, !
	, rb-add0 .v .n0/.newn0
	, rb-balance (.color .newn0 .pivot .n1)/.treex
	; rb-compare .pivot .v, !
	, rb-add0 .v .n1/.newn1
	, rb-balance (.color .n0 .pivot .newn1)/.treex
	; .color .n0 .v .n1 = .treex
#

rb-balance (BLACK .npn)/(RED (BLACK .n0 .p0 .n1) .p1 (BLACK .n2 .p2 .n3))
	:- .npn = (RED .npn1) .p2 .n3
	, (.npn1 = (RED .n0 .p0 .n1) .p1 .n2; .npn1 = .n0 .p0 (RED .n1 .p1 .n2))
	; .npn = .n0 .p0 (RED .npn1)
	, (.npn1 = (RED .n1 .p1 .n2) .p2 .n3; .npn1 = .n1 .p1 (RED .n2 .p2 .n3))
#
rb-balance .tree/.tree #

rb-compare .k0/_ .k1/_ :- !, .k0 < .k1 #
rb-compare .v0 .v1 :- .v0 < .v1 #

-------------------------------------------------------------------------------
-- 2-3 tree implementation

23t-member .tree .v :- bound .v, !, 23t-get .tree .v #
23t-member L:.v .v #
23t-member (T2 .n0 _ .n1) .v
	:- 23t-member .n0 .v
	; 23t-member .n1 .v
#
23t-member (T3 .n0 _ .n1 _ .n2) .v
	:- 23t-member .n0 .v
	; 23t-member .n1 .v
	; 23t-member .n2 .v
#

23t-get L:.v .v #
23t-get (T2 .n0 .pivot .n1) .v
	:- 23t-compare .v .pivot
	, !, 23t-get .n0 .v
	; 23t-get .n1 .v
#
23t-get (T3 .n0 .pivot0 .n1 .pivot1 .n2) .v
	:- 23t-compare .v .pivot0
	, !, 23t-get .n0 .v
	; 23t-compare .v .pivot1
	, !, 23t-get .n1 .v
	; 23t-get .n2 .v
#

23t-merge () .tree .tree #
23t-merge L:.v .tree0 .treex
	:- 23t-add .v .tree0/.treex
#
23t-merge (T2 .n0 _ .n1) .tree0 .treex
	:- 23t-merge .n0 .tree0 .tree1
	, 23t-merge .n1 .tree1 .treex
#
23t-merge (T3 .n0 _ .n1 _ .n2) .tree0 .treex
	:- 23t-merge .n0 .tree0 .tree1
	, 23t-merge .n1 .tree1 .tree2
	, 23t-merge .n2 .tree1 .treex
#

23t-add-list () .tree/.tree #
23t-add-list (.v, .vs) .tree0/.treex
	:- 23t-add .v .tree0/.tree1
	, 23t-add-list .vs .tree1/.treex
#

23t-add .v T/L:.v #
23t-add .v0 L:.v1/(T2 L:.v0 .v1 L:.v1) :- 23t-compare .v0 .v1 #
23t-add .v1 L:.v0/(T2 L:.v0 .v1 L:.v1) :- not 23t-compare .v1 .v0 #
23t-add .v (T2 .n0 .pivot .n1)/.tree1
	:- 23t-compare .v .pivot
	, 23t-add .v .n0/.newn0
	, 23t-merge-left-T2 (T2 .newn0 .pivot .n1)/.tree1
#
23t-add .v (T2 .n0 .pivot .n1)/.tree1
	:- not 23t-compare .v .pivot
	, 23t-add .v .n1/.newn1
	, 23t-merge-right-T2 (T2 .n0 .pivot .newn1)/.tree1
#
23t-add .v (T3 .n0 .pivot0 .n1 .pivot1 .n2)/.tree1
	:- 23t-compare .v .pivot0
	, 23t-add .v .n0/.newn0
	, 23t-merge-left-T3 (T3 .newn0 .pivot0 .n1 .pivot1 .n2)/.tree1
#
23t-add .v (T3 .n0 .pivot0 .n1 .pivot1 .n2)/.tree1
	:- not 23t-compare .v .pivot0, 23t-compare .v .pivot1
	, 23t-add .v .n1/.newn1
	, 23t-merge-middle-T3 (T3 .n0 .pivot0 .newn1 .pivot1 .n2)/.tree1
#
23t-add .v (T3 .n0 .pivot0 .n1 .pivot1 .n2)/.tree1
	:- not 23t-compare .v .pivot1
	, 23t-add .v .n2/.newn2
	, 23t-merge-right-T3 (T3 .n0 .pivot0 .n1 .pivot1 .newn2)/.tree1
#

23t-merge-left-T2 (
	T2 (T2 .n0 .pivot0 .n1) .pivot1 .n2
)/(
	T3 .n0 .pivot0 .n1 .pivot1 .n2
) :- !
#
23t-merge-left-T2 .tree/.tree #

23t-merge-right-T2 (
	T2 .n0 .pivot0 (T2 .n1 .pivot1 .n2)
)/(
	T3 .n0 .pivot0 .n1 .pivot1 .n2
) :- !
#
23t-merge-right-T2 .tree/.tree #

23t-merge-left-T3 (
	T3 (T2 .n0 .pivot0 .n1) .pivot1 .n2 .pivot2 .n3
)/(
	T2 (T2 .n0 .pivot0 .n1) .pivot1 (T2 .n2 .pivot2 .n3)
) :- !
#
23t-merge-left-T3 .tree/.tree #

23t-merge-middle-T3 (
	T3 .n0 .pivot0 (T2 .n1 .pivot1 .n2) .pivot2 .n3
)/(
	T2 (T2 .n0 .pivot0 .n1) .pivot1 (T2 .n2 .pivot2 .n3)
) :- !
#
23t-merge-middle-T3 .tree/.tree #

23t-merge-right-T3 (
	T3 .n0 .pivot0 .n1 .pivot1 (T2 .n2 .pivot2 .n3)
)/(
	T2 (T2 .n0 .pivot0 .n1) .pivot1 (T2 .n2 .pivot2 .n3)
) :- !
#
23t-merge-right-T3 .tree/.tree #

23t-compare .k0/_ .k1/_ :- !, .k0 < .k1 #
23t-compare .v0 .v1 :- .v0 < .v1 #

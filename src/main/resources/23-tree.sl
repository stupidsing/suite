-------------------------------------------------------------------------------
-- 2-3 tree implementation

tree23-member .tree .v :- bound .v, !, tree23-get .tree .v #
tree23-member L:.v .v #
tree23-member (T2 .n0 _ .n1) .v
	:- tree23-member .n0 .v
	; tree23-member .n1 .v
#
tree23-member (T3 .n0 _ .n1 _ .n2) .v
	:- tree23-member .n0 .v
	; tree23-member .n1 .v
	; tree23-member .n2 .v
#

tree23-get L:.v .v #
tree23-get (T2 .n0 .pivot .n1) .v
	:- tree23-compare .v .pivot
	, !, tree23-get .n0 .v
	; tree23-get .n1 .v
#
tree23-get (T3 .n0 .pivot0 .n1 .pivot1 .n2) .v
	:- tree23-compare .v .pivot0
	, !, tree23-get .n0 .v
	; tree23-compare .v .pivot1
	, !, tree23-get .n1 .v
	; tree23-get .n2 .v
#

tree23-merge () .tree .tree #
tree23-merge L:.v .tree0 .treex
	:- tree23-add .v .tree0/.treex
#
tree23-merge (T2 .n0 _ .n1) .tree0 .treex
	:- tree23-merge .n0 .tree0 .tree1
	, tree23-merge .n1 .tree1 .treex
#
tree23-merge (T3 .n0 _ .n1 _ .n2) .tree0 .treex
	:- tree23-merge .n0 .tree0 .tree1
	, tree23-merge .n1 .tree1 .tree2
	, tree23-merge .n2 .tree1 .treex
#

tree23-add-list () .tree/.tree #
tree23-add-list (.v, .vs) .tree0/.treex
	:- tree23-add .v .tree0/.tree1
	, tree23-add-list .vs .tree1/.treex
#

tree23-add .v ()/L:.v #
tree23-add .v0 L:.v1/(T2 L:.v0 .v1 L:.v1) :- tree23-compare .v0 .v1 #
tree23-add .v1 L:.v0/(T2 L:.v0 .v1 L:.v1) :- not tree23-compare .v1 .v0 #
tree23-add .v (T2 .n0 .pivot .n1)/.tree1
	:- tree23-compare .v .pivot
	, tree23-add .v .n0/.newn0
	, tree23-merge-left-T2 (T2 .newn0 .pivot .n1)/.tree1
#
tree23-add .v (T2 .n0 .pivot .n1)/.tree1
	:- not tree23-compare .v .pivot
	, tree23-add .v .n1/.newn1
	, tree23-merge-right-T2 (T2 .n0 .pivot .newn1)/.tree1
#
tree23-add .v (T3 .n0 .pivot0 .n1 .pivot1 .n2)/.tree1
	:- tree23-compare .v .pivot0
	, tree23-add .v .n0/.newn0
	, tree23-merge-left-T3 (T3 .newn0 .pivot0 .n1 .pivot1 .n2)/.tree1
#
tree23-add .v (T3 .n0 .pivot0 .n1 .pivot1 .n2)/.tree1
	:- not tree23-compare .v .pivot0, tree23-compare .v .pivot1
	, tree23-add .v .n1/.newn1
	, tree23-merge-middle-T3 (T3 .n0 .pivot0 .newn1 .pivot1 .n2)/.tree1
#
tree23-add .v (T3 .n0 .pivot0 .n1 .pivot1 .n2)/.tree1
	:- not tree23-compare .v .pivot1
	, tree23-add .v .n2/.newn2
	, tree23-merge-right-T3 (T3 .n0 .pivot0 .n1 .pivot1 .newn2)/.tree1
#

tree23-merge-left-T2 (
	T2 (T2 .n0 .pivot0 .n1) .pivot1 .n2
)/(
	T3 .n0 .pivot0 .n1 .pivot1 .n2
) :- !
#
tree23-merge-left-T2 .tree/.tree #

tree23-merge-right-T2 (
	T2 .n0 .pivot0 (T2 .n1 .pivot1 .n2)
)/(
	T3 .n0 .pivot0 .n1 .pivot1 .n2
) :- !
#
tree23-merge-right-T2 .tree/.tree #

tree23-merge-left-T3 (
	T3 (T2 .n0 .pivot0 .n1) .pivot1 .n2 .pivot2 .n3
)/(
	T2 (T2 .n0 .pivot0 .n1) .pivot1 (T2 .n2 .pivot2 .n3)
) :- !
#
tree23-merge-left-T3 .tree/.tree #

tree23-merge-middle-T3 (
	T3 .n0 .pivot0 (T2 .n1 .pivot1 .n2) .pivot2 .n3
)/(
	T2 (T2 .n0 .pivot0 .n1) .pivot1 (T2 .n2 .pivot2 .n3)
) :- !
#
tree23-merge-middle-T3 .tree/.tree #

tree23-merge-right-T3 (
	T3 .n0 .pivot0 .n1 .pivot1 (T2 .n2 .pivot2 .n3)
)/(
	T2 (T2 .n0 .pivot0 .n1) .pivot1 (T2 .n2 .pivot2 .n3)
) :- !
#
tree23-merge-right-T3 .tree/.tree #

tree23-compare .k0/_ .k1/_ :- !, .k0 < .k1 #
tree23-compare .v0 .v1 :- .v0 < .v1 #

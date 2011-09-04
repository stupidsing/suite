t23-add .k/.v .tree/.tree1
	:- t23-insert .k/.v .tree .tree1, !
#

t23-search .k/.v L:.k:.v #
t23-search .k/.v (T2 .n0 .p .n1)
	:- if (t23-compare .k .p) then (
		t23-search .k/.v .n0
	) else (
		t23-search .k/.v .n1
	)
#
t23-search .k/.v (T3 .n0 .p0 .n1 .p1 .n2)
	:- if (t23-compare .k .p0) then (
		t23-search .k/.v .n0
	) else-if (t23-compare .k .p1) then (
		t23-search .k/.v .n1
	) else (
		t23-search .k/.v .n2
	)
#

t23-insert .k/.v () L:.k:.v #
t23-insert .k/.v L:.k:.v L:.k:.v #
t23-insert .k0/.v0 L:.k1:.v1 (T2 L:.k0:.v0 .k1 L:.k1:.v1) :- t23-compare .k0 .k1 #
t23-insert .k1/.v1 L:.k0:.v0 (T2 L:.k0:.v0 .k1 L:.k1:.v1) :- not t23-compare .k1 .k0 #
t23-insert .k/.v (T2 .n0 .p .n1) .tree1
	:- t23-compare .k .p
	, t23-insert .k/.v .n0 .newn0
	, t23-merge-T2L (T2 .newn0 .p .n1) .tree1
#
t23-insert .k/.v (T2 .n0 .p .n1) .tree1
	:- not t23-compare .k .p
	, t23-insert .k/.v .n1 .newn1
	, t23-merge-T2R (T2 .n0 .p .newn1) .tree1
#
t23-insert .k/.v (T3 .n0 .p0 .n1 .p1 .n2) .tree1
	:- t23-compare .k .p0
	, t23-insert .k/.v .n0 .newn0
	, t23-merge-T3L (T3 .newn0 .p0 .n1 .p1 .n2) .tree1
#
t23-insert .k/.v (T3 .n0 .p0 .n1 .p1 .n2) .tree1
	:- not t23-compare .k .p0, t23-compare .k .p1
	, t23-insert .k/.v .n1 .newn1
	, t23-merge-T3M (T3 .n0 .p0 .newn1 .p1 .n2) .tree1
#
t23-insert .k/.v (T3 .n0 .p0 .n1 .p1 .n2) .tree1
	:- not t23-compare .k .p1
	, t23-insert .k/.v .n2 .newn2
	, t23-merge-T3R (T3 .n0 .p0 .n1 .p1 .newn2) .tree1
#

t23-merge-T2L
	(T2 (T2 .n0 .p0 .n1) .p1 .n2)
	(T3 .n0 .p0 .n1 .p1 .n2)
	:- !
#
t23-merge-T2L .t .t #

t23-merge-T2R
	(T2 .n0 .p0 (T2 .n1 .p1 .n2))
	(T3 .n0 .p0 .n1 .p1 .n2)
	:- !
#
t23-merge-T2R .t .t #

t23-merge-T3L
	(T3 (T2 .n0 .p0 .n1) .p1 .n2 .p2 .n3)
	(T2 (T2 .n0 .p0 .n1) .p1 (T2 .n2 .p2 .n3))
	:- !
#
t23-merge-T3L .t .t #

t23-merge-T3M
	(T3 .n0 .p0 (T2 .n1 .p1 .n2) .p2 .n3)
	(T2 (T2 .n0 .p0 .n1) .p1 (T2 .n2 .p2 .n3))
	:- !
#
t23-merge-T3M .t .t #

t23-merge-T3R
	(T3 .n0 .p0 .n1 .p1 (T2 .n2 .p2 .n3))
	(T2 (T2 .n0 .p0 .n1) .p1 (T2 .n2 .p2 .n3))
	:- !
#
t23-merge-T3R .t .t #

t23-compare .a .b :- .a < .b #

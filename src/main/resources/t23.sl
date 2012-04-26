-------------------------------------------------------------------------------
-- 2-3 tree implementation
--
-- t23-map		insert or retrieve key/value pair
-- t23-search	retrieve key/value pair (i.e. read only)

t23-map .tree/.tree1 .kv
	:- t23-put .kv .tree .tree1, !
#

t23-traverse L:.k:.v .k/.v #
t23-traverse (T2 .n0 _ .n1) .kv
	:- t23-traverse .n0 .kv
	; t23-traverse .n1 .kv
#
t23-traverse (T3 .n0 _ .n1 _ .n2) .kv
	:- t23-traverse .n0 .kv
	; t23-traverse .n1 .kv
	; t23-traverse .n2 .kv
#

t23-search L:.k:.v .k/.v #
t23-search (T2 .n0 .p .n1) .k/.v
	:- if (t23-compare .k .p) then (
		t23-search .n0 .k/.v
	) else (
		t23-search .n1 .k/.v
	)
#
t23-search (T3 .n0 .p0 .n1 .p1 .n2) .k/.v
	:- if (t23-compare .k .p0) then (
		t23-search .n0 .k/.v
	) else-if (t23-compare .k .p1) then (
		t23-search .n1 .k/.v
	) else (
		t23-search .n2 .k/.v
	)
#

t23-replace L:.k:.v0/L:.k:.v1 .k/.v0/.v1 #
t23-replace (T2 .n0 .p .n1)/.tree1 .k/.vv
	:- if (t23-compare .k .p) then (
		t23-replace .n0/.newn0 .k/.vv, .tree1 = T2 .newn0 .p .n1
	) else (
		t23-replace .n1/.newn1 .k/.vv, .tree1 = T2 .n0 .p .newn1
	)
#
t23-replace (T3 .n0 .p0 .n1 .p1 .n2)/.tree1 .k/.vv
	:- if (t23-compare .k .p0) then (
		t23-replace .n0/.newn0 .k/.vv, .tree1 = T3 .newn0 .p0 .n1 .p1 .n2
	) else-if (t23-compare .k .p1) then (
		t23-replace .n1/.newn1 .k/.vv, .tree1 = T3 .n0 .p0 .newn1 .p1 .n2
	) else (
		t23-replace .n2/.newn2 .k/.vv, .tree1 = T3 .n0 .p0 .n1 .p1 .newn2
	)
#

t23-put .k/.v () L:.k:.v #
t23-put .k/.v L:.k:.v L:.k:.v #
t23-put .k0/.v0 L:.k1:.v1 (T2 L:.k0:.v0 .k1 L:.k1:.v1) :- t23-compare .k0 .k1 #
t23-put .k1/.v1 L:.k0:.v0 (T2 L:.k0:.v0 .k1 L:.k1:.v1) :- not t23-compare .k1 .k0 #
t23-put .k/.v (T2 .n0 .p .n1) .tree1
	:- t23-compare .k .p
	, t23-put .k/.v .n0 .newn0
	, t23-merge-T2L (T2 .newn0 .p .n1) .tree1
#
t23-put .k/.v (T2 .n0 .p .n1) .tree1
	:- not t23-compare .k .p
	, t23-put .k/.v .n1 .newn1
	, t23-merge-T2R (T2 .n0 .p .newn1) .tree1
#
t23-put .k/.v (T3 .n0 .p0 .n1 .p1 .n2) .tree1
	:- t23-compare .k .p0
	, t23-put .k/.v .n0 .newn0
	, t23-merge-T3L (T3 .newn0 .p0 .n1 .p1 .n2) .tree1
#
t23-put .k/.v (T3 .n0 .p0 .n1 .p1 .n2) .tree1
	:- not t23-compare .k .p0, t23-compare .k .p1
	, t23-put .k/.v .n1 .newn1
	, t23-merge-T3M (T3 .n0 .p0 .newn1 .p1 .n2) .tree1
#
t23-put .k/.v (T3 .n0 .p0 .n1 .p1 .n2) .tree1
	:- not t23-compare .k .p1
	, t23-put .k/.v .n2 .newn2
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

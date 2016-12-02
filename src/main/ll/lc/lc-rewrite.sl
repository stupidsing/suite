lc-rewrite (AND .g0 .h0) (AND .g1 .h1)
	:- lc-rewrite .g0 .g1
	, lc-rewrite .h0 .h1
#
lc-rewrite (CALL .node0) (CALL .node1)
	:- lc-rewrite-node .node0 .node1
#
lc-rewrite CUT CUT
#
lc-rewrite (DEFINE-RULES .rules0 .goal0) (DEFINE-RULES .rules1 .goal1)
	:- lc-rewrite-rules .rules0 .rules1
	, lc-rewrite .goal0 .goal1
#
lc-rewrite (EQ .left0 .right0) (EQ .left1 .right1)
	:- lc-rewrite-node .left0 .left1
	, lc-rewrite-node .right0 .right1
#
lc-rewrite FAIL FAIL
#
lc-rewrite (LE .left0 .right0) (LE .left1 .right1)
	:- lc-rewrite-node .left0 .left1
	, lc-rewrite-node .right0 .right1
#
lc-rewrite (LT .left0 .right0) (LT .left1 .right1)
	:- lc-rewrite-node .left0 .left1
	, lc-rewrite-node .right0 .right1
#
lc-rewrite (NE .left0 .right0) (NE .left1 .right1)
	:- lc-rewrite-node .left0 .left1
	, lc-rewrite-node .right0 .right1
#
lc-rewrite (NOT .goal0) (NOT .goal1)
	:- lc-rewrite .goal0 .goal1
#
lc-rewrite (ONCE .goal0) (ONCE .goal1)
	:- lc-rewrite .goal0 .goal1
#
lc-rewrite (SYSTEM-CALL .node0) (SYSTEM-CALL .node1)
	:- lc-rewrite-node .node0 .node1
#
lc-rewrite YES YES
#

lc-rewrite-rules () ()
#
lc-rewrite-rules (.rule0, .rules0) (.rule1, .rules1)
	:- lc-rewrite-rule .rule0 .rule1
	, lc-rewrite-rules .rules0 .rules1
#

lc-rewrite-rule (RULE .head0 .tail0) (RULE .head1 .tail1)
	:- lc-rewrite-node .head0 .head1
	, lc-rewrite .tail0 .tail1
#

lc-rewrite-node (ATOM .atom) (ATOM .atom)
#
lc-rewrite-node (NUMBER .number) (NUMBER .number)
#
lc-rewrite-node (STRING .string) (STRING .string)
#
lc-rewrite-node (TREE .left0 .right0) (TREE .left1 .right1)
	:- lc-rewrite-node .left0 .left1
	, lc-rewrite-node .right0 .right1
#
lc-rewrite-node (VAR .var) (VAR .var)
#
lc-rewrite-node WILDCARD WILDCARD
#

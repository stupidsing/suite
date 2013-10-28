lc-transform (AND .g0 .h0) (AND .g1 .h1)
	:- lc-transform .g0 .g1
	, lc-transform .h0 .h1
#
lc-transform (CALL .node0) (CALL .node1)
	:- lc-transform-node .node0 .node1
#
lc-transform CUT CUT
#
lc-transform (DEFINE-RULES .rules0 .goal0) (DEFINE-RULES .rules1 .goal1)
	:- lc-transform-rules .rules0 .rules1
	, lc-transform .goal0 .goal1
#
lc-transform (EQ .left0 .right0) (EQ .left1 .right1)
	:- lc-transform-node .left0 .left1
	, lc-transform-node .right0 .right1
#
lc-transform FAIL FAIL
#
lc-transform (GE .left0 .right0) (GE .left1 .right1)
	:- lc-transform-node .left0 .left1
	, lc-transform-node .right0 .right1
#
lc-transform (GT .left0 .right0) (GT .left1 .right1)
	:- lc-transform-node .left0 .left1
	, lc-transform-node .right0 .right1
#
lc-transform (LE .left0 .right0) (LE .left1 .right1)
	:- lc-transform-node .left0 .left1
	, lc-transform-node .right0 .right1
#
lc-transform (LT .left0 .right0) (LT .left1 .right1)
	:- lc-transform-node .left0 .left1
	, lc-transform-node .right0 .right1
#
lc-transform (NE .left0 .right0) (NE .left1 .right1)
	:- lc-transform-node .left0 .left1
	, lc-transform-node .right0 .right1
#
lc-transform (NOT .goal0) (NOT .goal1)
	:- lc-transform .goal0 .goal1
#
lc-transform (ONCE .goal0) (ONCE .goal1)
	:- lc-transform .goal0 .goal1
#
lc-transform (SYSTEM-CALL .node0) (SYSTEM-CALL .node1)
	:- lc-transform-node .node0 .node1
#
lc-transform YES YES
#

lc-transform-rules () ()
#
lc-transform-rules (.rule0, .rules0) (.rule1, .rules1)
	:- lc-transform-rule .rule0 .rule1
	, lc-transform-rules .rules0 .rules1
#

lc-transform-rule (RULE .head0 .tail0) (RULE .head1 .tail1)
	:- lc-transform-node .head0 .head1
	, lc-transform .tail0 .tail1
#

lc-transform-node (ATOM .atom) (ATOM .atom)
#
lc-transform-node (NUMBER .number) (NUMBER .number)
#
lc-transform-node (STRING .string) (STRING .string)
#
lc-transform-node (TREE .left0 .right0) (TREE .left1 .right1)
	:- lc-transform-node .left0 .left1
	, lc-transform-node .right0 .right1
#
lc-transform-node (VAR .var) (VAR .var)
#
lc-transform-node WILDCARD WILDCARD
#

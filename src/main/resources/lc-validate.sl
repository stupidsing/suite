lc-validate (AND .g0 .h0) (AND .g1 .h1)
	:- lc-validate .g0 .g1
	, lc-validate .h0 .h1
#
lc-validate (CALL .node0) (CALL .node1)
	:- lc-validate-node .node0 .node1
#
lc-validate CUT CUT
#
lc-validate (DEFINE-RULES .rules0 .goal0) (DEFINE-RULES .rules1 .goal1)
	:- lc-validate-rules .rules0 .rules1
	, lc-validate .goal0 .goal1
#
lc-validate (EQ .left0 .right0) (EQ .left1 .right1)
	:- lc-validate-node .left0 .left1
	, lc-validate-node .right0 .right1
#
lc-validate FAIL FAIL
#
lc-validate (GE .left0 .right0) (GE .left1 .right1)
	:- lc-validate-node .left0 .left1
	, lc-validate-node .right0 .right1
#
lc-validate (GT .left0 .right0) (GT .left1 .right1)
	:- lc-validate-node .left0 .left1
	, lc-validate-node .right0 .right1
#
lc-validate (LE .left0 .right0) (LE .left1 .right1)
	:- lc-validate-node .left0 .left1
	, lc-validate-node .right0 .right1
#
lc-validate (LT .left0 .right0) (LT .left1 .right1)
	:- lc-validate-node .left0 .left1
	, lc-validate-node .right0 .right1
#
lc-validate (NE .left0 .right0) (NE .left1 .right1)
	:- lc-validate-node .left0 .left1
	, lc-validate-node .right0 .right1
#
lc-validate (NOT .goal0) (NOT .goal1)
	:- lc-validate .goal0 .goal1
#
lc-validate (ONCE .goal0) (ONCE .goal1)
	:- lc-validate .goal0 .goal1
#
lc-validate (SYSTEM-CALL .node0) (SYSTEM-CALL .node1)
	:- lc-validate-node .node0 .node1
#
lc-validate YES YES
#

lc-validate-rules () ()
#
lc-validate-rules (.rule0, .rules0) (.rule1, .rules1)
	:- lc-validate-rule .rule0 .rule1
	, lc-validate-rules .rules0 .rules1
#

lc-validate-rule (RULE .head0 .tail0) (RULE .head1 .tail1)
	:- lc-validate-node .head0 .head1
	, lc-validate .tail0 .tail1
#

lc-validate-node (ATOM .atom) (ATOM .atom)
#
lc-validate-node (NUMBER .number) (NUMBER .number)
#
lc-validate-node (STRING .string) (STRING .string)
#
lc-validate-node (TREE .left0 .right0) (TREE .left1 .right1)
	:- lc-validate-node .left0 .left1
	, lc-validate-node .right0 .right1
#
lc-validate-node (VAR .var) (VAR .var)
#
lc-validate-node WILDCARD WILDCARD
#

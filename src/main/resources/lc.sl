-------------------------------------------------------------------------------
-- logical compiler

compile .call .c0
	:- .c0 = (_ ENTER
		, _ ASSIGN-CLOSURE .provenReg .provenLabel
		, _ PUSH .provenReg
		, _ PUSH .provenReg
		, _ CALL-CONSTANT .callLabel
		, _ POP _
		, _ POP _
		, _ EXIT-VALUE false
		, .provenLabel EXIT-VALUE true
		, .c1
	)
	, compile-call .call () .c1/()/.callLabel
	, lc-assign-line-number 0 .c0
#

compile-call .call .pls .c0/.cx/.label
	:- .c0 = (.label ENTER
		, _ CUT-BEGIN .cutPoint
		, _ TOP .provenReg -2
		, .c1
	)
	, to.atom "!" .cutSymbol
	, replace .call/.call1 .cutSymbol/($$CUT .cutPoint .failLabel)
	, generalize-variables .call1/.call2 .variables
	, initialize-variables .call2/.call3 .variables
	, lc-compile .call3 (
		$$BYTECODE _ CALL-CLOSURE .provenReg .provenReg, fail
	) .pls .c1/.c2/.c3/.c4
	, .c2 = (.failLabel RETURN, .c3)
	, .c4 = (_ LEAVE, .cx)
#

generalize-variables .variable/$$REG:.n .variables
	:- is.atom .variable
	, to.atom "." .dot, starts.with .variable .dot
	, !, member .variables .variable/.n
	, !
#
generalize-variables .call0/.call1 .variables
	:- tree .call0 .left0 .operator .right0
	, tree .call1 .left1 .operator .right1
	, !
	, generalize-variables .left0/.left1 .variables
	, generalize-variables .right0/.right1 .variables
#
generalize-variables .call/.call _ #

initialize-variables .call/.call () :- ! #
initialize-variables .call0/($$BYTECODE _ NEW-NODE .n, .call1) (.variable/.n, .remains)
	:- initialize-variables .call0/.call1 .remains
#

--lc-compile .p :- write lc-compile .p, nl, fail #
lc-compile ($$BYTECODE .bytecode) .more .pls .c0/.cx/.d0/.dx
	:- !, .c0 = (.bytecode, .c1)
	, lc-compile .more () .pls .c1/.cx/.d0/.dx
#
lc-compile ($$SCOPE .call .pls1) .more .pls .c0/.cx/.d0/.dx
	:- !
	, (.more/.more1 = ()/(); .more1 = $$SCOPE .more .pls)
	, lc-compile .call .more1 .pls1 .c0/.cx/.d0/.dx
#
lc-compile fail _ _ .c/.c/.d/.d :- ! #
lc-compile () .more .pls .c0/.cx/.d0/.dx
	:- !, lc-compile .more () .pls .c0/.cx/.d0/.dx
#
lc-compile (.a, .b) .more .pls .c0/.cx/.d0/.dx
	:- !, lc-compile .a (.b, .more) .pls .c0/.cx/.d0/.dx
#
lc-compile (.a; .b) .more .pls .c0/.cx/.d0/.dx
	:- !
	, lc-compile .a ($$BYTECODE _ CALL-CONSTANT .label, fail) .pls .c0/.c1/.d0/.d1
	, lc-compile .b ($$BYTECODE _ CALL-CONSTANT .label, fail) .pls .c1/.cx/.d1/.d2
	, .d2 = (.label LABEL .label, .d3)
	, lc-compile .more () .pls .d3/.d4/.d5/.dx
	, .d4 = (_ RETURN, .d5)
#
lc-compile ($$CUT .cutPoint .failLabel) .more .pls .c0/.cx/.d0/.dx
	:- !, lc-compile .more () .pls .c0/.c1/.d0/.dx
	, .c1 = (_ CUT-FAIL .cutPoint .failLabel, .cx)
#
lc-compile (.a = .b) .more .pls .c0/.cx/.d0/.dx
	:- !
	, create-node .a .c0/.c1/.reg0
	, create-node .b .c1/.c2/.reg1
	, .c2 = (_ BIND .reg0 .reg1 .failLabel, .c3)
	, lc-compile .more () .pls .c3/.c4/.d0/.dx
	, .c4 = (.failLabel LABEL .failLabel, . BIND-UNDO, .cx)
#
lc-compile (.rules >> .call) .more .pls .c0/.cx/.d0/.dx
	:- !
	, categorize-rules .rules .groups
	, compile-rules .groups .pls/.pls1 .d1/.dx
	, !, lc-compile ($$SCOPE .call .pls1) .more .pls .c0/.cx/.d0/.d1
#
lc-compile .call .more .pls .c0/.cx/.d0/.dx
	:- call-prototype .call .proto
	, member .pls .proto/.callLabel
	, !, create-node .call .c0/.c1/.reg
	, .c1 = (_ ASSIGN-CLOSURE .provenReg .provenLabel
		, _ PUSH .provenReg
		, _ PUSH .reg
		, _ CALL-CONSTANT .callLabel
		, _ POP _
		, _ POP _
		, .cx
	)
	, .d0 = (.provenLabel LABEL .provenLabel, .d1)
	, lc-compile .more () .pls .d1/.d2/.d3/.dx
	, .d2 = (_ RETURN, .d3)
#
lc-compile .d _ _ _ :- write "Unknown expression" .d, nl, fail #

categorize-rules () _ #
categorize-rules (.rule # .remains) .groups
	:- decompose-rule .rule .head _
	, call-prototype .head .proto
	, member .groups .proto/.rules
	, member .rules .rule
	, !
	, categorize-rules .remains .groups
#

compile-rules () .pls/.pls .c/.c :- ! #
compile-rules (.proto/.rules, .remains) .pls/.plsx .c0/.cx
	:- flatten-rules .rules .call
	, .pls1 = (.proto/.callLabel, .pls)
	, compile-call .call .pls1 .c0/.c1/.callLabel
	, compile-rules .remains .pls1/.plsx .c1/.cx
#

flatten-rules () fail :- ! #
flatten-rules (.rule, .remains) (.head1; .tail1)
	:- decompose-rule .rule .head .tail
	, !, .head1 = ($$BYTECODE _ TOP .reg -1, $$REG:.reg = .head, .tail)
	, flatten-rules .remains .tail1
#

decompose-rule (.head :- .tail) .head .tail :- ! #
decompose-rule .head .head () #

create-node $$REG:.r .c/.c/.r #
create-node .a .c0/.cx/.reg :- is.atom .a, .c0 = (_ ASSIGN-CONSTANT .reg .a, .cx) #
create-node .i .c0/.cx/.reg :- is.int .i, .c0 = (_ ASSIGN-INT .reg .i, .cx) #
create-node .s .c0/.cx/.reg :- is.string .s, .c0 = (_ ASSIGN-CONSTANT .reg .s, .cx) #
create-node .tree .c0/.cx/.reg
	:- tree .tree .left .operator .right
	, create-node .left .c0/.c1/.regl
	, create-node .right .c1/.c2/.regr
	, .c2 = (_ FORM-TREE0 .regl .regr, _ FORM-TREE1 .operator .reg, .cx)
#

call-prototype (.head .remains0) (.head .remains1) :- params-prototype .remains0 .remains1, ! #
call-prototype .head .head #

params-prototype (_ .remains0) (() .remains1) :- params-prototype .remains0 .remains1, ! #
params-prototype .s () :- not (bound .s, tree .s _ ' ' _) #

lc-assign-line-number _ () #
lc-assign-line-number .n (.n _, .remains)
	:- let .n1 (.n + 1), lc-assign-line-number .n1 .remains
#

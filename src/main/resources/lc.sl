-------------------------------------------------------------------------------
-- logical compiler

compile-rule-group .rules .code
	:- flatten-rule-group .rules .call
	, compile .call .code
#

flatten-rule-group () () #
flatten-rule-group (.rule # .remains) (.head1; .tail1)
	:- member (.rule, (.rule :- ()),) (.head :- .tail)
	, !, .head1 = ($$BYTECODE (_ POP .reg), $$REG .reg = .head, .tail)
	, flatten-rule-group .remains .tail1
#

compile .call .c0
	:- .c0 = (_ ENTER, _ CUT-BEGIN .cutPoint, .c1)
	, to.atom "!" .cutSymbol
	, replace .call/.call1 .cutSymbol/($$CUT .cutPoint .failLabel)
	, generalize-variables .call1/.call2 .variables
	, initialize-variables .call2/.call3 .variables
	, lc-compile .call3 (
		$$BYTECODE (_ EXIT-VALUE true), $$BYTECODE (_ LEAVE), fail
	) .c1/.c2/.d0/()
	, .c2 = (.failLabel EXIT-VALUE false, .d0)
	, lc-assign-line-number 0 .c0
#

generalize-variables .variable/($$REG .n) .variables
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
initialize-variables .call0/($$BYTECODE (_ NEW-NODE .n), .call1) (.variable/.n, .remains)
	:- initialize-variables .call0/.call1 .remains
#

lc-compile ($$BYTECODE .bytecode) .more .c0/.cx/.d0/.dx
	:- .c0 = (.bytecode, .c1)
	, lc-compile .more () .c1/.cx/.d0/.dx
#
lc-compile fail _ .c/.c/.d/.d #
lc-compile () .more .c0/.cx/.d0/.dx :- lc-compile .more () .c0/.cx/.d0/.dx #
lc-compile (.a, .b) .more .c0/.cx/.d0/.dx :- lc-compile .a (.b, .more) .c0/.cx/.d0/.dx #
lc-compile (.a; .b) .more .c0/.cx/.d0/.dx
	:- lc-compile .a ($$BYTECODE (_ CALL .label), fail) .c0/.c1/.d0/.d1
	, lc-compile .b ($$BYTECODE (_ CALL .label), fail) .c1/.cx/.d1/.d2
	, .d2 = (.label LABEL .label, .d3)
	, lc-compile .more () .d3/.d4/.d5/.dx
	, .d4 = (_ RETURN, .d5)
#
lc-compile ($$CUT .cutPoint .failLabel) .more .c0/.cx/.d0/.dx
	:- lc-compile .more () .c0/.c1/.d0/.dx
	, .c1 = (_ CUT-FAIL .cutPoint .failLabel, .cx)
#
lc-compile (.a = .b) .more .c0/.cx/.d0/.dx
	:- create-node .a .c0/.c1/.reg0
	, create-node .b .c1/.c2/.reg1
	, .c2 = (_ BIND .reg0 .reg1 .failLabel, .c3)
	, lc-compile .more () .c3/.c4/.d0/.dx
	, .c4 = (.failLabel LABEL .failLabel, . BIND-UNDO, .cx)
#
lc-compile .d _ _ :- write "Unknown expression" .d, nl, fail #

create-node ($$REG .r) .c/.c/.r #
create-node .a .c0/.cx/.reg :- is.atom .a, .c0 = (_ ASSIGN-OBJECT .reg .a, .cx) #
create-node .i .c0/.cx/.reg :- is.int .i, .c0 = (_ ASSIGN-INT .reg .i, .cx) #
create-node .s .c0/.cx/.reg :- is.string .s, .c0 = (_ ASSIGN-OBJECT .reg .s, .cx) #
create-node .tree .c0/.cx/.reg
	:- tree .tree .left .operator .right
	, create-node .left .c0/.c1/.regl
	, create-node .right .c1/.c2/.regr
	, .c2 = (_ FORM-TREE0 .regl .regr, _ FORM-TREE1 .operator .reg, .cx)
#

lc-assign-line-number _ () #
lc-assign-line-number .n (.n _, .remains)
	:- let .n1 (.n + 1), lc-assign-line-number .n1 .remains
#

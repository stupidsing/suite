-------------------------------------------------------------------------------
-- logical compiler

compile .call .c0
	:- .c0 = (_ CUT-POINT .cutPoint .failLabel, .c1)
	, replace .call .call1 ! ($$CUT .cutPoint .failLabel) 
	, lc-compile .call1 $$PROVEN .c1/.c2/.d0/()
	, .c2 = (.failLabel EXIT-FAIL, .d0)
	, lc-assign-line-number 0 .c0
#

lc-compile $$PROVEN () .c0/.cx/.d/.d :- .c0 = (_ PROVEN, .cx) #
lc-compile fail _ .c/.c/.d/.d #
lc-compile () .more .c0/.cx/.d0/.dx :- lc-compile .more () .c0/.cx/.d0/.dx #
lc-compile (.a, .b) .more .c0/.cx/.d0/.dx :- lc-compile .a (.b, .more) .c0/.cx/.d0/.dx #
lc-compile (.a; .b) .more .c0/.cx/.d0/.dx
	:- lc-compile .a (CALL .label) .c0/.c1
	, lc-compile .b (CALL .label) .c1/.cx
	, .d0 = (.label LABEL .label, .d1)
	, lc-compile .more () .d1/.d2/.d3/.dx
	, .d2 = (_ RETURN, .d3)
#
lc-compile ($$CUT .cutPoint .failLabel) .more .c0/.cx/.d0/.dx
	:- .c0 = (_ CUT .cutPoint, .c1)
	, lc-compile .more () .c1/.c2/.d0/.dx
	, .c1 = (_ CUT-FAIL .cutPoint .failLabel, .cx)
#
lc-compile (.a = .b) .more .c0/.cx/.d0/.dx
	:- create-node .a .reg0 .c0/.c1
	, create-node .b .reg1 .c1/.c2
	, .c2 = (_ BIND .reg0 .reg1, _ JUMP-FAIL .failLabel, .c3)
	, lc-compile .more () .c3/.c4/.d0/.dx
	, .c4 = (.failLabel LABEL .failLabel, . BIND-UNDO, .cx)
#

create-node .a .reg .c0/.cx :- is.atom .a, .c0 = (_ ASSIGN-NODE .reg .a, .cx) #
create-node .i .reg .c0/.cx :- is.int .i, .c0 = (_ ASSIGN-NODE .reg .i, .cx) #
create-node .s .reg .c0/.cx :- is.string .s, .c0 = (_ ASSIGN-NODE .reg .s, .cx) #
create-node .tree .reg .c0/.cx
	:- tree .tree .left .operator .right
	, create-node .left .regl .c0/.c1
	, create-node .right .regr .c1/.c2
	, .c2 = (_ FORM-TREE .operator .regl .regr, .cx)
#

is-user-predicate .call :- rules .rules, has-rules .rules (.call :- _) #

has-rules (.rule # _) .rule #
has-rules (_ # .remains) .rule :- has-rules .remains .rule #

lc-assign-line-number _ () #
lc-assign-line-number .n (.n _, .remains)
	:- let .n1 (.n + 1), lc-assign-line-number .n1 .remains
#

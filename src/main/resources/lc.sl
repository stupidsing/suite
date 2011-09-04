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
	, lc-compile .call1 (
		$$BYTECODE _ CALL-CLOSURE .provenReg .provenReg, fail
	) .pls/_ .c1/.c2/.c3/.c4
	, .c2 = (.failLabel RETURN, .c3)
	, .c4 = (_ LEAVE, .cx)
#

--lc-compile .p :- write lc-compile .p, nl, fail #
lc-compile ($$BYTECODE .bytecode) .more .env .c0/.cx/.d0/.dx
	:- !, .c0 = (.bytecode, .c1)
	, lc-compile .more () .env .c1/.cx/.d0/.dx
#
lc-compile ($$SCOPE .call .pls1) .more .pls/.vs .c0/.cx/.d0/.dx
	:- !
	, (.more/.more1 = ()/(); .more1 = $$SCOPE .more .pls)
	, lc-compile .call .more1 .pls1/.vs .c0/.cx/.d0/.dx
#
lc-compile fail _ _ .c/.c/.d/.d :- ! #
lc-compile () .more .env .c0/.cx/.d0/.dx
	:- !, lc-compile .more () .env .c0/.cx/.d0/.dx
#
lc-compile (.a, .b) .more .env .c0/.cx/.d0/.dx
	:- !, lc-compile .a (.b, .more) .env .c0/.cx/.d0/.dx
#
lc-compile (.a; .b) .more .env .c0/.cx/.d0/.dx
	:- !
	, lc-compile .a ($$BYTECODE _ CALL-CONSTANT .label, fail) .env .c0/.c1/.d0/.d1
	, lc-compile .b ($$BYTECODE _ CALL-CONSTANT .label, fail) .env .c1/.cx/.d1/.d2
	, .d2 = (.label LABEL .label, .d3)
	, lc-compile .more () .env .d3/.d4/.d5/.dx
	, .d4 = (_ RETURN, .d5)
#
lc-compile ($$CUT .cutPoint .failLabel) .more .env .c0/.cx/.d0/.dx
	:- !, lc-compile .more () .env .c0/.c1/.d0/.dx
	, .c1 = (_ CUT-FAIL .cutPoint .failLabel, .cx)
#
lc-compile (.a = .b) .more .pls/.vs .c0/.cx/.d0/.dx
	:- !
	, create-node .a .vs .c0/.c1/.reg0
	, create-node .b .vs .c1/.c2/.reg1
	, .c2 = (_ BIND .reg0 .reg1 .failLabel, .c3)
	, lc-compile .more () .pls/.vs .c3/.c4/.d0/.dx
	, .c4 = (.failLabel LABEL .failLabel, . BIND-UNDO, .cx)
#
lc-compile (.rules >> .call) .more .pls/.vs .c0/.cx/.d0/.dx
	:- !
	, categorize-rules .rules .groups
	, predicate-labels .groups .pls/.pls1
	, compile-rules .groups .pls1 .d1/.dx
	, !, lc-compile ($$SCOPE .call .pls1) .more .pls/.vs .c0/.cx/.d0/.d1
#
lc-compile .call .more .pls/.vs .c0/.cx/.d0/.dx
	:- call-prototype .call .proto
	, member .pls .proto/.callLabel
	, !, create-node .call .vs .c0/.c1/.reg
	, .c1 = (_ ASSIGN-CLOSURE .provenReg .provenLabel
		, _ PUSH .provenReg
		, _ PUSH .reg
		, _ CALL-CONSTANT .callLabel
		, _ POP _
		, _ POP _
		, .cx
	)
	, .d0 = (.provenLabel LABEL .provenLabel, .d1)
	, lc-compile .more () .pls/.vs .d1/.d2/.d3/.dx
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

call-prototype (.name .ps) .name/.n :- params-length .ps .n, ! #
call-prototype .name .name #

params-length .ps .n
	:- if (bound .ps, .ps = _ .ps1) then (
		params-length .ps1 .n1, sum .n 1 .n1
	) else-if (.n > 1, .ps = _ .ps1) then (
		sum .n 1 .n1, params-length .ps1 .n1
	) else (
		(not bound .ps; .ps != _ _), .n = 1
	)
#

predicate-labels () .pls/.pls :- ! #
predicate-labels (.proto/_, .tail) .pls/(.proto/_, .pls1)
	:- predicate-labels .tail .pls/.pls1
#

compile-rules () _ .c/.c :- ! #
compile-rules (.proto/.rules, .remains) .pls .c0/.cx
	:- flatten-rules .rules .call
	, member .pls .proto/.callLabel
	, .l = '----------------'
	, .c0 = (_ REMARK .l .proto .l, .c1) -- debug purpose
	, compile-call .call .pls .c1/.c2/.callLabel
	, compile-rules .remains .pls .c2/.cx
#

flatten-rules () fail :- ! #
flatten-rules (.rule, .remains) (.head1; .tail1)
	:- decompose-rule .rule .head .tail
	, !, .head1 = ($$BYTECODE _ TOP .reg -1, $$REG:.reg = .head, .tail)
	, flatten-rules .remains .tail1
#

decompose-rule (.head :- .tail) .head .tail :- ! #
decompose-rule .head .head () #

create-node $$REG:.reg _ .c/.c/.reg :- ! #
create-node .var .vs .c0/.cx/.reg
	:- is-variable .var
	, !, member .vs .var/.reg/.created
	, (bound .created, .c0 = .cx
		; .created = CREATED, .c0 = (_ NEW-NODE .reg, .cx)
	)
	, !
#
create-node .a _ .c0/.cx/.reg :- is.atom .a, !, .c0 = (_ ASSIGN-CONSTANT .reg .a, .cx) #
create-node .i _ .c0/.cx/.reg :- is.int .i, !, .c0 = (_ ASSIGN-INT .reg .i, .cx) #
create-node .s _ .c0/.cx/.reg :- is.string .s, !, .c0 = (_ ASSIGN-CONSTANT .reg .s, .cx) #
create-node .tree .vs .c0/.cx/.reg
	:- tree .tree .left .operator .right
	, create-node .left .vs .c0/.c1/.regl
	, create-node .right .vs .c1/.c2/.regr
	, .c2 = (_ FORM-TREE0 .regl .regr, _ FORM-TREE1 .operator .reg, .cx)
#

is-variable .variable
	:- is.atom .variable
	, to.atom "." .dot, starts.with .variable .dot
#

lc-assign-line-number _ () #
lc-assign-line-number .n (.n _, .remains)
	:- let .n1 (.n + 1), lc-assign-line-number .n1 .remains
#

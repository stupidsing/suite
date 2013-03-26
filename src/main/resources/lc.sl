-------------------------------------------------------------------------------
-- logical program compiler

compile-logic .call .c0
	:- .c0 = (_ ENTER
		, _ ASSIGN-CONSTANT .returnReg true
		, _ ASSIGN-CLOSURE .provenReg .provenLabel
		, _ PUSH .provenReg
		, _ PUSH .provenReg
		, _ CALL-CONSTANT .callLabel
		, _ POP _
		, _ POP _
		, _ ASSIGN-CONSTANT .returnReg false
		, .provenLabel EXIT .returnReg
		, .c1
	)
	, lc-parse .call .call1
	, !, compile-call .call1 () .c1/()/.callLabel
	, !, lc-assign-line-number 0 .c0
#

compile-call .call .pls .c0/.cx/.label
	:- .c0 = (.label ENTER
		, _ CUT-BEGIN .cutPoint
		, _ TOP .provenReg -2
		, .c1
	)
	, replace .call/.call1 CUT/($$CUT .cutPoint .failLabel)
	, lc-compile .call1 (
		AND ($$BYTECODE _ CALL-CLOSURE .provenReg .provenReg) FAIL
	) .pls/_ .c1/.c2/.c3/.c4
	, .c2 = (.failLabel RETURN, .c3)
	, .c4 = (_ LEAVE, .cx)
#

lc-parse (.rules >> .clause) (DEFINE-RULES .rules1 .clause1)
	:- !, lc-parse-rules .rules .rules1, lc-parse-clause .clause .clause1
#
lc-parse .clause .clause1 :- lc-parse-clause .clause .clause1 #

lc-parse-rules () () :- ! #
lc-parse-rules (.rule # .rules) (.rule1, .rules1)
	:- lc-parse-rule .rule .rule1, lc-parse-rules .rules .rules1
#

lc-parse-rule (.head :- .tail) (RULE .head1 .tail1)
	:- !, lc-parse-call .head .head1, lc-parse-clause .tail .tail1
#
lc-parse-rule .head (RULE .head1 YES) :- lc-parse-call .head .head1 #

lc-parse-clause () YES :- ! #
lc-parse-clause fail FAIL :- ! #
lc-parse-clause .cut CUT :- to.atom "!" .cut, ! #
lc-parse-clause .tree (.oper1 .left1 .right1)
	:- tree .tree .left .oper .right
	, member (','/AND, ';'/OR,
		' = '/EQ, ' != '/NE, ' > '/GT, ' < '/LT, ' >= '/GE, ' <= '/LE,
	) .oper/.oper1, !
	, lc-parse-clause .left .left1
	, lc-parse-clause .right .right1
#
lc-parse-clause .call .callx
	:- lc-parse-call .call .call1
	, lc-call-prototype .call1 .proto
	, (lc-system-call-prototype .proto, !
		, lc-parse-pattern .call .call2
		, .callx = SYSTEM-CALL .call2
	; .callx = CALL .call1
	)
#
lc-parse-clause .d _ :- write "Unknown expression" .d, nl, fail #

lc-parse-call .head .head2
	:- !, enlist .head .head1, lc-parse-pattern .head1 .head2
#

lc-parse-pattern .tree .tree1
	:- tree .tree .left .oper .right
	, tree .tree1 .left1 .oper .right1, !
	, lc-parse-pattern .left .left1
	, lc-parse-pattern .right .right1
#
lc-parse-pattern .var (VAR .var) :- lc-is-variable .var, ! #
lc-parse-pattern .e .e #

--lc-compile .p :- write lc-compile .p, nl, fail #
lc-compile ($$BYTECODE .bytecode) .more .env .c0/.cx/.d0/.dx
	:- !, .c0 = (.bytecode, .c1)
	, lc-compile .more YES .env .c1/.cx/.d0/.dx
#
lc-compile ($$SCOPE .call .pls1) .more .pls/.vs .c0/.cx/.d0/.dx
	:- !
	, (.more/.more1 = YES/YES; .more1 = $$SCOPE .more .pls)
	, lc-compile .call .more1 .pls1/.vs .c0/.cx/.d0/.dx
#
lc-compile FAIL _ _ .c/.c/.d/.d :- ! #
lc-compile YES .more .env .c0/.cx/.d0/.dx
	:- !, lc-compile .more YES .env .c0/.cx/.d0/.dx
#
lc-compile (AND .a .b) .more .env .c0/.cx/.d0/.dx
	:- !, lc-compile .a (AND .b .more) .env .c0/.cx/.d0/.dx
#
lc-compile (OR .a .b) .more .env .c0/.cx/.d0/.dx
	:- !
	, .bc = CALL-CONSTANT .label
	, lc-compile .a (AND ($$BYTECODE _ .bc) FAIL) .env .c0/.c1/.d0/.d1
	, lc-compile .b (AND ($$BYTECODE _ .bc) FAIL) .env .c1/.cx/.d1/.d2
	, .d2 = (.label LABEL .label, .d3)
	, lc-compile .more YES .env .d3/.d4/.d5/.dx
	, .d4 = (_ RETURN, .d5)
#
lc-compile ($$CUT .cutPoint .failLabel) .more .env .c0/.cx/.d0/.dx
	:- !, lc-compile .more YES .env .c0/.c1/.d0/.dx
	, .c1 = (_ CUT-FAIL .cutPoint .failLabel, .cx)
#
lc-compile (EQ .a .b) .more .pls/.vs .c0/.cx/.d0/.dx
	:- !
	, lc-create-node .a .vs .c0/.c1/.reg0
	, lc-create-node .b .vs .c1/.c2/.reg1
	, .c2 = (_ BIND .reg0 .reg1 .failLabel, .c3)
	, lc-compile .more YES .pls/.vs .c3/.c4/.d0/.dx
	, .c4 = (.failLabel LABEL .failLabel, _ BIND-UNDO, .cx)
#
lc-compile (DEFINE-RULES .rules .call) .more .pls/.vs .c0/.cx/.d0/.dx
	:- !
	, lc-categorize-rules .rules .groups
	, lc-prototype-labels .groups .pls/.pls1
	, lc-compile-rules .groups .pls1 .d1/.dx
	, !, lc-compile ($$SCOPE .call .pls1) .more .pls/.vs .c0/.cx/.d0/.d1
#
lc-compile (CALL .call) .more .pls/.vs .c0/.cx/.d0/.dx
	:- !
	, lc-call-prototype .call .proto
	, lc-create-node .call .vs .c0/.c1/.reg
	, member .pls .proto/.callLabel
	, .c1 = (_ ASSIGN-CLOSURE .provenReg .provenLabel
		, _ PUSH .provenReg
		, _ PUSH .reg
		, _ CALL-CONSTANT .callLabel
		, _ POP _
		, _ POP _
		, .cx
	)
	, .d0 = (.provenLabel LABEL .provenLabel, .d1)
	, lc-compile .more YES .pls/.vs .d1/.d2/.d3/.dx
	, .d2 = (_ RETURN, .d3)
#
lc-compile (SYSTEM-CALL .call) .more .pls/.vs .c0/.cx/.d0/.dx
	:- !
	, lc-create-node .call .vs .c0/.c1/.reg
	, .c1 = (_ PROVE-SYS .reg .failLabel, .c2)
	, lc-compile .more YES .pls/.vs .c2/.c3/.d0/.dx
	, .c3 = (.failLabel LABEL .failLabel, .cx)
#

lc-categorize-rules () _ #
lc-categorize-rules (.rule, .remains) .groups
	:- .rule = RULE .head .tail
	, lc-call-prototype .head .proto
	, member .groups .proto/.rules
	, member .rules .rule
	, !
	, lc-categorize-rules .remains .groups
#

lc-flatten-rules () FAIL :- ! #
lc-flatten-rules (RULE .head .tail, .remains) (OR .head1 .tail1)
	:- .head1 = AND ($$BYTECODE _ TOP .reg -1) AND (EQ $$REG:.reg .head) .tail
	, lc-flatten-rules .remains .tail1
#

lc-call-prototype (.name, .call) .name/.n :- length .call .n #

lc-params-length .ps .n
	:- if (bound .ps, .ps = _ .ps1) then (
		lc-params-length .ps1 .n1, sum .n 1 .n1
	) else-if (.n > 1, .ps = _ .ps1) then (
		sum .n 1 .n1, lc-params-length .ps1 .n1
	) else (
		(not bound .ps; .ps != _ _), .n = 1
	)
#

lc-prototype-labels () .pls/.pls :- ! #
lc-prototype-labels (.proto/_, .tail) .pls/(.proto/_, .pls1)
	:- lc-prototype-labels .tail .pls/.pls1
#

lc-compile-rules () _ .c/.c :- ! #
lc-compile-rules (.proto/.rules, .remains) .pls .c0/.cx
	:- lc-flatten-rules .rules .call
	, member .pls .proto/.callLabel
	, .l = '-----'
	, .c0 = (_ REMARK .l .proto .l, .c1) -- debug purpose
	, compile-call .call .pls .c1/.c2/.callLabel
	, lc-compile-rules .remains .pls .c2/.cx
#

lc-create-node $$REG:.reg _ .c/.c/.reg :- ! #
lc-create-node (VAR .var) .vs .c0/.cx/.reg
	:- !, member .vs .var/.reg/.first
	, (bound .first, .c0 = .cx
		; .first = N, .c0 = (_ NEW-NODE .reg, .cx)
	)
	, !
#
lc-create-node .a _ .c0/.cx/.reg
	:- is.atom .a, !, .c0 = (_ ASSIGN-CONSTANT .reg .a, .cx)
#
lc-create-node .i _ .c0/.cx/.reg
	:- is.int .i, !, .c0 = (_ ASSIGN-INT .reg .i, .cx)
#
lc-create-node .s _ .c0/.cx/.reg
	:- is.string .s, !, .c0 = (_ ASSIGN-CONSTANT .reg .s, .cx)
#
lc-create-node .tree .vs .c0/.cx/.reg
	:- tree .tree .left .operator .right
	, lc-create-node .left .vs .c0/.c1/.regl
	, lc-create-node .right .vs .c1/.c2/.regr
	, .c2 = (_ FORM-TREE0 .regl .regr, _ FORM-TREE1 .operator .reg, .cx)
#

lc-system-call-prototype let/2 #
lc-system-call-prototype once/1 #
lc-system-call-prototype find.all/3 #

lc-is-variable .variable
	:- is.atom .variable, to.atom "." .dot, starts.with .variable .dot
#

lc-assign-line-number _ () #
lc-assign-line-number .n (.n _, .remains)
	:- let .n1 (.n + 1), lc-assign-line-number .n1 .remains
#

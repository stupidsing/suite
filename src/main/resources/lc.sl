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
	) .pls/()/_ .c1/.c2/.c3/.c4
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
lc-parse-clause (.p .do) (.p1 .do1)
	:- member (once/ONCE, not/NOT,) .p/.p1, !
	, lc-parse-clause .do .do1
#
lc-parse-clause .tree (.oper1 .left1 .right1)
	:- tree .tree .left .oper .right
	, member (','/AND, ';'/OR,) .oper/.oper1, !
	, lc-parse-clause .left .left1
	, lc-parse-clause .right .right1
#
lc-parse-clause .tree (.oper1 .left1 .right1)
	:- tree .tree .left .oper .right
	, member (
		' = '/EQ, ' != '/NE, ' > '/GT, ' < '/LT, ' >= '/GE, ' <= '/LE,
	) .oper/.oper1
	, !
	, lc-parse-pattern .left .left1
	, lc-parse-pattern .right .right1
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

lc-parse-call .head .head1 :- !, lc-parse-pattern .head .head1 #

lc-parse-pattern .tree (TREE .oper .left1 .right1)
	:- tree .tree .left .oper .right, !
	, lc-parse-pattern .left .left1
	, lc-parse-pattern .right .right1
#
lc-parse-pattern .var (VAR .var) :- lc-is-variable .var, ! #
lc-parse-pattern .wildcard (VAR .var) :- to.atom "_" .wildcard, temp .var, ! #
lc-parse-pattern .a (ATOM .a) :- is.atom .a #
lc-parse-pattern .i (NUMBER .i) :- is.int .i #
lc-parse-pattern .s (STRING .s) :- is.string .s #

--lc-compile .p :- write lc-compile .p, nl, fail #
lc-compile ($$BYTECODE .bytecode) .more .env .c0/.cx/.d0/.dx
	:- !, .c0 = (.bytecode, .c1)
	, lc-compile .more YES .env .c1/.cx/.d0/.dx
#
lc-compile ($$SCOPE .call .pls1) .more .pls/.v0/.vx .c0/.cx/.d0/.dx
	:- !
	, once (.more/.more1 = YES/YES; .more1 = $$SCOPE .more .pls)
	, lc-compile .call .more1 .pls1/.v0/.vx .c0/.cx/.d0/.dx
#
lc-compile FAIL _ _/.v/.v .c/.c/.d/.d :- ! #
lc-compile YES .more .env .c0/.cx/.d0/.dx
	:- !, lc-compile .more YES .env .c0/.cx/.d0/.dx
#
lc-compile (AND .a .b) .more .env .c0/.cx/.d0/.dx
	:- !, lc-compile .a (AND .b .more) .env .c0/.cx/.d0/.dx
#
lc-compile (OR .a .b) .more .pls/.v0/.vx .c0/.cx/.d0/.dx
	:- !
	, .bc = CALL-CONSTANT .label
	, lc-compile .a (AND ($$BYTECODE _ .bc) FAIL) .pls/.v0/.v1 .c0/.c1/.d0/.d1
	, lc-compile .b (AND ($$BYTECODE _ .bc) FAIL) .pls/.v1/.v2 .c1/.cx/.d1/.d2
	, .d2 = (.label LABEL .label, .d3)
	, lc-compile .more YES .pls/.v2/.vx .d3/.d4/.d5/.dx
	, .d4 = (_ RETURN, .d5)
#
lc-compile ($$CUT .cutPoint .failLabel) .more .env .c0/.cx/.d0/.dx
	:- !, lc-compile .more YES .env .c0/.c1/.d0/.dx
	, .c1 = (_ CUT-FAIL .cutPoint .failLabel, .cx)
#
lc-compile (ONCE .do) .more .env .c0/.cx/.d0/.dx
	:- !
	, .c0 = (_ CUT-BEGIN .cutPoint, .c1)
	, lc-compile .do (AND ($$CUT .cutPoint .failLabel) .more) .env .c1/.c2/.d0/.dx
	, .c2 = (.failLabel LABEL .failLabel, .cx)
#
lc-compile (NOT .do) .more .pls/.v0/.vx .c0/.cx/.d0/.dx
	:- !
	, .c0 = (_ CUT-BEGIN .cutPoint, .c1)
	, lc-compile .do (AND ($$CUT .cutPoint .failLabel) FAIL) .pls/.v0/.v1 .c1/.c2/.d0/.d1
	, .c2 = (.failLabel LABEL .failLabel, .c3)
	, lc-compile .more YES .pls/.v1/.vx .c3/.cx/.d1/.dx
#
lc-compile (EQ .a .b) .more .pls/.v0/.vx .c0/.cx/.d0/.dx
	:- !
	, lc-bind .a .b .v0/.v1 .c0/.c1/.c2/.cx
	, lc-compile .more YES .pls/.v1/.vx .c1/.c2/.d0/.dx
#
lc-compile (.oper .a .b) .more .pls/.v0/.vx .c0/.cx/.d0/.dx
	:- member (GE, GT, LE, LT, NE,) .oper
	, !
	, to.string .oper .os, concat "EVAL-" .os .is, to.atom .is .inst
	, lc-create-node .a .v0/.v1 .c0/.c1/.reg0
	, lc-create-node .b .v1/.v2 .c1/.c2/.reg1
	, .c2 = (_ .inst .resultReg .reg0 .reg1
		, _ IF-FALSE .failLabel .resultReg
		, .c3
	)
	, lc-compile .more YES .pls/.v2/.vx .c3/.c4/.d0/.dx
	, .c4 = (.failLabel LABEL .failLabel, .c5)
	, .c5 = .cx
#
lc-compile (DEFINE-RULES .rules .call) .more .pls/.v .c0/.cx/.d0/.dx
	:- !
	, lc-merge-rules .rules .groups
	, lc-prototype-labels .groups .pls/.pls1
	, lc-compile-rules .groups .pls1 .d1/.dx
	, !, lc-compile ($$SCOPE .call .pls1) .more .pls/.v .c0/.cx/.d0/.d1
#
lc-compile (CALL .call) .more .pls/.v0/.vx .c0/.cx/.d0/.dx
	:- !
	, lc-call-prototype .call .proto
	, lc-create-node .call .v0/.v1 .c0/.c1/.reg, (
		member .pls .proto/.callLabel, !
		, .c1 = (_ ASSIGN-CLOSURE .provenReg .provenLabel
			, _ PUSH .provenReg
			, _ PUSH .reg
			, _ CALL-CONSTANT .callLabel
			, _ POP _
			, _ POP _
			, .cx
		)
		, .d0 = (.provenLabel LABEL .provenLabel, .d1)
		, lc-compile .more YES .pls/.v1/.vx .d1/.d2/.d3/.dx
		, .d2 = (_ RETURN, .d3)
		; .c1 = (_ PROVE-INTERPRET .reg .failLabel, .c2)
		, lc-compile .more YES .pls/.v1/.vx .c2/.c3/.d0/.dx
		, .c3 = (.failLabel LABEL .failLabel, .cx)
	)
#
lc-compile (SYSTEM-CALL .call) .more .pls/.v0/.vx .c0/.cx/.d0/.dx
	:- !
	, lc-create-node .call .v0/.v1 .c0/.c1/.reg
	, .c1 = (_ PROVE-SYS .reg .failLabel, .c2)
	, lc-compile .more YES .pls/.v1/.vx .c2/.c3/.d0/.dx
	, .c3 = (.failLabel LABEL .failLabel, .cx)
#

lc-merge-rules () _ #
lc-merge-rules (.rule, .remains) .groups
	:- .rule = RULE .head .tail
	, lc-call-prototype .head .proto
	, member .groups .proto/.rules
	, member .rules .rule
	, !
	, lc-merge-rules .remains .groups
#

lc-bind (TREE .tree) .node .v .cs :- !, lc-bind0 .node (TREE .tree) .v .cs #
lc-bind .node0 .node1 .v .cs :- lc-bind0 .node0 .node1 .v .cs #

lc-bind0 .node .node .v/.v .c/.c/.f/.f :- not (.node = $$REG:_) #
lc-bind0 (TREE .oper .nl0 .nr0) (TREE .oper .nl1 .nr1) .v0/.vx .c0/.cx/.f0/.fx
	:- !
	, lc-bind .nl0 .nl1 .v0/.v1 .c0/.c1/.f1/.fx
	, lc-bind .nr0 .nr1 .v1/.vx .c1/.cx/.f0/.f1
#
lc-bind0 .node0 .node1 .v0/.vx .c0/.cx/.f0/.fx
	:- lc-create-node .node0 .v0/.v1 .c0/.c1/.reg0
	, lc-bind-register .reg0 .node1 .v1/.vx .c1/.cx/.f0/.fx
#

lc-bind-register .reg (TREE .oper .nl .nr) .v/.v .c0/.cx/.f0/.fx
	:- .c0 = (_ DECOMPOSE-TREE0 .reg .oper .f
		, _ DECOMPOSE-TREE1 .reg0 .reg1
		, .c1
	)
	, lc-bind-register .reg0 .nl .c1/.c2/.f1/.fx
	, lc-bind-register .reg1 .nr .c2/.cx/.f0/.f1
#
lc-bind-register .reg0 .node1 .v .c0/.cx/.f0/.fx
	:- lc-create-node .node1 .v .c0/.c1/.reg1
	, .c1 = (_ BIND .reg0 .reg1 .failLabel, .cx)
	, .f0 = (.failLabel BIND-UNDO, .fx)
#

lc-flatten-rules () FAIL :- ! #
lc-flatten-rules (RULE .head .tail, .remains) (OR .head1 .tail1)
	:- .head1 = AND ($$BYTECODE _ TOP .reg -1) AND (EQ $$REG:.reg .head) .tail
	, lc-flatten-rules .remains .tail1
#

lc-call-prototype (TREE _ .left _) .name :- lc-call-prototype .left .name #
lc-call-prototype .name .name #

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

lc-create-node $$REG:.reg .v/.v .c/.c/.reg :- ! #
lc-create-node (VAR .var) .v0/.vx .c0/.cx/.reg
	:- member .v0 .var/.reg, !
		, .v0 = .vx, .c0 = .cx
	; .vx = (.var/.reg, .v0), .c0 = (_ NEW-NODE .reg, .cx)
#
lc-create-node (ATOM .a) .v/.v .c0/.cx/.reg
	:- !, .c0 = (_ ASSIGN-CONSTANT .reg .a, .cx)
#
lc-create-node (NUMBER .i) .v/.v .c0/.cx/.reg
	:- !, .c0 = (_ ASSIGN-INT .reg .i, .cx)
#
lc-create-node (STRING .s) .v/.v .c0/.cx/.reg
	:- !, .c0 = (_ ASSIGN-CONSTANT .reg .s, .cx)
#
lc-create-node (TREE .operator .left .right) .v0/.vx .c0/.cx/.reg
	:- lc-create-node .left .v0/.v1 .c0/.c1/.regl
	, lc-create-node .right .v1/.vx .c1/.c2/.regr
	, .c2 = (_ FORM-TREE0 .regl .regr, _ FORM-TREE1 .operator .reg, .cx)
#

lc-system-call-prototype (ATOM bound) #
lc-system-call-prototype (ATOM clone) #
lc-system-call-prototype (ATOM concat) #
lc-system-call-prototype (ATOM dump) #
lc-system-call-prototype (ATOM dump.stack) #
lc-system-call-prototype (ATOM eval.js) #
lc-system-call-prototype (ATOM exec) #
lc-system-call-prototype (ATOM file.exists) #
lc-system-call-prototype (ATOM file.read) #
lc-system-call-prototype (ATOM file.write) #
lc-system-call-prototype (ATOM find.all) #
lc-system-call-prototype (ATOM generalize) #
lc-system-call-prototype (ATOM generalize.prefix) #
lc-system-call-prototype (ATOM hash) #
lc-system-call-prototype (ATOM home.dir) #
lc-system-call-prototype (ATOM is.atom) #
lc-system-call-prototype (ATOM is.int) #
lc-system-call-prototype (ATOM is.string) #
lc-system-call-prototype (ATOM is.tree) #
lc-system-call-prototype (ATOM let) #
lc-system-call-prototype (ATOM map.erase) #
lc-system-call-prototype (ATOM map.retrieve) #
lc-system-call-prototype (ATOM nl) #
lc-system-call-prototype (ATOM parse) #
lc-system-call-prototype (ATOM pretty.print) #
lc-system-call-prototype (ATOM random) #
lc-system-call-prototype (ATOM rpn) #
lc-system-call-prototype (ATOM same) #
lc-system-call-prototype (ATOM specialize) #
lc-system-call-prototype (ATOM starts.with) #
lc-system-call-prototype (ATOM string.length) #
lc-system-call-prototype (ATOM substring) #
lc-system-call-prototype (ATOM temp) #
lc-system-call-prototype (ATOM to.atom) #
lc-system-call-prototype (ATOM to.dump.string) #
lc-system-call-prototype (ATOM to.int) #
lc-system-call-prototype (ATOM to.string) #
lc-system-call-prototype (ATOM tree) #
lc-system-call-prototype (ATOM trim) #
lc-system-call-prototype (ATOM write) #

lc-is-variable .variable
	:- is.atom .variable, to.atom "." .dot, starts.with .variable .dot
#

lc-assign-line-number _ () #
lc-assign-line-number .n (.n _, .remains)
	:- let .n1 (.n + 1), lc-assign-line-number .n1 .remains
#

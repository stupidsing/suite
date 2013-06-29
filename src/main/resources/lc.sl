-------------------------------------------------------------------------------
-- logical program compiler

() :- import.file 'rbt.sl' #

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
		, .provenLabel LABEL
		, _ EXIT .returnReg
		, _ LEAVE
		, .c1
	)
	, lc-parse .call .call1 .nv
	, lc-define-new-variables .call1 .nv .call2
	, !, compile-call .call2 () .c1/()/.callLabel
	, !, lc-assign-line-number 0 .c0
#

compile-call .call .pls .c0/.cx/.label
	:- .c0 = (.label LABEL
		, _ ENTER
		, _ CUT-BEGIN .cutPoint
		, _ TOP .provenReg -2
		, .c1
	)
	, replace .call/.call1 CUT/($$CUT .cutPoint .failLabel)
	, lc-compile .call1 (AND ($$BYTECODE _ CALL-CLOSURE .provenReg) FAIL) .pls/() .c1/.c2/.c3/.c4
	, .c2 = (.failLabel LABEL, _ RETURN, .c3)
	, .c4 = (_ LEAVE, .cx)
#

lc-define-new-variables .parsed .nv (DEFINE-NEW-VARS .nvs .parsed)
	:- rbt-get-list .nv .nvs/()
#

lc-parse .clause .clause2 .nv
	:- lc-parse-sugar .clause .clause1
	, !, lc-parse .clause1 .clause2 .nv
#
lc-parse (.rules >> .clause) (DEFINE-RULES .rules1 .clause1) .nv
	:- !, lc-parse-rules .rules .rules1
	, lc-parse .clause .clause1 .nv
#
lc-parse () YES () :- ! #
lc-parse fail FAIL () :- ! #
lc-parse .cut CUT () :- to.atom "!" .cut, ! #
lc-parse (.p .do) (.p1 .do1) .nv
	:- member (not/NOT, once/ONCE,) .p/.p1, !
	, lc-parse .do .do1 .nv
#
lc-parse .tree (.oper1 .left1 .right1) .nv
	:- tree .tree .left .oper .right
	, member (','/AND, ';'/OR,) .oper/.oper1, !
	, lc-parse .left .left1 .nv0
	, lc-parse .right .right1 .nv1
	, lc-dict-merge .nv0 .nv1 .nv
#
lc-parse .tree (.oper1 .left1 .right1) .nv
	:- tree .tree .left .oper .right
	, member (
		' = '/EQ, ' != '/NE, ' > '/GT, ' < '/LT, ' >= '/GE, ' <= '/LE,
	) .oper/.oper1
	, !
	, lc-parse-pattern .left .left1 .nv0
	, lc-parse-pattern .right .right1 .nv1
	, lc-dict-merge .nv0 .nv1 .nv
#
lc-parse .call .callx .nv
	:- lc-parse-pattern .call .call1 .nv
	, lc-call-prototype .call1 .proto
	, once (lc-system-call-prototype .proto
		, .callx = SYSTEM-CALL .call1
	; .callx = CALL .call1
	), !
#
lc-parse .d _ :- write "Unknown expression" .d, nl, fail #

lc-parse-sugar (.a != .b) (not (.a = .b)) #

lc-parse-rules () () :- ! #
lc-parse-rules (.rule # .rules) (.rule1, .rules1)
	:- lc-parse-rule .rule .rule1, lc-parse-rules .rules .rules1
#

lc-parse-rule (.head :- .tail) (RULE .nv .head1 .tail1)
	:- !
	, lc-parse-pattern .head .head1 .nv0
	, lc-parse .tail .tail1 .nv1
	, lc-dict-merge .nv0 .nv1 .nv
#
lc-parse-rule .head .parsed :- lc-parse-rule (.head :- ()) .parsed #

lc-parse-pattern .tree (TREE .oper .left1 .right1) .nv
	:- tree .tree .left .oper .right, !
	, lc-parse-pattern .left .left1 .nv0
	, lc-parse-pattern .right .right1 .nv1
	, lc-dict-merge .nv0 .nv1 .nv
#
lc-parse-pattern .var (VAR .var) .nv
	:- lc-is-variable .var, !, lc-dict-add .var/_ ()/.nv
#
lc-parse-pattern .wildcard WILDCARD ()
	:- to.atom "_" .wildcard, temp .var, !
#
lc-parse-pattern .a (ATOM .a) () :- is.atom .a #
lc-parse-pattern .i (NUMBER .i) () :- is.int .i #
lc-parse-pattern .s (STRING .s) () :- is.string .s #

lc-compile (
	DEFINE-NEW-VARS (.var/.reg, .varRegs) .do
) .rem .pls/.vs .c0/.cx/.d0/.dx
	:- !
	, .c0 = (_ NEW-NODE .reg, .c1)
	, lc-compile (DEFINE-NEW-VARS .varRegs .do)
		.rem .pls/(.var/.reg, .vs) .c1/.cx/.d0/.dx
#
lc-compile (DEFINE-NEW-VARS () .do) .rem .env .c0/.cx/.d0/.dx
	:- !, lc-compile .do .rem .env .c0/.cx/.d0/.dx
#
lc-compile ($$BYTECODE .bytecode) .rem .env .c0/.cx/.d0/.dx
	:- !, .c0 = (.bytecode, .c1)
	, lc-compile .rem YES .env .c1/.cx/.d0/.dx
#
lc-compile ($$SCOPE .call .pls1) .rem .pls/.vs .c0/.cx/.d0/.dx
	:- !
	, once (.rem/.rem1 = YES/YES; .rem1 = $$SCOPE .rem .pls)
	, lc-compile .call .rem1 .pls1/.vs .c0/.cx/.d0/.dx
#
lc-compile FAIL _ _ .c/.c/.d/.d :- ! #
lc-compile YES .rem .env .c0/.cx/.d0/.dx
	:- !, lc-compile .rem YES .env .c0/.cx/.d0/.dx
#
lc-compile (AND .a .b) .rem .env .c0/.cx/.d0/.dx
	:- !, lc-compile .a (AND .b .rem) .env .c0/.cx/.d0/.dx
#
lc-compile (OR FAIL .do) .ps :- !, lc-compile .do .ps #
lc-compile (OR .do FAIL) .ps :- !, lc-compile .do .ps #
lc-compile (OR .a .b) .rem .pls/.vs .c0/.cx/.d0/.dx
	:- !
	, .bc = CALL-CONSTANT .label
	, .c0 = (_ BIND-MARK .pitReg, .c1)
	, lc-compile (AND .a ($$BYTECODE _ .bc)) FAIL .pls/.vs .c1/.c2/.d0/.d1
	, .c2 = (_ BIND-UNDO .pitReg, .c3)
	, lc-compile (AND .b ($$BYTECODE _ .bc)) FAIL .pls/.vs .c3/.cx/.d1/.d2
	, .d2 = (.label LABEL, .d3)
	, lc-compile .rem YES .pls/.vs .d3/.d4/.d5/.dx
	, .d4 = (_ RETURN, .d5)
#
lc-compile ($$CUT .cutPoint .failLabel) .rem .env .c0/.cx/.d0/.dx
	:- !, lc-compile .rem YES .env .c0/.c1/.d0/.dx
	, .c1 = (_ CUT-FAIL .cutPoint .failLabel, .cx)
#
lc-compile (ONCE .do) .rem .env .c0/.cx/.d0/.dx
	:- !
	, .c0 = (_ CUT-BEGIN .cutPoint, .c1)
	, lc-compile .do (
		AND ($$CUT .cutPoint .failLabel) .rem
	) .env .c1/.c2/.d0/.dx
	, .c2 = (.failLabel LABEL, .cx)
#
lc-compile (NOT .do) .rem .env .c0/.cx/.d0/.dx
	:- !
	, .c0 = (_ BIND-MARK .pit
		, _ CUT-BEGIN .cutPoint
		, .c1
	)
	, lc-compile .do (
		AND ($$CUT .cutPoint .failLabel) FAIL
	) .env .c1/.c2/.d0/.d1
	, lc-compile .rem YES .env .c2/.c3/.d1/.dx
	, .c3 = (.failLabel LABEL
		, _ BIND-UNDO .pit
		, .cx
	)
#
lc-compile (EQ .a .b) .rem .pls/.vs .c0/.cx/.d0/.dx
	:- !
	, lc-bind .a .b .vs .c0/.c1/.c2/.cx
	, lc-compile .rem YES .pls/.vs .c1/.c2/.d0/.dx
#
lc-compile (.oper .a .b) .rem .pls/.vs .c0/.cx/.d0/.dx
	:- member (EQ, GE, GT, LE, LT, NE,) .oper
	, !
	, to.string .oper .os, concat "EVAL-" .os .is, to.atom .is .inst
	, lc-create-node .a .vs .c0/.c1/.reg0
	, lc-create-node .b .vs .c1/.c2/.reg1
	, .c2 = (_ .inst .resultReg .reg0 .reg1
		, _ IF-FALSE .failLabel .resultReg
		, .c3
	)
	, lc-compile .rem YES .pls/.vs .c3/.c4/.d0/.dx
	, .c4 = (.failLabel LABEL, .c5)
	, .c5 = .cx
#
lc-compile (DEFINE-RULES .rules .call) .rem .pls/.vs .c0/.cx/.d0/.dx
	:- !
	, lc-merge-rules .rules .groups
	, lc-prototype-labels .groups .pls/.pls1
	, lc-compile-rules .groups .pls1 .d1/.dx
	, !, lc-compile ($$SCOPE .call .pls1) .rem .pls/.vs .c0/.cx/.d0/.d1
#
lc-compile (CALL .call) .rem .pls/.vs .c0/.cx/.d0/.dx
	:- !
	, lc-call-prototype .call .proto
	, lc-create-node .call .vs .c0/.c1/.reg, (
		member .pls .proto/.callLabel, !
		, .c1 = (_ ASSIGN-CLOSURE .provenReg .provenLabel
			, _ PUSH .provenReg
			, _ PUSH .reg
			, _ CALL-CONSTANT .callLabel
			, _ POP _
			, _ POP _
			, .cx
		)
		, .d0 = (.provenLabel LABEL, .d1)
		, lc-compile .rem YES .pls/.vs .d1/.d2/.d3/.dx
		, .d2 = (_ RETURN, .d3)
		; .c1 = (_ PROVE-INTERPRET .reg .failLabel, .c2)
		, lc-compile .rem YES .pls/.vs .c2/.c3/.d0/.dx
		, .c3 = (.failLabel LABEL, .cx)
	)
#
lc-compile (SYSTEM-CALL .call) .rem .pls/.vs .c0/.cx/.d0/.dx
	:- !
	, lc-create-node .call .vs .c0/.c1/.reg
	, .c1 = (_ PROVE-SYS .reg .failLabel, .c2)
	, lc-compile .rem YES .pls/.vs .c2/.c3/.d0/.dx
	, .c3 = (.failLabel LABEL, .cx)
#

lc-merge-rules () _ #
lc-merge-rules (.rule, .remains) .groups
	:- .rule = RULE .nv .head .tail
	, lc-call-prototype .head .proto
	, member .groups .proto/.rules
	, member .rules .rule
	, !
	, lc-merge-rules .remains .groups
#

lc-bind (TREE .tree) .node .vs .cs :- !, lc-bind0 .node (TREE .tree) .vs .cs #
lc-bind .node0 .node1 .vs .cs :- lc-bind0 .node0 .node1 .vs .cs #

lc-bind0 .node .node .vs .c/.c/.f/.f :- not (.node = $$REG:_), ! #
lc-bind0 (TREE .oper .nl0 .nr0) (TREE .oper .nl1 .nr1) .vs .c0/.cx/.f0/.fx
	:- !
	, lc-bind .nl0 .nl1 .vs .c0/.c1/.f1/.fx
	, lc-bind .nr0 .nr1 .vs .c1/.cx/.f0/.f1
#
lc-bind0 .node0 .node1 .vs .c0/.cx/.f0/.fx
	:- lc-create-node .node0 .vs .c0/.c1/.reg0
	, lc-bind-register .reg0 .node1 .vs .c1/.cx/.f0/.fx
#

lc-bind-register .reg (TREE .oper .nl .nr) .vs .c0/.cx/.f0/.fx
	:- .c0 = (_ DECOMPOSE-TREE0 .reg .failLabel
		, _ DECOMPOSE-TREE1 .oper .reg0 .reg1
		, .c1
	)
	, lc-bind-register .reg0 .nl .vs .c1/.c2/.f1/.f2
	, lc-bind-register .reg1 .nr .vs .c2/.cx/.f0/.f1
	, .f2 = (.failLabel LABEL, .fx)
#
lc-bind-register .reg0 .node1 .vs .c0/.cx/.f0/.fx
	:- lc-create-node .node1 .vs .c0/.c1/.reg1
	, .c1 = (_ BIND .reg0 .reg1 .failLabel, .cx)
	, .f0 = (.failLabel LABEL, .fx)
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

lc-flatten-rules () FAIL :- ! #
lc-flatten-rules (RULE .nv .head .tail, .remains) (OR .head2 .tail1)
	:- .head1 = AND ($$BYTECODE _ TOP .reg -1) AND (EQ $$REG:.reg .head) .tail
	, lc-define-new-variables .head1 .nv .head2
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

lc-create-node $$REG:.reg .vs .c/.c/.reg :- ! #
lc-create-node (VAR .var) .vs .c/.c/.reg :- member .vs .var/.reg, ! #
lc-create-node WILDCARD .vs .c0/.cx/.reg
	:- .c0 = (_ NEW-NODE .reg, .cx)
#
lc-create-node (ATOM .a) _ .c0/.cx/.reg
	:- !, .c0 = (_ ASSIGN-CONSTANT .reg .a, .cx)
#
lc-create-node (NUMBER .i) _ .c0/.cx/.reg
	:- !, .c0 = (_ ASSIGN-INT .reg .i, .cx)
#
lc-create-node (STRING .s) _ .c0/.cx/.reg
	:- !, .c0 = (_ ASSIGN-CONSTANT .reg .s, .cx)
#
lc-create-node (TREE .operator .left .right) .vs .c0/.cx/.reg
	:- lc-create-node .left .vs .c0/.c1/.regl
	, lc-create-node .right .vs .c1/.c2/.regr
	, .c2 = (_ FORM-TREE0 .regl .regr
		, _ FORM-TREE1 .operator .reg
		, .cx
	)
#

lc-system-call-prototype (ATOM .systemPredicate) :- system.predicate .systemPredicate #

lc-is-variable .variable
	:- is.atom .variable, to.atom "." .dot, starts.with .variable .dot
#

lc-assign-line-number _ () #
lc-assign-line-number .n (.n _, .remains)
	:- let .n1 (.n + 1), lc-assign-line-number .n1 .remains
#

lc-dict-get .v .t :- rbt-get .v .t, ! #

-- use replace, necessary to redefine already-defined variables
lc-dict-add .v .t0/.t1 :- rbt-replace .v .t0/.t1, ! #

lc-dict-merge .t0 .t1 .t2 :- rbt-merge-bind .t0 .t1 .t2, ! #

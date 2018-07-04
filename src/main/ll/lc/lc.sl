-------------------------------------------------------------------------------
-- logical program compiler

() :- import.path "cg-generate-code.sl"
	, import.path "rbt.sl"
#

cg-is-opt-tail-calls #

compile-logic .call (FRAME l:.code,)
	:- .c0 = (ENTER
		, ASSIGN-CONSTANT .returnReg c:true
		, ASSIGN-THUNK .provenReg l:.c1
		, BIND-MARK .pitReg
		, PUSH .provenReg
		, PUSH .provenReg
		, CALL l:.cc
		, POP-ANY
		, POP-ANY
		, BIND-UNDO .pitReg
		, ASSIGN-CONSTANT .returnReg c:false
		, .c1)
	, .c1 = (SET-RESULT .returnReg
		, LEAVE
		, EXIT
		,)
	, lc-parse .call .call1 .nv
	, lc-define-new-variables .call1 .nv .call2
	, !, lc-compile-call .call2 () .cc/()
	, cg-optimize .c0 .code
#

lc-compile-call .call .pls (FRAME l:.code, .c)/.c
	:- .c0 = (ENTER
		, BACKUP-CSP .cspReg
		, BACKUP-DSP .dspReg
		, TOP .provenReg -2
		, .c1)
	, .rem = AND (BYTECODE CALL-THUNK .provenReg) FAIL
	, lc-compile .call .rem .pls/()/(.cspReg .dspReg .c2) .c1/.c2
	, .c2 = (LEAVE
		, RETURN
		,)
	, cg-optimize .c0 .code
#

lc-define-new-variables .parsed .nv .parsed1
	:- rbt-get-list .nv .nvs/()
	, .parsed1 = DEFINE-NEW-VARS .nvs .parsed
#

lc-parse .clause .clause2 .nv
	:- lc-parse-sugar .clause .clause1
	, !, lc-parse .clause1 .clause2 .nv
#
lc-parse (.rules ~ .clause) (DEFINE-RULES .rules1 .clause1) .nv
	:- !, lc-parse-rules .rules .rules1
	, lc-parse .clause .clause1 .nv
#
lc-parse is.compiled YES () :- ! #
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
	, lc-dict-meld .nv0 .nv1 .nv
#
lc-parse .tree (.oper1 .left1 .right1) .nv
	:- tree .tree .left .oper .right
	, member (
		' = '/EQ, ' != '/NE, ' < '/LT, ' <= '/LE,
	) .oper/.oper1
	, !
	, lc-parse-pattern .left .left1 .nv0
	, lc-parse-pattern .right .right1 .nv1
	, lc-dict-meld .nv0 .nv1 .nv
#
lc-parse .call .callx .nv
	:- lc-parse-pattern .call .call1 .nv
	, lc-call-prototype .call1 .proto
	, once (lc-system-call-prototype .proto
		, .callx = SYSTEM-CALL .call1
	; .callx = CALL .call1
	), !
#
lc-parse .d _ :- write.error "Unknown expression" .d, nl, fail #

lc-parse-sugar (.a != .b) .parsed :- .parsed = not (.a = .b) #

lc-parse-rules () () :- ! #
lc-parse-rules (.rule # .rules) (.rule1, .rules1)
	:- lc-parse-rule .rule .rule1, lc-parse-rules .rules .rules1
#

lc-parse-rule (.head :- .tail) (RULE .nv .head1 .tail1)
	:- !
	, lc-parse-pattern .head .head1 .nv0
	, lc-parse .tail .tail1 .nv1
	, lc-dict-meld .nv0 .nv1 .nv
#
lc-parse-rule .head .parsed :- lc-parse-rule (.head :- ()) .parsed #

lc-parse-pattern .tree (TREE .oper .left1 .right1) .nv
	:- tree .tree .left .oper .right, !
	, lc-parse-pattern .left .left1 .nv0
	, lc-parse-pattern .right .right1 .nv1
	, lc-dict-meld .nv0 .nv1 .nv
#
lc-parse-pattern .var (VAR .var) .nv
	:- lc-is-variable .var, !, lc-dict-add .var/_ ()/.nv
#
lc-parse-pattern .wildcard WILDCARD ()
	:- to.atom "_" .wildcard, !
#
lc-parse-pattern .a (ATOM .a) () :- is.atom .a #
lc-parse-pattern .i (NUMBER .i) () :- is.int .i #
lc-parse-pattern .s (STRING .s) () :- is.string .s #

lc-compile (BYTECODE .bytecode) .rem .env .c0/.cx
	:- .c0 = (.bytecode, .c1)
	, lc-compile .rem YES .env .c1/.cx
#
lc-compile (
	DEFINE-NEW-VARS (.var/.reg, .varRegs) .do
) .rem .pls/.vs/.cut .c0/.cx
	:- .c0 = (NEW-NODE .reg, .c1)
	, lc-compile (DEFINE-NEW-VARS .varRegs .do)
		.rem .pls/(.var/.reg, .vs)/.cut .c1/.cx
#
lc-compile (DEFINE-NEW-VARS () .do) .rem .env .c0/.cx
	:- lc-compile .do .rem .env .c0/.cx
#
lc-compile (AND .a .b) .rem .env .c0/.cx
	:- lc-compile .a (AND .b .rem) .env .c0/.cx
#
lc-compile (CALL .call) .rem .pls/.vs/.cut .c0/.cx
	:- lc-call-prototype .call .proto
	, lc-create-node .call .vs .c0/.c1/.reg, (
		member .pls .proto/.cl, !
		, .c1 = (ASSIGN-THUNK .provenReg l:.d0
			, PUSH .provenReg
			, PUSH .reg
			, CALL l:.cl
			, POP-ANY
			, POP-ANY
			, .cx)
		, lc-compile .rem YES .pls/.vs/.cut .d0/(RETURN,)
		; .c1 = (PROVE-INTERPRET .reg l:.cx, .c2)
		, lc-compile .rem YES .pls/.vs/.cut .c2/.cx
	)
#
lc-compile CUT .rem .pls/.vs/.cut .c0/.cx
	:- lc-compile (CUT .cut) .rem .pls/.vs/.cut .c0/.cx
#
lc-compile (CUT .cspReg .dspReg .fl) .rem .env .c0/_
	:- lc-compile .rem YES .env .c0/.c1
	, .c1 = (RESTORE-DSP .dspReg
		, RESTORE-CSP .cspReg
		, .fl)
#
lc-compile (DEFINE-RULES .rules .call) .rem .pls/.vs/.cut .c0/.cx
	:- lc-merge-rules .rules .groups
	, lc-prototype-labels .groups .pls/.pls1
	, lc-compile-rules .groups .pls1
	, !, lc-compile (SCOPE .call .pls1) .rem .pls/.vs/.cut .c0/.cx
#
lc-compile (EQ .a .b) .rem .pls/.vs/.cut .c0/.cx
	:- lc-bind .a .b .vs .c0/.c1/.cx
	, lc-compile .rem YES .pls/.vs/.cut .c1/.cx
#
lc-compile FAIL _ _ .c/.c #
lc-compile (ONCE .do) .rem .env .c0/.cx
	:- .c0 = (BACKUP-CSP .cspReg
		, BACKUP-DSP .dspReg
		, .c1)
	, lc-compile .do (AND (CUT .cspReg .dspReg .cx) .rem) .env .c1/.cx
#
lc-compile (OR FAIL .do) .ps :- !, lc-compile .do .ps #
lc-compile (OR .do FAIL) .ps :- !, lc-compile .do .ps #
lc-compile (OR .a .b) .rem .pls/.vs/.cut .c0/.cx
	:- .c0 = (BIND-MARK .pitReg, .c1)
	, lc-compile (AND .a (BYTECODE CALL l:.d0)) FAIL .pls/.vs/.cut .c1/.c2
	, .c2 = (BIND-UNDO .pitReg, .c3)
	, lc-compile (AND .b (BYTECODE CALL l:.d0)) FAIL .pls/.vs/.cut .c3/.cx
	, lc-compile .rem YES .pls/.vs/.cut .d0/(RETURN,)
#
lc-compile (NOT .do) .rem .env .c0/.cx
	:- .c0 = (BIND-MARK .pit
		, BACKUP-CSP .cspReg
		, BACKUP-DSP .dspReg
		, .c1)
	, lc-compile .do (AND (CUT .cspReg .dspReg .c3) FAIL) .env .c1/.c2
	, lc-compile .rem YES .env .c2/.c3
	, .c3 = (BIND-UNDO .pit, .cx)
#
lc-compile (SCOPE .call .pls1) .rem .pls/.vs/.cut .c0/.cx
	:- once (.rem/.rem1 = YES/YES; .rem1 = SCOPE .rem .pls)
	, lc-compile .call .rem1 .pls1/.vs/.cut .c0/.cx
#
lc-compile (SYSTEM-CALL .call) .rem .pls/.vs/.cut .c0/.cx
	:- lc-create-node .call .vs .c0/.c1/.reg
	, .c1 = (PROVE-SYS .reg l:.cx, .c2)
	, lc-compile .rem YES .pls/.vs/.cut .c2/.cx
#
lc-compile YES .rem .env .c0/.cx
	:- lc-compile .rem YES .env .c0/.cx
#
lc-compile (.oper .a .b) .rem .pls/.vs/.cut .c0/.cx
	:- member (LE, LT, NE,) .oper
	, !
	, to.string .oper .os, concat "EVAL-" .os .is, to.atom .is .inst
	, lc-create-node .a .vs .c0/.c1/.reg0
	, lc-create-node .b .vs .c1/.c2/.reg1
	, .c2 = (.inst .resultReg .reg0 .reg1
		, IF-FALSE .resultReg l:.cx
		, .c3)
	, lc-compile .rem YES .pls/.vs/.cut .c3/.cx
#

lc-merge-rules () _ #
lc-merge-rules (.rule, .remains) .groups
	:- .rule = RULE _ .head _
	, lc-call-prototype .head .proto
	, member .groups .proto/.rules
	, member .rules .rule
	, !
	, lc-merge-rules .remains .groups
#

lc-bind (TREE .tree) .node .vs .cs :- !, lc-bind0 .node (TREE .tree) .vs .cs #
lc-bind .node0 .node1 .vs .cs :- lc-bind0 .node0 .node1 .vs .cs #

lc-bind0 .node .node _ .c/.c/_ :- not (.node = $$REG:_), ! #
lc-bind0 (TREE .oper .nl0 .nr0) (TREE .oper .nl1 .nr1) .vs .c0/.cx/.f
	:- !
	, lc-bind .nl0 .nl1 .vs .c0/.c1/.f
	, lc-bind .nr0 .nr1 .vs .c1/.cx/.f
#
lc-bind0 .node0 .node1 .vs .c0/.cx/.f
	:- lc-create-node .node0 .vs .c0/.c1/.reg0
	, lc-bind-register .reg0 .node1 .vs .c1/.cx/.f
#

lc-bind-register .reg (TREE .oper .nl .nr) .vs .c0/.cx/.f
	:- .c0 = (DECOMPOSE-TREE0 .reg l:.f
		, DECOMPOSE-TREE1 c:.oper .reg0 .reg1
		, .c1)
	, lc-bind-register .reg0 .nl .vs .c1/.c2/.f
	, lc-bind-register .reg1 .nr .vs .c2/.cx/.f
#
lc-bind-register .reg0 .node1 .vs .c0/.cx/.f
	:- lc-create-node .node1 .vs .c0/.c1/.reg1
	, .c1 = (BIND .reg0 .reg1 l:.f, .cx)
#

lc-compile-rules () _ :- ! #
lc-compile-rules (.proto/.rules, .remains) .pls
	:- lc-flatten-rules .rules .call .headReg
	, member .pls .proto/.c
	, .remark = REMARK r:('-----' .proto '-----')
	, .call1 = AND (BYTECODE .remark) AND (BYTECODE TOP .headReg -1) .call
	, lc-compile-call .call1 .pls .c/()
	, lc-compile-rules .remains .pls
#

lc-flatten-rules () FAIL _ :- ! #
lc-flatten-rules (RULE .nv .head .tail, .remains) (OR .head2 .tail1) .headReg
	:- .head1 = AND (EQ $$REG:.headReg .head) .tail
	, lc-define-new-variables .head1 .nv .head2
	, lc-flatten-rules .remains .tail1 .headReg
#

lc-call-prototype (TREE _ .left _) .name :- lc-call-prototype .left .name #
lc-call-prototype .name .name #

lc-params-length .ps .n
	:- if (bound .ps, .ps = _ .ps1) (
		lc-params-length .ps1 .n1, sum .n 1 .n1
	) if (1 < .n, .ps = _ .ps1) (
		sum .n 1 .n1, lc-params-length .ps1 .n1
	) (
		(not bound .ps; .ps != _ _), .n = 1
	)
#

lc-prototype-labels () .pls/.pls :- ! #
lc-prototype-labels (.proto/_, .tail) .pls/(.proto/_, .pls1)
	:- lc-prototype-labels .tail .pls/.pls1
#

lc-create-node $$REG:.reg _ .c/.c/.reg :- ! #
lc-create-node (VAR .var) .vs .c/.c/.reg :- member .vs .var/.reg, ! #
lc-create-node WILDCARD _ .c0/.cx/.reg
	:- .c0 = (NEW-NODE .reg, .cx)
#
lc-create-node (ATOM .a) _ .c0/.cx/.reg
	:- !, .c0 = (ASSIGN-CONSTANT .reg c:.a, .cx)
#
lc-create-node (NUMBER .i) _ .c0/.cx/.reg
	:- !, .c0 = (ASSIGN-INT .reg .i, .cx)
#
lc-create-node (STRING .s) _ .c0/.cx/.reg
	:- !, .c0 = (ASSIGN-CONSTANT .reg c:.s, .cx)
#
lc-create-node (TREE .operator .left .right) .vs .c0/.cx/.reg
	:- lc-create-node .left .vs .c0/.c1/.regl
	, lc-create-node .right .vs .c1/.c2/.regr
	, .c2 = (FORM-TREE0 .regl .regr
		, FORM-TREE1 c:.operator .reg
		, .cx)
#

lc-system-call-prototype .proto
	:- .proto = ATOM .systemPredicate
	, system.predicate .systemPredicate
#

lc-is-variable .variable
	:- is.atom .variable, to.atom "." .dot, starts.with .variable .dot
#

lc-dict-get .v .t :- rbt-get .v .t, ! #

-- use replace, necessary to redefine already-defined variables
lc-dict-add .v .t0/.t1 :- rbt-replace .v .t0/.t1, ! #

lc-dict-meld .t0 .t1 .t2 :- rbt-union-bind .t0 .t1 .t2, ! #

-------------------------------------------------------------------------------
-- functional program compiler

() :- import 'fc-type.sl' #

compile-function .do .c0
	:- .c0 = (_ ENTER, .c1)
	, fc-add-standard-funs .do .do1
	, fc-parse .do1 .parsed
	, infer-type .parsed ()/() _
	, fc-compile .parsed 0 .c1/.c2/.d0/()/.reg
	, .c2 = (_ EXIT .reg, .d0)
	, fc-assign-line-number 0 .c0
#

fc-parse (.var => .do) (FUN .var .do1) :- !, fc-parse .do .do1 #
fc-parse (.var as .type => .do) (FUN .var .do2)
	:- !, fc-parse-type .type .type1, .do1 = (VAR-TYPE .var .type1 .do2)
	, fc-parse .do1 .do2
#
fc-parse (define type .type = .def >> .do) (DEF-TYPE .type .def1 .do1)
	:- !, fc-parse-type .def .def1, fc-parse .do .do1
#
fc-parse (define .var = .value >> .do) (DEF-VAR .var .value1 .do1)
	:- !, fc-parse .value .value1, fc-parse .do .do1
#
fc-parse (define .var as .type = .value >> .do) (DEF-VAR .var .value1 .do1)
	:- !, fc-parse-type .type .type1, .do1 = (VAR-TYPE .var .type1 .do2)
	, fc-parse .value .value1, fc-parse .do .do2
#
fc-parse (.callee {.parameter}) (INVOKE .parameter1 .callee1)
	:- !, fc-parse .callee .callee1
	, fc-parse .parameter .parameter1
#
fc-parse .ifThenElse (IF .if1 .then1 .else1)
	:- member ((if .if then .then else .else), (.if ? .then | .else),) .ifThenElse, !
	, fc-parse .if .if1
	, fc-parse .then .then1
	, fc-parse .else .else1
#
fc-parse .left/.right .parsed
	:- !, fc-parse (corecursive-cons {.left} {.right}) .parsed
#
fc-parse (.left, .right) .parsed
	:- !, fc-parse (cons {.left} {.right}) .parsed
#
fc-parse (.name .elems) (TUPLE .name .elems2)
	:- !, en-list .elems .elems1, fc-parse .elems1 .elems2
#
fc-parse .tree (TREE .oper .left1 .right1)
	:- tree .tree .left .oper .right
	, member (' + ', ' - ', ' * ', ' / ', ' %% ',
		' = ', ' != ',
		' > ', ' < ', ' >= ', ' <= ',
	) .oper
	, !, fc-parse .left .left1
	, fc-parse .right .right1
#
fc-parse .b (BOOLEAN .b) :- fc-is-boolean .b, ! #
fc-parse .i (NUMBER .i) :- is.int .i, ! #
fc-parse .s (STRING .s) :- is.string .s, ! #
fc-parse () EMPTY :- ! #
fc-parse .v (VARIABLE .v) :- is.atom .v, ! #
fc-parse .d _ _ :- write "Unknown expression" .d, nl, fail #

fc-parse-type (.paramType => .returnType) (FUN .paramType1 .returnType1)
	:- !, fc-parse-type .paramType .paramType1
	, fc-parse-type .returnType .returnType1
#
fc-parse-type (one of .types) (ONE-OF .types1)
	:- !, fc-parse-types .types .types1
#
fc-parse-type (list of .type) (LIST-OF .type1) :- !, fc-parse-type .type .type1 #
fc-parse-type (.name .types) (TUPLE-OF .name .types2)
	:- !, enlist .types .types1
	, fc-parse-types .types1 .types2
#
fc-parse-type boolean BOOLEAN :- ! #
fc-parse-type number NUMBER :- ! #
fc-parse-type string STRING :- ! #

fc-parse-types () () :- ! #
fc-parse-types (.type, .types) (.type1, .types1)
	:- fc-parse-type .type1 .type1, fc-parse-types .types .types1
#

fc-compile (VAR-TYPE _ _ .do) .frame .cdr
	:- !, fc-compile .do .frame .cdr
#
fc-compile (FUN .var .do) .frame .c0/.cx/.d0/.dx/.reg
	:- !
	, let .frame1 (.frame + 1)
	, .c0 = (_ ASSIGN-CLOSURE .reg .funcLabel, .cx)
	, .d0 = (.funcLabel ENTER, .d1)
	, .d1 = (_ POP .varReg, .d2)
	, replace .do/.do1 (VARIABLE .var)/(%REG/.varReg/.frame1)
	, fc-compile .do1 .frame1 .d2/.d3/.d4/.dx/.returnReg
	, .d3 = (_ RETURN-VALUE .returnReg, _ LEAVE, .d4)
#
fc-compile (DEF-VAR .var .value .do) .frame .c0/.cx/.d0/.dx/.reg
	:- !
	, replace .value/.value1 (VARIABLE .var)/(%REG/.r1/.frame) -- Allows recursion
	, replace .do/.do1 (VARIABLE .var)/(%REG/.r1/.frame)
	, fc-compile .value1 .frame .c0/.c1/.d0/.d1/.r1
	, fc-compile .do1 .frame .c1/.cx/.d1/.dx/.reg
#
fc-compile .call .frame .cdr
	:- fc-default-fun .call .frame .cdr, !
#
fc-compile (INVOKE .parameter .callee) .frame .c0/.cx/.d0/.dx/.reg
	:- !
	, fc-compile .callee .frame .c0/.c1/.d0/.d1/.r1
	, fc-compile .parameter .frame .c1/.c2/.d1/.dx/.r2
	, .c2 = (_ PUSH .r2, _ CALL-CLOSURE .reg .r1, .cx)
#
fc-compile (IF .if .then .else) .frame .c0/.cx/.d0/.dx/.reg
	:- !
	, fc-compile .if .frame .c0/.c1/.d0/.d1/.cr
	, .c1 = (_ IF-FALSE .label1 .cr, .c2)
	, fc-compile .then .frame .c2/.c3/.d1/.d2/.reg
	, .c3 = (_ JUMP .label2, .label1 LABEL .label1, .c4)
	, fc-compile .else .frame .c4/.c5/.d2/.dx/.reg
	, .c5 = (.label2 LABEL .label2, .cx)
#
fc-compile (TREE .oper .left .right) .frame .c0/.cx/.d0/.dx/.reg
	:- !
	, fc-compile .left .frame .c0/.c1/.d0/.d1/.r1
	, fc-compile .right .frame .c1/.c2/.d1/.dx/.r2
	, .c2 = (_ EVALUATE .reg .r1 .oper .r2, .cx)
#
fc-compile %REG/.reg/.frame .frame .c/.c/.d/.d/.reg :- ! #
fc-compile %REG/.reg/.frame0 .frame .c0/.cx/.d/.d/.reg1
	:- !, let .frameDifference (.frame0 - .frame)
	, .c0 = (_ ASSIGN-FRAME-REG .reg1 .frameDifference .reg, .cx)
#
fc-compile (BOOLEAN .b) _ .c0/.cx/.d/.d/.reg :- !, .c0 = (_ ASSIGN-BOOL .reg .b, .cx) #
fc-compile (NUMBER .i) _ .c0/.cx/.d/.d/.reg :- !, .c0 = (_ ASSIGN-INT .reg .i, .cx) #
fc-compile (STRING .s) _ .c0/.cx/.d/.d/.reg :- !, .c0 = (_ ASSIGN-STR .reg .s, .cx) #
fc-compile EMPTY _ .c0/.cx/.d/.d/.reg :- !, .c0 = (_ ASSIGN-CONSTANT .reg (), .cx) #
fc-compile .d _ _ :- write "Unknown expression" .d, nl, fail #

fc-default-fun .call .frame .result
	:- fc-default-fun0 .call .frame .result 0
#

fc-default-fun0 (INVOKE .p .pred) .frame .c0/.cx/.d0/.dx/.reg .n
	:- !, let .n1 (.n + 1)
	, fc-compile .p .frame .c0/.c1/.d0/.d1/.r1
	, .c1 = (_ PUSH .r1, .c2)
	, fc-default-fun0 .pred .frame .c2/.cx/.d1/.dx/.reg .n1
#
fc-default-fun0 (VARIABLE .pred) _ .c0/.cx/.d/.d/.reg .n
	:- fc-define-default-fun .n .pred .call, !
	, .c0 = (_ SYS .call .reg .n, .cx)
#

-- "cons" and "corecursive-cons" differ only by type
fc-define-default-fun 2 cons CONS #
fc-define-default-fun 2 corecursive-cons CONS #
fc-define-default-fun 0 () EMPTY #
fc-define-default-fun 1 is-tree IS-TREE #
fc-define-default-fun 1 head HEAD #
fc-define-default-fun 2 log LOG #
fc-define-default-fun 1 tail TAIL #

fc-is-boolean true #
fc-is-boolean false #

fc-assign-line-number _ () #
fc-assign-line-number .n (.n _, .remains)
	:- let .n1 (.n + 1), fc-assign-line-number .n1 .remains
#

fc-add-standard-funs .p (
	define and = (x => y =>
		x ? y | false
	) >>
	define if-tree = (list => f1 => f2 =>
	    if (is-tree {list}) then (
	        f1 {head {list}} {tail {list}}
	    )
	    else f2
	) >>
	define join = (f => g => x =>
		g {f {x}}
	) >>
	define or = (x => y =>
		x ? true | y
	) >>
	define concat = (l1 => l2 =>
	    if-tree {l1} {h => t => h, concat {t} {l2}} {l2}
	) >>
	define fold = (fun => list =>
	    define h = head {list} >>
	    define t = tail {list} >>
	    is-tree {t} ? fun {h} {fold {fun} {t}} | h
	) >>
	define fold-left = (fun => init => list =>
	    if-tree {list}
	        {h => t => fold-left {fun} {fun {init} {h}} {t}}
	        {init}
	) >>
	define fold-right = (fun => init => list =>
	    if-tree {list}
	        {h => t => fun {h} {fold-right {fun} {init} {t}}}
	        {init}
	) >>
	define split = (fun => list =>
		if-tree {list} {fun} {}
	) >>
	define zip = (fun => l0 => l1 =>
	    if-tree {l0} {h0 => t0 =>
	        if-tree {l1} {h1 => t1 =>
	            fun {h0} {h1}, zip {fun} {t0} {t1}
	        } {}
	    } {}
	) >>
	define apply =
		fold-left {x => f => f {x}}
	>>
	define concat-list =
		fold-left {concat} {}
	>>
	define filter = (fun =>
	    fold-right {
	        item => list => fun {item} ? item, list | list
	    } {}
	) >>
	define map = (
		fun => fold-right {i => list => fun {i}, list} {}
	) >>
	define contains = (
		e => join {map {e1 => e1 = e}} {fold {or}}
	) >>
	.p
) #

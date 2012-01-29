-------------------------------------------------------------------------------
-- functional program compiler

() :- import 'fc-type.sl' #

compile-function .do .c0
	:- .c0 = (_ ENTER, .c1)
	, parse-fc .do .parsed
	, infer-type .parsed () _
	, compile-fc .parsed 0 .c1/.c2/.d0/()/.reg
	, .c2 = (_ EXIT .reg, .d0)
	, assign-line-number-fc 0 .c0
#

parse-fc (.varDef => .do) (FUNC .variable .do2)
	:- !, define-variable .varDef .variable .do .do1
	, parse-fc .do1 .do2
#
parse-fc (.varDef = .value >> .do) (SUBST .variable .value1 .do2)
	:- !, define-variable .varDef .variable .do .do1
	, parse-fc .value .value1, parse-fc .do1 .do2
#
parse-fc (.callee {.parameter}) (INVOKE .parameter1 .callee1)
	:- !, parse-fc .callee .callee1
	, parse-fc .parameter .parameter1
#
parse-fc .ifThenElse (IF .if1 .then1 .else1)
	:- member ((if .if then .then else .else), (.if ? .then | .else),) .ifThenElse, !
	, parse-fc .if .if1
	, parse-fc .then .then1
	, parse-fc .else .else1
#
parse-fc .left:.right .parsed :- !, parse-fc (cons {.left} {.right}) .parsed #
parse-fc .tree (TREE .oper .left1 .right1)
	:- tree .tree .left .oper .right
	, member (' + ', ' - ', ' * ', ' / ', ' %% ',
		' = ', ' != ',
		' > ', ' < ', ' >= ', ' <= ',
	) .oper
	, !, parse-fc .left .left1, parse-fc .right .right1
#
parse-fc .b (BOOLEAN .b) :- is.boolean .b, ! #
parse-fc .i (NUMBER .i) :- is.int .i, ! #
parse-fc .s (STRING .s) :- is.string .s, ! #
parse-fc () EMPTY :- ! #
parse-fc .v (VARIABLE .v) :- is.atom .v, ! #
parse-fc .d _ _ :- write "Unknown expression" .d, nl, fail #

define-variable .variable/.type .variable .do (VAR-TYPE .variable .type .do) :- ! #
define-variable .variable .variable .do .do #

compile-fc (VAR-TYPE _ _ .do) .frame .cdr
	:- !, compile-fc .do .frame .cdr
#
compile-fc (FUNC .variable .do) .frame .c0/.cx/.d0/.dx/.reg
	:- !
	, let .frame1 (.frame + 1)
	, .c0 = (_ ASSIGN-CLOSURE .reg .funcLabel, .cx)
	, .d0 = (.funcLabel ENTER, .d1)
	, .d1 = (_ POP .variableReg, .d2)
	, replace .do/.do1 (VARIABLE .variable)/(%REG/.variableReg/.frame1)
	, compile-fc .do1 .frame1 .d2/.d3/.d4/.dx/.returnReg
	, .d3 = (_ RETURN-VALUE .returnReg, _ LEAVE, .d4)
#
compile-fc (SUBST .variable .value .do) .frame .c0/.cx/.d0/.dx/.reg
	:- !
	, replace .value/.value1 (VARIABLE .variable)/(%REG/.r1/.frame) -- Allows recursion
	, replace .do/.do1 (VARIABLE .variable)/(%REG/.r1/.frame)
	, compile-fc .value1 .frame .c0/.c1/.d0/.d1/.r1
	, compile-fc .do1 .frame .c1/.cx/.d1/.dx/.reg
#
compile-fc .call .frame .cdr
	:- fc-system-predicate .call .frame .cdr, !
#
compile-fc (INVOKE .parameter .callee) .frame .c0/.cx/.d0/.dx/.reg
	:- !
	, compile-fc .callee .frame .c0/.c1/.d0/.d1/.r1
	, compile-fc .parameter .frame .c1/.c2/.d1/.dx/.r2
	, .c2 = (_ PUSH .r2, _ CALL-CLOSURE .reg .r1, .cx)
#
compile-fc (IF .if .then .else) .frame .c0/.cx/.d0/.dx/.reg
	:- !
	, compile-fc .if .frame .c0/.c1/.d0/.d1/.cr
	, .c1 = (_ IF-FALSE .label1 .cr, .c2)
	, compile-fc .then .frame .c2/.c3/.d1/.d2/.reg
	, .c3 = (_ JUMP .label2, .label1 LABEL .label1, .c4)
	, compile-fc .else .frame .c4/.c5/.d2/.dx/.reg
	, .c5 = (.label2 LABEL .label2, .cx)
#
compile-fc (TREE .oper .left .right) .frame .c0/.cx/.d0/.dx/.reg
	:- !
	, compile-fc .left .frame .c0/.c1/.d0/.d1/.r1
	, compile-fc .right .frame .c1/.c2/.d1/.dx/.r2
	, .c2 = (_ EVALUATE .reg .r1 .oper .r2, .cx)
#
compile-fc %REG/.reg/.frame .frame .c/.c/.d/.d/.reg :- ! #
compile-fc %REG/.reg/.frame0 .frame .c0/.cx/.d/.d/.reg1
	:- !, let .frameDifference (.frame0 - .frame)
	, .c0 = (_ ASSIGN-FRAME-REG .reg1 .frameDifference .reg, .cx)
#
compile-fc (BOOLEAN .b) _ .c0/.cx/.d/.d/.reg :- !, .c0 = (_ ASSIGN-BOOL .reg .b, .cx) #
compile-fc (NUMBER .i) _ .c0/.cx/.d/.d/.reg :- !, .c0 = (_ ASSIGN-INT .reg .i, .cx) #
compile-fc (STRING .s) _ .c0/.cx/.d/.d/.reg :- !, .c0 = (_ ASSIGN-STR .reg .s, .cx) #
compile-fc EMPTY _ .c0/.cx/.d/.d/.reg :- !, .c0 = (_ ASSIGN-CONSTANT .reg (), .cx) #
compile-fc .d _ _ :- write "Unknown expression" .d, nl, fail #

fc-system-predicate .call .frame .result
	:- fc-system-predicate0 .call .frame .result 0
#

fc-system-predicate0 (INVOKE .p .pred) .frame .c0/.cx/.d0/.dx/.reg .n
	:- !, let .n1 (.n + 1)
	, compile-fc .p .frame .c0/.c1/.d0/.d1/.r1
	, .c1 = (_ PUSH .r1, .c2)
	, fc-system-predicate0 .pred .frame .c2/.cx/.d1/.dx/.reg .n1
#
fc-system-predicate0 (VARIABLE .pred) _ .c0/.cx/.d/.d/.reg .n
	:- fc-define-system-predicate .n .pred .call, !
	, .c0 = (_ SYS .call .reg .n, .cx)
#

fc-define-system-predicate 2 cons CONS #
fc-define-system-predicate 0 () EMPTY #
fc-define-system-predicate 1 is-tree IS-TREE #
fc-define-system-predicate 1 head HEAD #
fc-define-system-predicate 2 log LOG #
fc-define-system-predicate 1 tail TAIL #

is.boolean true #
is.boolean false #

assign-line-number-fc _ () #
assign-line-number-fc .n (.n _, .remains)
	:- let .n1 (.n + 1), assign-line-number-fc .n1 .remains
#

-------------------------------------------------------------------------------
-- functional program compiler

compile .do .c0/.reg
	:- .c0 = (_ ENTER, .c1)
	, fc-compile .do 0 .c1/.c2/.d0/()/.reg
	, .c2 = (_ EXIT .reg, .d0)
	, assign-line-number 0 .c0
#

fc-compile (.variable => .do) .frame .c0/.cx/.d0/.dx/.reg
	:- !
	, let .frame1 (.frame + 1)
	, .c0 = (_ ASSIGN-CLOSURE .reg .funcLabel, .cx)
	, .d0 = (.funcLabel ENTER, .d1)
	, .d1 = (_ POP .variableReg, .d2)
	, replace .do .do1 .variable %REG/.variableReg/.frame1
	, fc-compile .do1 .frame1 .d2/.d3/.d4/.dx/.returnReg
	, .d3 = (_ RETURN .returnReg, .d4)
#
fc-compile (.variable = .value >> .do) .frame .c0/.cx/.d0/.dx/.reg
	:- !
	, replace .value .value1 .variable %REG/.r1/.frame -- Allows recursion
	, replace .do .do1 .variable %REG/.r1/.frame
	, fc-compile .value1 .frame .c0/.c1/.d0/.d1/.r1
	, fc-compile .do1 .frame .c1/.cx/.d1/.dx/.reg
#
fc-compile .call .frame .c0/.cx/.d0/.dx/.reg :-
	fc-system-predicate .call .frame .c0/.cx/.d0/.dx/.reg, !
#
fc-compile (.callee {.parameter}) .frame .c0/.cx/.d0/.dx/.reg
	:- !
	, fc-compile .callee .frame .c0/.c1/.d0/.d1/.r1
	, fc-compile .parameter .frame .c1/.c2/.d1/.dx/.r2
	, .c2 = (_ PUSH .r2, _ CALL-CLOSURE .reg .r1, .cx)
#
fc-compile (.if ? .then | .else) .frame .c0/.cx/.d0/.dx/.reg
	:- !
	, fc-compile .if .frame .c0/.c1/.d0/.d1/.cr
	, .c1 = (_ IF-FALSE .label1 .cr, .c2)
	, fc-compile .then .frame .c2/.c3/.d1/.d2/.reg
	, .c3 = (_ JUMP .label2, .label1 LABEL .label1, .c4)
	, fc-compile .else .frame .c4/.c5/.d2/.dx/.reg
	, .c5 = (.label2 LABEL .label2, .cx)
#
fc-compile .tree .frame .c0/.cx/.d0/.dx/.reg
	:- tree .tree .left .oper .right
	, member (' + ', ' - ', ' * ', ' / ',
		' = ', ' != ',
		' > ', ' < ', ' >= ', ' <= ',
	) .oper
	, !
	, fc-compile .left .frame .c0/.c1/.d0/.d1/.r1
	, fc-compile .right .frame .c1/.c2/.d1/.dx/.r2
	, .c2 = (_ EVALUATE .reg .r1 .oper .r2, .cx)
#
fc-compile %REG/.reg/.frame .frame .c/.c/.d/.d/.reg :- ! #
fc-compile %REG/.reg/.frame0 .frame .c0/.cx/.d/.d/.reg1
	:- !, let .frameDifference (.frame0 - .frame)
	, .c0 = (_ ASSIGN-FRAME-REG .reg1 .frameDifference .reg, .cx)
#
fc-compile .i _ .c0/.cx/.d/.d/.reg :- is.int .i, !, .c0 = (_ ASSIGN-INT .reg .i, .cx) #
fc-compile .s _ .c0/.cx/.d/.d/.reg :- is.string .s, !, .c0 = (_ ASSIGN-STR .reg .s, .cx) #
fc-compile .b _ .c0/.cx/.d/.d/.reg :- is.boolean .b, !, .c0 = (_ ASSIGN-BOOL .reg .b, .cx) #
fc-compile .d _ _ :- write "Unknown expression" .d, nl, fail #

fc-system-predicate .head:.tail .frame .result
	:- !, fc-system-predicate (cons {.head} {.tail}) .frame .result
#
fc-system-predicate .call .frame .result
	:- fc-system-predicate0 .call .frame .result 0
#

fc-system-predicate0 (.pred {.p}) .frame .c0/.cx/.d0/.dx/.reg .n
	:- !, let .n1 (.n + 1)
	, fc-compile .p .frame .c0/.c1/.d0/.d1/.r1
	, .c1 = (_ PUSH .r1, .c2)
	, fc-system-predicate0 .pred .frame .c2/.cx/.d1/.dx/.reg .n1
#
fc-system-predicate0 .pred _ .c0/.cx/.d/.d/.reg .n
	:- fc-define-system-predicate .n .pred .call, !
	, .c0 = (_ SYS .call .reg .n, .cx)
#

fc-define-system-predicate 2 cons CONS #
fc-define-system-predicate 0 () EMPTY #
fc-define-system-predicate 1 head HEAD #
fc-define-system-predicate 1 tail TAIL #

is.boolean true #
is.boolean false #

assign-line-number _ () #
assign-line-number .n (.n _, .remains) :- let .n1 (.n + 1), assign-line-number .n1 .remains #

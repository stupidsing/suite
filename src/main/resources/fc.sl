-------------------------------------------------------------------------------
-- functional program compiler

compile .do .c0/.reg
	:- .c0 = (_ ENTER, .c1)
	, fc-compile .do 0 .c1/.c2/.reg
	, .c2 = (_ EXIT .reg, ())
	, assign-line-number 0 .c0
#

fc-compile (.variable => .do) .frame .c0/.cx/.reg
	:- !
	, let .frame1 (.frame + 1)
	, .c0 = (_ JUMP .skipLabel, .funcLabel ENTER, .c1)
	, .c1 = (_ POP .variableReg, .c2)
	, replace .do .do1 .variable %REG/.variableReg/.frame1
	, fc-compile .do1 .frame1 .c2/.c3/.returnReg
	, .c3 = (_ RETURN .returnReg, .c4)
	, .c4 = (.skipLabel LABEL .skipLabel, _ ASSIGN-CLOSURE .reg .funcLabel, .cx)
#

fc-compile (.variable = .value >> .do) .frame .c0/.cx/.reg
	:- !
	, fc-compile .value .frame .c0/.c1/.r1
	, replace .do .do1 .variable %REG/.r1/.frame
	, fc-compile .do1 .frame .c1/.cx/.reg
#

fc-compile (.callee {.parameter}) .frame .c0/.cx/.reg
	:- !
	, fc-compile .callee .frame .c0/.c1/.r1
	, fc-compile .parameter .frame .c1/.c2/.r2
	, .c2 = (_ PUSH .r2, _ CALL-CLOSURE .reg .r1, .cx)
#

fc-compile (.if ? .then | .else) .frame .c0/.cx/.reg
	:- !
	, fc-compile .if .frame .c0/.c1/.cr
	, .c1 = (_ IF-FALSE .label1 .cr, .c2)
	, fc-compile .then .frame .c2/.c3/.reg
	, .c3 = (_ JUMP .label2, .label1 LABEL .label1, .c4)
	, fc-compile .else .frame .c4/.c5/.reg
	, .c5 = (.label2 LABEL .label2, .cx)
#

fc-compile .tree .frame .c0/.cx/.reg
	:- tree .tree .left .oper .right
	, (
		.oper = ' + '; .oper = ' - '; .oper = ' * '; .oper = ' / '
		; .oper = ' = '; .oper = ' != '
		; .oper = ' > '; .oper = ' < '; .oper = ' >= '; .oper = ' <= '
	), !
	, fc-compile .left .frame .c0/.c1/.r1
	, fc-compile .right .frame .c1/.c2/.r2
	, .c2 = (_ EVALUATE .reg .r1 .oper .r2, .cx)
#

fc-compile %REG/.reg/.frame .frame .c/.c/.reg :- ! #
fc-compile %REG/.reg/.frame0 .frame .c0/.cx/.reg1
	:- !, let .frameDifference (.frame0 - .frame)
	, .c0 = (_ ASSIGN-FRAME-REG .reg1 .frameDifference .reg, .cx)
#

fc-compile .i _ .c0/.cx/.reg :- is.int .i, !, .c0 = (_ ASSIGN-INT .reg .i, .cx) #
fc-compile .s _ .c0/.cx/.reg :- is.string .s, !, .c0 = (_ ASSIGN-STR .reg .s, .cx) #
fc-compile .b _ .c0/.cx/.reg :- is.boolean .b, !, .c0 = (_ ASSIGN-BOOL .reg .b, .cx) #

fc-compile .d _ _ :- write "Unknown expression" .d, nl, fail #

is.boolean true #
is.boolean false #

assign-line-number _ () #
assign-line-number .n (.n _, .remains) :- let .n1 (.n + 1), assign-line-number .n1 .remains #

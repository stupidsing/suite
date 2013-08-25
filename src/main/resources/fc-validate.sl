fc-validate (ATOM .a) (ATOM .a)
#
fc-validate (BOOLEAN .b) (BOOLEAN .b)
#
fc-validate (CONSTANT .c) (CONSTANT .c)
#
fc-validate (DEF-VAR .var .value .do0) (DEF-VAR .var .value .do1)
	:- fc-validate .do0 .do1
#
fc-validate (FUN .var .do0) (FUN .var .do1)
	:- fc-validate .do0 .do1
#
fc-validate (IF .if0 .then0 .else0) (IF .if1 .then1 .else1)
	:- fc-validate .if0 .if1
	, fc-validate .then0 .then1
	, fc-validate .else0 .else1
#
fc-validate (INVOKE .param0 .callee0) (INVOKE .param1 .callee1)
	:- fc-validate .param0 .param1
	, fc-validate .callee0 .callee1
#
fc-validate (NEW-VAR .var) (NEW-VAR .var)
#
fc-validate (NUMBER .i) (NUMBER .i)
#
fc-validate (OPTION _ .do0) (OPTION _ .do1)
	:- fc-validate .do0 .do1
#
fc-validate (PAIR .left0 .right0) (PAIR .left1 .right1)
	:- fc-validate .left0 .left1
	, fc-validate .right0 .right1
#
fc-validate (STRING .s) (STRING .s)
#
fc-validate (TREE .oper .left0 .right0) (TREE .oper .left1 .right1)
	:- fc-validate .left0 .left1
	, fc-validate .right0 .right1
#
fc-validate (USING _ .do0) (USING _ .do1)
	:- fc-validate .do0 .do1
#
fc-validate (VAR .var) (VAR .var)
#

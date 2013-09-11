-------------------------------------------------------------------------------
-- lazy functional program interpreter

fl (define .var = .value >> .do) .result
	:- !
	, replace .value .value1 .var .value1
	, replace .do .do1 .var .value1
	, fl .do1 .result
#
fl (head {.pair}) .result :- !, fl .pair (.result, _) #
fl (tail {.pair}) .result :- !, fl .pair (_, .result) #
fl (.fun {$}) .result :- !, fl .fun ($ => .result) #
fl (.fun {.value}) .result
	:- !
	, fl .fun (.var => .do)
	, replace .do .do1 .var .value
	, fl .do1 .result
#
fl (if .if then .then else .else) .result
	:- fl (.if {$}) true
	, !, .result = .then
	; !. .result = .else
#
fl (.a = .b) ($ => .c)
	:- fl (.a {$}) .a1
	, fl (.b {$}) .b1
	, bind .a1 .b1
	, !, .c = true
	; !, .c = false
#
fl (.a + .b) ($ => .c)
	:- !
	, fl (.a {$}) .a1
	, fl (.b {$}) .b1
	, let .c (.a1 + .b1)
#
fl (.var => .do) (.var => .do) :- ! #
fl (.a, .b) (.a1, .b1)
	:- !
	, fl .a .a1
	, fl .b .b1
#
fl .node ($ => .node) #

bind (.u0, .v0) (.u1, .v1)
	:- !
	, fl .u0 .ue0, fl .v0 .ve0
	, fl .u1 .ue1, fl .v1 .ve1
	, bind .ue0 .ue1
	, bind .ve0 .ve1
#
bind .n .n #

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
fl (.a + .b) ($ => .c)
	:- !
	, fl (.a {$}) .a1
	, fl (.b {$}) .b1
	, let .c (.a + .b)
#
fl (.var => .do) (.var => .do) :- ! #
fl (.a, .b) (.a, .b) :- ! #
fl .node ($ => .node) #

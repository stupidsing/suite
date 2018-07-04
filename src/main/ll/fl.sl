-------------------------------------------------------------------------------
-- lazy functional program interpreter

fl (define .var := .value ~ .do) .result
	:- !
	, replace .var .value1 .value .value1
	, replace .var .value1 .do .do1
	, fl .do1 .result
#
fl (head {.pair}) .result :- !, fl .pair (.result, _) #
fl (tail {.pair}) .result :- !, fl .pair (_, .result) #
fl (.fun {$}) .result :- !, fl .fun ($ => .result) #
fl (.fun {.value}) .result
	:- !
	, fl .fun (.var => .do)
	, replace .var .value .do .do1
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
fl .tree ($ => .c)
	:- tree .tree .a .oper .b
	, member (' + ', ' - ', ' * ', ' / ', ' %% ',) .oper
	, !
	, fl (.a {$}) .a1
	, fl (.b {$}) .b1
	, tree .tree1 .a1 .oper .b1
	, let .c .tree1
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

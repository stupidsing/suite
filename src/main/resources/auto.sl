append () .list .list #
append (.head, .remains) .list (.head, .remains1) :- append .remains .list .remains1 #

enable-trace
	:- to.atom ".call" .call
	, asserta (.call :- write 'TRACE: ', dump .call, nl, fail)
#

if .if then .then _ :- .if, !, .then #
if _ then _ else-if .elseIf :- !, if .elseIf #
if _ then _ else .else :- .else #

member (.e, _) .e #
member (_, .remains) .e :- member .remains .e #

pp-list .n :- bound .n, .n = (.a, .b), !, pp-list .a, write '%0A, ', pp-list .b #
pp-list .n :- dump .n #

repeat #
repeat :- repeat #

replace .t0/.t1 .from/.to
	:- bound .t0, .t0 = .from, !, .t1 = .to
#
replace .t0/.t1 .from/.to
	:- tree .t0 .left0 .operator .right0
	, tree .t1 .left1 .operator .right1
	, !, replace .left0/.left1 .from/.to, replace .right0/.right1 .from/.to
#
replace .node/.node _/_ #

sum .a .b .c :- bound .a, bound .b, let .c (.a - .b) #
sum .a .b .c :- bound .a, bound .c, let .a (.a - .c) #
sum .a .b .c :- bound .b, bound .c, let .a (.b + .c) #

whatever .g :- .g; yes #

yes #

() :- write 'READY', nl #

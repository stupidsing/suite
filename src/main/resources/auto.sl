yes #

repeat #
repeat :- repeat #

whatever .g :- .g; yes #

member (.e, _) .e #
member (_, .remains) .e :- member .remains .e #

replace .t0 .t1 .from .to
	:- bound .t0, .t0 = .from, !, .t1 = .to
#
replace .t0 .t1 .from .to
	:- tree .t0 .left0 .operator .right0
	, tree .t1 .left1 .operator .right1
	, !, replace .left0 .left1 .from .to, replace .right0 .right1 .from .to
#
replace .node .node _ _ #

append () .list .list #
append (.head, .remains) .list (.head, .remains1) :- append .remains .list .remains1 #

if .cond then .then else .else :- .cond, !, .then; .else #

was-success .call
	:- map.retrieve (WAS-SUCCESS .call) .success
	, (bound .success; is-success .call .success)
	, !, .success = ()
#

is-success .call () :- .call, ! #
is-success .call fail #

pp .n :- bound .n, .n = (.a, .b), !, pp .a, write '%0A, ', pp .b #
pp .n :- dump .n #

() :- write 'READY', nl #

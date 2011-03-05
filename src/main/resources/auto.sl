yes #

repeat #
repeat :- repeat #

whatever .g :- .g; yes #

member (.e, _) .e #
member (_, .remains) .e :- member .remains .e #

append () .list .list #
append (.head, .remains) .list (.head, .remains1) :- append .remains .list .remains1 #

was.success .call :-
	map.retrieve (WAS-SUCCESS .call, .success)
	, (bound .success; is.success .call .success)
	, !, .success = ()
#

is.success .call () :- .call, ! #
is.success .call fail #

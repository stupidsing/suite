append () .list .list #
append (.head, .tail) .list (.head, .tail1) :- append .tail .list .tail1 #

clear :- retract.all (_ :- _), ! #

list.fold ()/.u/.u _ _
#
list.fold (.e, .es)/.u0/.ux .v/.v0/.vx .goal
	:- clone (.v/.v0/.vx .goal) (.e/.u0/.u1 .goal1)
	, .goal1
	, list.fold .es/.u1/.ux .v/.v0/.vx .goal
#

list.query () _ _
#
list.query (.e, .es) .var .goal
	:- clone (.var .goal) (.e .goal1)
	, .goal1
	, list.query .es .var .goal
#

list.query2 () () _ _ _
#
list.query2 (.e, .es) (.f, .fs) .e_ .f_ .goal
	:- clone (.e_ .f_ .goal) (.e .f .goal1)
	, .goal1
	, list.query2 .es .fs .e_ .f_ .goal
#

member (.e, _) .e #
member (_, .tail) .e :- member .tail .e #

repeat #
repeat :- repeat #

sum .a .b .c :- bound .a, bound .b, let .c (.a - .b) #
sum .a .b .c :- bound .a, bound .c, let .b (.a - .c) #
sum .a .b .c :- bound .b, bound .c, let .a (.b + .c) #

trace .pred :- asserta (.pred .ps :- dump .pred .ps, nl, fail) #

yes #

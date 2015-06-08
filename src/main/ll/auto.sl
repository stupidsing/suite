append () .list .list #
append (.head, .tail) .list (.head, .tail1) :- append .tail .list .tail1 #

clear :- retract.all (_ :- _), ! #

list.fold _ _ ()/.u/.u
#
list.fold .v/.v0/.vx .goal (.e, .es)/.u0/.ux
	:- clone (.v/.v0/.vx .goal) (.e/.u0/.u1 .goal1)
	, .goal1
	, list.fold .v/.v0/.vx .goal .es/.u1/.ux
#

list.query _ _ ()
#
list.query .var .goal (.e, .es)
	:- clone (.var .goal) (.e .goal1)
	, .goal1
	, list.query .var .goal .es
#

list.query2 _ _ _ () ()
#
list.query2 .e_ .f_ .goal (.e, .es) (.f, .fs)
	:- clone (.e_ .f_ .goal) (.e .f .goal1)
	, .goal1
	, list.query2 .e_ .f_ .goal .es .fs
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

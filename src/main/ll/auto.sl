append () .list .list #
append (.head, .tail) .list (.head, .tail1) :- append .tail .list .tail1 #

clear :- retract.all (_ :- _), ! #

import.path .path
	:- suite.dir .homeDir
	, concat "file:" .homeDir "/src/main/ll/" .path .url
	, import.url .url
#

list.fold ()/.u/.u _ _
#
list.fold (.e, .es)/.u0/.ux .v/.v0/.vx .goal
	:- clone (.v/.v0/.vx .goal) (.e/.u0/.u1 .goal1)
	, .goal1
	, list.fold .es/.u1/.ux .v/.v0/.vx .goal
#

list.query () _ _
#
list.query (.v, .vs) .var .goal
	:- clone (.var .goal) (.v .g)
	, .g
	, list.query .vs .var .goal
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

zip () () () #
zip (.u, .us) (.v, .vs) (.u:.v, .uvs) :- zip .us .vs .uvs #

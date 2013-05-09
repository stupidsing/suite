append () .list .list #
append (.head, .tail) .list (.head, .tail1) :- append .tail .list .tail1 #

length () 0 #
length (_, .r) .l1 :- length .r .l0, let .l1 (.l0 + 1) #

member (.e, _) .e #
member (_, .tail) .e :- member .tail .e #

repeat #
repeat :- repeat #

replace .t0/.t1 .from/.to
	:- bound .t0, .t0 = .from, !, .t1 = .to
#
replace .t0/.t1 .from/.to
	:- tree .t0 .left0 .operator .right0
	, tree .t1 .left1 .operator .right1
	, !
	, replace .left0/.left1 .from/.to
	, replace .right0/.right1 .from/.to
#
replace .node/.node _/_ #

sum .a .b .c :- bound .a, bound .b, let .c (.a - .b) #
sum .a .b .c :- bound .a, bound .c, let .a (.a - .c) #
sum .a .b .c :- bound .b, bound .c, let .a (.b + .c) #

use .fn
	:- concat "src/main/resources/" .fn .fn1
	, (IMPORTED .fn1; assert IMPORTED .fn1, import.file .fn1)
#

whatever .g :- .g; yes #

yes #

() :- write 'READY', nl #

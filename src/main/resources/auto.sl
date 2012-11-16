append () .list .list #
append (.head, .tail) .list (.head, .tail1) :- append .tail .list .tail1 #

enable-trace
	:- to.atom ".call" .call
	, asserta (.call :- write 'TRACE: ', dump .call, nl, fail)
#

-- Converts tuple (a b c) to/from list (a, b, c,) with terminating nil
enlist .a .b :- bound .a, !, enlistf .a .b #
enlist .a .b :- bound .b, !, enlistb .a .b #

enlistb .item (.item,) :- ! #
enlistb () () :- ! #
enlistb (.item .items) (.item, .items1) :- !, enlist .items .items1 #

enlistf (.item .items) (.item, .items1) :- !, enlist .items .items1 #
enlistf () () :- ! #
enlistf .item (.item,) :- ! #

fold () .r/.r _ #
fold (.head, .tail) .r0/.rx .action
	:- generalize.prefix .action "_" (.head .r0/.r1 => .do)
	, .do
	, fold .tail .r1/.rx .action
#

if .if then .then _ :- .if, !, .then #
if _ then _ end-if :- ! #
if _ then _ else .else :- !, .else #
if _ then _ else-if .elseIf :- !, if .elseIf #

length () 0 #
length (_, .r) .l1 :- length .r .l0, let .l1 (.l0 + 1) #

member (.e, _) .e #
member (_, .tail) .e :- member .tail .e #

pp-indent 0 :- ! #
pp-indent .i :- write '    ', let .i1 (.i - 1), pp-indent .i1 #

pp-list .n :- bound .n, .n = (.a, .b), !, pp-list .a, write '%0A, ', pp-list .b #
pp-list .n :- dump .n #

pp-tree .t :- pp-tree0 .t 0 #

pp-tree0 .t .depth
	:- tree .t .l .op .r
	, !
	, let .depth1 (.depth + 1)
	, pp-tree0 .l .depth1
	, pp-indent .depth, write '<'
	, once (.op = ' ', write 'sp'; trim .op .op1, write .op1)
	, write '>', nl
	, pp-tree0 .r .depth1
#
pp-tree0 .e .depth :- pp-indent .depth, dump .e, nl #

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

twin-list (.e0, _) (.e1, _) .e0 .e1 #
twin-list (_, .es0) (_, .es1) .e0 .e1 :- twin-list .es0 .es1 .e0 .e1 #

use .fn
	:- concat "src/main/resources/" .fn .fn1
	, (IMPORTED .fn1; assert IMPORTED .fn1, import.file .fn1)
#

whatever .g :- .g; yes #

yes #

() :- write 'READY', nl #

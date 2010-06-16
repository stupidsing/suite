yes #

repeat #
repeat :- repeat #

whatever .g :- .g; yes #

member (.e, _) .e #
member (_, .remains) .e :- member .remains .e #

append () .list .list #
append (.head, .remains) .list (.head, .remains1) :- append .remains .list .remains1 #

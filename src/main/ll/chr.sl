-- constraint handling rules

chr-chain .rules .facts0/.factsx
	:- chr-deduce .rules .facts0/.facts1
	, !, chr-chain .rules .facts1/.factsx
#
chr-chain _ .facts/.facts -- termination
#

chr-deduce .rules .facts0/.factsx
	:- member .rules .rule0
	, clone .rule0 .rule1
	, chr-match .rule1 .facts0/.factsx
#

chr-match (if .if then .then end) .facts0/.factsx
	:- !, chr-match0 () .if .then () .facts0/.factsx
#
chr-match (if .if then .then when .when end) .facts0/.factsx
	:- !, chr-match0 () .if .then .when .facts0/.factsx
#
chr-match (given .given if .if then .then end) .facts0/.factsx
	:- !, chr-match0 .given .if .then () .facts0/.factsx
#
chr-match (given .given if .if then .then when .when end) .facts0/.factsx
	:- !, chr-match0 .given .if .then .when .facts0/.factsx
#

chr-match0 .given .if .then .when .facts0/.factsx
	:- chr-retract-list .if .facts0/.facts1
	, chr-retract-list .given .facts1/_
	, .when
	, !
	, chr-assert-list .then .facts1/.factsx
#

chr-retract-list .list .facts0/.factsx
	:- list.fold .list/.facts0/.factsx .c/.facts0_/.factsx_ (chr-retract .c .facts0_/.factsx_)
#

chr-retract .c (.c, .facts)/.facts #
chr-retract .c (.fact, .facts0)/(.fact, .factsx) :- chr-retract .c .facts0/.factsx #

chr-assert-list () .facts/.facts
#
chr-assert-list (.a = .b, .newFacts) .facts0/(.a = .b, .factsx) -- built-in syntactic equality
	:- !
	, replace .a .b .facts0 .facts1
	, chr-assert-list .newFacts .facts1/.factsx
#
chr-assert-list (.newFact, .newFacts) .facts0/(.newFact, .factsx)
	:- chr-assert-list .newFacts .facts0/.factsx
#

chr-test
	:- chr-chain (
		if (.x LE .x,) then () end,
		if (.x LE .y, .y LE .x,) then (.x = .y,) end,
		given (.x LE .y, .y LE .z,) if () then (.x LE .z,) end,
		given (.x LE .y,) if (.x LE .y,) if () then () end,
	) (
		A LE B, B LE C, C LE A,
	) .factsx
	, dump .factsx, nl
#

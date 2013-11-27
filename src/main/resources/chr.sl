-- Constraint handling rules

chr-chain .rules .facts0 .factsx
	:- chr-deduce .rules .facts0 .facts1
	, !, chr-chain .rules .facts1 .factsx
#
chr-chain _ .facts .facts -- Termination
#

chr-deduce .rules .facts0 .factsx
	:- member .rules .rule0
	, clone .rule0 .rule1
	, chr-match .rule1 .facts0 .factsx
#

chr-match (if .if then .then end) .facts0 .factsx
	:- !, chr-match0 () .if .then () .facts0 .factsx
#
chr-match (if .if then .then when .when end) .facts0 .factsx
	:- !, chr-match0 () .if .then .when .facts0 .factsx
#
chr-match (given .given if .if then .then end) .facts0 .factsx
	:- !, chr-match0 .given .if .then () .facts0 .factsx
#
chr-match (given .given if .if then .then when .when end) .facts0 .factsx
	:- !, chr-match0 .given .if .then .when .facts0 .factsx
#

chr-match0 .given .if .then .when .facts0 .factsx
	:- chr-list .facts0 .given
	, chr-retract-list .if .facts0 .facts1
	, .when
	, !
	, append .then .facts1 .factsx
#

chr-list _ () #
chr-list .facts (.c, .cs) :- member .facts .c, chr-list .facts .cs #

chr-retract-list () .facts .facts #
chr-retract-list (.c, .cs) .facts0 .factsx
	:- chr-retract .c .facts0 .facts1
	, chr-retract-list .cs .facts1 .factsx
#

chr-retract .c (.c, .facts) .facts #
chr-retract .c (.fact, .facts0) (.fact, .factsx) :- chr-retract .c .facts0 .factsx #

test
	:- chr-chain (
		if (.x <= .x,) then () end,
		if (.x <= .y, .y <= .x,) then (.x = .y,) end,
		given (.x <= .y, .y <= .z,) if () then (.x <= .z,) end,
		given (.x <= y,) if (.x <= .y,) if () then () end,
	) (
		A <= B, B <= C, C <= A,
	) .factsx
	, dump .factsx, nl	
#

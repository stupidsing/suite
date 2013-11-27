# Constraint handling rules

chr-chain
	:- chr-rule .rule0
	, generalize .rule0 .rule1
	, chr-match .rule1
	, !, chr-chain
#
chr-chain #
-- Terminated. you got nothing (?)

--chr-match (.head | .remove => .guard | .tail)
--	:- .head, .remove, .guard, assert .tail, retract .remove
--#
chr-match (given .given if .if then .tail when .when)
	:- chr .given
	, chr .if
	, .when
	, assert (chr .tail)
	, retract (chr .if)
#

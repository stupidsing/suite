# Constraint handling rules

chr
	:- chr-rule .rule0
	, generalize .rule0 .rule1
	, chr-match .rule1
	, !, chr
#
chr #
-- Terminated. you got nothing (?)

chr-match (.head | .remove => .guard | .tail)
	:- .head
	, .remove
	, assert .tail
	, retract .remove
#

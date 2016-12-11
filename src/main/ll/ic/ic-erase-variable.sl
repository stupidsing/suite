ic-erase-variable .frame/.vs (DECLARES .var .offset .size .do0) (DECLARES .var .offset .size .dox)
	:- !
	, ic-erase-variable .frame/(FRAME-VAR .var .frame .offset .size, .vs) .do0 .dox
#
ic-erase-variable .frame/.vs (METHOD0 .pss .do0) (METHOD0 .pss .dox)
	:- !
	, .frame1 = F .frame
	, ic-parameters-variables .frame1 12 .pss .vs1/.vs
	, ic-erase-variable .frame1/.vs1 .do0 .dox
#
ic-erase-variable .frame/.vs (VAR .var) (MEMORY .size (TREE ' + ' (NUMBER .offset) .this))
	:- !
	, once (member .vs (FRAME-VAR .var .frame1 .offset .size))
	, ic-this .frame .frame1 .this
#
ic-erase-variable .fv .do0 .dox
	:- ic-rewrite .do0 .dox .ts/()
	, ic-erase-variables .fv .ts
#

-- list.query will not work since the offset values in .fv are waiting to be bound.
-- the cloning effect in the list query predicate would divert the argument.
ic-erase-variables _ ()
#
ic-erase-variables .fv (.do0 .dox, .ts)
	:- ic-erase-variable .fv .do0 .dox
	, ic-erase-variables .fv .ts
#

ic-parameters-variables _ _ () .vs/.vs
#
ic-parameters-variables .frame .offset (MPS .var .size, .vars) (FRAME-VAR .var .frame .offset .size, .vs0)/.vsx
	:- let .offset1 (.offset + .size)
	, ic-parameters-variables .frame .offset1 .vars .vs0/.vsx
#

ic-this .frame .frame THIS #
ic-this (F .frame) .frame1 (MEMORY 4 .this) :- ic-this .frame .frame1 .this #

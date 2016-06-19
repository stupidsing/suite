ic-erase-stored .do0 .dox
	:- !, ic-erase-stored0 .do0 .do1 .dox/.do1
#

ic-erase-stored0 (METHOD0 .mps .m0) (METHOD0 .mps .m1) .e/.e
	:- !
	, ic-erase-stored .m0 .m1
#
ic-erase-stored0 (SEQ .a0 .b0) (SEQ .a1 .b1) .e/.e
	:- !
	, ic-erase-stored .a0 .a1
	, ic-erase-stored .b0 .b1
#
ic-erase-stored0 (STORED .value0) (VAR .var) .e0/.ex
	:- !
	, temp .var
	, ic-erase-stored .value0 .value1
	, .e0 = DECLARE MONO .var .type (SEQ (LET (VAR .var) (IN .var .type .value1)) .ex)
#
ic-erase-stored0 .do0 .dox .e0/.ex
	:- ic-rewrite .do0 .dox .ts/()
	, ic-erase-storeds .ts .e0/.ex
#

ic-erase-storeds () .e/.e
#
ic-erase-storeds (.do0 .dox, .ts) .e0/.ex
	:- ic-erase-stored0 .do0 .dox .e0/.e1
	, ic-erase-storeds .ts .e1/.ex
#

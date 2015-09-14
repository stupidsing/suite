ic-erase-type (DECLARE .type .var .do0) (ALLOC .size .var .dox)
	:- !
	, ic-type-size .type .size
	, ic-erase-type .do0 .dox
#
ic-erase-type (METHOD .params .do0) (METHOD () .dox)
	:- replace $$EBP (MEMORY 4 $$EBP) .do0 .do1
	, ic-replace-parameters .params 4 .do1 .do2
	, ic-erase-type .do2 .dox
#
ic-erase-type (OBJECT .type .var0) (MEMORY .size .varx)
	:- !
	, ic-type-size .type .size
	, ic-erase-type .var0 .varx
#
ic-erase-type .do0 .dox
	:- ic-rewrite .do0 .dox .ts/()
	, list.query .ts (.do0_ .dox_) (ic-erase-type .do0_ .dox_)
#

ic-replace-parameters () _ .do .do
#
ic-replace-parameters (PARAM .type .var, .vars) .s0 .do0 .dox
	:- ic-type-size .type .size
	, let .s (.s0 + .size)
	, replace (VAR .var) (MEMORY .size (TREE ' + ' $$EBP (NUMBER .s))) .do0 .do1
	, ic-replace-parameters .vars .s .do1 .dox
#

ic-type-size I32 4
#
ic-type-size (ARRAY-OF .arraySize .type) .size
	:- ic-type-size .type .elementSize
	, let .size (.arraySize * .elementSize)
#
ic-type-size (METHOD-OF _) 8
#
ic-type-size (PTR-OF _) 4
#
ic-type-size (TUPLE-OF _ ()) 0
#
ic-type-size (TUPLE-OF .name (.type, .types)) .size
	:- ic-type-size .type .size0
	, ic-type-size (TUPLE-OF .name .types) .size1
	, let .size (.size0 + .size1)
#

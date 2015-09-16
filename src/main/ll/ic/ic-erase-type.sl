ic-erase-type (DECLARE .type .var .do0) (ALLOC .size .var .dox)
	:- !
	, ic-type-size .type .size
	, ic-erase-type .do0 .dox
#
ic-erase-type (INDEX .type .array .index) (MEMORY .size (TREE ' + ' (REF .array) TREE ' * ' .index (NUMBER .size)))
	:- !
	, ic-type-size .type .size
#
ic-erase-type (METHOD .params .do0) (METHOD .pss .dox)
	:- zip .params .pss .list
	, list.query .list (PARAM .type .var):(PS .size .var) (ic-type-size .type .size)
	, ic-erase-type .do0 .dox
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

ic-type-size I32 4
#
ic-type-size (ARRAY-OF .arraySize .type) .size
	:- ic-type-size .type .elementSize
	, let .size (.arraySize * .elementSize)
#
ic-type-size (METHOD-OF _) 4
#
ic-type-size (METHOD2-OF _) 8
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

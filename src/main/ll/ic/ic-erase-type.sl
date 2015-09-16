ic-erase-type (DECLARE .type .var .do0) (ALLOC .size .var .dox)
	:- !
	, ic-type-size .type .size
	, ic-erase-type .do0 .dox
#
ic-erase-type (FIELD (STRUCT-OF .nameTypes) .field .do0) (MEMORY .size (TREE ' + ' (REF .dox) (NUMBER .offset)))
	:- !
	, ic-struct-offset .nameTypes .field .type .offset
	, ic-type-size .type .size
	, ic-erase-type .do0 .dox
#
ic-erase-type (INDEX .type .array0 .i0) (MEMORY .size (TREE ' + ' (REF .arrayx) TREE ' * ' .ix (NUMBER .size)))
	:- !
	, ic-type-size .type .size
	, ic-erase-type .array0 .arrayx
	, ic-erase-type .i0 .ix
#
ic-erase-type (METHOD0 .params .do0) (METHOD0 .pss .dox)
	:- zip .params .pss .list
	, list.query .list (PARAM .type .var):(PS .size .var) (ic-type-size .type .size)
	, ic-erase-type .do0 .dox
#
ic-erase-type (OBJECT .type .var0) (MEMORY .size .varx)
	:- !
	, ic-type-size .type .size
	, ic-erase-type .var0 .varx
#
ic-erase-type (PRAGMA (TYPE-CAST _) .do0) .dox
	:- !
	, ic-erase-type .do0 .dox
#
ic-erase-type .do0 .dox
	:- ic-rewrite .do0 .dox .ts/()
	, list.query .ts (.do0_ .dox_) (ic-erase-type .do0_ .dox_)
#

ic-struct-offset (.name .type, _) .name .type 0
#
ic-struct-offset (_ .dummyType, .nameTypes) .name .type .offset
	:- ic-struct-offset .nameTypes .name .type .offset0
	, ic-type-size .dummyType .offset1
	, let .offset (.offset0 + .offset1)
#

ic-type-size I32 4
#
ic-type-size (ARRAY-OF .arraySize .type) .size
	:- ic-type-size .type .elementSize
	, let .size (.arraySize * .elementSize)
#
ic-type-size (METHOD0-OF _) 4
#
ic-type-size (METHOD-OF _) 8
#
ic-type-size (POINTER-OF _) 4
#
ic-type-size (STRUCT-OF ()) 0
#
ic-type-size (STRUCT-OF (_ .type, .nameTypes)) .size
	:- ic-type-size .type .size0
	, ic-type-size (STRUCT-OF .nameTypes) .size1
	, let .size (.size0 + .size1)
#

ic-erase-type .do0 .dox
	:- ic-erase-type0 .do0 .dox, !
#
ic-erase-type .do0 .dox
	:- ic-rewrite .do0 .dox .ts/()
	, list.query .ts (.do0_ .dox_) (ic-erase-type .do0_ .dox_)
#

ic-erase-type0 (ARRAY .type ()) (ARRAYS .size ())
	:- ic-type-size .type .size
#
ic-erase-type0 (ARRAY .type (.elem0, .array0)) (ARRAYS .size (.elemx, .arrayx))
	:- ic-erase-type .elem0 .elemx
	, ic-erase-type (ARRAY .type .array0) (ARRAYS .size .arrayx)
#
ic-erase-type0 (BOOLEAN .b) (NUMBER .b)
#
ic-erase-type0 (DECLARE .mp .var .type .do0) (DECLARES .var _ .size .dox)
	:- dump (type-of {.var} = .mp .type), nl
	, ic-type-size .type .size
	, ic-erase-type .do0 .dox
#
ic-erase-type0 (FIELD (STRUCT-OF .nts) .name .do0) (MEMORY .size (TREE ' + ' (NUMBER .offset) .dox))
	:- ic-struct-offset .nts .name .type .offset
	, ic-type-size .type .size
	, ic-erase-type (REF .do0) .dox
#
ic-erase-type0 (IN .var _ .do0) .dox
	:- try (ic-erase-type .do0 .dox) .ex (throw .ex "%0Aat variable" .var)
#
ic-erase-type0 (INDEX .type .array .i0) (MEMORY .size (TREE ' + ' .pointer (TREE ' * ' (NUMBER .size) .ix)))
	:- ic-type-size .type .size
	, ic-erase-type (REF .array) .pointer
	, ic-erase-type .i0 .ix
#
ic-erase-type0 (METHOD0 .mps .do0) (METHOD0 .mpss .dox)
	:- zip .mps .mpss .list
	, list.query .list (MP _ .var .type):(MPS .var .size) (ic-type-size .type .size)
	, ic-erase-type .do0 .dox
#
ic-erase-type0 (NEW _ ()) (NEWS ())
#
ic-erase-type0 (NEW (STRUCT-OF .nts) (.name .value0, .nvs)) (NEWS (.size .offset .value1, .sovs))
	:- ic-erase-type .value0 .value1
	, ic-struct-offset .nts .name .type .offset
	, ic-type-size .type .size
	, ic-erase-type (NEW (STRUCT-OF .nts) .nvs) (NEWS .sovs)
#
ic-erase-type0 NULL (NUMBER 0)
#
ic-erase-type0 (OBJECT .type .var0) (MEMORY .size .varx)
	:- ic-type-size .type .size
	, ic-erase-type .var0 .varx
#
ic-erase-type0 (OFFSET .offset0 .pointer0) (TREE ' + ' .pointerx .offsetx)
	:- ic-erase-type .offset0 .offsetx
	, ic-erase-type .pointer0 .pointerx
#
ic-erase-type0 (REF (OBJECT _ .pointer0)) .pointerx
	:- ic-erase-type .pointer0 .pointerx
#
ic-erase-type0 (SIZE-OF .type) (NUMBER .size)
	:- ic-type-size .type .size
#
ic-erase-type0 (TYPE-CAST _ .do0) .dox
	:- ic-erase-type .do0 .dox
#

ic-struct-offset (.name .type, _) .name .type 0
#
ic-struct-offset (_ .dummyType, .nameTypes) .name .type .offset
	:- ic-struct-offset .nameTypes .name .type .offset0
	, ic-type-size .dummyType .offset1
	, let .offset (.offset0 + .offset1)
#

ic-type-size .type _
	:- not (bound .type)
	, ic-error "Cannot get size of" .type
#
ic-type-size BOOLEAN 4
#
ic-type-size I32 4
#
ic-type-size I8 1
#
ic-type-size (ARRAY-OF .arraySize .type) .size
	:- ic-type-size .type .elementSize
	, let .size (.arraySize * .elementSize)
#
ic-type-size (METHOD0-OF _ _) 4
#
ic-type-size (METHOD-OF _ _) 8
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

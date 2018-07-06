ic-erase-type .do0 .dox
	:- ic-erase-type_ .do0 .dox, !
#
ic-erase-type .do0 .dox
	:- ic-rewrite .do0 .dox .ts/()
	, list.query .ts (.do0_ .dox_) (ic-erase-type .do0_ .dox_)
#

ic-erase-type_ (ARRAY .type ()) (ARRAYS .size ())
	:- ic-type-size .type .size
#
ic-erase-type_ (ARRAY .type (.elem0, .array0)) (ARRAYS .size (.elemx, .arrayx))
	:- ic-erase-type .elem0 .elemx
	, ic-erase-type (ARRAY .type .array0) (ARRAYS .size .arrayx)
#
ic-erase-type_ (BOOLEAN .b) (NUMBER .b)
#
ic-erase-type_ (DECLARE .mp .var .type .do0) (DECLARES .var _ .size .dox)
	:- dump (type-of_{.var} = .mp .type), nl
	, ic-type-size .type .size
	, ic-erase-type .do0 .dox
#
ic-erase-type_ (FIELD (STRUCT-OF (.nts | .name .type)) .name .do0) (MEMORY .size (TREE ' + ' (NUMBER .offset) .dox))
	:- ic-type-size (STRUCT-OF .nts) .offset
	, ic-type-size .type .size
	, ic-erase-type (REF .do0) .dox
#
ic-erase-type_ (FIELD (STRUCT-OF (.nts | _)) .name .do0) .dox
	:- ic-erase-type (FIELD (STRUCT-OF .nts) .name .do0) .dox
#
ic-erase-type_ (IN .var _ .do0) .dox
	:- try (ic-erase-type .do0 .dox) .ex (throw .ex "%0Aat variable" .var)
#
ic-erase-type_ (INDEX .type .array .i0) (MEMORY .size (TREE ' + ' .pointer (TREE ' * ' (NUMBER .size) .ix)))
	:- ic-type-size .type .size
	, ic-erase-type (REF .array) .pointer
	, ic-erase-type .i0 .ix
#
ic-erase-type_ (METHOD0 .mps .do0) (METHOD0 .mpss .dox)
	:- zip .mps .mpss .list
	, list.query .list (MP _ .var .type):(MPS .var .size) (ic-type-size .type .size)
	, ic-erase-type .do0 .dox
#
ic-erase-type_ (NEW-STRUCT _ ()) (NEW-STRUCTS ())
#
ic-erase-type_ (NEW-STRUCT (STRUCT-OF .nts) (.name .value0, .nvs)) (NEW-STRUCTS (.size .offset .value1, .sovs))
	:- ic-erase-type .value0 .value1
	, ic-struct-type-offset .nts .name .type .offset
	, ic-type-size .type .size
	, ic-erase-type (NEW-STRUCT (STRUCT-OF .nts) .nvs) (NEW-STRUCTS .sovs)
#
ic-erase-type_ (NEW-TAG (TAG-OF .tts) .tag .value0) (NEW-TAGS .size .tag .value1)
	:- ic-erase-type .value0 .value1
	, ic-tag-type .tts .tag .type
	, ic-type-size .type .size
#
ic-erase-type_ NULL (NUMBER 0)
#
ic-erase-type_ (OBJECT .type .var0) (MEMORY .size .varx)
	:- ic-type-size .type .size
	, ic-erase-type .var0 .varx
#
ic-erase-type_ (OFFSET .offset0 .pointer0) (TREE ' + ' .pointerx .offsetx)
	:- ic-erase-type .offset0 .offsetx
	, ic-erase-type .pointer0 .pointerx
#
ic-erase-type_ (REF (OBJECT _ .pointer0)) .pointerx
	:- ic-erase-type .pointer0 .pointerx
#
ic-erase-type_ (SIZE-OF .type) (NUMBER .size)
	:- ic-type-size .type .size
#
ic-erase-type_ (TAG _ _ .do0) (MEMORY .size (TREE ' + ' (NUMBER 4) .dox))
	:- ic-type-size I32 .size
	, ic-erase-type (REF .do0) .dox
#
ic-erase-type_ (TYPE-CAST _ .do0) .dox
	:- ic-erase-type .do0 .dox
#

ic-struct-type-offset (.nts | .name .type) .name .type .offset
	:- ic-type-size (STRUCT-OF .nts) .offset
#
ic-struct-type-offset (.nts | _) .name .type .offset
	:- ic-struct-type-offset .nts .name .type .offset
#

ic-tag-type (_ | .tag .type) .tag .type
#
ic-tag-type (.tts | _) .tag .type
	:- ic-tag-type .tts .tag .type
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
ic-type-size (STRUCT-OF (.nts | _ .type)) .size
	:- ic-type-size .type .size0
	, ic-type-size (STRUCT-OF .nts) .size1
	, let .size (.size0 + .size1)
#
ic-type-size (TAG-OF ()) 4
#
ic-type-size (TAG-OF (.nts | _ .type)) .size
	:- ic-type-size .type .s
	, let .size0 (.s + 4)
	, ic-type-size (TAG-OF .nts) .size1
	, once (.size0 < .size1, .size = .size1; .size = .size0)
#

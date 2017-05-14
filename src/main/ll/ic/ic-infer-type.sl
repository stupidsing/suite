ic-infer-type .vs .do .typex
	:- once (ic-infer-type_ .vs .do .type0
		; ic-error "Cannot resolve type of" .do
	)
	, once (graph.bind .type0 .typex
		; ic-error "Cannot bind type of" .do "to" .typex
	)
#

ic-infer-type_ _ (ARRAY .type ()) (ARRAY-OF 0 .type)
#
ic-infer-type_ .vs (ARRAY .type (.elem, .array)) (ARRAY-OF .arraySize1 .type)
	:- ic-infer-type .vs .elem .type
	, ic-infer-type .vs (ARRAY .type .array) (ARRAY-OF .arraySize0 .type)
	, let .arraySize1 (.arraySize0 + 1)
#
ic-infer-type_ _ (ASM _) I32
#
ic-infer-type_ _ (BOOLEAN _) BOOLEAN
#
ic-infer-type_ .vs (DECLARE .mp .var .varType .do) .type
	:- ic-infer-type (.mp .var .varType, .vs) .do .type
#
ic-infer-type_ .vs (EXTEND-SIGNED .do) I32
	:- ic-infer-type .vs .do I8
#
ic-infer-type_ .vs (FIELD (STRUCT-OF .nts) .name .do) .type
	:- ic-infer-type .vs .do (STRUCT-OF .nts)
	, ic-field-tag-type .nts .name .type
#
ic-infer-type_ .vs (IF .if .then .else) .type
	:- ic-infer-type .vs .if .ifType
	, ic-condition-type .ifType
	, ic-infer-type .vs .then .type
	, ic-infer-type .vs .else .type
#
ic-infer-type_ .vs (INDEX .type .array .index) .type
	:- ic-infer-type .vs .array (ARRAY-OF _ .type)
	, ic-infer-type .vs .index I32
#
ic-infer-type_ .vs (IN .var .type .do) .type
	:- try (ic-infer-type .vs .do .type) .ex (throw .ex "%0Aat variable" .var)
#
ic-infer-type_ .vs (INVOKE .method .ips) .returnType
	:- ic-infer-type .vs .method (METHOD-OF .pos .returnType)
	, ic-match-parameter-types .vs .ips .pos
#
ic-infer-type_ .vs (LET .var .value) .type
	:- ic-infer-type .vs .var .type
	, ic-infer-type .vs .value .type
#
ic-infer-type_ .vs (METHOD0 () .do) (METHOD0-OF () .returnType)
	:- ic-infer-type .vs .do .returnType
#
ic-infer-type_ .vs (METHOD0 (MP .io .var .paramType, .mps) .do) (METHOD0-OF (PARAM-OF .io .paramType, .pos) .returnType)
	:- ic-infer-type (MONO .var .paramType, .vs) (METHOD0 .mps .do) (METHOD0-OF .pos .returnType)
#
ic-infer-type_ .vs (METHOD .this .method) (METHOD-OF .pos .returnType)
	:- ic-infer-type .vs .this I32
	, ic-infer-type .vs .method (METHOD0-OF .pos .returnType)
#
ic-infer-type_ _ (NEW-STRUCT .type ()) .type
#
ic-infer-type_ .vs (NEW-STRUCT .type (.name .value, .nvs)) .type
	:- .type = STRUCT-OF .nts
	, ic-field-tag-type .nts .name .t
	, ic-infer-type .vs .value .t
	, ic-infer-type .vs (NEW-STRUCT .type .nvs) .type
#
ic-infer-type_ .vs (NEW-TAG .type .tag _) .type
	:- ic-infer-type .vs .tag I32
#
ic-infer-type_ .vs (NEW-TAG .type .tag .value) .type
	:- .type = TAG-OF .nts
	, ic-field-tag-type .nts .tag .t
	, ic-infer-type .vs .value .t
	, ic-infer-type .vs (NEW-TAG .type .tag .value) .type
#
ic-infer-type_ _ NOP _
#
ic-infer-type_ _ NULL (POINTER-OF _)
#
ic-infer-type_ _ (NUMBER _) I32
#
ic-infer-type_ .vs (OBJECT .type .pointer) .type
	:- ic-infer-type .vs .pointer (POINTER-OF .type)
#
ic-infer-type_ .vs (OFFSET .offset .pointer) (POINTER-OF .type)
	:- ic-infer-type .vs .offset I32
	, ic-infer-type .vs .pointer (POINTER-OF .type)
#
ic-infer-type_ .vs (POST-ADD-NUMBER .pointer _) I32
	:- ic-infer-type .vs .pointer I32
#
ic-infer-type_ _ (PRAGMA TYPE-SKIP-CHECK _) _
	:- !
#
ic-infer-type_ .vs (PRAGMA (TYPE-VERIFY .var .varType) .do) .type
	:- !
	, graph.generalize .varType .varType1
	, ic-infer-type .vs .var .varType1
	, ic-infer-type .vs .do .type
#
ic-infer-type_ .vs (PRAGMA _ .do) .type
	:- ic-infer-type .vs .do .type
#
ic-infer-type_ .vs (PRE-ADD-NUMBER .pointer _) I32
	:- ic-infer-type .vs .pointer I32
#
ic-infer-type_ .vs (REF .object) (POINTER-OF .type)
	:- ic-infer-type .vs .object .type
#
ic-infer-type_ .vs (SEQ .do0 .do1) .type
	:- ic-infer-type .vs .do0 _
	, ic-infer-type .vs .do1 .type
#
ic-infer-type_ _ (SIZE-OF _) I32
#
ic-infer-type_ .vs (SNIPPET .snippet) I32
	:- ic-infer-type .vs .snippet _
#
ic-infer-type_ _ (STRING _) I32
#
ic-infer-type_ .vs (NEW-STRUCT .type (.name .value, .nvs)) .type
	:- .type = STRUCT-OF .nts
	, ic-field-tag-type .nts .name .t
	, ic-infer-type .vs .value .t
	, ic-infer-type .vs (NEW-STRUCT .type .nvs) .type
#
ic-infer-type_ .vs (TAG (TAG-OF .nts) .name .do) .type
	:- ic-infer-type .vs .do (TAG-OF .nts)
	, ic-field-tag-type .nts .name .type
#
ic-infer-type_ _ THIS I32
#
ic-infer-type_ .vs (TREE .op .value0 .value1) .type
	:- once (
		member (' = ', ' != ', ' <= ', ' < ',) .op, .type = BOOLEAN
		; .type = .valueType
	)
	, ic-infer-type .vs .value0 .valueType
	, ic-infer-type .vs .value1 .valueType
#
ic-infer-type_ .vs (TYPE-CAST .type .do) .type
	:- ic-infer-type .vs .do _
#
ic-infer-type_ .vs (VAR .var) .type
	:- once (member .vs (MONO .var .type)
		; member .vs (POLY .var .type0), clone .type0 .type
	)
#
ic-infer-type_ .vs (WHILE .while .do) I32
	:- ic-infer-type .vs .while .whileType
	, ic-condition-type .whileType
	, ic-infer-type .vs .do _
#

ic-match-parameter-types _ () ()
#
ic-match-parameter-types .vs (IP .io .do, .ips) (PARAM-OF .io .type, .pos)
	:- ic-infer-type .vs .do .type
	, ic-match-parameter-types .vs .ips .pos
#

ic-field-tag-type (.nts | .name .type) .name .type
	:- once (bound .nts; ic-error "Cannot access field of unknown type")
#
ic-field-tag-type (.nts | _ _) .name .type
	:- ic-field-tag-type .nts .name .type
#

ic-condition-type .type
	:- once (.type = BOOLEAN; .type = POINTER-OF _)
#

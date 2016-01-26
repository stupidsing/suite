ic-infer-type .vs .do .typex
	:- once (
		ic-infer-type0 .vs .do .type0
		, graph.bind .type0 .typex
		; ic-error "Cannot resolve type of" .do "to" .typex
	)
#

ic-infer-type0 _ (ARRAY .type ()) (ARRAY-OF 0 .type)
#
ic-infer-type0 .vs (ARRAY .type (.elem, .array)) (ARRAY-OF .arraySize1 .type)
	:- ic-infer-type .vs .elem .type
	, ic-infer-type .vs (ARRAY .type .array) (ARRAY-OF .arraySize0 .type)
	, let .arraySize1 (.arraySize0 + 1)
#
ic-infer-type0 _ (ASM _) I32
#
ic-infer-type0 _ (BOOLEAN _) BOOLEAN
#
ic-infer-type0 .vs (DECLARE .mp .var .varType .do) .type
	:- ic-infer-type (.mp .var .varType, .vs) .do .type
#
ic-infer-type0 .vs (FIELD (STRUCT-OF .nts) .name .do) .type
	:- ic-infer-type .vs .do (STRUCT-OF .nts)
	, ic-field-type .nts .name .type
#
ic-infer-type0 .vs (EXTEND-SIGNED .do) I32
	:- ic-infer-type .vs .do I8
#
ic-infer-type0 .vs (IF .if .then .else) .type
	:- ic-infer-type .vs .if .ifType
	, ic-condition-type .ifType
	, ic-infer-type .vs .then .type
	, ic-infer-type .vs .else .type
#
ic-infer-type0 .vs (INDEX .type .array .index) .type
	:- ic-infer-type .vs .array (ARRAY-OF _ .type)
	, ic-infer-type .vs .index I32
#
ic-infer-type0 .vs (IN .var .type .do) .type
	:- try (ic-infer-type .vs .do .type) .ex (throw .ex "%0Aat variable" .var)
#
ic-infer-type0 .vs (INVOKE .method .ips) .returnType
	:- ic-infer-type .vs .method (METHOD-OF .pos .returnType)
	, ic-match-parameter-types .vs .ips .pos
#
ic-infer-type0 .vs (LET .var .value) .type
	:- ic-infer-type .vs .var .type
	, ic-infer-type .vs .value .type
#
ic-infer-type0 .vs (METHOD0 () .do) (METHOD0-OF () .returnType)
	:- ic-infer-type .vs .do .returnType
#
ic-infer-type0 .vs (METHOD0 (MP .io .var .paramType, .mps) .do) (METHOD0-OF (PARAM-OF .io .paramType, .pos) .returnType)
	:- ic-infer-type (MONO .var .paramType, .vs) (METHOD0 .mps .do) (METHOD0-OF .pos .returnType)
#
ic-infer-type0 .vs (METHOD .this .method) (METHOD-OF .pos .returnType)
	:- ic-infer-type .vs .this I32
	, ic-infer-type .vs .method (METHOD0-OF .pos .returnType)
#
ic-infer-type0 _ (NEW .type ()) .type
#
ic-infer-type0 .vs (NEW .type (.name .value, .nvs)) .type
	:- .type = STRUCT-OF .nts
	, ic-field-type .nts .name .t
	, ic-infer-type .vs .value .t
	, ic-infer-type .vs (NEW .type .nvs) .type
#
ic-infer-type0 _ NOP _
#
ic-infer-type0 _ NULL (POINTER-OF _)
#
ic-infer-type0 _ (NUMBER _) I32
#
ic-infer-type0 .vs (OBJECT .type .pointer) .type
	:- ic-infer-type .vs .pointer (POINTER-OF .type)
#
ic-infer-type0 .vs (OFFSET .offset .pointer) (POINTER-OF .type)
	:- ic-infer-type .vs .offset I32
	, ic-infer-type .vs .pointer (POINTER-OF .type)
#
ic-infer-type0 .vs (POST-ADD-NUMBER .pointer _) I32
	:- ic-infer-type .vs .pointer I32
#
ic-infer-type0 .vs (PRE-ADD-NUMBER .pointer _) I32
	:- ic-infer-type .vs .pointer I32
#
ic-infer-type0 .vs (REF .object) (POINTER-OF .type)
	:- ic-infer-type .vs .object .type
#
ic-infer-type0 .vs (SEQ .do0 .do1) .type
	:- ic-infer-type .vs .do0 _
	, ic-infer-type .vs .do1 .type
#
ic-infer-type0 _ (SIZE-OF _) I32
#
ic-infer-type0 .vs (SNIPPET .snippet) I32
	:- ic-infer-type .vs .snippet _
#
ic-infer-type0 _ (STRING _) I32
#
ic-infer-type0 _ THIS I32
#
ic-infer-type0 .vs (TREE .op .value0 .value1) .type
	:- once (
		member (' = ', ' != ', ' <= ', ' < ', ' >= ', ' > ',) .op, .type = BOOLEAN
		; .type = .valueType
	)
	, ic-infer-type .vs .value0 .valueType
	, ic-infer-type .vs .value1 .valueType
#
ic-infer-type0 .vs (TYPE-CAST .type .do) .type
	:- ic-infer-type .vs .do _
#
ic-infer-type0 .vs (VAR .var) .type
	:- once (member .vs (MONO .var .type)
		; member .vs (POLY .var .type0), clone .type0 .type
	)
#
ic-infer-type0 .vs (WHILE .while .do) I32
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

ic-field-type .nts .name .type
	:- once (bound .nts; ic-error "Cannot access field of unknown type")
	, member .nts (.name .type)
#

ic-condition-type .type
	:- once (.type = BOOLEAN; .type = I32; .type = METHOD0-OF _ _; .type = POINTER-OF _)
#

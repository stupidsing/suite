ic-infer-type .vs .do .typex
	:- ic-infer-type0 .vs .do .type0
	, graph.bind .type0 .typex
#
ic-infer-type _ .do .type
	:- ic-error "Cannot resolve type of" .do "to" .type
#

ic-infer-type-list _ () ()
#
ic-infer-type-list .vs (.do, .dos) (.type, .types)
	:- ic-infer-type .vs .do .type
	, ic-infer-type-list .vs .dos .types
#

ic-infer-type0 _ (ASM _) I32
#
ic-infer-type0 .vs (DECLARE .mp .var .varType .do) .type
	:- ic-infer-type (.mp .var .varType, .vs) .do .type
#
ic-infer-type0 .vs (FIELD (STRUCT-OF .nameTypes) .name .do) .type
	:- ic-infer-type .vs .do (STRUCT-OF .nameTypes)
	, once (bound .nameTypes; ic-error "Cannot access field of unknown type")
	, member .nameTypes (.name .type)
#
ic-infer-type0 .vs (IF .if .then .else) .type
	:- ic-infer-type .vs .if .ifType
	, ic-return-type .ifType
	, ic-infer-type .vs .then .type
	, ic-infer-type .vs .else .type
#
ic-infer-type0 .vs (INDEX .type .array .index) .type
	:- ic-infer-type .vs .array (ARRAY-OF _ .type)
	, ic-infer-type .vs .index I32
#
ic-infer-type0 .vs (IN .var .do) .type
	:- try (ic-infer-type .vs .do .type) .ex (throw .ex "%0Aat variable" .var)
#
ic-infer-type0 .vs (INVOKE .method .params) .returnType
	:- ic-infer-type .vs .method (METHOD-OF .paramTypes .returnType)
	, ic-infer-type-list .vs .params .paramTypes
#
ic-infer-type0 .vs (LET .var .value) .type
	:- ic-infer-type .vs .var .type
	, ic-infer-type .vs .value .type
#
ic-infer-type0 .vs (METHOD0 () .do) (METHOD0-OF () .returnType)
	:- ic-infer-type .vs .do .returnType
	, ic-return-type .returnType
#
ic-infer-type0 .vs (METHOD0 (PARAM .var .paramType, .params) .do) (METHOD0-OF (.paramType, .paramTypes) .returnType)
	:- ic-infer-type (MONO .var .paramType, .vs) (METHOD0 .params .do) (METHOD0-OF .paramTypes .returnType)
#
ic-infer-type0 .vs (METHOD .this .method) (METHOD-OF .paramTypes .returnType)
	:- ic-infer-type .vs .this I32
	, ic-infer-type .vs .method (METHOD0-OF .paramTypes .returnType)
#
ic-infer-type0 _ (NEW .type ()) .type
	:- .type = STRUCT-OF ()
#
ic-infer-type0 .vs (NEW .type (.n .v, .nvs)) .type
	:- ic-infer-type .vs .v .t
	, .type = STRUCT-OF (.n .t, .nts)
	, ic-infer-type .vs (NEW (STRUCT-OF .nts) .nvs) (STRUCT-OF .nts)
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
ic-infer-type0 .vs (SNIPPET .snippet) I32
	:- ic-infer-type .vs .snippet _
#
ic-infer-type0 _ (STRING _) I32
#
ic-infer-type0 _ THIS I32
#
ic-infer-type0 .vs (TREE .op .value0 .value1) .type
	:- once (member (' = ', ' != ',) .op; .type = I32)
	, ic-infer-type .vs .value0 .type
	, ic-infer-type .vs .value1 .type
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
	:- ic-infer-type .vs .while I32
	, ic-infer-type .vs .do _
#

ic-return-type .type
	:- once (.type = I32; .type = METHOD0-OF _ _; .type = POINTER-OF _)
#

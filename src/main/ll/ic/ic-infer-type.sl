ic-infer-type _ (ASM _) I32
#
ic-infer-type .vs (DECLARE .var .varType .do) .type
	:- ic-infer-type (.var .varType, .vs) .do .type
#
ic-infer-type .vs (FIELD (STRUCT-OF .nameTypes) .field .do) .fieldType
	:- ic-infer-type .vs .do (STRUCT-OF .nameTypes)
	, once (bound .nameTypes; ic-error "Cannot access field of unknown type")
	, member .nameTypes (.field .fieldType)
#
ic-infer-type .vs (IF .if .then .else) .type
	:- ic-infer-type .vs .if .ifType
	, once (.ifType = I32; .ifType = POINTER-OF _)
	, ic-infer-type .vs .then .type
	, ic-infer-type .vs .else .type
#
ic-infer-type .vs (INDEX .type .array .index) .type
	:- ic-infer-type .vs .array (ARRAY-OF _ .type)
	, ic-infer-type .vs .index I32
#
ic-infer-type .vs (INVOKE .method .params) .returnType
	:- ic-infer-type .vs .method (METHOD-OF .paramTypes .returnType)
	, zip .params .paramTypes .list
	, list.query .list .param:.paramType (ic-infer-type .vs .param .paramType)
#
ic-infer-type .vs (LET .var .value) I32
	:- ic-infer-type .vs .var .type
	, ic-infer-type .vs .value .type
#
ic-infer-type .vs (METHOD0 () .do) (METHOD0-OF () .returnType)
	:- ic-infer-type .vs .do .returnType
#
ic-infer-type .vs (METHOD0 (PARAM .var .paramType, .params) .do0) (METHOD0-OF (.paramType, .paramTypes) .returnType)
	:- replace (VAR .var) (OBJECT .paramType NULL) .do0 .do1
	, ic-infer-type .vs (METHOD0 .params .do1) (METHOD0-OF .paramTypes .returnType)
#
ic-infer-type .vs (METHOD .this .method) (METHOD-OF .paramTypes .returnType)
	:- ic-infer-type .vs .this I32
	, ic-infer-type .vs .method (METHOD0-OF .paramTypes .returnType)
#
ic-infer-type _ NOP _
#
ic-infer-type _ NULL _
#
ic-infer-type _ (NUMBER _) I32
#
ic-infer-type .vs (OBJECT .type .pointer) .type
	:- ic-infer-type .vs .pointer I32
#
ic-infer-type .vs (POST-ADD-NUMBER .pointer _) I32
	:- ic-infer-type .vs .pointer I32
#
ic-infer-type .vs (PRAGMA (IN .var) .do) .type
	:- try (ic-infer-type .vs .do .type) .ex (throw .ex "%0Aat variable" .var)
#
ic-infer-type .vs (PRAGMA (TYPE-CAST .type) .do) .type
	:- ic-infer-type .vs .do _
#
ic-infer-type .vs (PRE-ADD-NUMBER .pointer _) I32
	:- ic-infer-type .vs .pointer I32
#
ic-infer-type .vs (REF .object) I32
	:- ic-infer-type .vs .object _
#
ic-infer-type .vs (SEQ .do0 .do1) .type
	:- ic-infer-type .vs .do0 _
	, ic-infer-type .vs .do1 .type
#
ic-infer-type _ (SNIPPET _) I32
#
ic-infer-type _ (STRING _) I32
#
ic-infer-type _ THIS I32
#
ic-infer-type .vs (TREE _ .value0 .value1) I32
	:- ic-infer-type .vs .value0 I32
	, ic-infer-type .vs .value1 I32
#
ic-infer-type .vs (VAR .var) .type
	:- once (member .vs (.var .type))
#
ic-infer-type .vs (WHILE .while .do) I32
	:- ic-infer-type .vs .while I32
	, ic-infer-type .vs .do I32
#
ic-infer-type _ .do .type
	:- ic-error "Cannot resolve type of" .do "to" .type
#

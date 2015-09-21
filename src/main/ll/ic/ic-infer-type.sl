ic-infer-type _ (ASM _) I32
#
ic-infer-type .ts (DECLARE .varType .var .do) .type
	:- ic-infer-type (.var .varType, .ts) .do .type
#
ic-infer-type .ts (FIELD (STRUCT-OF .nameTypes) .field .do) .fieldType
	:- ic-infer-type .ts .do (STRUCT-OF .nameTypes)
	, once (bound .nameTypes; ic-error "Cannot access field of unknown type")
	, member .nameTypes (.field .fieldType)
#
ic-infer-type .ts (IF .if .then .else) .type
	:- ic-infer-type .ts .if .ifType
	, once (.ifType = I32; .ifType = POINTER-OF _)
	, ic-infer-type .ts .then .type
	, ic-infer-type .ts .else .type
#
ic-infer-type .ts (INDEX .type .array .index) .type
	:- ic-infer-type .ts .array (ARRAY-OF _ .type)
	, ic-infer-type .ts .index I32
#
ic-infer-type .ts (INVOKE .method .params) .returnType
	:- ic-infer-type .ts .method (METHOD-OF .paramTypes .returnType)
	, zip .params .paramTypes .list
	, list.query .list .param:.paramType (ic-infer-type .ts .param .paramType)
#
ic-infer-type .ts (LET .var .value) I32
	:- ic-infer-type .ts .var .type
	, ic-infer-type .ts .value .type
#
ic-infer-type .ts (METHOD0 () .do) (METHOD0-OF () .returnType)
	:- ic-infer-type .ts .do .returnType
#
ic-infer-type .ts (METHOD0 (PARAM .paramType .var, .params) .do0) (METHOD0-OF (.paramType, .paramTypes) .returnType)
	:- replace (VAR .var) (OBJECT .paramType NULL) .do0 .do1
	, ic-infer-type .ts (METHOD0 .params .do1) (METHOD0-OF .paramTypes .returnType)
#
ic-infer-type .ts (METHOD .this .method) (METHOD-OF .paramTypes .returnType)
	:- ic-infer-type .ts .this I32
	, ic-infer-type .ts .method (METHOD0-OF .paramTypes .returnType)
#
ic-infer-type _ NOP _
#
ic-infer-type _ NULL _
#
ic-infer-type _ (NUMBER _) I32
#
ic-infer-type .ts (OBJECT .type .pointer) .type
	:- ic-infer-type .ts .pointer I32
#
ic-infer-type .ts (POST-ADD-NUMBER .pointer _) I32
	:- ic-infer-type .ts .pointer I32
#
ic-infer-type .ts (PRAGMA (IN .var) .do) .type
	:- try (ic-infer-type .ts .do .type) .ex (throw .ex "%0Aat variable" .var)
#
ic-infer-type .ts (PRAGMA (TYPE-CAST .type) .do) .type
	:- ic-infer-type .ts .do _
#
ic-infer-type .ts (PRE-ADD-NUMBER .pointer _) I32
	:- ic-infer-type .ts .pointer I32
#
ic-infer-type .ts (REF .object) I32
	:- ic-infer-type .ts .object _
#
ic-infer-type .ts (SEQ .do0 .do1) .type
	:- ic-infer-type .ts .do0 _
	, ic-infer-type .ts .do1 .type
#
ic-infer-type _ (SNIPPET _) I32
#
ic-infer-type _ (STRING _) I32
#
ic-infer-type _ THIS I32
#
ic-infer-type .ts (TREE _ .value0 .value1) I32
	:- ic-infer-type .ts .value0 I32
	, ic-infer-type .ts .value1 I32
#
ic-infer-type .ts (VAR .var) .type
	:- member .ts (.var .type)
#
ic-infer-type .ts (WHILE .while .do) I32
	:- ic-infer-type .ts .while I32
	, ic-infer-type .ts .do I32
#
ic-infer-type _ .do .type
	:- ic-error "Cannot resolve type of" .do "to" .type
#

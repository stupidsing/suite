ic-infer-type (ASM _) I32
#
ic-infer-type (DECLARE .varType .var .do0) .type
	:- replace (VAR .var) (OBJECT .varType NULL) .do0 .do1
	, ic-infer-type .do1 .type
#
ic-infer-type (FIELD (STRUCT-OF .nameTypes) .field .do) .fieldType
	:- ic-infer-type .do (STRUCT-OF .nameTypes)
	, once (bound .nameTypes; ic-error "Cannot access field of unknown type")
	, member .nameTypes (.field .fieldType)
#
ic-infer-type (IF .if .then .else) .type
	:- ic-infer-type .if .type
	, ic-infer-type .then .type
	, ic-infer-type .else .type
#
ic-infer-type (INDEX .type .array .index) .type
	:- ic-infer-type .array (ARRAY-OF _ .type)
	, ic-infer-type .index I32
#
ic-infer-type (INVOKE .method .params) .returnType
	:- ic-infer-type .method (METHOD-OF .paramTypes .returnType)
	, zip .params .paramTypes .list
	, list.query .list .param:.paramType (ic-infer-type .param .paramType)
#
ic-infer-type (LET .var .value) I32
	:- ic-infer-type .var .type
	, ic-infer-type .value .type
#
ic-infer-type (METHOD0 () .do) (METHOD0-OF () .returnType)
	:- ic-infer-type .do .returnType
#
ic-infer-type (METHOD0 (PARAM .paramType .var, .params) .do0) (METHOD0-OF (.paramType, .paramTypes) .returnType)
	:- replace (VAR .var) (OBJECT .paramType NULL) .do0 .do1
	, ic-infer-type (METHOD0 .params .do1) (METHOD0-OF .paramTypes .returnType)
#
ic-infer-type (METHOD .this .method) (METHOD-OF .paramTypes .returnType)
	:- ic-infer-type .this I32
	, ic-infer-type .method (METHOD0-OF .paramTypes .returnType)
#
ic-infer-type NOP _
#
ic-infer-type NULL _
#
ic-infer-type (NUMBER _) I32
#
ic-infer-type (OBJECT .type .pointer) .type
	:- ic-infer-type .pointer I32
#
ic-infer-type (POST-ADD-NUMBER .pointer _) I32
	:- ic-infer-type .pointer I32
#
ic-infer-type (PRAGMA (IN .var) .do) .type
	:- try (ic-infer-type .do .type) .ex (throw .ex "%0Aat variable" .var)
#
ic-infer-type (PRAGMA (TYPE-CAST .type) .do) .type
	:- ic-infer-type .do _
#
ic-infer-type (PRE-ADD-NUMBER .pointer _) I32
	:- ic-infer-type .pointer I32
#
ic-infer-type (REF OBJECT _ .pointer) I32
	:- ic-infer-type .pointer I32
#
ic-infer-type (SEQ .do0 .do1) I32
	:- ic-infer-type .do0 _
	, ic-infer-type .do1 I32
#
ic-infer-type (SNIPPET _) I32
#
ic-infer-type (STRING _) I32
#
ic-infer-type THIS I32
#
ic-infer-type (TREE _ .value0 .value1) I32
	:- ic-infer-type .value0 I32
	, ic-infer-type .value1 I32
#
ic-infer-type (WHILE .while .do) I32
	:- ic-infer-type .while I32
	, ic-infer-type .do I32
#
ic-infer-type .do .type
	:- ic-error "Cannot resolve type of" .do "to" .type
#

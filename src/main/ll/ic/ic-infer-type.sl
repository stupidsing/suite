ic-infer-type $$EBP I32
#
ic-infer-type (ASM _) I32
#
ic-infer-type (DECLARE .varType .var .do0) .type
	:- replace (VAR .var) (OBJECT .varType NOP) .do0 .do1
	, ic-infer-type .do1 .type
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
ic-infer-type (INVOKE .this .sub .params) I32
	:- ic-infer-type .this I32
	, ic-infer-type .sub (METHOD-OF .paramTypes)
	, zip .params .paramTypes .list
	, list.query .list .param:.paramType (ic-infer-type .param .paramType)
#
ic-infer-type (INVOKE2 .method2 .params) I32
	:- ic-infer-type .method2 (METHOD2-OF .paramTypes)
	, zip .params .paramTypes .list
	, list.query .list .param:.paramType (ic-infer-type .param .paramType)
#
ic-infer-type (LET .var .value) .type
	:- ic-infer-type .var .type
	, ic-infer-type .value .type
#
ic-infer-type (METHOD () .do) (METHOD-OF ())
	:- ic-infer-type .do I32
#
ic-infer-type (METHOD (PARAM .paramType .var, .params) .do0) (METHOD-OF (.paramType, .paramTypes))
	:- replace (VAR .var) (OBJECT .paramType NOP) .do0 .do1
	, ic-infer-type (METHOD .params .do1) (METHOD-OF .paramTypes)
#
ic-infer-type (METHOD2 .this .method) (METHOD2-OF .paramTypes)
	:- ic-infer-type .this I32
	, ic-infer-type .method (METHOD-OF .paramTypes)
#
ic-infer-type (NUMBER _) I32
#
ic-infer-type NOP I32
#
ic-infer-type (OBJECT .type .pointer) .type
	:- ic-infer-type .pointer I32
#
ic-infer-type (POST-ADD-NUMBER .pointer _) I32
	:- ic-infer-type .pointer I32
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
ic-infer-type (TREE _ .value0 .value1) I32
	:- ic-infer-type .value0 I32
	, ic-infer-type .value1 I32
#
ic-infer-type (WHILE .while .do) I32
	:- ic-infer-type .while I32
	, ic-infer-type .do I32
#
ic-infer-type .do _
	:- ic-error "Cannot resolve type of" .do
#

ic-infer-type $$EBP I32
#
ic-infer-type (ASM _) I32
#
ic-infer-type (DECLARE .type _ _) .type
#
ic-infer-type (INVOKE .this .sub .params) I32
	:- ic-infer-type .this I32
	, ic-infer-type .sub (METHOD-OF .types)
	, zip .params .types .list
	, list.query .list .param:.type (ic-infer-type .param .type)
#
ic-infer-type (INVOKE2 .method2 .params) I32
	:- ic-infer-type .method2 (METHOD2-OF .types)
	, zip .params .types .list
	, list.query .list .param:.type (ic-infer-type .param .type)
#
ic-infer-type (IF .if .then .else) .type
	:- ic-infer-type .if .type
	, ic-infer-type .then .type
	, ic-infer-type .else .type
#
ic-infer-type (LET .var .value) .type
	:- ic-infer-type .var .type
	, once (ic-infer-type .value .type
		; ic-error "in" .var
	)
#
ic-infer-type (METHOD .params .do) (METHOD-OF .paramTypes)
	:- ic-infer-type .do I32
	, zip .params .paramTypes .list
	, list.query .list (PARAM .type _):.type ()
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
ic-infer-type (POST-ADD-NUMBER .pointer .i) I32
	:- ic-infer-type .pointer I32
	, ic-infer-type .i I32
#
ic-infer-type (PRE-ADD-NUMBER .pointer .i) I32
	:- ic-infer-type .pointer I32
	, ic-infer-type .i I32
#
ic-infer-type (REF OBJECT _ .pointer) I32
	:- ic-infer-type .pointer I32
#
ic-infer-type (SEQ _ .do1) I32
	:- ic-infer-type .do1 I32
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
	:- ic-error "Cannot resolve types" .do
#

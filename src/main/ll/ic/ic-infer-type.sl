ic-infer-type $$EBP I32
#
ic-infer-type (ALLOC .type _ _) .type
#
ic-infer-type (ASM .i) I32
#
ic-infer-type (INVOKE .this .sub .actualParams) I32
	:- ic-infer-type .this I32
	, ic-infer-type .sub (METHOD-OF .declaredParams)
	, ic-infer-parameter-type .actualParams (METHOD-OF .declaredParams)
#
ic-infer-type (IF .if .then .else) .type
	:- ic-infer-type .then .type
	, ic-infer-type .else .type
#
ic-infer-type (LET .var .value) .type
	:- ic-infer-type .var .type
	, ic-infer-type .value .type
#
ic-infer-type (MEMORY .type _) .type
#
ic-infer-type (METHOD .params .do) (METHOD-OF .params)
	:- ic-infer-type .do I32
#
ic-infer-type (NUMBER .i) I32
#
ic-infer-type NOP I32
#
ic-infer-type (POST-ADD-NUMBER .pointer .i) I32
	:- ic-infer-type .pointer I32
	, ic-infer-type .i I32
#
ic-infer-type (PRE-ADD-NUMBER .pointer .i) I32
	:- ic-infer-type .pointer I32
	, ic-infer-type .i I32
#
ic-infer-type (REF MEMORY _ .pointer) I32
	:- ic-infer-type .pointer I32
#
ic-infer-type (SEQ .do0 .do1) I32
	:- ic-infer-type .do1 I32
#
ic-infer-type (SNIPPET .snippet) I32
#
ic-infer-type (STRING .s) I32
#
ic-infer-type (TREE .op .value0 .value1) I32
#
ic-infer-type (WHILE .while .do) I32
	:- ic-infer-type .while I32
	, ic-infer-type .do I32
#

ic-infer-parameter-type () ()
#
ic-infer-parameter-type (.p, .ps) (PARAM .type _, .params)
	:- ic-infer-type .p .type
	, ic-infer-parameter-type .ps .params
#

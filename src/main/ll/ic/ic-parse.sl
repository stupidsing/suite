ic-parse (asm .i) (ASM .i)
	:- ! -- Assembler might have variables, skip processing
#
ic-parse (declare .var = .value; .do) (DECLARE .var _ (SEQ (LET (VAR .var) (PRAGMA (IN .var) .value1)) .do1))
	:- !
	, is.atom .var
	, ic-parse .value .value1
	, ic-parse .do .do1
#
ic-parse .do0 .parsed :- ic-parse-better-option .do0 .parsed, !
#
ic-parse .do0 .parsed
	:- ic-parse-sugar .do0 .do1
	, ic-parse .do1 .parsed
#
ic-parse (declare .var as .t; .do) (DECLARE .var .type .do1)
	:- is.atom .var
	, ic-parse .do .do1
	, ic-parse-type .t .type
#
ic-parse (declare .var; .do) (DECLARE .var _ .do1)
	:- is.atom .var
	, ic-parse .do .do1
#
ic-parse (.do^.field) (FIELD _ .field .do1)
	:- ic-parse .do .do1
#
ic-parse (if .if then .then else .else) (IF .if1 .then1 .else1)
	:- ic-parse .if .if1
	, ic-parse .then .then1
	, ic-parse .else .else1
#
ic-parse .array:.index (INDEX _ .array1 .index1)
	:- ic-parse .array .array1
	, ic-parse .index .index1
#
ic-parse (.sub [.params]) (INVOKE .sub1 .params1) -- Traditional subroutine invocation
	:- ic-parse .sub .sub1
	, zip .params .params1 .list
	, list.query .list .param:.param1 (ic-parse .param .param1)
#
ic-parse (let .var = .value) (LET .var1 .value1)
	:- ic-parse .var .var1
	, ic-parse .value .value1
#
ic-parse (baseless [.params] .do) (METHOD0 .params1 .do1) -- Traditional subroutine definition
	:- zip .params .params1 .list
	, list.query .list .param:.param1 (ic-parse-parameter .param .param1)
	, ic-parse .do .do1
#
ic-parse (function [.params] .do) (METHOD THIS .method) -- Traditional subroutine definition
	:- ic-parse (baseless [.params] .do) .method
#
ic-parse () NOP
#
ic-parse null NULL
#
ic-parse .i (NUMBER .i)
	:- is.int .i
#
ic-parse `.pointer` (OBJECT _ .pointer1)
	:- ic-parse .pointer .pointer1
#
ic-parse (no-type .do) (PRAGMA (TYPE-CAST _) .do1)
	:- ic-parse .do .do1
#
ic-parse (.do as .t) (PRAGMA (TYPE-CAST .type) .do1)
	:- ic-parse-type .t .type
	, ic-parse .do .do1
#
ic-parse (& .var) (REF .var1)
	:- ic-parse .var .var1
#
ic-parse (.do0;) .parsed
	:- ic-parse .do0 .parsed
#
ic-parse (.do0; .do1) (SEQ .parsed0 .parsed1)
	:- not (.do0 = constant _ = _; .do0 = declare _; .do0 = declare _ = _; .do0 = declare-pointer _)
	, ic-parse .do0 .parsed0
	, ic-parse .do1 .parsed1
#
ic-parse (snippet .snippet) (SNIPPET .snippet1)
	:- ic-parse .snippet .snippet1
#
ic-parse .s (STRING .s)
	:- is.string .s
#
ic-parse this THIS
#
ic-parse .expr (TREE .op .expr0 .expr1)
	:- (tree .expr .value0 .op .value1; .expr = .value0 .op .value1)
	, ic-operator .op _
	, ic-parse .value0 .expr0
	, ic-parse .value1 .expr1
#
ic-parse .var (VAR .var)
	:- is.atom .var
#
ic-parse (while .while do .do) (WHILE .while1 .do1)
	:- ic-parse .while .while1
	, ic-parse .do .do1
#
ic-parse .do _
	:- ic-error "Unknown expression" .do
#

ic-parse-better-option (.var =+ .i) .do
	:- is.int .i, ic-parse .var .var1, .do = PRE-ADD-NUMBER .var1 .i
#
ic-parse-better-option (.var += .i) .do
	:- is.int .i, ic-parse .var .var1, .do = POST-ADD-NUMBER .var1 .i
#

ic-parse-sugar (.a && .b) (if .a then .b else 0)
#
ic-parse-sugar (.a || .b) (if .a then 1 else .b)
#
ic-parse-sugar (.var =+ .inc) (declare .p as int = & .var; declare .o = `.p`; let `.p` = .o + .inc; .o)
	:- temp .p, temp .o
#
ic-parse-sugar (.var += .inc) (declare .p as int = & .var; let `.p` = `.p` + .inc)
	:- temp .p
#
ic-parse-sugar (constant .var = .value; .do) .do1
	:- generalize (.var .value) (.var1 .value1)
	, rewrite .var1 .value1 .do .do1
#
ic-parse-sugar (declare-pointer .var to .type; .do) (declare .mem as .type; declare .var = & .mem; .do)
	:- is.atom .var, temp .mem
#
ic-parse-sugar false 0
#
ic-parse-sugar (for (.init; .cond; .step) .do) (.init; while .cond do (.do; .step))
#
ic-parse-sugar (not .b) (if .b then 0 else 1)
#
ic-parse-sugar (replace .var = .value; .do) .do1
	:- replace .var .value1 .value .value1
	, replace .var .value1 .do .do1
#
ic-parse-sugar (var .var = .value; .do) (var .var; let .var = .value; .do)
	:- is.atom .var
#
ic-parse-sugar true 1
#

ic-parse-parameter (.param as .t) (PARAM .type .param)
	:- ic-parse-type .t .type
#
ic-parse-parameter .p .param
	:- not (.p = _/_), .param = PARAM I32 .p
#

ic-parse-type (.tv0 => .type0) .typex
	:- ic-parse-type .tv0 .tv1
	, ic-parse-type .type0 .type1
	, replace .tv1 _ .type1 .typex
#
ic-parse-type (fix .tv0 .type0) .typex
	:- ic-parse-type .tv0 .tv1
	, ic-parse-type .type0 .type1
	, replace .tv1 .typex .type1 .typex
#
ic-parse-type (.t * .size) (ARRAY-OF .size .type)
	:- ic-parse-type .t .type
#
ic-parse-type int I32
#
ic-parse-type byte I8
#
ic-parse-type (baseless [.ts] .rt) (METHOD0-OF .types .returnType)
	:- zip .ts .types .list
	, list.query .list .t:.type (ic-parse-type .t .type)
	, ic-parse-type .rt .returnType
#
ic-parse-type (function .m .rt) (METHOD-OF .types .returnType)
	:- ic-parse-type (baseless .m .rt) (METHOD0-OF .types .returnType)
#
ic-parse-type pointer:.t (POINTER-OF .type)
	:- ic-parse-type .t .type
#
ic-parse-type (struct .nts) (STRUCT-OF .nameTypes)
	:- zip .nts .nameTypes .list
	, list.query .list (.name as .t):(.name .type) (ic-parse-type .t .type)
#
ic-parse-type :.typeVar (VAR .typeVar)
#

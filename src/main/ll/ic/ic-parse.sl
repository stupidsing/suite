ic-parse (asm .i) (ASM .i)
	:- ! -- Assembler might have variables, skip processing
#
ic-parse .do0 .parsed
	:- ic-parse-sugar .do0 .do1
	, ic-parse .do1 .parsed
#
ic-parse this $$EBP
#
ic-parse (allocate .var/.size; .do) (ALLOC .size .var .do1)
	:- is.atom .var
	, ic-parse .do .do1
#
ic-parse (copy/.size .target .source) (COPY .size .target1 .source1)
	:- ic-parse .source .source1
	, ic-parse .target .target1
#
ic-parse (if .if then .then else .else) (IF .if1 .then1 .else1)
	:- ic-parse .if .if1
	, ic-parse .then .then1
	, ic-parse .else .else1
#
ic-parse (.this:.sub [.params]) (INVOKE .this1 .sub1 .params1) -- Traditional subroutine invocation
	:- ic-parse .this .this1
	, ic-parse .sub .sub1
	, list.query2 .params .params1 .param .param1 (ic-parse .param .param1)
#
ic-parse () NOP
#
ic-parse .i (NUMBER .i)
	:- is.int .i
#
ic-parse (let .var = .value) (LET .var1 .value1)
	:- ic-parse .var .var1
	, ic-parse .value .value1
#
ic-parse `.pointer` (MEMORY 4 .pointer1)
	:- ic-parse .pointer .pointer1
#
ic-parse ([.params] .do) (METHOD .params1 .do1) -- Traditional subroutine definition
	:- list.query2 .params .params1 .param .param1 (ic-parse .param .param1)
	, ic-parse .do .do1
#
ic-parse (.var =+ .i) (PRE-ADD-NUMBER .var1 .i)
	:- is.int .i, ic-parse .var .var1
#
ic-parse (.var += .i) (POST-ADD-NUMBER .var1 .i)
	:- is.int .i, ic-parse .var .var1
#
ic-parse (& .var) (REF .var1)
	:- ic-parse .var .var1
#
ic-parse (.do0; .do1) (SEQ .parsed0 .parsed1)
	:- not (.do0 = allocate _; .do0 = constant _ = _; .do0 = declare _; .do0 = declare _ = _)
	, ic-parse .do0 .parsed0
	, ic-parse .do1 .parsed1
#
ic-parse (snippet .snippet) (SNIPPET .snippet1)
	:- ic-parse .snippet .snippet1
#
ic-parse .s (STRING .s)
	:- is.string .s
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

ic-parse-sugar (.a && .b) (if .a then .b else 0)
#
ic-parse-sugar (.a || .b) (if .a then 1 else .b)
#
ic-parse-sugar (.var =+ .inc) (declare .p = & .var; declare .o = `.p`; let `.p` = .o + .inc; .o)
	:- temp .p, temp .o
#
ic-parse-sugar (.var += .inc) (declare .p = & .var; let `.p` = `.p` + .inc)
	:- temp .p
#
ic-parse-sugar (constant .var = .value; .do) .do1
	:- generalize (.var .value) (.var1 .value1)
	, rewrite .var1 .value1 .do .do1
#
ic-parse-sugar (declare .var; .do) (allocate .var/4; .do)
	:- is.atom .var
#
ic-parse-sugar (declare .var = .value; .do) (declare .var; let .var = .value; .do)
	:- is.atom .var
#
ic-parse-sugar false 0
#
ic-parse-sugar (for (.init; .cond; .step) .do) (.init; while .cond do (.do; .step))
#
ic-parse-sugar (not .b) (if .b then 0 else 1)
#
ic-parse-sugar true 1
#

ic-parse-type int I32
#
ic-parse-type [.t] (ARRAY-OF .type)
	:- ic-parse-type .t .type
#
ic-parse-type (p^.t) (PTR-OF .type)
	:- ic-parse-type .t .type
#
ic-parse-type (.name {.ts}) (TUPLE-OF .name .types)
	:- list.query2 .ts .types .t .type (fc-parse .t .type)
#

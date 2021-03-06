ic-parse (asm .i) (ASM .i)
	:- ! -- assembler might have variables, skip processing
#
ic-parse .do0 .parsed
	:- ic-parse-better-option .do0 .parsed, !
#
ic-parse .do0 .parsed
	:- ic-parse-sugar .do0 .do1
	, ic-parse .do1 .parsed
#
ic-parse (array .array0) (ARRAY _ .array1)
	:- zip .array0 .array1 .list
	, list.query .list .elem0:.elem1 (ic-parse .elem0 .elem1)
#
ic-parse false (BOOLEAN 0)
#
ic-parse true (BOOLEAN 1)
#
ic-parse (declare .t .var = .value; .do) (DECLARE POLY .var .type (SEQ (LET (VAR .var) (IN .var .type .value1)) .do1))
	:- is.atom .var
	, !
	, try (ic-parse .value .value1) .ex (throw .ex "%0Aat variable" .var)
	, ic-parse .do .do1
	, ic-parse-type .t .type
#
ic-parse (declare .var = .value; .do) (DECLARE MONO .var .type (SEQ (LET (VAR .var) (IN .var .type .value1)) .do1))
	:- is.atom .var
	, !
	, try (ic-parse .value .value1) .ex (throw .ex "%0Aat variable" .var)
	, ic-parse .do .do1
#
ic-parse (declare .var; .do) (DECLARE MONO .var _ .do1)
	:- is.atom .var
	, ic-parse .do .do1
#
ic-parse (signature .var = .t; .do) (DECLARE MONO .var .type .do1)
	:- is.atom .var
	, ic-parse .do .do1
	, ic-parse-type .t .type
#
ic-parse (extend .do) (EXTEND-SIGNED .do1)
	:- ic-parse .do .do1
#
ic-parse .do/.name (FIELD _ .name .do1)
	:- is.atom .name
	, not (.name = *)
	, ic-parse .do .do1
#
ic-parse (if .if then .then else .else) (IF .if1 .then1 .else1)
	:- ic-parse .if .if1
	, ic-parse .then .then1
	, ic-parse .else .else1
#
ic-parse (if-bind (.v0 := .v1) then .then else .else) .parsed
	:- !
	, ic-parse .v0 .vp0
	, ic-parse .v1 .vp1
	, ic-parse .then .thenp
	, ic-parse .else .elsep
	, ic-bind .vp0 .vp1 .thenp .elsep .parsed
#
ic-parse .array/:.index (INDEX _ .array1 .index1)
	:- ic-parse .array .array1
	, ic-parse .index .index1
#
ic-parse (.sub [.params]) (INVOKE .sub1 .ips) -- traditional subroutine invocation
	:- ic-parse .sub .sub1
	, zip .params .ips .list
	, list.query .list .param:.ip (ic-parse-invoke-parameter .param .ip)
#
ic-parse ({.var} = .value) (LET .var1 .value1)
	:- ic-parse .var .var1
	, ic-parse .value .value1
#
ic-parse (baseless [.params] .do) (METHOD0 .mps .do1) -- traditional subroutine definition
	:- zip .params .mps .list
	, list.query .list .param:.mp (ic-parse-method-parameter .param .mp)
	, ic-parse .do .do1
#
ic-parse (function [.params] .do) (METHOD THIS .method) -- traditional subroutine definition
	:- ic-parse (baseless [.params] .do) .method
#
ic-parse (new .type .nvs0) (NEW-STRUCT .type1 .nvs1)
	:- ic-parse-type .type .type1
	, zip .nvs0 .nvs1 .list
	, list.query .list (.n = .v0):(.n .v1) (ic-parse .v0 .v1)
#
ic-parse (newt .type .tag .value) (NEW-TAG .type1 .tag1 .value1)
	:- ic-parse .tag .tag1
	, ic-parse-type .type .type1
	, ic-parse .value .value1
#
ic-parse () NOP
#
ic-parse null NULL
#
ic-parse .i (NUMBER .i)
	:- is.int .i
#
ic-parse .pointer/* (OBJECT _ .pointer1)
	:- ic-parse .pointer .pointer1
#
ic-parse (.pointer +offset .offset) (OFFSET .offset1 .pointer1)
	:- ic-parse .pointer .pointer1
	, ic-parse .offset .offset1
#
ic-parse .v (PRAGMA NEW (VAR .nv))
	:- to.string .v "_", temp .nv
#
ic-parse .v (PRAGMA NEW (VAR .nv))
	:- ic-parse-bind-variable .v .nv
#
ic-parse (& .var) (REF .var1)
	:- ic-parse .var .var1
#
ic-parse (.do;) .parsed
	:- ic-parse .do .parsed
#
ic-parse (.do0; .do1) (SEQ .parsed0 .parsed1)
	:- not (.do0 = constant _ = _; .do0 = declare _; .do0 = declare _ = _; .do0 = signature _ = _)
	, ic-parse .do0 .parsed0
	, ic-parse .do1 .parsed1
#
ic-parse (size-of .type) (SIZE-OF .type1)
	:- ic-parse-type .type .type1
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
	, ic-operator .op _ _
	, ic-parse .value0 .expr0
	, ic-parse .value1 .expr1
#
ic-parse (no-type .do) (TYPE-CAST _ .do1)
	:- ic-parse .do .do1
#
ic-parse (.do as .t) (TYPE-CAST .type .do1)
	:- ic-parse-type .t .type
	, ic-parse .do .do1
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

ic-parse-sugar (.a | .b [.list]) (.b [.a, .list])
#
ic-parse-sugar (.a && .b) (if .a then .b else false)
#
ic-parse-sugar (.a || .b) (if .a then true else .b)
#
ic-parse-sugar (.p +f .f) (& .p/*/.f)
#
ic-parse-sugar (.var =+ .inc) (declare .p = & .var; declare .o = .p/*; {.p/*} = .o + .inc; .o)
	:- temp .p, temp .o
#
ic-parse-sugar (.var += .inc) (declare .p = & .var; {.p/*} = .p/* + .inc)
	:- temp .p
#
ic-parse-sugar (address .value) ((& .value) as int)
#
ic-parse-sugar (constant .var = .value; .do) .do1
	:- generalize (.var .value) (.var1 .value1)
	, rewrite .var1 .value1 .do .do1
#
ic-parse-sugar (for (.init; .cond; .step) do .do) (.init; while .cond do (.do; .step))
#
ic-parse-sugar (for .var in (.start, .end) do .do) (declare .var = .start; while (.var < .end) do (.do; .var += 1))
#
ic-parse-sugar (fn [.ps] .do) (function [out result, .ps] .do)
#
ic-parse-sugar (new ()) (new (struct ()) ())
#
ic-parse-sugar (new (.name = .value, .nvs0)) (new (struct (.tns | .type .name)) (.name = .value, .nvs1))
	:- to.atom "_" .type
	, ic-parse-sugar (new .nvs0) (new (struct .tns) .nvs1)
#
ic-parse-sugar (not .b) (if .b then (no-type false) else (no-type true))
#
ic-parse-sugar (return .e) ({result} = .e)
#
ic-parse-sugar (var .var = .value; .do) (var .var; {.var} = .value; .do)
	:- is.atom .var
#

ic-parse-invoke-parameter (out .param) .ip
	:- ic-parse .param .param1
	, .ip = IP OUT .param1
#
ic-parse-invoke-parameter .param .ip
	:- ic-parse .param .param1
	, .ip = IP IN .param1
#

ic-parse-method-parameter (out .param) (MP OUT .param _)
#
ic-parse-method-parameter (out .t .param) (MP OUT .param .type)
	:- ic-parse-type .t .type
#
ic-parse-method-parameter (.t .param) (MP IN .param .type)
	:- ic-parse-type .t .type
#
ic-parse-method-parameter .p .mp
	:- not (.p = _/_), .mp = MP IN .p _
#

ic-parse-type .t _
	:- to.string .t "_"
#
ic-parse-type (.tv0 => .type0) .typex
	:- ic-parse-type .tv0 .tv1
	, ic-parse-type .type0 .type1
	, graph.replace .tv1 _ .type1 .typex
#
ic-parse-type (fix .tv0 .type0) .typex
	:- ic-parse-type .tv0 .tv1
	, ic-parse-type .type0 .type1
	, graph.replace .tv1 .typex .type1 .typex
#
ic-parse-type (.t * .size) (ARRAY-OF _ .type)
	:- to.string .size "_"
	, ic-parse-type .t .type
#
ic-parse-type (.t * .size) (ARRAY-OF .size .type)
	:- ic-parse-type .t .type
#
ic-parse-type boolean BOOLEAN
#
ic-parse-type int I32
#
ic-parse-type byte I8
#
ic-parse-type (baseless [.ts] .rt) (METHOD0-OF .pos .returnType)
	:- zip .ts .pos .list
	, list.query .list .t:(PARAM-OF IN .type) (ic-parse-type .t .type)
	, ic-parse-type .rt .returnType
#
ic-parse-type (function .m .rt) (METHOD-OF .pos .returnType)
	:- ic-parse-type (baseless .m .rt) (METHOD0-OF .pos .returnType)
#
ic-parse-type pointer:.t (POINTER-OF .type)
	:- ic-parse-type .t .type
#
ic-parse-type (struct ()) (STRUCT-OF ())
#
ic-parse-type (struct (.nts | .t .name)) (STRUCT-OF (.nameTypes | .name .type))
	:- ic-parse-type .t .type
	, ic-parse-type (struct .nts) (STRUCT-OF .nameTypes)
#
ic-parse-type (tag ()) (TAG-OF ())
#
ic-parse-type (tag (.tts | .tag .t)) (TAG-OF (.tagTypes | .tag1 .type))
	:- ic-parse-type .t .type
	, ic-parse .tag .tag1
	, ic-parse-type (tag .tts) (TAG-OF .tagTypes)
#
ic-parse-type :.typeVar .typeVar
#
ic-parse-type .t _
	:- ic-error "Unknown type" .t
#

ic-parse-bind-variable .v .vd
	:- is.atom .v, to.string .v .s0, substring .s0 0 1 "$"
	, !, substring .s0 1 0 .s1, to.atom .s1 .vd
#

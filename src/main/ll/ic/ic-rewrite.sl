ic-rewrite $$EBP $$EBP .ts/.ts
#
ic-rewrite (ASM .i) (ASM .i) .ts/.ts
#
ic-rewrite (ALLOC .size .var .do0) (ALLOC .size .var .do1) (.do0 .do1, .ts)/.ts
#
ic-rewrite (DECLARE .type0 .var .do0) (DECLARE .type1 .var .do1) (.do0 .do1, .ts)/.ts
	:- ic-rewrite-type .type0 .type1
#
ic-rewrite (INDEX .type0 .array0 .index0) (INDEX .type1 .array1 .index1) (.array0 .array1, .index0 .index1, .ts)/.ts
	:- ic-rewrite-type .type0 .type1
#
ic-rewrite (IF .if0 .then0 .else0) (IF .if1 .then1 .else1) (.if0 .if1, .then0 .then1, .else0 .else1, .ts)/.ts
#
ic-rewrite (INVOKE .this0 .sub0 .params0) (INVOKE .this1 .sub1 .params1) (.this0 .this1, .sub0 .sub1, .ts0)/.tsx
	:- ic-rewrite-list .params0 .params1 .ts0/.tsx
#
ic-rewrite (INVOKE2 .sub0 .params0) (INVOKE2 .sub1 .params1) (.sub0 .sub1, .ts0)/.tsx
	:- ic-rewrite-list .params0 .params1 .ts0/.tsx
#
ic-rewrite (LET .var0 .value0) (LET .var1 .value1) (.var0 .var1, .value0 .value1, .ts)/.ts
#
ic-rewrite (MEMORY .size .pointer0) (MEMORY .size .pointer1) (.pointer0 .pointer1, .ts)/.ts
#
ic-rewrite (METHOD .params0 .do0) (METHOD .params1 .do1) (.do0 .do1, .ts)/.ts
	:- zip .params0 .params1 .list, list.query .list .param0:.param1 (ic-rewrite-parameter .param0 .param1)
#
ic-rewrite (METHOD2 .this0 .method0) (METHOD2 .this1 .method1) (.this0 .this1, .method0 .method1, .ts)/.ts
#
ic-rewrite NOP NOP .ts/.ts
#
ic-rewrite (NUMBER .i) (NUMBER .i) .ts/.ts
#
ic-rewrite (OBJECT .type0 .pointer0) (OBJECT .type1 .pointer1) (.pointer0 .pointer1, .ts)/.ts
	:- ic-rewrite-type .type0 .type1
#
ic-rewrite (POST-ADD-NUMBER .var0 .i) (POST-ADD-NUMBER .var1 .i) (.var0 .var1, .ts)/.ts
#
ic-rewrite (PRAGMA _ .do0) (PRAGMA _ .do1) (.do0 .do1, .ts)/.ts
#
ic-rewrite (PRE-ADD-NUMBER .var0 .i) (PRE-ADD-NUMBER .var1 .i) (.var0 .var1, .ts)/.ts
#
ic-rewrite (REF .var0) (REF .var1) (.var0 .var1, .ts)/.ts
#
ic-rewrite (REG .reg) (REG .reg) .ts/.ts
#
ic-rewrite (SEQ .a0 .b0) (SEQ .a1 .b1) (.a0 .a1, .b0 .b1, .ts)/.ts
#
ic-rewrite (SNIPPET .s0) (SNIPPET .s1) (.s0 .s1, .ts)/.ts
#
ic-rewrite (STRING .s) (STRING .s) .ts/.ts
#
ic-rewrite (TREE .op .a0 .b0) (TREE .op .a1 .b1) (.a0 .a1, .b0 .b1, .ts)/.ts
#
ic-rewrite (VAR .var) (VAR .var) .ts/.ts
#
ic-rewrite (WHILE .while0 .do0) (WHILE .while1 .do1) (.while0 .while1, .do0 .do1, .ts)/.ts
#

ic-rewrite-list () () .ts/.ts #
ic-rewrite-list (.p0, .ps0) (.px, .psx) (.p0 .px, .ts0)/.tsx :- ic-rewrite-list .ps0 .psx .ts0/.tsx #

ic-rewrite-parameter .param0 .param1
	:- .param0 = PARAM .type0 .var
	, .param1 = PARAM .type1 .var
	, ic-rewrite-type .type0 .type1
#
ic-rewrite-parameter .param0 .param1
	:- .param0 = PS .size .var
	, .param1 = PS .size .var
#

ic-rewrite-type I8 I8
#
ic-rewrite-type I32 I32
#
ic-rewrite-type (ARRAY-OF .size .type0) (ARRAY-OF .size .type1)
	:- ic-rewrite-type .type0 .type1
#
ic-rewrite-type (METHOD-OF .types0) (METHOD-OF .types1)
	:- zip .types0 .types1 .list
	, list.query .list .type0:.type1 (ic-rewrite-type .type0 .type1)
#
ic-rewrite-type (PTR-OF .type0) (PTR-OF .type1)
	:- ic-rewrite-type .type0 .type1
#
ic-rewrite-type (TUPLE-OF .name ()) (TUPLE-OF .name ())
#
ic-rewrite-type (TUPLE-OF .name (.type0, .types0)) (TUPLE-OF .name (.type1, .types1))
	:- ic-rewrite-type .type0 .type1
	, ic-rewrite-type (TUPLE-OF .name .types0) (TUPLE-OF .name .types1)
#

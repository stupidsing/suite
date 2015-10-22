ic-rewrite (ASM .i) (ASM .i) .ts/.ts
#
ic-rewrite (DECLARE .mp .var .type0 .do0) (DECLARE .mp .var .type1 .do1) (.do0 .do1, .ts)/.ts
	:- member (MONO, POLY,) .mp
	, ic-rewrite-type .type0 .type1
#
ic-rewrite (DECLARES .var .offset .size .do0) (DECLARES .var .offset .size .do1) (.do0 .do1, .ts)/.ts
#
ic-rewrite (FIELD .type0 .name .do0) (FIELD .type1 .name .do1) (.do0 .do1, .ts)/.ts
	:- ic-rewrite-type .type0 .type1
#
ic-rewrite (INDEX .type0 .array0 .index0) (INDEX .type1 .array1 .index1) (.array0 .array1, .index0 .index1, .ts)/.ts
	:- ic-rewrite-type .type0 .type1
#
ic-rewrite (IF .if0 .then0 .else0) (IF .if1 .then1 .else1) (.if0 .if1, .then0 .then1, .else0 .else1, .ts)/.ts
#
ic-rewrite (IN .var .do0) (IN .var .do1) (.do0 .do1, .ts)/.ts
#
ic-rewrite (INVOKE .sub0 .params0) (INVOKE .sub1 .params1) (.sub0 .sub1, .ts0)/.tsx
	:- ic-rewrite-list .params0 .params1 .ts0/.tsx
#
ic-rewrite (LET .var0 .value0) (LET .var1 .value1) (.var0 .var1, .value0 .value1, .ts)/.ts
#
ic-rewrite (MEMORY .size .pointer0) (MEMORY .size .pointer1) (.pointer0 .pointer1, .ts)/.ts
#
ic-rewrite (METHOD0 .params0 .do0) (METHOD0 .params1 .do1) (.do0 .do1, .ts)/.ts
	:- zip .params0 .params1 .list, list.query .list .param0:.param1 (ic-rewrite-parameter .param0 .param1)
#
ic-rewrite (METHOD .this0 .method0) (METHOD .this1 .method1) (.this0 .this1, .method0 .method1, .ts)/.ts
#
ic-rewrite (NEW .type0 ()) (NEW .type1 ()) .ts/.ts
	:- ic-rewrite-type .type0 .type1
#
ic-rewrite (NEW .type0 (.k .v0, .nvs0)) (NEW .type1 (.k .v1, .nvs1)) (.v0 .v1, .ts0)/.tsx
	:- ic-rewrite (NEW .type0 .nvs0) (NEW .type1 .nvs1) .ts0/.tsx
#
ic-rewrite (NEWS ()) (NEWS ()) .ts/.ts
#
ic-rewrite (NEWS (.o .s .v0, .osvs0)) (NEWS (.o .s .v1, .osvs1)) (.v0 .v1, .ts0)/.tsx
	:- ic-rewrite (NEWS .osvs0) (NEWS .osvs1) .ts0/.tsx
#
ic-rewrite NOP NOP .ts/.ts
#
ic-rewrite NULL NULL .ts/.ts
#
ic-rewrite (NUMBER .i) (NUMBER .i) .ts/.ts
#
ic-rewrite (OBJECT .type0 .pointer0) (OBJECT .type1 .pointer1) (.pointer0 .pointer1, .ts)/.ts
	:- ic-rewrite-type .type0 .type1
#
ic-rewrite (OFFSET .offset0 .pointer0) (OFFSET .offset1 .pointer1) (.offset0 .offset1, .pointer0 .pointer1, .ts)/.ts
#
ic-rewrite (POST-ADD-NUMBER .var0 .i) (POST-ADD-NUMBER .var1 .i) (.var0 .var1, .ts)/.ts
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
ic-rewrite THIS THIS .ts/.ts
#
ic-rewrite (TREE .op .a0 .b0) (TREE .op .a1 .b1) (.a0 .a1, .b0 .b1, .ts)/.ts
#
ic-rewrite (TYPE-CAST .type0 .do0) (TYPE-CAST .type1 .do1) (.do0 .do1, .ts)/.ts
	:- ic-rewrite-type .type0 .type1
#
ic-rewrite (VAR .var) (VAR .var) .ts/.ts
#
ic-rewrite (WHILE .while0 .do0) (WHILE .while1 .do1) (.while0 .while1, .do0 .do1, .ts)/.ts
#

ic-rewrite-list () () .ts/.ts #
ic-rewrite-list (.p0, .ps0) (.px, .psx) (.p0 .px, .ts0)/.tsx :- ic-rewrite-list .ps0 .psx .ts0/.tsx #

ic-rewrite-parameter .param0 .param1
	:- .param0 = PARAM .var .type0
	, .param1 = PARAM .var .type1
	, ic-rewrite-type .type0 .type1
#
ic-rewrite-parameter .param0 .param1
	:- .param0 = PS .var .size
	, .param1 = PS .var .size
#

ic-rewrite-type I8 I8
#
ic-rewrite-type I32 I32
#
ic-rewrite-type (ARRAY-OF .size .type0) (ARRAY-OF .size .type1)
	:- ic-rewrite-type .type0 .type1
#
ic-rewrite-type (METHOD0-OF .types0 .returnType0) (METHOD0-OF .types1 .returnType1)
	:- zip .types0 .types1 .list
	, list.query .list .type0:.type1 (ic-rewrite-type .type0 .type1)
	, ic-rewrite-type .returnType0 .returnType1
#
ic-rewrite-type (METHOD-OF .types0 .returnType0) (METHOD-OF .types1 .returnType1)
	:- zip .types0 .types1 .list
	, list.query .list .type0:.type1 (ic-rewrite-type .type0 .type1)
	, ic-rewrite-type .returnType0 .returnType1
#
ic-rewrite-type (POINTER-OF .type0) (POINTER-OF .type1)
	:- ic-rewrite-type .type0 .type1
#
ic-rewrite-type (STRUCT-OF ()) (STRUCT-OF ())
#
ic-rewrite-type (STRUCT-OF (.name .type0, .nameTypes0)) (STRUCT-OF (.name .type1, .nameTypes1))
	:- ic-rewrite-type .type0 .type1
	, ic-rewrite-type (STRUCT-OF .nameTypes0) (STRUCT-OF .nameTypes1)
#

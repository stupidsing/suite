ic-rewrite (ASM .i) (ASM .i) .ts/.ts
#
ic-rewrite (ARRAY .type ()) (ARRAY .type ()) .ts/.ts
#
ic-rewrite (ARRAY .type0 (.e0, .array0)) (ARRAY .type1 (.e1, .array1)) (.e0 .e1, .ts0)/.tsx
	:- ic-rewrite (ARRAY .type0 .array0) (ARRAY .type1 .array1) .ts0/.tsx
#
ic-rewrite (ARRAYS .size ()) (ARRAYS .size ()) .ts/.ts
#
ic-rewrite (ARRAYS .size (.e0, .array0)) (ARRAYS .size (.e1, .array1)) (.e0 .e1, .ts0)/.tsx
	:- ic-rewrite (ARRAYS .size .array0) (ARRAYS .size .array1) .ts0/.tsx
#
ic-rewrite (BOOLEAN .i) (BOOLEAN .i) .ts/.ts
#
ic-rewrite (DECLARE .mp .var .type .do0) (DECLARE .mp .var .type .do1) (.do0 .do1, .ts)/.ts
	:- member (MONO, POLY,) .mp
#
ic-rewrite (DECLARES .var .offset .size .do0) (DECLARES .var .offset .size .do1) (.do0 .do1, .ts)/.ts
#
ic-rewrite (EXTEND-SIGNED .do0) (EXTEND-SIGNED .do1) (.do0 .do1, .ts)/.ts
#
ic-rewrite (FIELD .type .name .do0) (FIELD .type .name .do1) (.do0 .do1, .ts)/.ts
#
ic-rewrite (IF .if0 .then0 .else0) (IF .if1 .then1 .else1) (.if0 .if1, .then0 .then1, .else0 .else1, .ts)/.ts
#
ic-rewrite (IN .var .type .do0) (IN .var .type .do1) (.do0 .do1, .ts)/.ts
#
ic-rewrite (INDEX .type .array0 .index0) (INDEX .type .array1 .index1) (.array0 .array1, .index0 .index1, .ts)/.ts
#
ic-rewrite (INVOKE .sub0 ()) (INVOKE .sub1 ()) (.sub0 .sub1, .ts)/.ts
#
ic-rewrite (INVOKE .sub0 (IP .io .param0, .ips0)) (INVOKE .sub1 (IP .io .param1, .ips1)) (.param0 .param1, .ts0)/.tsx
	:- ic-rewrite (INVOKE .sub0 .ips0) (INVOKE .sub1 .ips1) .ts0/.tsx
#
ic-rewrite (LET .var0 .value0) (LET .var1 .value1) (.var0 .var1, .value0 .value1, .ts)/.ts
#
ic-rewrite (MEMORY .size .pointer0) (MEMORY .size .pointer1) (.pointer0 .pointer1, .ts)/.ts
#
ic-rewrite (METHOD0 .mps0 .do0) (METHOD0 .mps1 .do1) (.do0 .do1, .ts)/.ts
	:- zip .mps0 .mps1 .list
	, list.query .list .mp0:.mp1 (ic-rewrite-method-parameter .mp0 .mp1)
#
ic-rewrite (METHOD .this0 .method0) (METHOD .this1 .method1) (.this0 .this1, .method0 .method1, .ts)/.ts
#
ic-rewrite (NEW-STRUCT .type ()) (NEW-STRUCT .type ()) .ts/.ts
#
ic-rewrite (NEW-STRUCT .type0 (.k .v0, .nvs0)) (NEW-STRUCT .type1 (.k .v1, .nvs1)) (.v0 .v1, .ts0)/.tsx
	:- ic-rewrite (NEW-STRUCT .type0 .nvs0) (NEW-STRUCT .type1 .nvs1) .ts0/.tsx
#
ic-rewrite (NEW-STRUCTS ()) (NEW-STRUCTS ()) .ts/.ts
#
ic-rewrite (NEW-STRUCTS (.s .o .v0, .sovs0)) (NEW-STRUCTS (.s .o .v1, .sovs1)) (.v0 .v1, .ts0)/.tsx
	:- ic-rewrite (NEW-STRUCTS .sovs0) (NEW-STRUCTS .sovs1) .ts0/.tsx
#
ic-rewrite (NEW-TAG .type0 .tag .value0) (NEW-TAG .type1 .tag .value1) (.value0 .value1, .ts)/.ts
	:- ic-rewrite-type .type0 .type1
#
ic-rewrite (NEW-TAGS .size .tag .value0) (NEW-TAGS .size .tag .value1) (.value0 .value1, .ts)/.ts
#
ic-rewrite NOP NOP .ts/.ts
#
ic-rewrite NULL NULL .ts/.ts
#
ic-rewrite (NUMBER .i) (NUMBER .i) .ts/.ts
#
ic-rewrite (OBJECT .type .pointer0) (OBJECT .type .pointer1) (.pointer0 .pointer1, .ts)/.ts
#
ic-rewrite (OFFSET .offset0 .pointer0) (OFFSET .offset1 .pointer1) (.offset0 .offset1, .pointer0 .pointer1, .ts)/.ts
#
ic-rewrite (POST-ADD-NUMBER .var0 .i) (POST-ADD-NUMBER .var1 .i) (.var0 .var1, .ts)/.ts
#
ic-rewrite (PRAGMA .pragma0 .do0) (PRAGMA .pragma1 .do1) .ts0/.tsx
	:- ic-rewrite-pragma .pragma0 .pragma1 .ts0/.ts1
	, .ts1 = (.do0 .do1, .tsx)
#
ic-rewrite (PRE-ADD-NUMBER .var0 .i) (PRE-ADD-NUMBER .var1 .i) (.var0 .var1, .ts)/.ts
#
ic-rewrite (REF .var0) (REF .var1) (.var0 .var1, .ts)/.ts
#
ic-rewrite (REG .reg) (REG .reg) .ts/.ts
#
ic-rewrite (SEQ .a0 .b0) (SEQ .a1 .b1) (.a0 .a1, .b0 .b1, .ts)/.ts
#
ic-rewrite (SIZE-OF .type) (SIZE-OF .type) .ts/.ts
#
ic-rewrite (SNIPPET .s0) (SNIPPET .s1) (.s0 .s1, .ts)/.ts
#
ic-rewrite (STRING .s) (STRING .s) .ts/.ts
#
ic-rewrite THIS THIS .ts/.ts
#
ic-rewrite (TREE .op .a0 .b0) (TREE .op .a1 .b1) (.a0 .a1, .b0 .b1, .ts)/.ts
#
ic-rewrite (TYPE-CAST .type .do0) (TYPE-CAST .type .do1) (.do0 .do1, .ts)/.ts
#
ic-rewrite (VAR .var) (VAR .var) .ts/.ts
#
ic-rewrite (WHILE .while0 .do0) (WHILE .while1 .do1) (.while0 .while1, .do0 .do1, .ts)/.ts
#

ic-rewrite-method-parameter .mp0 .mp1
	:- .mp0 = MP .io .var .type
	, .mp1 = MP .io .var .type
	, member (IN, OUT,) .io
#
ic-rewrite-method-parameter .mps0 .mps1
	:- .mps0 = MPS .var .size
	, .mps1 = MPS .var .size
#

ic-rewrite-pragma NEW NEW .ts/.ts
#
ic-rewrite-pragma TYPE-SKIP-CHECK TYPE-SKIP-CHECK .ts/.ts
#
ic-rewrite-pragma (TYPE-VERIFY .v0 .type) (TYPE-VERIFY .v1 .type) .ts0/.tsx
	:- ic-rewrite .v0 .v1 .ts0/.tsx
	, !
#

ic-rewrite-type .free .free
	:- not bound .free, !
#
ic-rewrite-type BOOLEAN BOOLEAN
#
ic-rewrite-type I8 I8
#
ic-rewrite-type I32 I32
#
ic-rewrite-type (ARRAY-OF .size .type0) (ARRAY-OF .size .type1)
	:- ic-rewrite-type .type0 .type1
#
ic-rewrite-type (METHOD0-OF .pos0 .returnType0) (METHOD0-OF .pos1 .returnType1)
	:- zip .pos0 .pos1 .list
	, list.query .list (PARAM-OF .io .type0):(PARAM-OF .io .type1) (
		ic-rewrite-type .type0 .type1
	)
	, ic-rewrite-type .returnType0 .returnType1
#
ic-rewrite-type (METHOD-OF .pos0 .returnType0) (METHOD-OF .pos1 .returnType1)
	:- ic-rewrite-type (METHOD0-OF .pos0 .returnType0) (METHOD0-OF .pos1 .returnType1)
#
ic-rewrite-type (POINTER-OF .type0) (POINTER-OF .type1)
	:- ic-rewrite-type .type0 .type1
#
ic-rewrite-type (STRUCT-OF ()) (STRUCT-OF ())
#
ic-rewrite-type (STRUCT-OF (.nameTypes0 | .name .type0)) (STRUCT-OF (.nameTypes1 | .name .type1))
	:- ic-rewrite-type .type0 .type1
	, ic-rewrite-type (STRUCT-OF .nameTypes0) (STRUCT-OF .nameTypes1)
#

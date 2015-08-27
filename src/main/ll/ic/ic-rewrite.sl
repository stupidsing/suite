ic-rewrite () () .ts/.ts
#
ic-rewrite .var .var .ts/.ts
	:- is.atom .var
#
ic-rewrite .imm .imm .ts/.ts
	:- is.int .imm
#
ic-rewrite (.a0; .b0) (.a1; .b1) (.a0 .a1, .b0 .b1, .ts)/.ts
#
ic-rewrite ([.vars] .do0) ([.vars] .do1) (.do0 .do1, .ts)/.ts
#
ic-rewrite (.this0:.fun0 [.ps0]) (.this1:.fun1 [.ps1]) (.this0 .this1, .fun0 .fun1, .ts0)/.tsx
	:- ic-rewrite-list .ps0 .ps1 .ts0/.tsx
#
ic-rewrite (& .value0) (& .value1) (.value0 .value1, .ts)/.ts
#
ic-rewrite (`.pointer0`) (`.pointer1`) (.pointer0 .pointer1, .ts)/.ts
#
ic-rewrite .expr0 .expr1 (.u0 .u1, .v0 .v1, .ts)/.ts
	:- tree .expr0 .u0 .op .v0
	, tree .expr1 .u1 .op .v1
	, ic-operator .op _
#
ic-rewrite (.u0 .op .v0) (.u1 .op .v1) (.u0 .u1, .v0 .v1, .ts)/.ts
	:- ic-operator .op _
#
ic-rewrite (allocate .var/.size; .do0) (allocate .var/.size; .do1) (.do0 .do1, .ts)/.ts
#
ic-rewrite (asm {.asm}) (asm {.asm}) .ts/.ts
#
ic-rewrite (if .if0 then .then0 else .else0) (if .if1 then .then1 .else .else1) .ts0/.tsx
	:- .ts0 = (.if0 .if1, .then0 .then1, .else0 .else1, .tsx)
#
ic-rewrite (let .ref0 = .value0) (let .ref1 = .value1) (.ref0 .ref1, .value0 .value1, .ts)/.ts
#
ic-rewrite (snippet .do0) (snippet .do1) (.do0 .do1, .ts)/.ts
#
ic-rewrite (while .while0 do .do0) (while .while1 do .do1) (.while0 .while1, .do0 .do1, .ts)/.ts
#

ic-rewrite-list () () .ts/.ts
#
ic-rewrite-list (.p0, .ps0) (.p1, .ps1) .ts0/.tsx
	:- .ts0 = (.p0 .p1, .ts1)
	, ic-rewrite .ps0 .ps1 .ts1/.tsx
#

ic-rewrite-type I32 I32 .ts/.ts
#
ic-rewrite-type (ARRAY-OF .size .t0) (ARRAY-OF .size .t1) (.t0 .t1, .ts)/.ts
#
ic-rewrite-type (PTR-OF .t0) (PTR-OF .t1) (.t0 .t1, .ts)/.ts
#
ic-rewrite-type (TUPLE-OF .name ()) (TUPLE-OF .name ()) .ts/.ts
#
ic-rewrite-type (TUPLE-OF .name (.t0, .ts0)) (TUPLE-OF .name (.t1, .ts1)) .ts0/.tsx
	:- ic-rewrite-type .t0 .t1 .ts0/.ts1
	, ic-rewrite-type (TUPLE-OF .name .ts0) (TUPLE-OF .name .ts1) .ts1/.tsx
#

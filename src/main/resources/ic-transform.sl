ic-transform .var .var .ts/.ts
	:- is.atom .var
#
ic-transform .imm .imm .ts/.ts
	:- is.int .imm
#
ic-transform () () .ts/.ts
#
ic-transform (.a0; .b0) (.a1; .b1) (.a0 .a1, .b0 .b1, .ts)/.ts
#
ic-transform (.var => .do0) (.var => .do1) (.do0 .do1, .ts)/.ts
#
ic-transform (.fun0 {.param0}) (.fun1 {.param1}) (.fun0 .fun1, .param0 .param1, .ts)/.ts
#
ic-transform (& .value0) (& .value1) (.value0 .value1, .ts)/.ts
#
ic-transform (`.pointer0` = .value0) (`.pointer1` = .value1) (.pointer0 .pointer1, .value0 .value1, .ts)/.ts
#
ic-transform .expr0 .expr1 (.u0 .u1, .v0 .v1, .ts)/.ts
	:- tree .expr0 .u0 .op .v0
	, tree .expr1 .u1 .op .v1
	, ic-operator .op _
#
ic-transform (.u0 .op .v0) (.u1 .op .v1) (.u0 .u1, .v0 .v1, .ts)/.ts
	:- ic-operator .op _
#
ic-transform (asm {.asm}) (asm {.asm}) .ts/.ts
#
ic-transform (declare .var >> .do0) (declare .var >> .do1) (.do0 .do1, .ts)/.ts
#
ic-transform (if .if0 then .then0 else .else0) (if .if1 then .then1 .else .else1) .ts0/.tsx
	:- .ts0 = (.if0 .if1, .then0 .then1, .else0 .else1, .tsx)
#
ic-transform (while .while0 do .do0) (while .while1 do .do1) (.while0 .while1, .do0 .do1, .ts)/.ts
#

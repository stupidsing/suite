ic-compile .fs (.do0; .do1) .e0/.ex
	:- ic-compile .fs .do0 .e0/.e1
	, ic-compile .fs .do1 .e1/.ex
#
ic-compile _ (asm {.e0/.ex}) .e0/.ex
#
ic-compile .fs (let .var := .value >> .do) .e0/.ex
	:- ic-compile .fs .value .e0/.e1
	, .e1 = (_ PUSH EAX, .e2)
	, replace .var `EBP - .fs` .do .do1
	, let .fs1 (.fs + 4)
	, ic-compile .fs1 .do1 .e2/.e3
	, .e3 = (_ POP EDI, .ex)
#
ic-compile _ (.var => .do) .e0/.ex
	:- .e0 = (_ JMP DWORD .label
		, .funLabel PUSH EBP
		, _ MOV (EBP, ESP)
		, .e1)
	, replace EBP `EBP` .do .do1
	, replace .var `EBP + 4` .do .do1
	, ic-compile 0 .do .e1/.e2
	, .e2 = (_ MOV (ESP, EBP)
		, _ POP EBP
		, _ RET
		, .label MOV (EAX, .funLabel)
	, .ex)
#
ic-compile .fs (.fun {.param}) .e0/.ex
	:- , ic-compile .fs .param .e0/.e1
	, .e1 = (_ PUSH EAX, .e2)
	, ic-compile .fs .fun .e2/.e3
	, .e3 = (_ CALL `EAX`, .ex)
#
ic-compile .fs (& `.pointer`) .e0/.ex
	:- ic-compile .fs .pointer .e0/.ex
#
ic-compile .fs (`.pointer` = .value) .e0/.ex
	:- ic-compile .fs .pointer .e0/.e1
	, .e1 = (_ PUSH EAX, .e2)
	, ic-compile .fs .value .e2/.e3
	, .e3 = (_ POP EDI, _ MOV (`EDI`, EAX), .ex)
#
ic-compile .fs (.value0 + .value1) .e0/.ex
	:- ic-compile .fs .value0 .e0/.e1
	, .e1 = (_ PUSH EAX, .e2)
	, ic-compile .fs .value1 .e2/.e3
	, .e3 = (_ POP EBX
		, _ ADD (EAX, EBX)
		, .ex)
#
ic-compile .fs `.value` .e0/.ex
	:- ic-compile .fs .value .e0/.e1
	, .e1 = (_ MOV (EAX, `EAX`), .ex)
#
ic-compile _ EBP (_ MOV (EAX, EBP), .e)/.e
#
ic-compile _ .imm (_ PUSH .imm, .e)/.e
	:- is.int .imm
#

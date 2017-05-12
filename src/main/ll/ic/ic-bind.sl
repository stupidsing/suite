ic-bind .v0 .v1 .then .else .parsed
	:- .then1 = PRAGMA (TYPE-VERIFY (TREE ' = ' .v0 .v1) BOOLEAN) .then
	, ic-bind_ .v0 .v1 .then1 .else .parsed
#

ic-bind_ .v0 (PRAGMA NEW (VAR .nv)) .then _ (DECLARE MONO .nv _ (SEQ (LET (VAR .nv) (PRAGMA TYPE-SKIP-CHECK .v0)) .then))
	:- !
#
ic-bind_ (PRAGMA _ .v0) .v1 .then .else .parsed
	:- !
	, ic-bind_ .v0 .v1 .then .else .parsed
#
ic-bind_ .v0 (PRAGMA _ .v1) .then .else .parsed
	:- !
	, ic-bind_ .v0 .v1 .then .else .parsed
#
ic-bind_ (ARRAY .type ()) (ARRAY .type ()) .then _ .then
#
ic-bind_ (ARRAY .type (.head0, .tail0)) (ARRAY .type (.head1, .tail1)) .then .else .parsed
	:- .array0 = ARRAY .type .tail0
	, .array1 = ARRAY .type .tail1
	, ic-bind-pair .head0 .head1 .array0 .array1 .then .else .parsed
#
ic-bind_ (NEW-STRUCT _ ()) (NEW-STRUCT _ ()) .then _ .then
#
ic-bind_ (NEW-STRUCT .structType (.name = .value0, .nvs0)) (NEW-STRUCT .structType (.name = .value1, .nvs1)) .then .else .parsed
	:- .structType = STRUCT-OF (.nameTypes | .name _)
	, !
	, .struct0 = NEW-STRUCT (STRUCT-OF .nameTypes) .nvs0
	, .struct1 = NEW-STRUCT (STRUCT-OF .nameTypes) .nvs1
	, ic-bind-pair .value0 .value1 .struct0 .struct1 .then .else .parsed
#
ic-bind_ .v0 .v1 .then .else (
	IF (PRAGMA TYPE-SKIP-CHECK (TREE ' = ' .v0 .v1)) .then .else
) #

ic-bind-pair .h0 .t0 .h1 .t1 .then .else .parsed
	:- temp .elseVar
	, .else1 = INVOKE (VAR .elseVar) ()
	, ic-bind_ .h0 .h1 .then1 .else1 .parsed0
	, ic-bind_ .t0 .t1 .then .else1 .then1
	, .parsed = DECLARE MONO .elseVar _ (SEQ (LET (VAR .elseVar) (METHOD THIS .else)) .parsed0)
#

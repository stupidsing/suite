fc-bind .v0 .v1 .then .else .parsed
	:- .then1 = PRAGMA (TYPE-VERIFY (TREE ' = ' .v0 .v1) BOOLEAN) .then
	, fc-bind_ .v0 .v1 .then1 .else .parsed
#

fc-bind_ .v0 (PRAGMA NEW (VAR .nv)) .then _ (DEF-VARS (.nv (PRAGMA TYPE-SKIP-CHECK .v0),) .then)
	:- !
#
fc-bind_ (PRAGMA _ .v0) .v1 .then .else .parsed
	:- !
	, fc-bind_ .v0 .v1 .then .else .parsed
#
fc-bind_ .v0 (PRAGMA _ .v1) .then .else .parsed
	:- !
	, fc-bind_ .v0 .v1 .then .else .parsed
#
fc-bind_ (CONS .type .h0 .t0) (CONS .type .h1 .t1) .then .else .parsed
	:- !, fc-bind-pair .h0 .t0 .h1 .t1 .then .else .parsed
#
fc-bind_ .v0 (CONS .type .h1 .t1) .then .else (
	DEF-VARS (.elseVar (WRAP .else),) (DECONS .type .v0 .headVar .tailVar .then1 .else1)
) :- !
	, temp .elseVar, temp .headVar, temp .tailVar
	, .else1 = UNWRAP (VAR .elseVar)
	, fc-bind-pair (VAR .headVar) (VAR .tailVar) .h1 .t1 .then .else1 .then1
#
fc-bind_ .v0 .v1 .then .else (
	IF (PRAGMA TYPE-SKIP-CHECK (TREE ' = ' .v0 .v1)) .then .else
) #

fc-bind-pair .h0 .t0 .h1 .t1 .then .else .parsed
	:- temp .elseVar
	, .else1 = UNWRAP (VAR .elseVar)
	, fc-bind_ .h0 .h1 .then1 .else1 .parsed0
	, fc-bind_ .t0 .t1 .then .else1 .then1
	, .parsed = DEF-VARS (.elseVar (WRAP .else),) .parsed0
#

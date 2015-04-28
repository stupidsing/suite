fc-bind .v0 .v1 .then .else .parsed
	:- fc-bind-cons .v0 .h0 .t0
	, fc-bind-cons .v1 .h1 .t1
	, !, .then1 = PRAGMA ( -- Handle type fails like .v0 = (1; true;)
		TYPE-VERIFY (TREE ' = ' .v0 .v1) BOOLEAN
	) .then
	, fc-bind-pair .h0 .t0 .h1 .t1 .then1 .else .parsed
#
fc-bind (PAIR .p0 .q0) (PAIR .p1 .q1) .then .else .parsed
	:- !, fc-bind-pair .p0 .q0 .p1 .q1 .then .else .parsed
#
fc-bind .v0 (NEW-VAR .nv) .then _ (DEF-VARS (.nv .v0,) .then)
	:- !
#
fc-bind .v0 .v1 .then .else (
	DEF-VARS (.elseVar (WRAP .else), .v0var .v0,) (
		IF (INVOKE (VAR .v0var) (VAR +is-list)) (
			DEF-VARS (
				.headVar (INVOKE (VAR .v0var) (VAR +lhead)),
				.tailVar (INVOKE (VAR .v0var) (VAR +ltail)),
			)
			.then1
		) .else1
	)
) :- fc-bind-cons .v1 .h1 .t1
	, !
	, temp .elseVar, temp .v0var, temp .headVar, temp .tailVar
	, .else1 = UNWRAP (VAR .elseVar)
	, fc-bind-pair (VAR .headVar) (VAR .tailVar) .h1 .t1 .then .else1 .then1
#
fc-bind .v0 (PAIR .p1 .q1) .then .else (
	DEF-VARS (.elseVar (WRAP .else), .v0var (PRAGMA (TYPE-CAST UP _) .v0),) (
		IF (INVOKE (VAR .v0var) (VAR +is-pair)) (
			DEF-VARS (
				.leftVar (INVOKE (VAR .v0var) (VAR +pleft)),
				.rightVar (INVOKE (VAR .v0var) (VAR +pright)),
			) .then1
		) .else1
	)
) :- !
	, temp .elseVar, temp .v0var, temp .leftVar, temp .rightVar
	, .else1 = UNWRAP (VAR .elseVar)
	, fc-bind-pair (VAR .leftVar) (VAR .rightVar) .p1 .q1 .then .else1 .then1
#
fc-bind .v0 (PRAGMA _ .v1) .then .else .parsed
	:- !
	, fc-bind .v0 .v1 .then .else .parsed
#
fc-bind .v0 .v1 .then .else (
	IF (TREE ' = ' .v0 .v1) .then .else
) #

fc-bind-cons (INVOKE .t INVOKE .h VAR +lcons) .h .t #
fc-bind-cons (INVOKE .t INVOKE .h VAR +pcons) .h .t #

fc-bind-pair .h0 .t0 .h1 .t1 .then .else (DEF-VARS (.elseVar (WRAP .else),) .parsed)
	:- temp .elseVar
	, .else1 = UNWRAP (VAR .elseVar)
	, fc-bind .h0 .h1 .then1 .else1 .parsed
	, fc-bind .t0 .t1 .then .else1 .then1
#

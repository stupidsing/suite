() :- import.file 'rbt.sl'
#

-- 1. convert implication operators: (P => Q) becomes (~P ^ Q)
prove0 (IMP .p .q) (AND (NOT .p1) .q1)
	:- !, prove0 .p .p1, prove0 .q .q1
#
prove0 .p .p1
	:- transform .p .p1 .ts
	, prove0-list .ts
#

prove0-list () #
prove0-list (.p .p1, .ts) .ts :- prove0 .p .p1 , prove0-list .ts
#

-- 2. move negation inwards
prove1 (NOT (AND .p .q)) (NOT (OR (NOT .p1) (NOT .q1)))
	:- !, prove1 .p .p1, prove1 .q .q1
#
prove1 (NOT (OR .p .q)) (NOT (AND (NOT .p1) (NOT .q1)))
	:- !, prove1 .p .p1, prove1 .q .q1
#
prove1 .p .p1 :- transform .p .p1 .ts, prove1-list .ts #

prove1-list () #
prove1-list (.p .p1, .ts) .ts :- prove1 .p .p1, prove1-list .ts #

-- 2a. remove double negatives
prove1a (NOT NOT .p) .p1 :- !, prove1a .p .p1 #
prove1a .p .p1 :- transform .p .p1 .ts, prove1a-list .ts #

prove1a-list () #
prove1a-list (.p .p1, .ts) .ts :- prove1a .p .p1, prove1a-list .ts #

-- 3. skolemising
prove2 .vars (FOR-ALL .var .p) (FOR-ALL .var .p1)
	:- !, prove2 (.var, .vars) .p .p1
#
prove2 .vars (THERE-EXISTS .var .p) .p1
	:- !, temp .functionName
	, form-function .vars .functionName .var1
	, replace .var .var1 .p .p1
#
prove2 .vars .p .p1
	:- transform .p .p1 .ts
	, prove2-list .vars .ts
#

prove2-list _ () #
prove2-list .vars (.p .p1, .ts) .ts
	:- prove2 .vars .p .p1
	, prove2-list .vars .ts
#

form-function () .p .p #
form-function (.var, .vars) .p (FN .var .p1) :- form-function .vars .p .p1 #

-- 4. moving "for-all" outwards
prove3 (AND (FOR-ALL .e .p) .q) (FOR-ALL .e .pq1)
	:- !, prove3 (AND .p .q) .pq1
#
prove3 (AND .p (FOR-ALL .e .q)) (FOR-ALL .e .pq1)
	:- !, prove3 (AND .p .q) .pq1
#
prove3 (NOT (FOR-ALL .e .p)) (FOR-ALL .e (NOT .p1))
	:- !, prove3 .p .p1
#
prove3 (OR (FOR-ALL .e .p) .q) (FOR-ALL .e .pq1)
	:- !, prove3 (OR .p .q) .pq1
#
prove3 (OR .p (FOR-ALL .e .q)) (FOR-ALL .e .pq1)
	:- !, prove3 (OR .p .q) .pq1
#
prove3 .p .p1 :- transform .p .p1 .ts, prove3-list .ts
#

prove3-list () #
prove3-list (.p .p1, .ts) .ts :- prove3 .p .p1 , prove3-list .ts #

-- 5. distributing AND over OR
prove4 (OR (AND .p .q) .r) .pqr :- !, prove4 (AND (OR .p .r) (OR .q .r)) .pqr #
prove4 (OR .p (AND .q .r)) .pqr :- !, prove4 (AND (OR .p .q) (OR .p .r)) .pqr #
prove4 .p .p1 :- transform .p .p1 .ts, prove4-list .ts
#

prove4-list () #
prove4-list (.p .p1, .ts) .ts :- prove4 .p .p1 , prove4-list .ts #

-- 6. putting into clauses (ANDs of ORs form)
prove5-index (OR .p .q)
	:- !, prove5-index .p, prove5-index .q
#
prove5-index .p
	:- prove5-index-ands .p ()/.rbt
#
-- ...

prove5-index-ands (AND .p .q) .rb0/.rbx
	:- !
	, prove5-index-ands .p .rb0/.rb1
	, prove5-index-ands .q .rb1/.rbx
#
prove5-index-ands .p .rb0/.rbx
	:- rbt-get .rb0 .p:.list
	, once (bound .list; .list = ())
	, rbt-replace .p:(.pre:.post, .list) .rb0/.rbx
#

transform (IMP .p .q) (AND (NOT .p1) .q1) (.p .p1, .q .q1,) :- ! #
transform (AND .p .q) (AND .p1 .q1) (.p .p1, .q .q1,) :- ! #
transform (FOR-ALL .e .p) (FOR-ALL .e .p1) (.p .p1,) :- ! #
transform (NOT .p) (NOT .p1) (.p .p1,) :- ! #
transform (OR .p .q) (OR .p1 .q1) (.p .p1, .q .q1,) :- ! #
transform (THERE-EXISTS .e .p) (THERE-EXISTS .e .p1) (.p .p1,) :- ! #
transform (VAR .var) (VAR .var) () #

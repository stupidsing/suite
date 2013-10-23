-- 1. convert implication operators: (P => Q) becomes (~P ^ Q)
prove0 (IMP .p .q) (AND (NOT .p1) .q1)
	:- !, prove0 .p .p1, prove0 .q .q1
#
prove0 .p .p1
	:- validate .p .p1 .ts
	, prove-list0 .ts
#

prove-list0 () #
prove-list0 (.p .p1, .ts) .ts
	:- prove0 .p .p1
	, prove-list0 .ts
#

-- 2. move negation inwards
prove1 (NOT (AND .p .q)) (NOT (OR (NOT .p1) (NOT .q1)))
	:- !, prove1 .p .p1, prove1 .q .q1
#
prove1 (NOT (OR .p .q)) (NOT (AND (NOT .p1) (NOT .q1)))
	:- !, prove1 .p .p1, prove1 .q .q1
#
prove1 .p .p1 :- validate .p .p1 .ts, prove-list1 .ts #

prove-list1 () #
prove-list1 (.p .p1, .ts) .ts :- prove1 .p .p1, prove-list1 .ts #

-- 3. skolemising
prove2 .vars (FOR-ALL .var .p) (FOR-ALL .var .p1)
	:- !, prove2 (.var, .vars) .p .p1
#
prove2 .vars (THERE-EXISTS .var .p) .p1
	:- !, temp .functionName
	, form-function .vars .functionName .var1
	, replace .p .p1 .var .var1
#
prove2 .vars .p .p1
	:- validate .p .p1 .ts
	, prove-list2 .vars .ts
#

prove-list2 _ () #
prove-list2 .vars (.p .p1, .ts) .ts
	:- prove2 .vars .p .p1
	, prove-list2 .vars .ts
#

form-function () .p .p #
form-function (.var, .vars) .p (FN .var .p1) :- form-function .vars .p .p1 #

-- 4. moving "for-all" outwards
prove3 (AND .p .q) (AND .p1 .q1)
	:- !, prove3 .p .p1, prove3 .q .q1
#
prove3 (FOR-ALL .e .p) (FOR-ALL .e .p1)
	:- !, prove3 .p .p1
#
prove3 (NOT .p) (NOT .p1)
	:- !, prove3 .p .p1
#
prove3 (OR .p .q) (OR .p1 .q1)
	:- !, prove3 .p .p1, prove3 .q .q1
#
prove3 (THERE-EXISTS .e .p) (THERE-EXISTS .e .p1)
	:- !, prove3 .p .p1
#
prove3 (VAR .var) (VAR .var) #
prove3 .p .p1
	:- validate .p .p1 .ts
	, prove-list3 .ts
#

prove-list3 () #
prove-list3 (.p .p1, .ts) .ts
	:- prove3 .p .p1
	, prove-list3 .ts
#

-- 5. distributing AND over OR
-- 6. putting into clauses (ANDs of ORs form)

validate (IMP .p .q) (AND (NOT .p1) .q1) (.p .p1, .q .q1,) :- ! #
validate (AND .p .q) (AND .p1 .q1) (.p .p1, .q .q1,) :- ! #
validate (FOR-ALL .e .p) (FOR-ALL .e .p1) (.p .p1,) :- ! #
validate (NOT .p) (NOT .p1) (.p .p1,) :- ! #
validate (OR .p .q) (OR .p1 .q1) (.p .p1, .q .q1,) :- ! #
validate (THERE-EXISTS .e .p) (THERE-EXISTS .e .p1) (.p .p1,) :- ! #
validate (VAR .var) (VAR .var) () #

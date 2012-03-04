-------------------------------------------------------------------------------
-- precompile code for basic functions for functional precompiler
--

fc-setup-precompile
	:- .pc = .frame/.ve .ca/.cb/.da/.db/.resultReg
	, fc-add-standard-funs ($$PRECOMPILE .pc) .do1
	, !, fc-parse .do1 .parsed
	, !, infer-type .parsed ()/()/() _
	, .c0 = (_ ENTER, .c1)
	, .c2 = (_ EXIT .reg, .d0)
	, !, fc-compile .parsed 0/() .c1/.c2/.d0/()/.reg
	, !
	, .ca/.da = .cb/.db
	, .prog = (
		fc-compile-over .do .c0
			:- fc-compile .do .frame/.ve .ca/.cb/.da/.db/.resultReg
		#
	)
	, to.dump.string .prog .prog1
	, file-write 'src/main/resources/fc-precompiled.sl' .prog1
#

fc-parse ($$PRECOMPILE .pc) ($$PRECOMPILE .pc) :- ! #

infer-type-rule ($$PRECOMPILE _) _ .tr/.tr NUMBER :- ! #

-- Eager evaluation
fc-compile ($$PRECOMPILE .frame/.ve .cdr) .frame/.ve .cdr :- ! #

-- Lazy evaluation
fc-compile0 ($$PRECOMPILE .frame/.ve .cdr) .frame/.ve .cdr :- ! #

() :- import 'fc.sl'
	, import 'fc-lazy-evaluation.sl'
#

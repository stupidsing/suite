-------------------------------------------------------------------------------
-- precompile code for basic functions for functional precompiler
--

fc-setup-precompile
	:- .pc = .ve/.te/.oe .tra/.trb .frame/.ve .ca/.cb/.da/.db/.resultReg
	, fc-add-standard-funs ($$PRECOMPILE .pc) .do1
	, !, fc-parse .do1 .parsed
	, !, infer-type-rule .parsed ()/()/() .tr0/.trx NUMBER
	, not not ( -- Test type correctness
		.tra = .trb, resolve-types .tr0/.trx
	)
	, .c0 = (_ ENTER, .c1)
	, .c2 = (_ EXIT .reg, .d0)
	, !, fc-compile .parsed 0/() .c1/.c2/.d0/()/.reg
	, !
	, .prog = (
		fc-compile-over .do .c0
			:- infer-type-rule .do .ve/.te/.oe .tra/.trb _
			, resolve-types .tr0/.trx
			, !, fc-compile .do .frame/.ve .ca/.cb/.da/.db/.resultReg
		#
	)
	, to.dump.string .prog .prog1
	, file-write 'src/main/resources/fc-precompiled.sl' .prog1
#

fc-parse ($$PRECOMPILE .pc) ($$PRECOMPILE .pc) :- ! #

infer-type-rule ($$PRECOMPILE .vto .tr/.tr _) .vto .tr/.tr NUMBER
	:- !
#

-- Eager evaluation
fc-compile ($$PRECOMPILE _ _ .fve .cdr) .ve .cdr :- ! #

-- Lazy evaluation
fc-compile0 ($$PRECOMPILE _ _ .fve .cdr) .ve .cdr :- ! #

() :- import 'fc.sl'
	, import 'fc-lazy-evaluation.sl'
#

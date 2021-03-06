-------------------------------------------------------------------------------
-- precompile code for basic functions for functional precompiler

fc-precompile-lib-if-required .lib
	:- fc-library-filename .lib .filename0
	, fc-precompiled-library-filename .lib .filename1
	, jar .jar
	, once (file.exists .jar, file.time .jar .sourceTime0; .sourceTime0 = 0)
	, once (file.exists .filename0, file.time .filename0 .sourceTime1; .sourceTime1 = 0)
	, once (file.exists .filename1, file.time .filename1 .targetTime; .targetTime = 0)
	, once (.sourceTime0 <= .targetTime, .sourceTime1 <= .targetTime; fc-precompile-lib .lib)
#

fc-precompile-lib .lib
	:- fc-precompiled-library-filename .lib .filename
	, fc-load-library .lib .do0 .do1
	, fc-precompile .lib .do1/.do0 .preds
	, !, write 'Saving file' .filename, nl
	, persist.save .preds .filename
#

fc-precompile .lib .do0/($$PRECOMPILE .pc) .preds
	:- .pc = .ves/.tes .fcs
	, !, write "Parsing program", nl
	, !, fc-parse .do0 .do1
	, !, write "Inferencing types", nl
	, !, fc-infer-type .do1 NUMBER
	, !, .pred0 = (
		fc-infer-type_-use-lib .lib .ve/.te .do .type
			:- fc-dict-union-replace .ve .ves .ve1
			, append .te .tes .te1
			, fc-infer-type_ .ve1/.te1 .do .type
	)
	, !, write 'Verifying intermediate output', nl
	, once (not (is.cyclic .do1); fc-error "Cyclic data detected")
	, !, write "Lazyifying", nl
	, !, fc-lazyify .do1 .dol2
	, !, write "Reducing tail recursions", nl
	, !, fc-reduce-tail-recursion .do1 .do2
	, !, write "Optimizing", nl
	, !, fc-optimize-flow .do2 .do3
	, !, fc-optimize-flow .dol2 .dol3
	, !, fc-precompile-compile EAGER .lib .fcs .do3 .pred1
	, !, fc-precompile-compile LAZY .lib .fcs .dol3 .pred2
	, !, .preds = (.pred0 # .pred1 # .pred2 #)
#

fc-precompile-compile .mode .lib .fcs .parsed .pred
	:- !, write 'Pre-compiling in' .mode 'mode', nl
	, fc-precompile-compile-node .parsed .frame0/() .c0/.cx/.reg
	, .fcs = .frame1/.ves .cs0/.csx/.regs
	, .pred = (
		fc-compile-use-lib .mode .lib .do .frame0/.ve .c0/.cx/.reg
			:- fc-dict-union-bind .ve .ves .ve1 -- import and export symbols
			, fc-compile .do .frame1/.ve1 .cs0/.csx/.regs
	)
#

fc-precompile-compile-node (USE .mode EXTERNAL .lib .do) .frame/.ve .c0/.cx/.reg
	:- !, write 'Loading pre-compiled library' .lib, nl
	, fc-load-precompiled-library .lib (_ # .eagerPred # .lazyPred #)
	, once (.mode = EAGER, .pred = .eagerPred; .pred = .lazyPred)
	, clone .pred (
		fc-compile-use-lib .mode .lib ($$PRECOMPILE _ .frame/.ve1 _) _/() _ :- .tail
	)
	, once .tail
	, fc-dict-union-bind .ve .ve1 .ve2
	, fc-precompile-compile-node .do .frame/.ve2 .c0/.cx/.reg
#
fc-precompile-compile-node .parsed .frame/.ve .c0/.cx/.reg
	:- fc-compile .parsed .frame/.ve .c0/.cx/.reg
#

-- parser
fc-parse ($$PRECOMPILE .pc) ($$PRECOMPILE .pc) :- ! #

-- type inferencer
fc-infer-type_ .uvt ($$PRECOMPILE .uvt _) NUMBER :- ! #

-- lazyifier and optimizer
fc-rewrite ($$PRECOMPILE .pc) ($$PRECOMPILE .pc) .ts/.ts :- ! #

-- code generator
fc-compile ($$PRECOMPILE _ .fve .cr) .fve .cr :- ! #

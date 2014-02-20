-------------------------------------------------------------------------------
-- functional program compiler
--
-- .mode can be EAGER or LAZY

() :- import.file 'cg-generate-code.sl'
	, import.file 'fc-evaluate.sl'
	, import.file 'fc-infer-type.sl'
	, import.file 'fc-lazyify.sl'
	, import.file 'fc-optimize.sl'
	, import.file 'fc-parse.sl'
	, import.file 'fc-transform.sl'
	, import.file 'rbt.sl'
#

compile-function .mode .do0 .c0
	:- .c0 = (_ ENTER, .c1)
	, !, fc-parse .do0 .do1
	, !, fc-infer-type-rule .do1 ()/()/() .tr/() _
	, !, fc-resolve-type-rules .tr
	, once (not is.cyclic .do1; fc-error "Cyclic data detected")
	, !, once (.mode = LAZY, fc-lazyify-fun .do1 .do2
		; .mode = EAGER, .do1 = .do2
	)
	, !, fc-optimize .do2 .do3
	, !, fc-compile .do3 0/() .c1/.c2/.d0/()/.reg
	, .c2 = (_ RETURN-VALUE .reg, _ LEAVE, .d0)
	, !, cg-generate-code .c0
#

fc-load-library .lib .do0 .dox
	:- memoize .node0 (fc-load-library0 .lib .node0) (.do0 .dox,)
#

fc-load-library0 .lib .slfx
	:- fc-add-functions .lib .slfx, !
	; once (
		home.dir .homeDir
		, concat .homeDir "/src/main/resources/" .lib ".slf" .slfFilename
		, whatever (file.exists .slfFilename
			, file.read .slfFilename .slf
			, to.atom ".p" .var
			, concat .var " (" .slf .var ")" .slf1
			, parse .slf1 .slf2
			, generalize .slf2 .slfx
		)
	)
#

fc-load-precompiled-library .lib .node
	:- memoize .node0 (fc-load-precompiled-library0 .lib .node0) (.node,)
#

fc-load-precompiled-library0 .lib .precompiled
	:- fc-precompiled-library-filename .lib .rpnFilename
	, file.read .rpnFilename .rpn
	, rpn .precompiled .rpn
#

fc-precompiled-library-filename .lib .filename
	:- home.dir .homeDir
	, concat .homeDir "/precompiled/" .lib ".rpn" .filename
#

fc-frame-difference .frame0 .frame1 0 :- same .frame0 .frame1, ! #
fc-frame-difference .frame0 (.frame1 + 1) .frameDiff
	:- not is.tree .frame0, !
	, fc-frame-difference .frame0 .frame1 .frameDiff0
	, let .frameDiff (.frameDiff0 - 1)
#
fc-frame-difference (.frame0 + 1) (.frame1 + 1) .frameDiff
	:- !, fc-frame-difference .frame0 .frame1 .frameDiff
#

fc-define-default-fun 2 _compare COMPARE #
fc-define-default-fun 1 _ijavacls GET-INTRINSIC #
fc-define-default-fun 1 _ijavaobj0 CALL-INTRINSIC0 #
fc-define-default-fun 2 _ijavaobj1 CALL-INTRINSIC1 #
fc-define-default-fun 3 _ijavaobj2 CALL-INTRINSIC2 #
fc-define-default-fun 4 _ijavaobj3 CALL-INTRINSIC3 #
fc-define-default-fun 2 _lcons CONS-LIST #
fc-define-default-fun 1 _lhead HEAD #
fc-define-default-fun 1 _ltail TAIL #
fc-define-default-fun 2 _pcons CONS-PAIR #
fc-define-default-fun 1 _pleft HEAD #
fc-define-default-fun 1 _pright TAIL #
fc-define-default-fun 1 is-list IS-CONS #
fc-define-default-fun 1 is-pair IS-CONS #

fc-operator .oper
	:- member (' + ', ' - ', ' * ', ' / ', ' %% ',
		' = ', ' != ',
		' > ', ' < ', ' >= ', ' <= ',
		',', ';',
		' . ',
	) .oper
#

fc-error .m :- !, write .m, nl, fail #

fc-dict-get .v .t :- rbt-get .v .t, ! #

-- use replace, necessary to redefine already-defined variables
fc-dict-add .v .t0/.t1 :- rbt-replace .v .t0/.t1, ! #

fc-dict-merge-replace .t0 .t1 .t2 :- rbt-merge-replace .t0 .t1 .t2, ! #

fc-dict-member .v .t :- rbt-member .v .t #

-- There are few functions that are not pure: ijavaobj*
-- Logs are considered 'invisible', so they are not counted.

fc-add-functions STANDARD .p (
	define compare = (a => b => _compare {a} {b}) >>
	define cons = (head => tail => _lcons {head} {tail}) >>
	define first = (tuple => _pleft {tuple}) >>
	define head = (list => _lhead {list}) >>
	define ijavacls = (name => _ijavacls {name}) >>
	define ijavaobj0 = (name => _ijavaobj0 {name}) >>
	define ijavaobj1 = (name => p0 => _ijavaobj1 {name} {p0}) >>
	define ijavaobj2 = (name => p0 => p1 => _ijavaobj2 {name} {p0} {p1}) >>
	define ijavaobj3 = (name => p0 => p1 => p2 => _ijavaobj3 {name} {p0} {p1} {p2}) >>
	define second = (tuple => _pright {tuple}) >>
	define tail = (list => _ltail {list}) >>
	define _popen as ([string] -> string -> data-of Stream) =
		atom:`CLASS!suite.lp.intrinsic.Intrinsics$Popen` | ijavacls | ijavaobj2
	>>
	define log as (:t => :t -> :t) =
		atom:`CLASS!suite.lp.intrinsic.Intrinsics$Log1` | ijavacls | ijavaobj1
	>>
	define log2 as (:t => string -> :t -> :t) =
		atom:`CLASS!suite.lp.intrinsic.Intrinsics$Log2` | ijavacls | ijavaobj2
	>>
	define source as (data-of Stream -> string) =
		atom:`CLASS!suite.lp.intrinsic.Intrinsics$Source_` | ijavacls | ijavaobj1
	>>
	define throw as (any -> any) =
		atom:`CLASS!suite.lp.intrinsic.Intrinsics$Throw` | ijavacls | ijavaobj1
	>>
	define and = (x => y =>
		if x then y else false
	) >>
	define drop = (n => list =>
		if (n > 0 && is-list {list})
		then (list | tail | drop {n - 1})
		else list
	) >>
	define flip = (f => x => y =>
		f {y} {x}
	) >>
	define fold-left = (fun => init => -- possible for tail recursion optimization
		case
		|| `$h; $t` => fold-left {fun} {fun {init} {h}} {t}
		|| anything => init
	) >>
	define fold-right = (fun => init => -- possible for short-circuit evaluation
		case
		|| `$h; $t` => fun {h} {fold-right {fun} {init} {t}}
		|| anything => init
	) >>
	define greater = (a => b =>
		if (a > b) then a else b
	) >>
	define id = (v =>
		v
	) >>
	define invoke = (f => x =>
		f {x}
	) >>
	define iterate = (f => init =>
		init; iterate {f} {f {init}}
	) >>
	define lesser = (a => b =>
		if (a > b) then b else a
	) >>
	define not = (x =>
		if x then false else true
	) >>
	define or = (x => y =>
		if x then true else y
	) >>
	define bisect = (fun =>
		define bisect0 = (prevs => list =>
			case
			|| (list = `$elem; $elems`)
				if (fun {elem}) then
					let `$listt, $listf` = bisect0 {prevs} {elems} >>
					(elem; listt), listf
				else (prevs, list)
			|| (), prevs
		) >>
		bisect0 {}
	) >>
	define repeat = (n => elem =>
		if (n > 0) then (elem; repeat {n - 1} {elem}) else ()
	) >>
	define scan-left = (fun => init =>
		case
		|| `$h; $t` => init; scan-left {fun} {fun {init} {h}} {t}
		|| anything => init;
	) >>
	define scan-right = (fun => init =>
		case
		|| `$h; $t` =>
			let r = scan-right {fun} {init} {t} >>
			fun {h} {head {r}}; r
		|| anything => init;
	) >>
	define str-to-int = (s =>
		let unsigned-str-to-int = fold-left {v => d => v * 10 + d - 48} {0} >>
			if (is-list {s} && head {s} = 45)
			then (`0 - ` . unsigned-str-to-int . tail)
			else unsigned-str-to-int
		{s}
	) >>
	define take = (n => list =>
		if (n > 0 && is-list {list})
		then (list | tail | take {n - 1} | cons {list | head})
		else ()
	) >>
	define take-drop = (n => list =>
		if (n > 0 && is-list {list}) then
			let `$t1, $d1` = (list | tail | take-drop {n - 1}) >>
			cons {list | head} {t1}, d1
		else (, list)
	) >>
	define take-while = (fun =>
		case
		|| `$elem; $elems` =>
			if (fun {elem}) then (elem; take-while {fun} {elems}) else ()
		|| anything => ()
	) >>
	define tget0 =
		first
	>>
	define tget1 =
		first . second
	>>
	define tget2 =
		first . second . second
	>>
	define unfold-right = (fun => init =>
		let r = fun {init} >>
		if (is-list {r})
		then (r | tail | head | unfold-right {fun} | cons {r | head})
		else ()
	) >>
	define zip = (fun =>
		case
		|| `$h0; $t0` =>
			case
			|| `$h1; $t1` => fun {h0} {h1}; zip {fun} {t0} {t1}
			|| anything => ()
		|| anything => anything => ()
	) >>
	define append =
		case
		|| `$h; $t` => cons {h} . append {t}
		|| anything => id
	>>
	define apply =
		fold-right {`.`} {id}
	>>
	define partition = (fun =>
		fold-right {
			e => `$l0, $l1` => if (fun {e}) then ((e; l0), l1) else (l0, (e; l1))
		} {(), ()}
	) >>
	define fold = (fun => list =>
		fold-left {fun} {list | head} {list | tail}
	) >>
	define filter = (fun =>
		fold-right {
			item => list => if (fun {item}) then (item; list) else list
		} {}
	) >>
	define get = (n =>
		head . (tail | repeat {n} | apply)
	) >>
	define heads =
		scan-left {cons/} {}
	>>
	define length =
		fold-left {v => e => v + 1} {0}
	>>
	define map = (fun =>
		fold-right {i => list => fun {i}; list} {}
	) >>
	define popen = (command => in =>
		do # in | _popen {command} | source
	) >>
	define reverse =
		fold-left {cons/} {}
	>>
	define substring = (start => end => list =>
		let len = length {list} >>
		let s = (if (start >= 0) then start else (len + start)) >>
		let e = (if (end > 0) then end else (len + end)) >>
		list | take {e} | drop {s}
	) >>
	define tails =
		scan-right {cons} {}
	>>
	define uniq =
		fold-right {item => list =>
			case
			|| (list = `item; $t`) list
			|| item; list
		} {}
	>>
	define concat =
		fold-right {append} {}
	>>
	define cross = (fun => l1 => l2 =>
		l1 | map {e1 => l2 | map {e1 | fun}}
	) >>
	define int-to-str = (i =>
		let unsigned-int-to-str =
			reverse
			. map {`+ 48`}
			. unfold-right {i => if (i != 0) then (i % 10; i / 10;) else ()}
		>> i |
			if (i > 0) then
				unsigned-int-to-str
			else-if (i < 0) then
				append {"-"} . unsigned-int-to-str . `0 -`
			else
				anything => "0"
	) >>
	define maximum =
		fold {greater}
	>>
	define merge = (merger => list =>
		let len = length {list} >>
		if (len > 1) then
			let `$list0, $list1` = take-drop {len / 2} {list} >>
			merger {list0 | merge {merger}} {list1 | merge {merger}}
		else list
	) >>
	define minimum =
		fold {lesser}
	>>
	define range = (start => end => inc =>
		unfold-right {i => if (i < end) then (i; i + inc;) else ()} {start}
	) >>
	define sh = (command =>
		"sh"; "-c"; command; | popen
	) >>
	define starts-with =
		case
		|| `$sh; $st` =>
			case
			|| `sh; $t` => starts-with {st} {t}
			|| anything => false
		|| anything => anything => true
	>>
	define split = (separator =>
		map {take-while {`!= separator`} . tail}
		. filter {`= separator` . head}
		. filter {not . `=` {}}
		. tails . cons {separator}
	) >>
	define transpose = (m =>
		let height = length {m} >>
		let width = if (height > 0) then (m | head | length) else 0 >>
		if (width > 0) then
			let w1 = width - 1 >>
			let gets = (tail | repeat {w1} | tails | reverse) >>
			gets | map {f => map {head . apply {f}} {m}}
		else ()
	) >>
	define contains = (m =>
		fold-left {or} {false} . map {m | starts-with} . tails
	) >>
	define dump as (:t => :t -> string) = skip-type-check (
		define type-of = ijavacls {atom:`CLASS!suite.lp.intrinsic.Intrinsics$TypeOf`} >>
		define atom-string = ijavacls {atom:`CLASS!suite.lp.intrinsic.Intrinsics$AtomString`} >>
		let dump0 = (prec => n =>
			let type = _ijavaobj1 {type-of} {n} >>
			if (n = ()) then
				"()"
			else-if (type = TREE) then
				concat {dump0 {true} {n | head}; "; "; dump0 {false} {n | tail};}
				| if prec then (s => concat {"("; s; ")";}) else id
			else-if (type = ATOM) then
				ijavaobj1 {atom-string} {n}
			else
				int-to-str {n}
		) >>
		dump0 {false}
	) >>
	define ends-with = (end =>
		starts-with {end | reverse} . reverse
	) >>
	define group =
		define group0 = (list0 => list1 =>
			if (list0 = `$k0, $v0; $t0`) then
				if (list1 = `$k1, $v1; $t1`) then
					case
					|| (k0 < k1) (k0, v0; group0 {t0} {list1})
					|| (k0 > k1) (k1, v1; group0 {list0} {t1})
					|| k0, append {v0} {v1}; group0 {t0} {t1}
				else list0
			else list1
		) >>
		merge {group0} . map {`$k, $v` => k, (v;)}
	>>
	define join = (separator =>
		concat . map {separator; | append/}
	) >>
	define quick-sort = (cmp =>
		case
		|| `$pivot; $t` =>
			let `$l0, $l1` = partition {cmp/ {pivot}} {quick-sort {cmp} {t}} >>
			concat {l0; (pivot;); l1;}
		|| anything => ()
	) >>
	define merge-sort =
		concat . map {second} . group . map {v => v, v}
	>>
	.p
) #

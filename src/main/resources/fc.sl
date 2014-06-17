-------------------------------------------------------------------------------
-- functional program compiler
--
-- .mode can be EAGER or LAZY

() :- import.path 'cg-generate-code.sl'
	, import.path 'fc-compile.sl'
	, import.path 'fc-infer-type.sl'
	, import.path 'fc-lazyify.sl'
	, import.path 'fc-optimize.sl'
	, import.path 'fc-parse.sl'
	, import.path 'fc-rewrite.sl'
	, import.path 'rbt.sl'
#

-- TODO perform cg-optimize but not in imported, precompiled code
compile-function .mode .do0 (PROC .c0,)
	:- !, fc-parse .do0 .do1
	, !, fc-infer-type-rule .do1 ()/()/() .tr/() _
	, !, fc-resolve-type-rules .tr
	, once (not is.cyclic .do1; fc-error "Cyclic data detected")
	, !, once (.mode = LAZY, fc-lazyify-fun .do1 .do2
		; .mode = EAGER, .do1 = .do2
	)
	, !, fc-optimize .do2 .do3
	, !, fc-compile .do3 0/() .c0/.c1/.c2/()/.reg
	, .c1 = (RETURN-VALUE .reg, .c2)
#

fc-load-library .lib .do0 .dox
	:- memoize .node0 (fc-load-library0 .lib .node0) (.do0 .dox,)
#

fc-load-library0 .lib .slfx
	:- fc-add-functions .lib .slfx, !
	; once (home.dir .homeDir
		, concat .homeDir "/src/main/resources/" .lib ".slf" .slfFilename
		, file.exists .slfFilename
		, file.read .slfFilename .slf
		, to.atom ".p" .var
		, concat .var " (" .slf .var ")" .slf1
		, parse .slf1 .slf2
		, generalize .slf2 .slfx
	; fc-error "Library" .lib "not found"
	)
#

fc-load-precompiled-library .lib .node
	:- memoize .node0 (fc-load-precompiled-library0 .lib .node0) (.node,)
#

fc-load-precompiled-library0 .lib .precompiled
	:- fc-precompiled-library-filename .lib .filename
	, persist.load .precompiled .filename
#

fc-precompiled-library-filename .lib .filename
	:- home.dir .homeDir
	, concat .homeDir "/precompiled/" .lib ".node.gz" .filename
#

fc-frame-difference .frame0 .frame1 0
	:- same .frame0 .frame1, !
#
fc-frame-difference .frame0 (.frame1 + 1) .frameDiff
	:- not is.tree .frame0, !
	, fc-frame-difference .frame0 .frame1 .frameDiff0
	, let .frameDiff (.frameDiff0 - 1)
#
fc-frame-difference (.frame0 + 1) (.frame1 + 1) .frameDiff
	:- !, fc-frame-difference .frame0 .frame1 .frameDiff
#

fc-define-default-fun 1 +callintrn0 CALL-INTRINSIC #
fc-define-default-fun 2 +callintrn1 CALL-INTRINSIC #
fc-define-default-fun 3 +callintrn2 CALL-INTRINSIC #
fc-define-default-fun 4 +callintrn3 CALL-INTRINSIC #
fc-define-default-fun 2 +compare COMPARE #
fc-define-default-fun 1 +getintrn GET-INTRINSIC #
fc-define-default-fun 2 +lcons CONS-LIST #
fc-define-default-fun 1 +lhead HEAD #
fc-define-default-fun 1 +ltail TAIL #
fc-define-default-fun 2 +pcons CONS-PAIR #
fc-define-default-fun 1 +pleft HEAD #
fc-define-default-fun 1 +pright TAIL #
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

fc-error .m :- !, write.error .m, nl, fail #

fc-dict-get .v .t :- rbt-get .v .t, ! #

-- use replace, necessary to redefine already-defined variables
fc-dict-add .v .t0/.t1 :- rbt-replace .v .t0/.t1, ! #

fc-dict-union-bind .t0 .t1 .t2 :- rbt-union-bind .t0 .t1 .t2, ! #

fc-dict-union-insert .t0 .t1 .t2 :- rbt-union-insert .t0 .t1 .t2, ! #

fc-dict-union-replace .t0 .t1 .t2 :- rbt-union-replace .t0 .t1 .t2, ! #

fc-dict-member .v .t :- rbt-member .v .t #

-- There are few functions that are not pure: callintrn*
-- Logs are considered 'invisible', so they are not counted.

fc-add-functions STANDARD .p (
	data (optional {:t}) over :t as None >>
	data (optional {:t}) over :t as (Value :t) >>
	define callintrn0 := name => +callintrn0 {name} >>
	define callintrn1 := name => p0 => +callintrn1 {name} {p0} >>
	define callintrn2 := name => p0 => p1 => +callintrn2 {name} {p0} {p1} >>
	define callintrn3 := name => p0 => p1 => p2 => +callintrn3 {name} {p0} {p1} {p2} >>
	define compare := a => b => +compare {a} {b} >>
	define cons := head => tail => +lcons {head} {tail} >>
	define first := tuple => +pleft {tuple} >>
	define head := list => +lhead {list} >>
	define getintrn := name => +getintrn {name} >>
	define second := tuple => +pright {tuple} >>
	define tail := list => +ltail {list} >>
	define +popen := ([string] -> string -> data-of Stream) of
		atom:CLASS!suite.lp.intrinsic.MonadIntrinsics$Popen | getintrn | callintrn2
	>>
	define log := (:t => :t -> :t) of
		atom:CLASS!suite.lp.intrinsic.Intrinsics$Log1 | getintrn | callintrn1
	>>
	define log2 := (:t => string -> :t -> :t) of
		atom:CLASS!suite.lp.intrinsic.Intrinsics$Log2 | getintrn | callintrn2
	>>
	define source := (data-of Stream -> string) of
		atom:CLASS!suite.lp.intrinsic.Intrinsics$Source_ | getintrn | callintrn1
	>>
	define throw := (any -> any) of
		atom:CLASS!suite.lp.intrinsic.Intrinsics$Throw | getintrn | callintrn1
	>>
	define and := x => y =>
		if x then y else false
	>>
	define drop := n => list =>
		if (n > 0 && is-list {list})
		then (list | tail | drop {n - 1})
		else list
	>>
	define flip := f => x => y =>
		f {y} {x}
	>>
	define fold-left := fun => init => -- possible for tail call optimization in eager mode
		case
		|| `$h; $t` => fold-left {fun} {fun {init} {h}} {t}
		|| anything => init
	>>
	define fold-right := fun => init => -- possible for short-circuit evaluation in lazy mode
		case
		|| `$h; $t` => fun {h} {fold-right {fun} {init} {t}}
		|| anything => init
	>>
	define greater := a => b =>
		if (a > b) then a else b
	>>
	define id := v =>
		v
	>>
	define invoke := f => x =>
		f {x}
	>>
	define iterate := f => init =>
		init | f | iterate {f} | `init;`
	>>
	define lesser := a => b =>
		if (a > b) then b else a
	>>
	define not := x =>
		if x then false else true
	>>
	define or := x => y =>
		if x then true else y
	>>
	define repeat := e =>
		e; repeat {e}
	>>
	define scan-left := fun => init =>
		case
		|| `$h; $t` => init; scan-left {fun} {fun {init} {h}} {t}
		|| anything => init;
	>>
	define scan-right := fun => init =>
		case
		|| `$h; $t` =>
			let r := scan-right {fun} {init} {t} >>
			fun {h} {head {r}}; r
		|| anything => init;
	>>
	define str-to-int := s =>
		let unsigned-str-to-int := fold-left {v => d => v * 10 + d - 48} {0} >>
			if (is-list {s} && head {s} = +'-')
			then (`0 -` . unsigned-str-to-int . tail)
			else unsigned-str-to-int
		{s}
	>>
	define take := n => list =>
		if (n > 0 && is-list {list})
		then (list | tail | take {n - 1} | cons {list | head})
		else ()
	>>
	define take-drop := n => list =>
		if (n > 0 && is-list {list}) then
			let `$t1, $d1` := list | tail | take-drop {n - 1} >>
			cons {list | head} {t1}, d1
		else (, list)
	>>
	define unfold-left := (:a => :b => (:a -> optional {:a, :b}) -> :a -> [:b]) of (
		define unfold-left0 := (:a => :b => [:b] -> (:a -> optional {:a, :b}) -> :a -> [:b]) of (
			list => fun => init =>
				if (fun {init} = `Value ($init1, $elem)`)
				then (unfold-left0 {elem; list} {fun} {init1})
				else list
		) >>
		unfold-left0 {}
	) >>
	define unfold-right := (:a => :b => (:a -> optional {:b, :a}) -> :a -> [:b]) of (
		fun => init =>
			if (fun {init} = `Value ($e, $init1)`)
			then (init1 | unfold-right {fun} | cons {e})
			else ()
	) >>
	define zip := fun => list0 => list1 =>
		unfold-right {
			case
			|| `($h0; $t0), ($h1; $t1)` => Value (fun {h0} {h1}, (t0, t1))
			|| anything => None
		} {list0, list1}
	>>
	define append :=
		fold-right {cons} | flip
	>>
	define apply :=
		fold-right {`.`} {id}
	>>
	define bisect := fun =>
		fold-right {
			h => `$t0, $f0` =>
			if (fun {h}) then ((h; t0), f0) else (t0, (h; f0))
		} {(), ()}
	>>
	define fold := fun => list =>
		fold-left {fun} {list | head} {list | tail}
	>>
	define filter := fun =>
		fold-right {
			item => list => if (fun {item}) then (item; list) else list
		} {}
	>>
	define heads :=
		scan-left {cons/} {}
	>>
	define length :=
		fold-left {v => e => v + 1} {0}
	>>
	define map := fun =>
		fold-right {cons . fun} {}
	>>
	define partition := fun =>
		fold-right {
			e => `$l0, $l1` => if (fun {e}) then ((e; l0), l1) else (l0, (e; l1))
		} {(), ()}
	>>
	define popen := command => in =>
		return # in | +popen {command} | source
	>>
	define replicate := flip {e =>
		unfold-left {i => if (i != 0) then (Value (i - 1, e)) else None}
	} >>
	define reverse :=
		fold-left {cons/} {}
	>>
	define substring := start => end => list =>
		let len := length {list} >>
		let s := if (start >= 0) then start else (len + start) >>
		let e := if (end > 0) then end else (len + end) >>
		list | take {e} | drop {s}
	>>
	define tails :=
		scan-right {cons} {}
	>>
	define take-while := fun =>
		fold-right {h => if (fun {h}) then (cons {h}) else (t => ())} {}
	>>
	define uniq :=
		fold-right {item => list =>
			case
			|| (list = `item; $t`) list
			|| item; list
		} {}
	>>
	define concat :=
		fold-right {append} {}
	>>
	define cross := fun => l1 => l2 =>
		l1 | map {e1 => l2 | map {e1 | fun}}
	>>
	define get := n =>
		head . (tail | replicate {n} | apply)
	>>
	define int-to-str := i =>
		let unsigned-int-to-str :=
			reverse
			. map {`+ +'0'`}
			. unfold-right {i => if (i != 0) then (Value (i % 10, i / 10)) else None}
		>> i |
			if (i > 0) then
				unsigned-int-to-str
			else-if (i < 0) then
				append {"-"} . unsigned-int-to-str . `0 -`
			else
				anything => "0"
	>>
	define maximum :=
		fold {greater}
	>>
	define merge := merger => list =>
		let len := length {list} >>
		if (len > 1) then
			let `$list0, $list1` := take-drop {len / 2} {list} >>
			merger {list0 | merge {merger}} {list1 | merge {merger}}
		else list
	>>
	define minimum :=
		fold {lesser}
	>>
	define range := start => end => inc =>
		unfold-right {i => if (i < end) then (Value (i, i + inc)) else None} {start}
	>>
	define sh := command =>
		"sh"; "-c"; command; | popen
	>>
	define starts-with :=
		case
		|| `$sh; $st` =>
			case
			|| `sh; $t` => starts-with {st} {t}
			|| anything => false
		|| anything => anything => true
	>>
	define split := separator =>
		map {take-while {`!= separator`} . tail}
		. filter {`= separator` . head}
		. filter {not . `=` {}}
		. tails . cons {separator}
	>>
	define transpose := m =>
		let height := length {m} >>
		let width := if (height > 0) then (m | head | length) else 0 >>
		if (width > 0) then
			let w1 := width - 1 >>
			let gets := tail | replicate {w1} | tails | reverse >>
			gets | map {f => map {head . apply {f}} {m}}
		else ()
	>>
	define contains := m =>
		fold-left {or} {false} . map {m | starts-with} . tails
	>>
	define dump := (:t => :t -> string) of skip-type-check (
		define type-of := getintrn {atom:CLASS!suite.lp.intrinsic.Intrinsics$TypeOf} >>
		define atom-string := getintrn {atom:CLASS!suite.lp.intrinsic.Intrinsics$AtomString} >>
		let dump0 := prec => n =>
			let type := +callintrn1 {type-of} {n} >>
			if (n = ()) then
				"()"
			else-if (type = TREE) then
				concat {dump0 {true} {n | head}; "; "; dump0 {false} {n | tail};}
				| if prec then (s => concat {"("; s; ")";}) else id
			else-if (type = ATOM) then
				callintrn1 {atom-string} {n}
			else
				int-to-str {n}
		>>
		dump0 {false}
	) >>
	define ends-with := end =>
		starts-with {end | reverse} . reverse
	>>
	define group :=
		define group0 := list0 => list1 =>
			if (list0 = `$k0, $v0; $t0`) then
				if (list1 = `$k1, $v1; $t1`) then
					case
					|| (k0 < k1) (k0, v0; group0 {t0} {list1})
					|| (k0 > k1) (k1, v1; group0 {list0} {t1})
					|| k0, append {v0} {v1}; group0 {t0} {t1}
				else list0
			else list1
		>>
		merge {group0} . map {`$k, $v` => k, (v;)}
	>>
	define join := separator =>
		concat . map {separator; | append/}
	>>
	define quick-sort := cmp =>
		case
		|| `$pivot; $t` =>
			let `$l0, $l1` := partition {cmp/ {pivot}} {quick-sort {cmp} {t}} >>
			concat {l0; (pivot;); l1;}
		|| anything => ()
	>>
	define merge-sort :=
		concat . map {second} . group . map {v => v, v}
	>>
	.p
) #

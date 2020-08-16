'use strict';

// https://www.colourlovers.com/palette/373610/Melon_Ball_Surprise
let cellBackground = '#DDDDDD';
let palette = ['#D1F2A5', '#EFFAB4', '#FFC48C', '#FF9F80', '#F56991',];

let icons = [ '🍋', '🌲', '💖', '🐬', '🐵', '🍊', '🍇', '💮', '✴️', ];
icons[-1] = '💀';
// 😂

let fiveinarow = evalscripts(['fun', 'render',])
.then(ns => (cc, view) => {
	let { fun: { rand, }, render: { renderAgain, }, } = ns;

	let freeze = false; // if we are accepting game inputs

	let randomstones = n => {
		let stones = [];
		for (let i = 0; i < n; i++) stones.push({ d: rand(0, nStoneTypes), });
		return stones;
	};

	let mutate = (() => {
		let emptycount = vm => {
			let n = 0;
			cc.for_xy((x, y,) => n += vm.board[x][y].d != null ? 0 : 1);
			return n;
		};

		let setcell = (vm, vmc1) => {
			let vmt0 = vm.board;
			vmt0 = vmt0 != null ? vmt0 : cc.arrayx();
			let vmr0 = vmt0[vmc1.x];
			vmr0 = vmr0 != null ? vmr0 : cc.arrayy(vmc1.x);
			// let vmr1 = vmr0.map(vmc => vmc != vmc0 ? vmc : vmc1);
			// let vmt1 = vmt0.map(vmr => vmr != vmr0 ? vmr : vmr1);
			// return { ...vm, board: vmt1, };
			return { ...vm, board: { ...vmt0, [vmc1.x]: { ...vmr0, [vmc1.y]: vmc1, }, }, };
		};

		return {
			checkfiveinarow: vm => {
				let eating = {};
				if (!freeze)
					for (let [dx, dy] of eatdirs)
						(0 < dx * 1000 + dy ? cc.for_xy : cc.back_xy)((x, y,) => {
							let step = 0;
							let x1, y1;
							while (true
								&& (x1 = x + step * dx) != null
								&& (y1 = y + step * dy) != null
								&& cc.inbounds(x1, y1)
								&& vm.board[x][y].d != null
								&& vm.board[x][y].d == vm.board[x1][y1].d) step++;
							if (5 <= step)
								for (let i = 0; i < step; i++) {
									let x_ = x + i * dx;
									let y_ = y + i * dy;
									eating[`${x_},${y_}`] = vm.board[x_][y_];
								}
						});
				let list = Object.values(eating);
				for (let eaten of list)
					vm = setcell(vm, { ...eaten, d: null, });
				vm = { ...vm, score: vm.score + list.length, };
				document.title = `${vm.score} - Five in a row`;
				return { isFiveInARow: 0 < list.length, vm, };
			},
			drop: (vm, stones) => {
				if (stones.length < emptycount(vm))
					for (let stone of stones)
						while(true) {
							let { x, y, } = cc.random_xy();
							if (vm.board[x][y].d == null) {
								vm = setcell(vm, { ...vm.board[x][y], d: stone.d, });
								break;
							}
						}
				else {
					freeze = true;
					cc.for_xy((x, y,) => {
						if (vm.board[x][y].d == null)
							vm = setcell(vm, { ...vm.board[x][y], d: -1, });
					});
					vm = { ...vm, notification: { c: 4, message: 'game over', }, };
				}
				return vm;
			},
			moveonestep: (vm, fr, to) => {
				let vmc0 = vm.board[fr.x][fr.y];
				let vmc1 = vm.board[to.x][to.y];
				let d0 = vmc0.d;
				let d1 = vmc1.d;
				vm = setcell(vm, { ...vmc0, d: d1, });
				vm = setcell(vm, { ...vmc1, d: d0, });
				return vm;
			},
			setcell,
		};
	})();

	let change = f => renderAgain(view, vm0 => {
		let vm1 = f(vm0);
		// console.log(vm1);
		return vm1;
	});

	let movefromto = (vmc0, vmcx) => {
		let board;
		change(vm => { board = vm.board; return vm; });

		let search = (xy0, xyx, isMovable) => {
			let key = xy => `${xy.x},${xy.y}`;
			let todos = [{ x: xy0.x, y: xy0.y, prev: null, },];
			let dones = {};
			let kx = key(xyx);
			while (0 < todos.length) { // breadth-first search
				let todo = todos.shift();
				let { x, y, } = todo;
				let k = key(todo);
				if (!dones.hasOwnProperty(k)) {
					let neighbours = movedirs
						.map(([dx, dy]) => ({ x: x + dx, y: y + dy, prev: todo, }))
						.filter(({ x, y, }) => cc.inbounds(x, y) && isMovable(x, y));
					todos.push(...neighbours);
					dones[k] = todo;
					if (k == kx) return todo;
				}
			}
			return null;
		};

		let node = search(vmc0, vmcx, (x, y,) => board[x][y].d == null);

		let rec = (path, cb) => {
			let prev = path.prev;
			if (prev != null)
				rec(prev, () => {
					let timeout = setTimeout(() => {
						change(vm => mutate.moveonestep(vm, prev, path));
						cb();
						clearTimeout(timeout);
					}, 50);
				});
			else
				cb();
		};

		if (node != null) {
			freeze = true;
			rec(node, () => {
				freeze = false;
				change(vm_ => {
					let { isFiveInARow, vm, } = mutate.checkfiveinarow(vm_);
					if (!isFiveInARow) {
						vm = mutate.drop(vm, vm.nextstones);
						vm = { ...vm, nextstones: randomstones(3), };
						vm = mutate.checkfiveinarow(vm).vm;
					}
					return vm;
				});
			});
		} else
			console.log('no path between', vmc0, vmcx);
	};

	let vw = {
		change,
		init: () => change(vm_ => {
			freeze = false;
			let vm = {
				nextstones: randomstones(3),
				notification: null, // { c: 1, message: 'welcome!', },
				score: 0,
				select_xy: null,
			};
			cc.for_xy((x, y,) => vm = mutate.setcell(vm, { x, y, d: null, }));
			return mutate.drop(vm, randomstones(Math.ceil(cc.area * .3)));
		}),
		movefromto,
		select: (x, y) => {
			change(vm => {
				let vmc = vm.board[x][y];
				vm = mutate.setcell(vm, { ...vmc, selected: true, });
				return { ...vm, select_xy: { x, y, }, };
			});
		},
		unselect: () => {
			let select_xy0;
			change(vm => {
				select_xy0 = vm.select_xy;
				if (select_xy0 != null) {
					let vmc = vm.board[select_xy0.x][select_xy0.y];
					vm = mutate.setcell(vm, { ...vmc, selected: false, });
				}
				return { ...vm, select_xy: null };
			});
			return select_xy0;
		},
	}

	let handle = (() => {
		let dragsource = null;
		let dragtarget = null;

		return {
			click: (ev, vmc) => {
				if (!freeze) {
					let select_xy0 = vw.unselect();

					if (vmc.d != null)
						vw.select(vmc.x, vmc.y);
					else if (select_xy0 != null)
						vw.movefromto(select_xy0, vmc);
				}
			},
			clicknotification: (ev, vm) => vw.init(),
			close: () => vw.change(vm => null),
			dragend: (ev, vm) => dragsource == dragtarget || vw.movefromto(dragsource, dragtarget),
			dragenter: (ev, vm) => dragtarget = vm,
			dragstart: (ev, vm) => {
				dragsource = vm;

				let dragIcon = document.createElement('span');
				dragIcon.style.background = cellBackground;
				dragIcon.style.fontSize = '18px';
				dragIcon.style.height = '40px';
				dragIcon.style.width = '30px';
				dragIcon.innerText = icons[vm.d];

				// let dragIcon = document.createElement('img');
				// dragIcon.src = 'http://img3.wikia.nocookie.net/__cb20140410195936/pokemon/images/archive/e/e1/20150101093317!025Pikachu_OS_anime_4.png';
				// dragIcon.style.width = '500px';

				let div = document.createElement('div');
				div.style.left = '-500px';
				div.style.position = 'absolute';
				div.style.top = '-500px';
				div.appendChild(dragIcon);

				document.querySelector('body').appendChild(div);

				ev.dataTransfer.setData('text/plain', '');
				ev.dataTransfer.setDragImage(div, 15, 20);
			},
		};
	})();

	return { handle, vw, };
})
.then(gamef => ({ gamef, icons, palette, }))
.catch(console.error);

loadedmodule = fiveinarow;

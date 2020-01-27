'use strict';

const usp = new URLSearchParams(window.location.search);
const colorsp = usp.get('colors');
const sizep = usp.get('size');

let size = sizep != null ? +sizep : 7;
let nStoneTypes = colorsp != null ? +colorsp : 5;

let sizex = size;
let sizey = size;
let startx = 0, endx = startx + sizex;
let starty = 0, endy = starty + sizey;

let rand = n => Math.floor(Math.random() * n);

let randomstones = n => {
	let stones = [];
	for (let i = 0; i < n; i++) stones.push({ d: rand(nStoneTypes) });
	return stones;
};

let cc = {
	back_xy: f => {
		for (let x = endx - 1; startx <= x; x--)
			for (let y = endy - 1; starty <= y; y--)
				f(x, y);
	},
	for_xy: f => {
		for (let x = startx; x < endx; x++)
			for (let y = starty; y < endy; y++)
				f(x, y);
	},
	inbounds: (x, y) => startx <= x && x < endx && starty <= y && y < endy,
	random_xy: () => ({ x: startx + rand(sizex), y: starty + rand(sizey), }),
};

let freeze = false; // if we are accepting game inputs

let mutate = (() => {
	let setcell = (vm, vmc1) => {
		let vmt0 = vm.board;
		vmt0 = vmt0 != null ? vmt0 : { length: endx };
		let vmr0 = vmt0[vmc1.x];
		vmr0 = vmr0 != null ? vmr0 : { length: endy };
		// let vmr1 = vmr0.map(vmc => vmc != vmc0 ? vmc : vmc1);
		// let vmt1 = vmt0.map(vmr => vmr != vmr0 ? vmr : vmr1);
		// return { ...vm, board: vmt1 };
		return { ...vm, board: { ...vmt0, [vmc1.x]: { ...vmr0, [vmc1.y]: vmc1 } } };
	};

	let checkfiveinarow = vm => {
		let isFiveInARow = false;
		if (!freeze)
			for (let [dx, dy] of eatdirs)
				(0 < dx * sizey + dy ? cc.for_xy : cc.back_xy)((x, y) => {
					let step = 0;
					let x1, y1;
					while (true
						&& (x1 = x + step * dx) != null
						&& (y1 = y + step * dy) != null
						&& cc.inbounds(x1, y1)
						&& vm.board[x][y].d != null
						&& vm.board[x][y].d == vm.board[x1][y1].d) step++;
					if (5 <= step) {
						isFiveInARow = true;
						for (let i = 0; i < step; i++)
							vm = setcell(vm, { ...vm.board[x + i * dx][y + i * dy], d: null, });
						vm = { ...vm, score: vm.score + step };
						document.title = `${vm.score} - Five in a row`;
					}
				});
		return { isFiveInARow, vm };
	};

	return {
		checkfiveinarow,
		drop: (vm, stones) => {
			if (stones.length < mutate.emptycount(vm))
				for (let stone of stones)
					while(true) {
						let { x, y } = cc.random_xy();
						if (vm.board[x][y].d == null) {
							vm = setcell(vm, { ...vm.board[x][y], d: stone.d });
							break;
						}
					}
			else {
				freeze = true;
				cc.for_xy((x, y) => {
					if (vm.board[x][y].d == null)
						vm = setcell(vm, { ...vm.board[x][y], d: -1 });
				});
				vm = { ...vm, notification: { c: 4, message: 'game over', } };
			}
			return vm;
		},
		emptycount: vm => {
			let n = 0;
			cc.for_xy((x, y) => n += vm.board[x][y].d != null ? 0 : 1);
			return n;
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

let vw = (() => {
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
			let todos = [{ x: xy0.x, y: xy0.y, prev: null, }];
			let dones = {};
			let kx = key(xyx);
			while (0 < todos.length) { // breadth-first search
				let todo = todos.shift();
				let { x, y, } = todo;
				let k = key(todo);
				if (!dones.hasOwnProperty(k)) {
					let neighbours = movedirs
						.map(([dx, dy]) => ({ x: x + dx, y: y + dy, prev: todo, }))
						.filter(({ x, y }) => startx <= x && x < endx && starty <= y && y < endy && isMovable(x, y));
					todos.push(...neighbours);
					dones[k] = todo;
					if (k == kx) return todo;
				}
			}
			return null;
		};

		let node = search(vmc0, vmcx, (x, y) => board[x][y].d == null);

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
					let { isFiveInARow, vm } = mutate.checkfiveinarow(vm_);
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

	return {
		change,
		init: () => change(vm_ => {
			let vm = {
				//board: range(startx, endx).map(x => range(starty, endy).map(y => ({ d: null, x, y, })).list()).list(),
				nextstones: randomstones(3),
				notification: null, // { c: 1, message: 'welcome!', },
				score: 0,
				select_xy: null,
			};
			cc.for_xy((x, y) => vm = mutate.setcell(vm, { x, y, d: null, }));
			return mutate.drop(vm, randomstones(Math.ceil(sizex * sizey * .3)));
		}),
		movefromto,
		select: (x, y) => {
			change(vm => {
				let vmc = vm.board[x][y];
				vm = mutate.setcell(vm, { ...vmc, selected: true, });
				return { ...vm, select_xy: {x, y} };
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
})();

let handleclick = (vmc, ev) => {
	if (!freeze) {
		let select_xy0 = vw.unselect();

		if (vmc.d != null)
			vw.select(vmc.x, vmc.y);
		else if (select_xy0 != null)
			vw.movefromto(select_xy0, vmc);
	}
};

let handleclose = () => vw.change(vm => null);

// https://www.colourlovers.com/palette/373610/Melon_Ball_Surprise
let palette = ['#D1F2A5', '#EFFAB4', '#FFC48C', '#FF9F80', '#F56991',];

let icons = [ '🍋', '🌲', '💖', '🐬', '🐵', '🍊', '🍇', '💮', '✴️', ];
icons[-1] = '💀';
// 😂

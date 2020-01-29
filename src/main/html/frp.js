'use strict';

let frp = function() {
	let newPusher = () => { // FRP dispatcher
		let pushees = [];
		let push_ = data => { for (let pushee of pushees) pushee(data); };
		let wire_ = pushee => pushees.push(pushee);

		let redirect_ = tf => {
			let pusher = newPusher();
			wire_(data => tf(data, pusher.push));
			return pusher;
		};

		return {
			append: other => {
				let pusher = newPusher();
				wire_(pusher.push);
				other.wire(pusher.push);
				return pusher;
			},
			close: () => pushees = [], // for garbage collection
			concatmap: f => redirect_((data, push) => f(data).wire(push)),
			delay: time => redirect_((data, push) => setTimeout(() => push(data), time)),
			edge: () => {
				let data_;
				return redirect_((data, push) => {
					if (data != data_) push(data);
					data_ = data;
				});
			},
			filter: f => redirect_((data, push) => { if (f(data)) push(data); }),
			fold: (f, value) => redirect_((data, push) => push(value = f(value, data))),
			last: () => {
				let data_;
				wire_(data => data_ = data);
				return () => data_;
			},
			map: f => redirect_((data, push) => push(f(data))),
			merge: (other, f) => {
				let v0, v1;
				let pusher = newPusher();
				let push1 = () => pusher.push(f(v0, v1));
				wire_(data => { v0 = data; push1(); });
				f.wire(data => { v1 = data; push1(); });
				return pusher;
			},
			push: push_,
			read: () => {
				let list = [];
				wire_(data => list.push(data));
				return () => {
					let r = read(list);
					list = [];
					return r;
				};
			},
			redirect: redirect_,
			resample: commander => {
				let data_;
				wire_(data => data_ = data);
				return commander.redirect((data, push) => push(data_));
			},
			unique: () => {
				let list = [];
				return redirect_((data, push) => {
					if (!read(list).fold(false, (b_, d) => b_ || e == data)) {
						push(data);
						list.push(data);
					}
				});
			},
			wire: wire_,
		};
	};

	let kbkeypushers = {};
	let mouseclickpusher = newPusher();
	let mousemovepusher = newPusher();
	let motionpusher = newPusher();
	let orientationpusher = newPusher();

	let kbpressed_ = (e, down) => {
		let pusher = kbkeypushers[(!(e.which)) ? e.keyCode : (e.which ? e.which : 0)];
		if (pusher) pusher.push(down);
	};

	document.onkeydown = e => kbpressed_(e, true);
	document.onkeyup = e => kbpressed_(e, false);
	document.onmousedown = e => mouseclickpusher.push(true);
	document.onmousemove = e => {
		let e1 = (!e) ? window.event : e;
		let x;
		let y;
		if (e1.pageX || e1.pageY) {
			x = e1.pageX;
			y = e1.pageY;
		} else if (e1.clientX || e1.clientY) {
			x = e1.clientX + document.body.scrollLeft + document.documentElement.scrollLeft;
			y = e1.clientY + document.body.scrollTop + document.documentElement.scrollTop;
		}
		mousemovepusher.push({ x: x, y: y });
	};
	document.onmouseup = e => mouseclickpusher.push(false);

	if (window.DeviceMotionEvent)
		window.addEventListener('devicemotion', e => {
			motionpusher.push({
				a: e.acceleration,
				aig: e.accelerationIncludingGravity,
				rr: e.rotationRate,
				interval: e.interval,
			});
		}, false);
	else log('device motion not supported');

	if (window.DeviceOrientationEvent)
		window.addEventListener('deviceorientation', e => {
			orientationpusher.push({
				lr: e.gamma, // the left-to-right tilt in degrees, where right is positive
				fb: e.beta, // the front-to-back tilt in degrees, where front is positive
				dir: e.alpha, // the compass direction the device is facing in degrees
			});
		}, false);
	else log('device orientation not supported');

	let keypressed = keycode => {
		let pusher;
		if (!(pusher = kbkeypushers[keycode])) pusher = kbkeypushers[keycode] = newPusher();
		return pusher;
	};
	let keydownpusher = keypressed(40).map(d => d ? 1 : 0);
	let keyleftpusher = keypressed(37).map(d => d ? -1 : 0);
	let keyrightpusher = keypressed(39).map(d => d ? 1 : 0);
	let keyuppusher = keypressed(38).map(d => d ? -1 : 0);

	return {
		animframe: () => {
			let pusher = newPusher();
			let tick = () => {
				pusher.push(true);
				requestAnimationFrame(tick);
			}
			requestAnimationFrame(tick);
			return pusher;
		},
		fetch: (input, init) => {
			let pusher = newPusher();
			fetch(input, init)
				.then(response => pusher.push(response.json()))
				.catch(error => console.error(error));
		},
		http: url => {
			let pusher = newPusher();
			let xhr = new XMLHttpRequest();
			xhr.addEventListener('load', () => { pusher.push(this.responseText); });
			xhr.open('GET', url);
			xhr.send();
		},
		kb: {
			arrowx: keyleftpusher.append(keyrightpusher), // .fold(0, (a, b) => a + b).last();
			arrowy: keyuppusher.append(keydownpusher), // .fold(0, (a, b) => a + b).last();
			keypressed: keypressed,
		},
		motion: motionpusher,
		mouse: {
			click: mouseclickpusher,
			move: mousemovepusher,
		},
		orientation: orientationpusher,
		tick: timeout => {
			let pusher = newPusher();
			let tick = () => {
				pusher.push(true);
				setTimeout(tick, timeout);
			}
			setTimeout(tick, timeout);
			return pusher;
		},
	};
} ();

frp;

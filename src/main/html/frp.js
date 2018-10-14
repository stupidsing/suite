"use strict";

let frp = function() {
	let signal = () => { // FRP dispatcher
		let receivers = [];
		let fire_ = data => { for (let receiver of receivers) receiver(data); };
		let redirect_ = tf => {
			let signal1 = signal();
			wire_(data => tf(data, signal1));
			return signal1;
		};
		let wire_ = receiver => receivers.push(receiver);
		return {
			append: signal_ => {
				let signal1 = signal();
				wire_(signal1.fire);
				signal_.wire(signal1.fire);
				return signal1;
			},
			close: () => receivers = [], // for garbage collection
			concatmap: f => redirect_((data, signal1) => f(data).wire(signal1.fire)),
			delay: time => redirect_((data, signal1) => setTimeout(() => signal1.fire(data), time)),
			edge: () => {
				let data_;
				return redirect_((data, signal1) => {
					if(data != data_) signal1.fire(data);
					data_ = data;
				});
			},
			filter: f => redirect_((data, signal1) => { if (f(data)) signal1.fire(data); }),
			fire: fire_,
			fold: (f, value) => redirect_((data, signal1) => signal1.fire(value = f(value, data))),
			last: () => {
				let data_;
				wire_(data => data_ = data);
				return () => data_;
			},
			map: f => redirect_((data, signal1) => signal1.fire(f(data))),
			merge: (signal_, f) => {
				let v0, v1;
				let signal1 = signal();
				let fire1 = () => signal1.fire(f(v0, v1));
				wire_(data => { v0 = data; fire1(); });
				f.wire(data => { v1 = data; fire1(); });
				return signal1;
			},
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
			resample: signal_ => {
				let data_;
				wire_(data => data_ = data);
				return signal_.redirect((data, signal1) => signal1.fire(data_));
			},
			unique: () => {
				let list = [];
				return redirect_((data, signal1) => {
					if (!read(list).fold(false, (b_, d) => b_ || e == data)) {
						signal1.fire(data);
						list.push(data);
					}
				});
			},
			wire: wire_,
		};
	};

	let kbkeysignals = {};
	let mouseclicksignal = signal();
	let mousemovesignal = signal();
	let motionsignal = signal();
	let orientationsignal = signal();

	let kbpressed_ = (e, down) => {
		let signal = kbkeysignals[(!(e.which)) ? e.keyCode : (e.which ? e.which : 0)];
		if (signal) signal.fire(down);
	};

	document.onkeydown = e => kbpressed_(e, true);
	document.onkeyup = e => kbpressed_(e, false);
	document.onmousedown = e => mouseclicksignal.fire(true);
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
		mousemovesignal.fire({ x: x, y: y });
	};
	document.onmouseup = e => mouseclicksignal.fire(false);

	if (window.DeviceMotionEvent)
		window.addEventListener("devicemotion", e => {
			motionsignal.fire({
				a: e.acceleration,
				aig: e.accelerationIncludingGravity,
				rr: e.rotationRate,
				interval: e.interval,
			});
		}, false);
	else log("device motion not supported");

	if (window.DeviceOrientationEvent)
		window.addEventListener("deviceorientation", e => {
			orientationsignal.fire({
				lr: e.gamma, // the left-to-right tilt in degrees, where right is positive
				fb: e.beta, // the front-to-back tilt in degrees, where front is positive
				dir: e.alpha, // the compass direction the device is facing in degrees
			});
		}, false);
	else log("device orientation not supported");

	let keypressed = keycode => {
		let signal_;
		if (!(signal_ = kbkeysignals[keycode])) signal_ = kbkeysignals[keycode] = signal();
		return signal_;
	};
	let keydownsignal = keypressed(40).map(d => d ? 1 : 0);
	let keyleftsignal = keypressed(37).map(d => d ? -1 : 0);
	let keyrightsignal = keypressed(39).map(d => d ? 1 : 0);
	let keyupsignal = keypressed(38).map(d => d ? -1 : 0);

	return {
		animframe: () => {
			let signal_ = signal();
			let tick = () => {
				signal_.fire(true);
				requestAnimationFrame(tick);
			}
			requestAnimationFrame(tick);
			return signal_;
		},
		fetch: (input, init) => {
			let signal_ = signal();
			fetch(input, init)
				.then(response => signal_.fire(response.json()))
				.catch(error => console.error(error));
		},
		http: url => {
			let signal_ = signal();
			let xhr = new XMLHttpRequest();
			xhr.addEventListener('load', () => { signal_.fire(this.responseText); });
			xhr.open('GET', url);
			xhr.send();
		},
		kb: {
			arrowx: keyleftsignal.append(keyrightsignal), // .fold(0, (a, b) => a + b).last();
			arrowy: keyupsignal.append(keydownsignal), // .fold(0, (a, b) => a + b).last();
			keypressed: keypressed,
		},
		motion: motionsignal,
		mouse: {
			click: mouseclicksignal,
			move: mousemovesignal,
		},
		orientation: orientationsignal,
		tick: timeout => {
			let signal_ = signal();
			let tick = () => {
				signal_.fire(true);
				setTimeout(tick, timeout);
			}
			setTimeout(tick, timeout);
			return signal_;
		},
	};
} ();

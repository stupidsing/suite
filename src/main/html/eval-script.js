'use strict';

let loadedmodule = null;

let { evalscript, loadsrcscript, } = (() => {
	let cache = {};

	let loadsrcscript = url => new Promise((resolve, reject) => {
		let cb = () => resolve(cache[url] = loadedmodule);

		let script = document.createElement('script');
		script.onreadystatechange = cb;
		script.onload = cb;
		script.src = url;
		script.type = 'text/javascript';

		document.head.appendChild(script);
	});

	let evalscript = url => {
		let r = cache[url];

		if (r != null)
			return Promise.resolve(r);
		else if (window.hasOwnProperty('cordova'))
			return loadsrcscript(url);
		else {
			if (document.URL.startsWith('file://'))
				url = 'https://raw.githubusercontent.com/stupidsing/suite/master/src/main/html/' + url;

			return fetch(url, {
				cache: 'default',
				credentials: 'omit',
				method: 'GET',
				mode: 'cors',
				redirect: 'follow',
				referrer: 'no-referrer',
			})
			.then(response => {
				if (response.ok) return response.text(); else throw response.statusText;
			})
			.then(text => eval(text))
			.then(m => cache[url] = m)
			.catch(error => console.error('evalscript()', url, error));
		}
	};

	return { evalscript, loadsrcscript, };
})();

'use strict';

let loadedmodule = null;

let { addevalscript, evalscript, } = (() => {
	let cache = {};

	let loadsrcscript = url => new Promise((resolve, reject) => {
		let cb = () => resolve(cache[url] = loadedmodule);

		let script = document.createElement('script');
		script.onerror = reject;
		script.onload = cb;
		script.onreadystatechange = cb;
		script.src = url;
		script.type = 'text/javascript';

		document.head.appendChild(script);
	});

	let evalscript = async url => {
		let r = cache[url];

		if (r != null)
			return r; // already loaded
		else if (document.location.protocol === 'file:')
			return loadsrcscript(url); // local file to be loaded using script element
		else {

			// remote file to be loaded using fetch()
			let response = await fetch(url, {
				cache: 'default',
				credentials: 'omit',
				method: 'GET',
				mode: 'cors',
				redirect: 'follow',
				referrer: 'no-referrer',
			});
			if (response.ok)
				return cache[url] = eval(await response.text());
			else
				throw response.statusText;
		}
	};

	let addevalscript = scr => async ns => ({ ...ns, [scr]: await evalscript(scr + '.js'), });

	return { addevalscript, evalscript, };
})();

import { existsSync, readdirSync, readFileSync } from 'fs';

let stateDir = `${process.env.HOME}/.warrior`;

let readJsonIfExists = name => {
	let filename = `${name}.json`;
	return existsSync(filename) ? JSON.parse(readFileSync(filename)) : null;
};

let getStateFilename = key => `${stateDir}/${key}`;

let ec2Class = () => {
	let class_ = 'ec2';

	let getKey = ({ name, attributes }) => [
		name,
		attributes['InstanceType'],
	].join('_');

	let getStateFilename_ = resource => getStateFilename(getKey(resource));

	let delete_ = (state, key) => [
		`aws ec2 terminate-instance --instance-ids ${state.InstanceId}`,
		`rm -f ${getStateFilename(key)}.json`,
	];

	let upsert = (state, resource) => {
		let commands = [];

		if (state == null) {
			let { name, attributes: { InstanceType, SubnetId } } = resource;
			commands.push(...[
				`aws ec2 run-instances --instance-type ${InstanceType} --subnet-id ${SubnetId} --tag-specifications '${JSON.stringify([
					{ ResourceType: class_, Tags: [{ Key: 'Name', Value: name }] }
				])}' | jq .Instances[0] > ${getStateFilename_(resource)}.json`,
			]);
			state = {};
		}

		return commands;
	};

	return {
		delete_,
		getKey,
		refresh: (resource, id) => [
			`aws ec2 describe-instances --instance-ids ${id} | jq .Reservations[0] | .Instances[0] > ${getStateFilename_(resource)}.json`,
		],
		upsert,
	};
};

let subnetClass = () => {
	let class_ = 'subnet';

	let getKey = ({ name, attributes }) => [
		name,
		attributes['VpcId'],
		attributes['AvailabilityZone'],
		attributes['MapPublicIpOnLaunch'] ? 'public' : 'private',
	].join('_');

	let getStateFilename_ = resource => getStateFilename(getKey(resource));

	let delete_ = (state, key) => [
		`aws ec2 delete-vpc --subnet-id ${state.SubnetId}`,
		`rm -f ${getStateFilename(key)}.json`,
	];

	let upsert = (state, resource) => {
		let commands = [];

		if (state == null) {
			let { name, attributes: { AvailabilityZone, MapPublicIpOnLaunch, VpcId } } = resource;
			commands.push(...[
				`aws ec2 create-subnet --availability-zone ${AvailabilityZone} --map-public-ip-on-launch ${MapPublicIpOnLaunch} --tag-specifications '${JSON.stringify([
					{ ResourceType: class_, Tags: [{ Key: 'Name', Value: name }] }
				])} --vpc-id ${VpcId}' | jq .Subnet > ${getStateFilename_(resource)}.json`,
			]);
			state = {};
		}

		return commands;
	};

	return {
		delete_,
		getKey,
		refresh: (resource, id) => [
			`aws ec2 describe-subnets --subnet-ids ${id} | jq .Subnets[0] > ${getStateFilename_(resource)}.json`,
		],
		upsert,
	};
};

let vpcClass = () => {
	let class_ = 'vpc';
	let getKey = ({ name }) => name;
	let getStateFilename_ = resource => getStateFilename(getKey(resource));

	let delete_ = (state, key) => [
		`aws ec2 delete-vpc --vpc-id ${state.VpcId}`,
		`rm -f ${getStateFilename(key)}.json`,
	];

	let upsert = (state, resource) => {
		let { attributes } = resource;
		let commands = [];

		if (state == null) {
			let { name, attributes: { CidrBlockAssociationSet } } = resource;
			commands.push(...[
				`aws ec2 create-vpc --cidr-block ${CidrBlockAssociationSet[0].CidrBlock} --tag-specifications '${JSON.stringify([
					{ ResourceType: class_, Tags: [{ Key: 'Name', Value: name }] }
				])}' | jq .Vpc > ${getStateFilename_(resource)}.json`,
			]);
			state = { CidrBlockAssociationSet: [{ CidrBlock: attributes['CidrBlockAssociationSet'][0]['CidrBlock'] }] };
		}

		// let VpcId = `$(aws ec2 describe-vpcs --filter Name:${name} | jq -r .Vpcs[0].VpcId)`;
		let VpcId = `$(cat ${getStateFilename_(resource)}.json | jq -r .VpcId)`;

		{
			let prop = 'CidrBlockAssociationSet';
			let map0 = Object.fromEntries(state[prop].map(({ CidrBlock, AssociationId }) => [CidrBlock, AssociationId]));
			let map1 = Object.fromEntries(attributes[prop].map(({ CidrBlock, AssociationId }) => [CidrBlock, AssociationId]));
			for (let [CidrBlock, AssociationId] of Object.entries(map0)) {
				if (!map1.hasOwnProperty(CidrBlock)) {
					commands.push(`aws ec2 disassociate-vpc-cidr-block --vpc-id ${VpcId} --association-id ${AssociationId}`);
				}
			}
			for (let [CidrBlock, AssociationId] of Object.entries(map1)) {
				if (!map0.hasOwnProperty(CidrBlock)) {
					commands.push(`aws ec2 associate-vpc-cidr-block --vpc-id ${VpcId} --cidr-block ${CidrBlock}`);
				}
			}
		}
		{
			let prop = 'EnableDnsHostnames';
			if (state[prop] !== attributes[prop]) {
				commands.push(
					`aws ec2 modify-vpc-attribute --vpc-id ${VpcId} ${attributes[prop] ? `--` : `--no-`}enable-dns-hostnames`,
					`echo ${attributes[prop]} > ${getStateFilename_(resource)}.${prop}.json`);
			}
		}
		{
			let prop = 'EnableDnsSupport';
			if (state[prop] !== attributes[prop]) {
				commands.push(
					`aws ec2 modify-vpc-attribute --vpc-id ${VpcId} ${attributes[prop] ? `--` : `--no-`}enable-dns-support`,
					`echo ${attributes[prop]} > ${getStateFilename_(resource)}.${prop}.json`);
			}
		}

		return commands;
	};

	return {
		delete_,
		getKey,
		getState: resource => {
			let stateFilename = getStateFilename_(resource);
			let state = readJsonIfExists(`${stateFilename}.json`);
			return state ? {
				...state,
				EnableDnsHostnames: readJsonIfExists(`${stateFilename}.EnableDnsHostnames`),
				EnableDnsSupport: readJsonIfExists(`${stateFilename}.EnableDnsSupport`),
			} : null;
		},
		refresh: (resource, id) => [
			`aws ec2 describe-vpcs --vpc-ids ${id} | jq .Vpcs[0] > ${getStateFilename_(resource)}.json`,
			`aws ec2 describe-vpc-attribute --vpc-id ${id} --attribute enableDnsHostnames | jq -r .EnableDnsHostnames.Value > ${getStateFilename_(resource)}.EnableDnsHostnames.json`,
			`aws ec2 describe-vpc-attribute --vpc-id ${id} --attribute enableDnsSupport | jq -r .EnableDnsSupport.Value > ${getStateFilename_(resource)}.EnableDnsSupport.json`,
		],
		upsert,
	};
};

let objectByClass = {
	ec2: ec2Class(),
	subnet: subnetClass(),
	vpc: vpcClass(),
};

let get = (resource, path) => {
	let { class_ } = resource;
	let { getKey } = objectByClass[class_];
	return `$(cat ${getStateFilename(getKey(resource))}.json | jq -r ${path})`;
};

let getResources = () => {
	let vpc = {
		class_: 'vpc',
		name: 'npt-cloud-vpc',
		attributes: {
			CidrBlockAssociationSet: [{ CidrBlock: '10.25.0.0/16' }],
			EnableDnsHostnames: true,
			EnableDnsSupport: true,
		},
	};

	let subnetPublic = {
		class_: 'subnet',
		name: 'npt-cloud-subnet-public',
		attributes: {
			AvailabilityZone: 'ap-southeast-1a',
			MapPublicIpOnLaunch: true,
			VpcId: get(vpc, '.VpcId'),
		},
	};

	let subnetPrivate = {
		class_: 'subnet',
		name: 'npt-cloud-subnet-private',
		attributes: {
			AvailabilityZone: 'ap-southeast-1a',
			MapPublicIpOnLaunch: false,
			VpcId: get(vpc, '.VpcId'),
		},
	};

	let ec2 = {
		class_: 'ec2',
		name: 'npt-cloud-ec2-0',
		attributes: {
			InstanceType: 't3.nano',
			SubnetId: get(subnetPrivate, '.SubnetId'),
		},
	};

	return [vpc, subnetPublic, subnetPrivate, ec2];
};

let stateFilenames = readdirSync(stateDir);

let stateByKey = Object.fromEntries(stateFilenames.map(stateFilename => {
	let [key] = stateFilename.split('.');
	let state = JSON.parse(readFileSync(`${stateDir}/${stateFilename}`));
	return [key, state];
}));

let resourceByKey = Object.fromEntries(getResources().map(resource => {
	let { getKey } = objectByClass[resource.class_];
	let key = getKey(resource);
	return [key, resource];
}));

let commands = [];

for (let [key, resource] of Object.entries(resourceByKey)) {
	let { upsert } = objectByClass[resource.class_];
	let state = stateByKey[key];
	commands.push(
		'',
		`# ${state ? 'update' : 'create'} ${key}`,
		...upsert(state, resource));
}

for (let [key, state] of Object.entries(stateByKey)) {
	let { delete_ } = objectByClass[resource.class_];
	let resource = resourceByKey[key];
	if (!resourceByKey) {
		commands.push(
			'',
			`# delete ${key}`,
			...delete_(state, key));
	}
}

console.log(commands.join('\n'));

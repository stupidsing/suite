import { existsSync, readFileSync } from 'fs';

let stateDir = '/tmp';

let readJsonIfExists = name => {
	let filename = `${name}.json`;
	return existsSync(filename) ? JSON.parse(readFileSync(filename)) : null;
};

let getStateFilename = key => `${stateDir}/${key}`;

let subnetClass = () => {
	let getKey = ({ name, attributes }) => [
		name,
		attributes['VpcId'],
		attributes['AvailabilityZone'],
		attributes['MapPublicIpOnLaunch'] ? 'public' : 'private',
	].join('_');

	let getStateFilename_ = resource => getStateFilename(getKey(resource));

	let create = resource => {
		let { name, attributes: { AvailabilityZone, MapPublicIpOnLaunch, VpcId } } = resource;
		return [
			`aws ec2 create-subnet --availability-zone ${AvailabilityZone} --map-public-ip-on-launch ${MapPublicIpOnLaunch} --tag-specifications '${JSON.stringify([
				{ ResourceType: 'subnet', Tags: [{ Key: 'Name', Value: name }] }
			])} --vpc-id ${VpcId}' | jq .Subnet > ${getStateFilename_(resource)}.json`,
		];}
	;

	let delete_ = (state, resource) => [
		`aws ec2 delete-vpc --subnet-id ${state.SubnetId}`,
		`rm -f ${getStateFilename_(resource)}.json`,
	];

	let upsert = (state, resource) => {
		let commands = [];

		if (state == null) {
			commands.push(...create(resource));
			state = {};
		}

		return commands;
	};

	return {
		delete_,
		getKey,
		getState: resource => readJsonIfExists(`${getStateFilename_(resource)}.json`),
		refresh: (resource, id) => [
			`aws ec2 describe-subnets --subnet-ids ${id} | jq .Subnets[0] > ${getStateFilename_(resource)}.json`,
		],
		upsert,
	};
};

let vpcClass = () => {
	let getKey = ({ name }) => name;
	let getStateFilename_ = resource => getStateFilename(getKey(resource));

	let create = resource => {
		let { name, attributes: { CidrBlockAssociationSet } } = resource;
		return [
			`aws ec2 create-vpc --cidr-block ${CidrBlockAssociationSet[0].CidrBlock} --tag-specifications '${JSON.stringify([
				{ ResourceType: 'vpc', Tags: [{ Key: 'Name', Value: name }] }
			])}' | jq .Vpc > ${getStateFilename_(resource)}.json`,
		];
	};

	let delete_ = (state, resource) => [
		`aws ec2 delete-vpc --vpc-id ${state.VpcId}`,
		`rm -f ${getStateFilename_(resource)}.json`,
	];

	let upsert = (state, resource) => {
		let { attributes } = resource;
		let commands = [];

		if (state == null) {
			commands.push(...create(resource));
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
	subnet: subnetClass(),
	vpc: vpcClass(),
};

let get = (resource, path) => {
	let { class_ } = resource;
	let { getKey } = objectByClass[class_];
	return `$(cat ${getStateFilename(getKey(resource))}.json | jq -r ${path})`;
};

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

let commands = [];

for (let resource of [vpc, subnetPublic, subnetPrivate]) {
	let { delete_, getState, upsert } = objectByClass[resource.class_];
	let state = getState(resource);
	if (!resource.delete_) {
		commands.push(
			'',
			`# ${state ? 'update' : 'create'} ${resource.class_} ${resource.name}`,
			...upsert(state, resource));
	} else {
		commands.push(
			'',
			`# ${'delete'} ${resource.class_} ${resource.name}`,
			...delete_(state, resource));
	}
}

console.log(commands.join('\n'));

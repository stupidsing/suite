import { existsSync, readFileSync } from 'fs';

let stateDir = '/tmp';

let readJsonIfExists = name => {
	let filename = `${name}.json`;
	return existsSync(filename) ? JSON.parse(readFileSync(filename)) : null;
};

let getStateFilename = name => `${stateDir}/${name}`;

let subnetClass = () => {
	let getKey = ({ name, attributes }) => [name, attributes['CidrBlock'], attributes['VpcId']].join('_');

	let create = resource => {
		let { name, attributes: { VpcId } } = resource;
		return [
			`aws ec2 create-subnet --tag-specifications '${JSON.stringify([
				{ ResourceType: 'vpc', Tags: [{ Key: 'Name', Value: name }] }
			])} --vpc-id ${VpcId}' | jq .Vpc > ${getStateFilename(name)}.json`,
		];}
	;

	let delete_ = (name, state, key) => [
		`aws ec2 delete-vpc --subnet-id ${state.SubnetId}`,
		`rm -f ${getStateFilename(name)}.json`,
	];

	let update = (name, state, resource) => {
		let { attributes } = resource;
		let commands = [];

		if (state == null) {
			commands.push(...create(resource));
			state = { CidrBlock: attributes['CidrBlock'], VpcId: attributes['VpcId'] };
		}

		{
			let key = 'CidrBlock';
			if (state[key] !== attributes[key]) {
				return [...delete_(name, state), ...update(name, null, resource)];
			}
		}

		{
			let key = 'VpcId';
			if (state[key] !== attributes[key]) {
				return [...delete_(name, state), ...update(name, null, resource)];
			}
		}

		return commands;
	};

	return {
		create,
		delete_,
		getKey,
		getState: ({ name }) => readJsonIfExists(`${getStateFilename(name)}.json`),
		refresh: (name, id) => [
			`aws ec2 describe-subnets --subnet-ids ${id} | jq .Subnets[0] > ${getStateFilename(name)}.json`,
		],
		update,
	};
};

let vpcClass = () => {
	let create = ({ name, attributes: { CidrBlockAssociationSet } }) => [
		`aws ec2 create-vpc --cidr-block ${CidrBlockAssociationSet[0].CidrBlock} --tag-specifications '${JSON.stringify([
			{ ResourceType: 'vpc', Tags: [{ Key: 'Name', Value: name }] }
		])}' | jq .Vpc > ${getStateFilename(name)}.json`,
	];

	let delete_ = (name, state, key) => [
		`aws ec2 delete-vpc --vpc-id ${state.VpcId}`,
		`rm -f ${getStateFilename(name)}.json`,
	];

	let update = (name, state, resource) => {
		let { attributes } = resource;
		let commands = [];

		if (state == null) {
			commands.push(...create(resource));
			state = { CidrBlockAssociationSet: [{ CidrBlock: attributes['CidrBlockAssociationSet'][0]['CidrBlock'] }] };
		}

		// let VpcId = `$(aws ec2 describe-vpcs --filter Name:${name} | jq -r .Vpcs[0].VpcId)`;
		let VpcId = `$(cat ${getStateFilename(name)}.json | jq -r .VpcId)`;

		{
			let key = 'CidrBlockAssociationSet';
			let map0 = Object.fromEntries(state[key].map(({ CidrBlock, AssociationId }) => [CidrBlock, AssociationId]));
			let map1 = Object.fromEntries(attributes[key].map(({ CidrBlock, AssociationId }) => [CidrBlock, AssociationId]));
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
			let key = 'EnableDnsHostnames';
			if (state[key] !== attributes[key]) {
				commands.push(
					`aws ec2 modify-vpc-attribute --vpc-id ${VpcId} ${attributes[key] ? `--` : `--no-`}enable-dns-hostnames`,
					`echo ${attributes[key]} > ${getStateFilename(name)}.${key}.json`);
			}
		}
		{
			let key = 'EnableDnsSupport';
			if (state[key] !== attributes[key]) {
				commands.push(
					`aws ec2 modify-vpc-attribute --vpc-id ${VpcId} ${attributes[key] ? `--` : `--no-`}enable-dns-support`,
					`echo ${attributes[key]} > ${getStateFilename(name)}.${key}.json`);
			}
		}

		return commands;
	};

	return {
		create,
		delete_,
		getKey: ({ name }) => name,
		getState: ({ name }) => {
			let stateFilename = getStateFilename(name);
			let state = readJsonIfExists(`${stateFilename}.json`);
			return state ? {
				...state,
				EnableDnsHostnames: readJsonIfExists(`${stateFilename}.EnableDnsHostnames`),
				EnableDnsSupport: readJsonIfExists(`${stateFilename}.EnableDnsSupport`),
			} : null;
		},
		refresh: (name, id) => [
			`aws ec2 describe-vpcs --vpc-ids ${id} | jq .Vpcs[0] > ${getStateFilename(name)}.json`,
			`aws ec2 describe-vpc-attribute --vpc-id ${id} --attribute enableDnsHostnames | jq -r .EnableDnsHostnames.Value > ${getStateFilename(name)}.EnableDnsHostnames.json`,
			`aws ec2 describe-vpc-attribute --vpc-id ${id} --attribute enableDnsSupport | jq -r .EnableDnsSupport.Value > ${getStateFilename(name)}.EnableDnsSupport.json`,
		],
		update,
	};
};

let objectByClass = {
	subnet: subnetClass(),
	vpc: vpcClass(),
};

let get = ({ name }, path) => `$(cat ${getStateFilename(name)}.json | jq -r ${path})`;

let vpc = {
	class_: 'vpc',
	name: 'npt-cloud-vpc',
	attributes: {
		CidrBlockAssociationSet: [{ CidrBlock: '10.25.0.0/16' }],
		EnableDnsHostnames: true,
		EnableDnsSupport: true,
	},
};

let subnet = {
	class_: 'subnet',
	name: 'npt-cloud-subnet',
	attributes: {
		VpcId: get(vpc, '.VpcId'),
	},
};

for (let resource of [vpc, subnet]) {
	let { delete_, getKey, getState, update } = objectByClass[resource.class_];
	let state = getState(resource);
	if (!resource.delete_) {
		console.log(update(resource.name, state, resource).join('\n'));
	} else {
		console.log(delete_(resource.name, state, getKey(resource)).join('\n'));
	}
}

import { existsSync, readFileSync } from 'fs';

let stateDir = '/tmp';

let readJsonIfExists = name => {
	let filename = `${name}.json`;
	return existsSync(filename) ? JSON.parse(readFileSync(filename)) : null;
};

let getStateFilename = key => `${stateDir}/${key}`;

let subnetClass = () => {
	let getKey = ({ name, attributes }) => [name, attributes['VpcId'], attributes['AvailabilityZone']].join('_');

	let create = resource => {
		let { name, attributes: { AvailabilityZone, VpcId } } = resource;
		return [
			`aws ec2 create-subnet --availability-zone ${AvailabilityZone} --tag-specifications '${JSON.stringify([
				{ ResourceType: 'vpc', Tags: [{ Key: 'Name', Value: name }] }
			])} --vpc-id ${VpcId}' | jq .Vpc > ${getStateFilename(getKey(resource))}.json`,
		];}
	;

	let delete_ = (state, resource) => [
		`aws ec2 delete-vpc --subnet-id ${state.SubnetId}`,
		`rm -f ${getStateFilename(getKey(resource))}.json`,
	];

	let update = (state, resource) => {
		let commands = [];

		if (state == null) {
			commands.push(...create(resource));
			state = {};
		}

		return commands;
	};

	return {
		create,
		delete_,
		getKey,
		getState: resource => readJsonIfExists(`${getStateFilename(getKey(resource))}.json`),
		refresh: (resource, id) => [
			`aws ec2 describe-subnets --subnet-ids ${id} | jq .Subnets[0] > ${getStateFilename(getKey(resource))}.json`,
		],
		update,
	};
};

let vpcClass = () => {
	let getKey = ({ name }) => name;

	let create = resource => {
		let { name, attributes: { CidrBlockAssociationSet } } = resource;
		return [
			`aws ec2 create-vpc --cidr-block ${CidrBlockAssociationSet[0].CidrBlock} --tag-specifications '${JSON.stringify([
				{ ResourceType: 'vpc', Tags: [{ Key: 'Name', Value: name }] }
			])}' | jq .Vpc > ${getStateFilename(getKey(resource))}.json`,
		];
	};

	let delete_ = (state, resource) => [
		`aws ec2 delete-vpc --vpc-id ${state.VpcId}`,
		`rm -f ${getStateFilename(getKey(resource))}.json`,
	];

	let update = (state, resource) => {
		let { attributes } = resource;
		let commands = [];

		if (state == null) {
			commands.push(...create(resource));
			state = { CidrBlockAssociationSet: [{ CidrBlock: attributes['CidrBlockAssociationSet'][0]['CidrBlock'] }] };
		}

		// let VpcId = `$(aws ec2 describe-vpcs --filter Name:${name} | jq -r .Vpcs[0].VpcId)`;
		let VpcId = `$(cat ${getStateFilename(getKey(resource))}.json | jq -r .VpcId)`;

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
					`echo ${attributes[prop]} > ${getStateFilename(getKey(resource))}.${prop}.json`);
			}
		}
		{
			let prop = 'EnableDnsSupport';
			if (state[prop] !== attributes[prop]) {
				commands.push(
					`aws ec2 modify-vpc-attribute --vpc-id ${VpcId} ${attributes[prop] ? `--` : `--no-`}enable-dns-support`,
					`echo ${attributes[prop]} > ${getStateFilename(getKey(resource))}.${prop}.json`);
			}
		}

		return commands;
	};

	return {
		create,
		delete_,
		getKey,
		getState: resource => {
			let stateFilename = getStateFilename(getKey(resource));
			let state = readJsonIfExists(`${stateFilename}.json`);
			return state ? {
				...state,
				EnableDnsHostnames: readJsonIfExists(`${stateFilename}.EnableDnsHostnames`),
				EnableDnsSupport: readJsonIfExists(`${stateFilename}.EnableDnsSupport`),
			} : null;
		},
		refresh: (resource, id) => [
			`aws ec2 describe-vpcs --vpc-ids ${id} | jq .Vpcs[0] > ${getStateFilename(getKey(resource))}.json`,
			`aws ec2 describe-vpc-attribute --vpc-id ${id} --attribute enableDnsHostnames | jq -r .EnableDnsHostnames.Value > ${getStateFilename(getKey(resource))}.EnableDnsHostnames.json`,
			`aws ec2 describe-vpc-attribute --vpc-id ${id} --attribute enableDnsSupport | jq -r .EnableDnsSupport.Value > ${getStateFilename(getKey(resource))}.EnableDnsSupport.json`,
		],
		update,
	};
};

let objectByClass = {
	subnet: subnetClass(),
	vpc: vpcClass(),
};

let get = (resource, path) => {
	let { class_, name } = resource;
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

let subnet = {
	class_: 'subnet',
	name: 'npt-cloud-subnet',
	attributes: {
		AvailabilityZone: 'ap-southeast-1a',
		VpcId: get(vpc, '.VpcId'),
	},
};

for (let resource of [vpc, subnet]) {
	let { delete_, getKey, getState, update } = objectByClass[resource.class_];
	let state = getState(resource);
	if (!resource.delete_) {
		console.log(update(state, resource).join('\n'));
	} else {
		console.log(delete_(state, resource).join('\n'));
	}
}

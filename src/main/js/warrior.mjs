import { readFileSync } from 'fs';

let stateDir = '/tmp';

let vpcClass = () => {
	let stateFilename = name => `${stateDir}/${name}`;

	let create = ({ name, attributes: { CidrBlock } }) => [
		`aws ec2 create-vpc --cidr-block ${CidrBlock} --tag-specifications '${JSON.stringify([
			{ ResourceType: 'vpc', Tags: [{ Key: 'Name', Value: name }] }
		])}' | jq .Vpc > ${stateFilename(name)}.json`,
	];

	let delete_ = (name, id) => [
		`aws ec2 delete-vpc --vpc-id ${id}`, `rm -f ${stateFilename(name)}.json`,
	];

	// let findIdByName = name => `aws ec2 describe-vpcs --filter Name:${name} | jq -r .Vpcs[0].VpcId`;
	let findIdByName = name => `cat ${stateFilename(name)}.json | jq -r .VpcId`;

	return {
		create,
		delete_,
		findIdByName,
		refresh: (name, id) => [
			`aws ec2 describe-vpcs --vpc-ids ${id} | jq .Vpcs[0] > ${stateFilename(name)}.json`,
			`aws ec2 describe-vpc-attribute --vpc-id ${id} --attribute enableDnsHostnames | jq -r .EnableDnsHostnames.Value > ${stateFilename(name)}.EnableDnsHostnames.json`,
			`aws ec2 describe-vpc-attribute --vpc-id ${id} --attribute enableDnsSupport | jq -r .EnableDnsSupport.Value > ${stateFilename(name)}.EnableDnsSupport.json`,
		],
		update: (resource0, resource1) => {
			let { class_, name } = resource0 ?? resource1;
			let commands = [];

			if (resource0 == null) commands.push(...create(resource1));

			let state0 = resource0 ? JSON.parse(readFileSync(`${stateFilename(name)}.json`)) : null;
			let { attributes: attributes0 } = resource0 ?? { class_, name, attributes: {} };
			let { attributes: attributes1 } = resource1 ?? { class_, name, attributes: {} };
			let vpcId = '$(' + findIdByName(name) + ')';

			/* {
				let key = 'xxx';
				if (attributes0[key] !== attributes1[key]) {
					return [...delete_(name, vpcId), ...create(name)];
				}
			} */
			{
				let key = 'CidrBlock';
				if (attributes0[key] !== attributes1[key]) {
					if (attributes0[key] != null) {
						commands.push(`aws ec2 disassociate-vpc-cidr-block --vpc-id ${vpcId} --association-id ${state0.CidrBlockAssociationSet.find(r => r.CidrBlock === attributes0[key]).AssociationId}`);
					}
					if (attributes1[key] != null) {
						commands.push(`aws ec2 associate-vpc-cidr-block --vpc-id ${vpcId} --cidr-block ${attributes1[key]}`);
					}
				}
			}
			{
				let key = 'EnableDnsHostnames';
				if (attributes0[key] !== attributes1[key]) {
					commands.push(
						`aws ec2 modify-vpc-attribute --vpc-id ${vpcId} ${attributes1[key] ? `--` : `--no-`}enable-dns-hostnames`,
						`echo ${attributes1[key]} > ${stateFilename(name)}.EnableDnsHostnames.json`);
				}
			}
			{
				let key = 'EnableDnsSupport';
				if (attributes0[key] !== attributes1[key]) {
					commands.push(
						`aws ec2 modify-vpc-attribute --vpc-id ${vpcId} ${attributes1[key] ? `--` : `--no-`}enable-dns-support`,
						`echo ${attributes1[key]} > ${stateFilename(name)}.EnableDnsSupport.json`);
				}
			}

			if (resource1 == null) {
				commands.push(...delete_(name, vpcId));
			}

			return commands;
		},
	};
};

let resource0 = null;

let resource1 = {
	class_: 'vpc',
	name: 'npt-cloud-vpc',
	attributes: {
		CidrBlock: '10.25.0.0/16',
		EnableDnsHostnames: true,
		EnableDnsSupport: true,
	},
};

let objectByClass = { vpc: vpcClass() };

let object = objectByClass[(resource0 ?? resource1).class_];

console.log(object.update(resource0, resource1).join('\n'));

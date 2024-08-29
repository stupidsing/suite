import { readFileSync } from 'fs';

let stateDir = '/tmp';

let vpcObject = () => {
	let stateFilename = name => `${stateDir}/${name}.json`;

	let create = resource => [`aws ec2 create-vpc --tag-specifications '${JSON.stringify([{ ResourceType: 'vpc', Tags: [{ Key: 'Name', Value: resource.name }] }])}' | jq .Vpc | cat > ${stateFilename(resource.name)}`];
	let delete_ = (name, id) => [`aws ec2 delete-vpc --vpc-id ${id}`, `rm -f ${stateFilename(name)}`];
	//let findIdByName = name => [`aws ec2 describe-vpcs --filter Name:${name} | jq -r .Vpcs[0].VpcId`];
	let findIdByName = name => `cat ${stateFilename(name)} | jq -r .VpcId`;

	return {
		create,
		delete_,
		findIdByName,
		refresh: (name, id) => [`aws ec2 describe-vpcs --vpc-ids ${id} | jq .Vpcs[0] | tee ${stateFilename(name)}`],
		update: (resource0, resource1) => {
			let { class_, name } = resource0 ?? resource1;
			let commands = [];

			if (resource0 == null) commands.push(...create(resource1));

			let state0 = resource0 ? JSON.parse(readFileSync(stateFilename(name))) : null;
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
				let key = 'cidr-block';
				if (attributes0[key] !== attributes1[key]) {
					if (attributes0[key] != null) {
						commands.push(`aws ec2 disassociate-vpc-cidr-block --association-id TODO --vpc-id ${vpcId}`);
					}
					if (attributes1[key] != null) {
						commands.push(`aws ec2 associate-vpc-cidr-block --cidr-block ${attributes1[key]} --vpc-id ${vpcId}`);
					}
				}
			}
			{
				let key = 'enable-dns-hostnames';
				if (attributes0[key] !== attributes1[key]) {
					commands.push(`aws ec2 modify-vpc-attribute ${attributes1[key] ? `--${key}` : `--no-${key}`} --vpc-id ${vpcId}`);
				}
			}
			{
				let key = 'enable-dns-support';
				if (attributes0[key] !== attributes1[key]) {
					commands.push(`aws ec2 modify-vpc-attribute ${attributes1[key] ? `--${key}` : `--no-${key}`} --vpc-id ${vpcId}`);
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
	name: 'aqt-cloud-vpc',
	attributes: {
		'cidr-block': '10.25.0.0/16',
		'enable-dns-hostnames': true,
		'enable-dns-support': true,
	},
};

let objectByClass = { vpc: vpcObject() };

let object = objectByClass[(resource0 ?? resource1).class_];

let commands = object.update(resource0, resource1);

console.log(commands.join('\n'));

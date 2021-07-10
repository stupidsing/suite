package suite.funp;

import suite.funp.Funp_.Funp;
import suite.funp.Funp_.FunpMetadata;
import suite.util.Switch;

public class SwitchFunp extends Switch<Funp> {

	private FunpMetadata metadata;

	public SwitchFunp(Funp in) {
		super(in);
		metadata = in.getMetadata();
	}

	public Funp result() {
		var result = super.result();
		if (result != null)
			result.setMetadata(metadata);
		return result;
	}

}

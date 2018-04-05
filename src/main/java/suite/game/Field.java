package suite.game;

import java.util.List;

import suite.game.Hex.XY;

public class Field<O> {

	private List<Occupation> occupations;

	public class Occupation {
		private XY xy;
		private int radius;
		private O object;
	}

	public Field(List<Occupation> occupations) {
		this.occupations = occupations;
	}

	public O getObject(XY xy) {
		Hex hex = new Hex();
		for (var occupation : occupations)
			if (hex.distance(hex.diff(xy, occupation.xy)) < occupation.radius)
				return occupation.object;
		return null;
	}

}

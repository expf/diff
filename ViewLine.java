import java.util.*;
import java.io.IOException;


public class ViewLine extends ArrayList<ViewString> {
	public enum Type { SAME, LEFT, RIGHT }
	private final Type type;

	public ViewLine(Type type) {
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	public void printTo(Appendable a) throws IOException {
		switch(type) {
		case SAME:
			a.append(' ');
			break;
		case LEFT:
			a.append('<');
			break;
		case RIGHT:
			a.append('>');
			break;
		}
		for (ViewString s : this) {
			a.append(s.getString());
		}
		a.append('\n');
	}
}

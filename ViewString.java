import java.util.*;


public class ViewString {
	String s;
	boolean diffPart;

	public ViewString(String s, boolean diffPart) {
		this.s = s.replace("\t", "  ");
		this.diffPart = diffPart;
	}

	public String getString() {
		return s;
	}

	public boolean isDiffPart() {
		return diffPart;
	}

}

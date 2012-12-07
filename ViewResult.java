import java.util.*;
import java.io.*;


public class ViewResult extends ArrayList<ViewLine> {

	public void printTo(File file) throws IOException {
		Writer w = new FileWriter(file);
		try {
			printTo(w);
		} finally {
			w.close();
		}
	}

	public void printTo(Appendable a) throws IOException {
		for (ViewLine l : this) {
			l.printTo(a);
		}
	}

}

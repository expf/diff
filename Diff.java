//import java.awt.List;
import java.awt.Container;
import java.awt.Canvas;
import java.awt.ScrollPane;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Frame;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.EnumMap;
import javax.swing.JFileChooser;


public class Diff {
	private static String encoding = null;
	private static FrameSet fs = new FrameSet();

	public static void main(String args[]) throws Exception {
		init(args);
		try {
			fs.waitForClosed();
		} catch ( InterruptedException e ) {}
	}

	private static void init(String args[]) throws Exception {
		List<String> files = new ArrayList<String>();
		for (int i=0; i<args.length; i++) {
			String arg = args[i];
			if (arg.equals("-c") && i < args.length-1) {
				i++;
				encoding = args[i];
			} else {
				files.add(arg);
			}
		}

		File f1;
		File f2;
		if (files.size() >= 2) {
			f1 = new File(files.get(0));
			f2 = new File(files.get(1));
		} else {
			String dir = files.size() > 0 ? files.get(0) : null;
			Frame w = new Frame();
			f1 = getFile(w, dir);
			f2 = null;
			if (f1 != null) {
				f2 = getFile(w, dir);
			}
			w.dispose();
			if (f2 == null) return;
		}
		addDiffWindow(f1, f2);
	}

	static void addDiffWindow(File f1, File f2) throws IOException {
		DiffTarget target = new DiffTarget();
		target.readFile(f1, encoding, 0);
		target.readFile(f2, encoding, 1);
		DiffWindow window = new DiffWindow(target);
		fs.addFrame(window);
	}

	private static File getFile(Frame w, String dir) throws Exception {
		JFileChooser fc = dir == null ?
			new JFileChooser() :
			new JFileChooser(dir);
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		int returnVal = fc.showOpenDialog(w);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			return fc.getSelectedFile();
		} else {
			return null;
		}
	}

}


class DiffWindow extends Frame implements ItemListener, ActionListener {
	private final DiffTarget target;

	private final java.awt.List list;
	private final java.awt.List dirList;
	private final ScrollPane pane;
	private final DiffViewCanvas canvas;
	private final DiffPartCanvas part;

	public DiffWindow(DiffTarget target) {
		super("diff");
		this.target = target;
		setLayout(new BorderLayout());

		Font font = new Font("Monospaced", Font.PLAIN, 10);

		List<String> fileList = target.getFileList();
		List<String> directoryList = target.getDirectoryList();
		int numFile = fileList.size();
		int numDirectory = directoryList.size();
		int listSize = Math.max(Math.min(numFile, 20), Math.min(numDirectory, 10));

		list = new java.awt.List(listSize);
		list.setFont(font);
		list.addItemListener(this);
		list.addActionListener(this);
		for (String file : fileList) {
			list.add(file);
		}

		dirList = new java.awt.List(listSize);
		dirList.setFont(font);
		dirList.addActionListener(this);
		for (String file : directoryList) {
			dirList.add(file);
		}

		Container listContainer = new Container();
		listContainer.setLayout(new BorderLayout());
		listContainer.add(list, BorderLayout.CENTER);
		listContainer.add(dirList, BorderLayout.EAST);
		add(listContainer, BorderLayout.NORTH);

		canvas = new DiffViewCanvas(font);
		pane = new ScrollPane();
		pane.add(canvas);
		pane.setSize(1000, 600);
		add(pane, BorderLayout.CENTER);

		part = new DiffPartCanvas();
		add(part, BorderLayout.EAST);

		if (numFile == 1) {
			list.select(0);
			ViewResult result = target.getResult(fileList.get(0));
			canvas.setResult(result);
			part.setResult(result);
		}
	}

	public void itemStateChanged(ItemEvent e) {
		String selected = list.getSelectedItem();
		ViewResult result = target.getResult(selected);
		canvas.setResult(result);
		pane.setScrollPosition(0, 0);
		pane.doLayout();
		part.setResult(result);
		canvas.repaint();
		part.repaint();
	}

	public void actionPerformed(ActionEvent event) {
		Object source = event.getSource();
		if (source == list) {
			actionList(event);
		} else if (source == dirList) {
			actionDir(event);
		}
	}

	private void actionList(ActionEvent event) {
		JFileChooser fc = new JFileChooser();
		fc.setDialogType(JFileChooser.SAVE_DIALOG);
		int returnVal = fc.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			try {
				canvas.getResult().printTo(file);
			} catch (IOException e) {
				System.out.println(e.toString());
			}
		}
	}

	private void actionDir(ActionEvent event) {
		File[] files = target.getDirectory(event.getActionCommand());
		if (files == null) {
			return;
		}
		try {
			Diff.addDiffWindow(files[0], files[1]);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

}


class DiffViewCanvas extends Canvas {
	private final Font font;
	private final FontMetrics fontMetrics;
	private final int lineHeight;
	private final Map<ViewLine.Type, Color> background;
	private Dimension size;
	private ViewResult result;

	public DiffViewCanvas(Font font) {
		this.size = new Dimension(1, 1);
		this.font = font;
		this.fontMetrics = getFontMetrics(font);
		this.lineHeight = fontMetrics.getHeight();
		this.background = new EnumMap<ViewLine.Type, Color>(ViewLine.Type.class);
		this.background.put(ViewLine.Type.SAME, new Color(255, 255, 255));
		this.background.put(ViewLine.Type.LEFT, new Color(128, 255, 255));
		this.background.put(ViewLine.Type.RIGHT, new Color(255, 255, 128));
	}

	public Dimension getPreferredSize() {
		return size;
	}

	public void setResult(ViewResult result) {
		this.result = result;
		if (result == null) {
			size = new Dimension(1, 1);
			return;
		}
		int lineCount = 0;
		int maxWidth = 0;
		for (ViewLine line : result) {
			lineCount++;
			int width = 0;
			for (ViewString str : line) {
				width += fontMetrics.stringWidth(str.getString());
			}
			if (width > maxWidth) maxWidth = width;
		}
		size = new Dimension(maxWidth, lineHeight * lineCount);
	}

	public ViewResult getResult() {
		return result;
	}

	public void paint(Graphics g) {
		if (result == null) return;
		int numLine = result.size();
		int ascent = fontMetrics.getAscent();
		Rectangle rect = g.getClip().getBounds();
		double clipTop = rect.getY();
		int top = (int)(clipTop / lineHeight);
		int bottom = (int)((clipTop + rect.getHeight()) / lineHeight) + 1;
		if (top < 0) top = 0;
		if (bottom >= numLine) bottom = numLine - 1;
		g.setFont(font);
		for (int i=top; i<=bottom; i++) {
			ViewLine line = result.get(i);
			g.setColor(background.get(line.getType()));
			g.fillRect((int)rect.getX(), lineHeight * i, (int)rect.getWidth(), lineHeight);
			int x = 0;
			for (ViewString str : line) {
				String s = str.getString();
				int width = fontMetrics.stringWidth(s);
				if (str.isDiffPart()) {
					g.setColor(Color.RED);
					g.fillRect(x, ascent + lineHeight * i, width, 2);
				}
				g.setColor(Color.BLACK);
				g.drawString(s, x, ascent + lineHeight * i);
				x += width;
			}
		}
	}

}


class DiffPartCanvas extends Canvas {
	private final Dimension size = new Dimension(16, 16);
	private final Color color = new Color(255, 64, 64);
	private final int over = 1;
	private final int marginTop = 16;
	private final int marginBottom = 32;
	private boolean[] bits;

	public void setResult(ViewResult result) {
		if (result == null) {
			bits = null;
			return;
		}
		int len = result.size();
		boolean bits[] = new boolean[len];
		for (int i=0; i<len; i++) {
			bits[i] = (result.get(i).getType() != ViewLine.Type.SAME);
		}
		this.bits = bits;
	}

	public Dimension getPreferredSize() {
		return size;
	}

	public void paint(Graphics g) {
		if (bits == null) return;
		int len = bits.length;
		int height = getHeight() - marginTop - marginBottom;
		if (height <= 0) return;
		int width = getWidth();
		g.setColor(color);
		int start = 0;
		int end = -100;
		for (int i=0; i<len; i++) {
			if (bits[i]) {
				int a = (int)(((long)i * height) / len) + marginTop;
				int s = a - over;
				int e = a + (height / len) + over;
				if (s > end + 1) {
					if (end >= 0) g.fillRect(0, start, width, end - start + 1);
					start = s;
					end = e;
				} else {
					end = e;
				}
			}
		}
		if (end >= 0) g.fillRect(0, start, width, end - start + 1);
	}
}

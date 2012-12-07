import java.io.*;
import java.util.*;


class CharUnitCode implements DiffUnitCode {
	public static CharUnitCode instance = new CharUnitCode();

	private static final int PUNC = 0;
	private static final int PUNC_OPEN = 1;
	private static final int PUNC_CLOSE = 2;
	private static final int CONNECT = 3;
	private static final int UPPER = 4;
	private static final int LOWER = 5;
	private static final int TITLE = 6;
	private static final int LETTER = 7;
	private static final int MARK = 8;
	private static final int DIGIT = 9;
	private static final int NUM_TYPE = 10;

	private final int[] types = new int[256];
	private final int[] rank = new int[NUM_TYPE * NUM_TYPE];

	private static int idx(int a, int b) {
		return a + b * NUM_TYPE;
	}

	private CharUnitCode() {
		types[Character.UPPERCASE_LETTER + 128] = UPPER;
		types[Character.LOWERCASE_LETTER + 128] = LOWER;
		types[Character.TITLECASE_LETTER + 128] = TITLE;
		types[Character.MODIFIER_LETTER + 128] = LETTER;
		types[Character.OTHER_LETTER + 128] = LETTER;
		types[Character.NON_SPACING_MARK + 128] = MARK;
		types[Character.ENCLOSING_MARK + 128] = MARK;
		types[Character.COMBINING_SPACING_MARK + 128] = MARK;
		types[Character.DECIMAL_DIGIT_NUMBER + 128] = DIGIT;
		types[Character.LETTER_NUMBER + 128] = LETTER;
		types[Character.OTHER_NUMBER + 128] = LETTER;
		types[Character.SPACE_SEPARATOR + 128] = PUNC;
		types[Character.LINE_SEPARATOR + 128] = PUNC;
		types[Character.PARAGRAPH_SEPARATOR + 128] = PUNC;
		types[Character.CONTROL + 128] = PUNC;
		types[Character.FORMAT + 128] = PUNC;
		types[Character.PRIVATE_USE + 128] = LETTER;
		types[Character.SURROGATE + 128] = LETTER;
		types[Character.DASH_PUNCTUATION + 128] = CONNECT;
		types[Character.START_PUNCTUATION + 128] = PUNC_OPEN;
		types[Character.END_PUNCTUATION + 128] = PUNC_CLOSE;
		types[Character.CONNECTOR_PUNCTUATION + 128] = CONNECT;
		types[Character.OTHER_PUNCTUATION + 128] = PUNC;
		types[Character.MATH_SYMBOL + 128] = PUNC;
		types[Character.CURRENCY_SYMBOL + 128] = PUNC;
		types[Character.MODIFIER_SYMBOL + 128] = LETTER;
		types[Character.OTHER_SYMBOL + 128] = PUNC;
		types[Character.INITIAL_QUOTE_PUNCTUATION + 128] = PUNC_OPEN;
		types[Character.FINAL_QUOTE_PUNCTUATION + 128] = PUNC_CLOSE;

		for (int i=0; i<NUM_TYPE; i++) {
			for (int j=0; j<NUM_TYPE; j++) {
				rank[idx(i, j)] = i==j ? 1000 : 2000;
			}
		}
		for (int i=0; i<NUM_TYPE; i++) {
			rank[idx(i, MARK)] = 1;
		}
		for (int i=0; i<NUM_TYPE; i++) {
			rank[idx(i, CONNECT)] = 1500;
			rank[idx(CONNECT, i)] = 1500;
		}
		for (int i=0; i<NUM_TYPE; i++) {
			rank[idx(i, PUNC)] = 3000;
			rank[idx(i, PUNC_OPEN)] = 3000;
			rank[idx(i, PUNC_CLOSE)] = 3000;
			rank[idx(PUNC, i)] = 3000;
			rank[idx(PUNC_OPEN, i)] = 3000;
			rank[idx(PUNC_CLOSE, i)] = 3000;
		}
		rank[idx(UPPER, UPPER)] = 507;
		rank[idx(LOWER, LOWER)] = 505;
		rank[idx(DIGIT, DIGIT)] = 504;
		rank[idx(TITLE, TITLE)] = 509;
		rank[idx(UPPER, LOWER)] = 506;
		rank[idx(TITLE, LOWER)] = 506;
		rank[idx(UPPER, TITLE)] = 508;
		rank[idx(TITLE, UPPER)] = 509;
		rank[idx(LOWER, UPPER)] = 509;
		rank[idx(LOWER, TITLE)] = 509;
		rank[idx(UPPER, DIGIT)] = 508;
		rank[idx(LOWER, DIGIT)] = 508;
		rank[idx(TITLE, DIGIT)] = 508;

	}

	public int getSepLevel(int a, int b) {
		int typeA = types[Character.getType(a) + 128];
		int typeB = types[Character.getType(b) + 128];
		return rank[idx(typeA, typeB)];
	}

	public int getNestLevelDelta(int a) {
		int type = types[Character.getType(a) + 128];
		if (type == PUNC_OPEN) return 1;
		if (type == PUNC_CLOSE) return -1;
		return 0;
	}
}

class StringNum implements DiffUnitCode {
	private static final byte[] nestLevel = new byte[128];
	static {
		nestLevel['('] = 1;
		nestLevel[')'] = -1;
		nestLevel['['] = 1;
		nestLevel[']'] = -1;
		nestLevel['{'] = 1;
		nestLevel['}'] = -1;
	}

	private final Map<String, Integer> map = new HashMap<String, Integer>();
	private final List<String> list = new ArrayList<String>();

	public Integer get(String str) {
		Integer num = map.get(str);
		if (num == null) {
			num = Integer.valueOf(list.size());
			map.put(str, num);
			list.add(str);
		}
		return num;
	}

	public String getString(int i) {
		return list.get(i);
	}

	public int getSepLevel(int a, int b) {
		return 0;
	}

	public int getNestLevelDelta(int a) {
		String s = list.get(a);
		int len = s.length();
		int nest = 0;
		for (int i=0; i<len; i++) {
			char c = s.charAt(i);
			if (c < 128) {
				nest += nestLevel[c];
				if (c == '/') {
					if (i != 0 && s.charAt(i-1) == '*') nest--;
					if (i != len-1 && s.charAt(i+1) == '*') nest++;
				}
			}
		}
		return nest;
	}
}


class FilePair {
	public static final char CHAR_SAME = ' ';
	public static final char CHAR_DIFF = '*';
	public static final char CHAR_ONLY_LEFT = '<';
	public static final char CHAR_ONLY_RIGHT = '>';
	public static final char CHAR_NONE = '/';

	private final StringNum stringNum;
	int[] content1;
	int[] content2;

	public FilePair(StringNum stringNum) {
		this.stringNum = stringNum;
	}

	public void readFile(File file, String encoding, int index) throws IOException, UnsupportedEncodingException {
		InputStream in = new FileInputStream(file);
		Reader reader = encoding == null ?
			new InputStreamReader(in) :
			new InputStreamReader(in, encoding);
		BufferedReader r = new BufferedReader(reader);
		List<Integer> lineList = new ArrayList<Integer>();
		String line;
		while ((line = r.readLine()) != null) {
			lineList.add(stringNum.get(line));
		}
		r.close();
		int lineCount = lineList.size();
		int[] content = new int[lineCount];
		for (int i=0; i<lineCount; i++) {
			content[i] = lineList.get(i).intValue();
		}
		switch (index) {
		case 0:
			content1 = content;
			break;
		case 1:
			content2 = content;
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	public char getCompareChar() {
		return content1 == null
			? content2 == null
				? CHAR_NONE
				: CHAR_ONLY_RIGHT
			: content2 == null
				? CHAR_ONLY_LEFT
				: Arrays.equals(content1, content2)
					? CHAR_SAME
					: CHAR_DIFF
			;
	}

}


public class DiffTarget {
	private final Map<String, FilePair> fileList =
		new TreeMap<String, FilePair>();
	private final Map<String, List<File>> directoryList =
		new TreeMap<String, List<File>>();
	private final StringNum stringNum = new StringNum();

	public void readFile(File targetFile, String encoding, int index) throws IOException, UnsupportedEncodingException {
		if (targetFile.isDirectory()) {
			for (File file : targetFile.listFiles()) {
				if (file.isFile()) {
					addFile(file.getName(), file, encoding, index);
				} else if (file.isDirectory()) {
					addDirectory(file.getName(), file, index);
				}
			}
			addDirectory("..", targetFile.getParentFile(), index);
		} else if (targetFile.isFile()) {
			addFile("", targetFile, encoding, index);
			addDirectory(".", targetFile.getParentFile(), index);
		} else {
			// ì¡éÍÉtÉ@ÉCÉãÇÃèÍçá
		}
	}

	private void addFile(String filename, File file, String encoding, int index) throws IOException {
		FilePair pair = fileList.get(filename);
		if (pair == null) {
			pair = new FilePair(stringNum);
			fileList.put(filename, pair);
		}
		pair.readFile(file, encoding, index);
	}

	private void addDirectory(String filename, File file, int index) {
		List<File> pair = directoryList.get(filename);
		if (pair == null) {
			pair = new ArrayList<File>();
			directoryList.put(filename, pair);
		}
		while (pair.size() <= index) pair.add(null);
		pair.set(index, file);
	}

	public List<String> getFileList() {
		List<String> list = new ArrayList<String>();
		for (Map.Entry<String, FilePair> entry : fileList.entrySet()) {
			String name = entry.getKey();
			FilePair pair = entry.getValue();
			list.add(pair.getCompareChar() + name);
		}
		return list;
	}

	public ViewResult getResult(String item) {
		if (item == null || item.length() < 1) return null;
		char type = item.charAt(0);
		String filename = item.substring(1);
		FilePair pair = fileList.get(filename);
		if (pair == null) return null;
		switch (type) {
		case FilePair.CHAR_SAME:
		case FilePair.CHAR_ONLY_LEFT:
			return createResult(pair.content1);
		case FilePair.CHAR_ONLY_RIGHT:
			return createResult(pair.content2);
		case FilePair.CHAR_DIFF:
			return createResult(pair.content1, pair.content2);
		default:
			return null;
		}
	}

	public ViewResult createResult(int[] content) {
		ViewResult result = new ViewResult();
		for (int i=0; i<content.length; i++) {
			String lineString = stringNum.getString(content[i]);
			ViewLine line = new ViewLine(ViewLine.Type.SAME);
			line.add(new ViewString(lineString, false));
			result.add(line);
		}
		return result;
	}

	public ViewResult createResult(int[] content1, int[] content2) {
		List<Logic.Path> path = Logic.diff(content1, content2, stringNum);
		ViewResult result = new ViewResult();
		StringBuilder str1 = new StringBuilder();
		StringBuilder str2 = new StringBuilder();
		for (Logic.Path p : path) {
			String lineString = stringNum.getString(p.getVal());
			switch (p.getType()) {
			case Logic.SAME:
				setDiffPart(result, str1, str2);
				str1.setLength(0);
				str2.setLength(0);
				{
					ViewLine line = new ViewLine(ViewLine.Type.SAME);
					line.add(new ViewString(lineString, false));
					result.add(line);
				}
				break;
			case Logic.LEFT:
				str1.append(lineString).append('\n');
				break;
			case Logic.RIGHT:
				str2.append(lineString).append('\n');
				break;
			default:
				throw new RuntimeException();
			}
		}
		setDiffPart(result, str1, str2);
		return result;
	}

	private static void setDiffPart(ViewResult result, CharSequence str1, CharSequence str2) {
		int[] chars1 = toIntArray(str1);
		int[] chars2 = toIntArray(str2);
		List<Logic.Path> path = Logic.diff(chars1, chars2, CharUnitCode.instance);
		List<Logic.Path> path1 = new ArrayList<Logic.Path>();
		List<Logic.Path> path2 = new ArrayList<Logic.Path>();
		for (Logic.Path p : path) {
			switch (p.getType()) {
			case Logic.SAME:
				appendCharPath(path1, p, result, ViewLine.Type.LEFT);
				appendCharPath(path2, p, result, ViewLine.Type.RIGHT);
				break;
			case Logic.LEFT:
				appendCharPath(path1, p, result, ViewLine.Type.LEFT);
				break;
			case Logic.RIGHT:
				appendCharPath(path2, p, result, ViewLine.Type.RIGHT);
				break;
			default:
				throw new RuntimeException();
			}
		}
		if (!path1.isEmpty()) appendResult(path1, result, ViewLine.Type.LEFT);
		if (!path2.isEmpty()) appendResult(path2, result, ViewLine.Type.RIGHT);
	}

	private static int[] toIntArray(CharSequence s) {
		int len = s.length();
		int[] intArray = new int[len];
		for (int i=0; i<len; i++) {
			intArray[i] = s.charAt(i);
		}
		return intArray;
	}

	private static void appendCharPath(List<Logic.Path> path, Logic.Path p, ViewResult result, ViewLine.Type lineType) {
		if (p.getVal() == '\n') {
			appendResult(path, result, lineType);
			path.clear();
		} else {
			path.add(p);
		}
	}

	private static void appendResult(List<Logic.Path> path, ViewResult result, ViewLine.Type lineType) {
		ViewLine line = new ViewLine(lineType);
		boolean diffPart = false;
		StringBuilder sb = new StringBuilder();
		for (Logic.Path p : path) {
			boolean newDiffPart = (p.getType() != Logic.SAME);
			if (diffPart != newDiffPart) {
				if (sb.length() > 0) {
					line.add(new ViewString(sb.toString(), diffPart));
					sb.setLength(0);
				}
				diffPart = newDiffPart;
			}
			sb.append((char)p.getVal());
		}
		if (sb.length() > 0) {
			line.add(new ViewString(sb.toString(), diffPart));
		}
		result.add(line);
	}

	public List<String> getDirectoryList() {
		List<String> list = new ArrayList<String>();
		for (Map.Entry<String, List<File>> entry : directoryList.entrySet()) {
			String name = entry.getKey();
			List<File> pair = entry.getValue();
			File dir1 = pair.size() >= 1 ? pair.get(0) : null;
			File dir2 = pair.size() >= 2 ? pair.get(1) : null;
			if (dir1 != null && dir2 != null) {
				list.add(name);
			} else if (dir1 != null && dir2 == null) {
				list.add(FilePair.CHAR_ONLY_LEFT + name);
			} else if (dir1 == null && dir2 != null) {
				list.add(FilePair.CHAR_ONLY_RIGHT + name);
			}
		}
		return list;
	}

	public File[] getDirectory(String item) {
		char first = item.charAt(0);
		if (first == FilePair.CHAR_ONLY_LEFT || first == FilePair.CHAR_ONLY_RIGHT) {
			return null;
		}
		File[] res = new File[2];
		List<File> pair = directoryList.get(item);
		res[0] = pair.get(0);
		res[1] = pair.get(1);
		return res;
	}

}

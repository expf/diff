import java.util.*;
import java.io.PrintStream;



public class Logic {

	// 外部公開用

	public static final byte SAME = 1;
	public static final byte LEFT = 2;
	public static final byte RIGHT = 3;

	public static class Path {
		private final byte type;
		private final int val;

		public Path(byte type, int val) {
			this.type = type;
			this.val = val;
		}

		public byte getType() {
			return type;
		}

		public int getVal() {
			return val;
		}
	}


	// diffメインアルゴリズム

	private static class PathInfo {
		private final int min;
		private final int max;
		private final long[] info;

		public PathInfo(int min, int max) {
			this.min = min;
			this.max = max;
			this.info = new long[max - min + 1];
		}

		public void set(int i, int step, int sameCount, byte direction) {
			info[i - min] = step + ((long)sameCount << 30) + ((long)direction << 60);
		}

		public int getStep(int i) {
			if (i < min || i > max) return -1;
			return (int)(info[i - min] & 0x3fffffff);
		}

		public int getSameCount(int i) {
			return (int)((info[i - min] >> 30) & 0x3fffffff);
		}

		public byte getDirection(int i) {
			return (byte)((info[i - min] >> 60) & 0x3);
		}

	}

	private static void tr(
		PathInfo info, int[] c1, int[] c2, int i,
		PathInfo prev1, PathInfo prev2) {
		int st1 = prev1.getStep(i - 1) + 1;
		int st2 = prev2.getStep(i + 1);
		int x;
		byte dir;
		if (st1 > st2) {
			x = st1;
			dir = LEFT;
		} else {
			x = st2;
			dir = RIGHT;
		}
		int y = x - i;

		int len1 = c1.length;
		int len2 = c2.length;
		assert x >= 0 && x <= len1 && y >= 0 && y <= len2;

		int sameCount = 0;
		while (x < len1 && y < len2 && c1[x] == c2[y]) {
			++x; ++y; ++sameCount;
		}
		info.set(i, x, sameCount, dir);
	}

	static List<PathInfo> searchPath(int[] c1, int[] c2) {
		int len1 = c1.length;
		int len2 = c2.length;
		int delta = len1 - len2;

		List<PathInfo> pathList = new ArrayList<PathInfo>();
		int max = delta > 0 ? delta : 0;
		int min = delta < 0 ? delta : 0;
		PathInfo prevInfo = new PathInfo(0, -1);

		while (prevInfo.getStep(delta) < len1) {
			PathInfo info = new PathInfo(min, max);
			for (int i=min; i<delta; i++) {
				tr(info, c1, c2, i, info, prevInfo);
			}
			for (int i=max; i>delta; i--) {
				tr(info, c1, c2, i, prevInfo, info);
			}
			tr(info, c1, c2, delta, info, info);
			pathList.add(info);
			prevInfo = info;
			--min; ++max;
		}
		return pathList;
	}


	// 結果取得

	private static class Segment {
		int leftBlanch;
		int rightBlanch;
		int sameCount;
		int leftCount;
		int rightCount;
		Segment(int leftBlanch, int rightBlanch, int sameCount,
		        int leftCount, int rightCount) {
			this.leftBlanch = leftBlanch;
			this.rightBlanch = rightBlanch;
			this.sameCount = sameCount;
			this.leftCount = leftCount;
			this.rightCount = rightCount;
		}
	}

	private static long calcSepLevel(int orgSepLevel, int nestLevel) {
		int h = (orgSepLevel / 100) * 10 + 1;
		int l = orgSepLevel % 100;
		return 1000000000000L * h - 100L * nestLevel + l;
	}

	private static int getSepPoint(
		int[] a, int i1, int i2,
		int maxLen, DiffUnitCode code) {
		int count = 0;
		while (a[i1-count-1] == a[i2-count-1]) {
			count++;
			if (count >= maxLen) return maxLen;
		}

		long maxSepLevel = Long.MIN_VALUE;
		int nestLevel = 0;
		int maxSepIndex = 0;
		while (true) {
			long sepLevel =
				calcSepLevel(code.getSepLevel(a[i1-count-1], a[i1-count]), nestLevel);
			if (sepLevel >= maxSepLevel) {
				maxSepLevel = sepLevel;
				maxSepIndex = count;
			}
			if (count <= 0) break;
			nestLevel += code.getNestLevelDelta(a[i1-count]);
			count--;
		}
		return maxSepIndex;
	}

	static LinkedList<Segment> tracePath(
		List<PathInfo> pathList,
		int[] c1, int[] c2, DiffUnitCode code)
		{
		int pathIndex = pathList.size() - 1;
		int index1 = c1.length;
		int index2 = c2.length;
		int delta = index1 - index2;
		LinkedList<Segment> list = new LinkedList<Segment>();
		list.addFirst(new Segment(index1, index2, 0, 0, 0));
		int l=0;
		int r=0;
		while (true) {
			PathInfo info = pathList.get(pathIndex);
			int sameCount = info.getSameCount(index1 - index2);
			byte dir = info.getDirection(index1 - index2);
			assert index1 == info.getStep(index1 - index2);

			boolean finish = (index1 == sameCount && index2 == sameCount);

			if (sameCount > 0) {
				if (r == 0 && l > 0) {
					int back = getSepPoint(c1, index1, index1+l, sameCount, code);
					if (back > 0) {
						list.getFirst().sameCount += back;
						sameCount -= back;
						index1 -= back;
						index2 -= back;
					}
				} else if (l == 0 && r > 0) {
					int back = getSepPoint(c2, index2, index2+r, sameCount, code);
					if (back > 0) {
						list.getFirst().sameCount += back;
						sameCount -= back;
						index1 -= back;
						index2 -= back;
					}
				}
			}
			if (sameCount > 0 || finish) {
				list.addFirst(new Segment(index1, index2, sameCount, l, r));
				if (finish) break;
				l = 0;
				r = 0;
				index1 -= sameCount;
				index2 -= sameCount;
			}

			switch (dir) {
			case LEFT:
				index1--;
				l++;
				if (index1 - index2  >= delta) {
					pathIndex--;
				}
				break;
			case RIGHT:
				index2--;
				r++;
				if (index1 - index2  <= delta) {
					pathIndex--;
				}
				break;
			default:
				assert false;
			}
		}
		assert pathIndex == 0;

		return list;
	}


	// 返却値作成・メイン

	static List<Path> makeResult(LinkedList<Segment> list, int[] c1, int[] c2) {
		List<Path> ret = new ArrayList<Path>();
		for (Segment segment : list) {
			for (int i=0; i<segment.sameCount; i++) {
				ret.add(new Path(SAME, c1[segment.leftBlanch-segment.sameCount+i]));
			}
			for (int i=0; i<segment.leftCount; i++) {
				ret.add(new Path(LEFT, c1[segment.leftBlanch+i]));
			}
			for (int i=0; i<segment.rightCount; i++) {
				ret.add(new Path(RIGHT, c2[segment.rightBlanch+i]));
			}
		}
		return ret;
	}

	public static List<Path> diff(int[] c1, int[] c2, DiffUnitCode code) {
		List<PathInfo> pathList = searchPath(c1, c2);
		LinkedList<Segment> list = tracePath(pathList, c1, c2, code);
		return makeResult(list, c1, c2);
	}


	static void test(int[] c1, int[] c2) {
		for (Path p : diff(c1, c2, new TestCode())) {
			char c;
			switch (p.getType()) {
			case SAME:
				c = '=';
				break;
			case LEFT:
				c = '<';
				break;
			case RIGHT:
				c = '>';
				break;
			default:
				c = '?';
				break;
			}
			System.out.print(" " + c + p.getVal());
		}
		System.out.println();
	}
	public static void main(String args[]) throws Exception {
		test(
			new int[]{1,2,3,4,5,6,7,8,7,6,5,4,9},
			new int[]{1,2,1,3,4,8,7,6,3,4,9,9}
			);
		test(
			new int[]{1,10,11,22,10,11,2,10,21,22,10,21,3},
			new int[]{1,10,11,2,10,21,3}
			);
		test(
			new int[]{1,2,3,1,4},
			new int[]{1,4}
			);
	}

	static class TestCode implements DiffUnitCode {
		public int getSepLevel(int a, int b) {
			return a/10 == b/10 ? 1 : 2;
		}
		public int getNestLevelDelta(int a) {
			return 0;
		}
	}

}
/*
 =1 =2 >1 =3 =4 <5 <6 <7 =8 =7 =6 <5 >3 =4 =9 >9
 =1 =10 =11 <22 <10 <11 =2 =10 <21 <22 <10 =21 =3
 <1 <2 <3 =1 =4
*/

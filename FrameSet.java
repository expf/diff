import java.awt.Frame;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import java.util.HashSet;


public class FrameSet implements WindowListener {
	HashSet<Frame> set = new HashSet<Frame>();

	public void addFrame(Frame f) {
		f.pack();
		f.setVisible(true);
		synchronized(this) {
			set.add(f);
		}
		f.addWindowListener(this);
	}

	public void windowOpened(WindowEvent e) {}
	public void windowClosing(WindowEvent e) {
		Frame f = (Frame)e.getSource();
		f.dispose();
	}
	public synchronized void windowClosed(WindowEvent e) {
		set.remove(e.getSource());
		notifyAll();
	}
	public void windowIconified(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowActivated(WindowEvent e) {}
	public void windowDeactivated(WindowEvent e) {}

	public synchronized void waitForClosed() throws InterruptedException {
		while (set.size() > 0) { wait(); }
	}

}

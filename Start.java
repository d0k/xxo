import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

public class Start extends Frame implements ActionListener {
	private static final long serialVersionUID = 0L;
	private TextField server;
	private Button c, start, stop;
	private Server s = null;

	public Start() {
		super("Tic-Tac-Toe");
		setLayout(new GridLayout(0,2));

		c = new Button("Start Client");
		c.addActionListener(this);
		add(c);

		server = new TextField("localhost");
		add(server);

		start = new Button("Start Server");
		start.addActionListener(this);
		add(start);
		stop = new Button("Stop Server");
		stop.addActionListener(this);
		add(stop);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing (WindowEvent e) {
				System.exit(0);
			}
		});
		setSize(200, 200);
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((int)(Math.random()*(dim.getHeight()-200)), (int)(Math.random()*(dim.getHeight()-200)));
		setVisible(true);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == c) {
			try {
				new Client(server.getText());
			} catch (IOException io) {
				new MessageBox(getTitle(), "Error connecting to Server: " + io.getMessage());
			}
		} else if (e.getSource() == start) {
			if (s != null)
				s.interrupt();
			s = new Server();
			s.start();
		} else if (e.getSource() == stop) {
			if (s != null)
				s.interrupt();
			s = null;
		}
	}

	public static void main(String[] args) {
		new Start();
	}
}

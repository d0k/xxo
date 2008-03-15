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
		setVisible(true);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == c) {
			try {
				new Client(server.getText());
			} catch (IOException io) {
				javax.swing.JOptionPane.showMessageDialog(this, "Error connecting to Server: " + io.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
			}
		} else if (e.getSource() == start) {
			s = new Server();
			s.start();
		} else if (e.getSource() == stop) {
			if (s != null)
				s.interrupt();
		}
	}

	public static void main(String[] args) {
		new Start();
	}
}

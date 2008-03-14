import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class Client extends Frame {
	private static final long serialVersionUID = 0L;
	private Server.States[][] grid = new Server.States[3][3];
	private Socket server;
	private boolean done = false, won, tie = false;

	public Client(String server) throws UnknownHostException, IOException {
		super("Tic-Tac-Toe");
		setSize(300, 300);
		setResizable(false);
		setVisible(true);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing (WindowEvent e) {
				done = true;
				try {
					Client.this.server.close();
				} catch (IOException io) {}
				dispose();
			}
		});
		addMouseListener(new Mouse());
		for(int i = 0; i < grid.length; i++) {
			for(int j = 0; j < grid[i].length; j++) {
				grid[i][j] = Server.States.NONE;
			}
		}
		this.server = new Socket(server, 12345);
		new Getter().start();
	}

	private void set(int x, int y) {
		try {
			int b = (x&3) << 2;
			b |= (y&3);
			server.getOutputStream().write(b);
		} catch (IOException e) {}
	}

	@Override
	public void paint(Graphics g) {
		g.drawLine(100, 0, 100, 300);
		g.drawLine(200, 0, 200, 300);
		g.drawLine(0, 100, 300, 100);
		g.drawLine(0, 200, 300, 200);
		for(int i = 0; i < grid.length; i++) {
			for(int j = 0; j < grid[i].length; j++) {
				if (grid[i][j] == Server.States.X) {
					g.drawString("X", i*100+50, j*100+50);
				} else if (grid[i][j] == Server.States.O) {
					g.drawString("O", i*100+50, j*100+50);
				}
			}
		}
		if (done) {
			if (tie) {
				g.drawString("TIE", 100, 100);
			} else {
				if (won) {
					g.drawString("WINNER", 100, 100);
				} else {
					g.drawString("LOSER", 100, 100);
				}
			}
		}
	}

	private class Getter extends Thread {
		@Override
		public void run() {
			while (!done) {
				try {
					byte got = (byte)server.getInputStream().read();
					int x = Protocol.x(got);
					int y = Protocol.y(got);
					if (Protocol.opcode(got)  == 1) {
						grid[x][y] = Server.States.X;
					} else if (Protocol.opcode(got)  == 2) {
						grid[x][y] = Server.States.O;
					}
					if (Protocol.opcode(got) == 3) {
						done = true;
						won = true;
					}				
					if (Protocol.opcode(got)  == 4) {
						done = true;
						won = false;
					}
					if (Protocol.opcode(got) == 5) {
						done = true;
						tie = true;
					}
					repaint();
				} catch (IOException e) {}

			}
		}
	}

	private class Mouse extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (!done)
				set(e.getX()/100, e.getY()/100);
		}
	}
}

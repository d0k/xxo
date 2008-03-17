import java.io.*;
import java.net.*;

public class Server extends Thread {
	private ServerSocket server;
	private Socket client1, client2, last;
	private boolean done = false;
	private int port;

	public enum States {
		X, O, NONE
	}

	public static void clearGrid(States[][] grid) {
		for(int i = 0; i < grid.length; i++) {
			for(int j = 0; j < grid[i].length; j++) {
				grid[i][j] = States.NONE;
			}
		}
	}

	private States[][] grid = new States[3][3];

	public Server() {
		this(12345);
	}

	public Server(int port) {
		this.port = port;
	}

	private void write(int tobothclients) throws IOException {
		write(tobothclients, tobothclients);
	}

	private void write(int toclient1, int toclient2) throws IOException {
		client1.getOutputStream().write(toclient1);
		client2.getOutputStream().write(toclient2);
	}

	private synchronized void set(Socket client, int x, int y) throws IOException {
		if (grid[x][y] == States.NONE && x < grid.length && y < grid[0].length && client != last) {
			byte b = 0;
			if (client == client1) {
				grid[x][y] = States.X;
				b = Protocol.SETX << 4;
			} else if (client == client2) {
				grid[x][y] = States.O;
				b = Protocol.SETO << 4;
			}
			b |= (x&3) << 2;
			b |= (y&3);

			write((int)b);

			// check for win
			for (int i = 0; i < grid.length; i++) {
				if (grid[i][0] != States.NONE && grid[i][0] == grid[i][1] && grid[i][0] == grid[i][2] // waagerecht
				                                                                                   || grid[0][i] != States.NONE && grid[0][i] == grid[1][i] && grid[0][i] == grid[2][i]) // senkrecht
					sendDone(client, false);
			}
			if (grid[1][1] != States.NONE && (grid[0][0] == grid[1][1] && grid[0][0] == grid[2][2]
			                                                                                    || grid[2][0] == grid[1][1] && grid[2][0] == grid[0][2]))
				sendDone(client, false);

			if (!done) {
				// check for tie
				boolean tie = true;
				for (int i = 0; i < grid.length; i++) {
					for (int j = 0; j < grid[0].length; j++) {
						if (grid[i][j] == States.NONE)
							tie = false;
					}
				}
				if (tie)
					sendDone(client, true);
			}

			last = client;
		}
	}

	private void sendDone(Socket client, boolean tie) throws IOException {
		final byte win = Protocol.WIN << 4;
		final byte lose = Protocol.LOSS << 4;

		if (tie) {
			write(Protocol.TIE << 4);
			System.err.println("Tie!");
		} else if (client == client1) {
			write(win, lose);
			System.err.println("X has won!");
		} else {
			write(lose, win);
			System.err.println("O has won!");
		}
		try {
			Thread.sleep(2500);
		} catch (InterruptedException e) {}

		clearGrid(grid);
		write(Protocol.NEWROUND << 4);
		System.err.println("Starting new round ...");
	}

	@Override
	public void run() {
		while (!isInterrupted()) {
			System.err.println("Server waiting for connections ...");
			clearGrid(grid);
			client1 = client2 = null;
			try {
				server = new ServerSocket(port);
			} catch (IOException e) {
				System.err.println(e.getMessage());
				break;
			}
			while ((client1 == null || client2 == null) && !done) {
				Socket client = null;
				try {
					client = server.accept();
					if (client1 == null)
						client1 = client;
					else
						client2 = client;
					System.err.println("New connection from " + client.getInetAddress());
				} catch (IOException e) {}
			}
			try {
				server.close();
			} catch (IOException e) {}

			// pick the beginner
			last = (Math.random() >= 0.5) ? client1 : client2;

			if (done)
				break;

			// remove data which was already sent
			try {
				client1.getInputStream().skip(client1.getInputStream().available());
				client2.getInputStream().skip(client2.getInputStream().available());
			} catch (IOException e) {}

			Client c1 = new Client(client1);
			Client c2 = new Client(client2);
			c1.start();
			c2.start();
			try {
				c1.join();
				c2.join();
			} catch (InterruptedException e) { break; }
		}
	}

	@Override
	public void interrupt() {
		super.interrupt();
		done = true;
		try {
			if (server != null)
				server.close();
		} catch (IOException e) {}
		killclient(client1);
		killclient(client2);
		System.err.println("Server terminated!");
	}

	private synchronized void killclient(Socket client) {
		if (client != null) {
			try { client.getOutputStream().write(Protocol.FAIL << 4); } catch (IOException io) {}
			try { client.close(); } catch (IOException io) {}
		}
		client = null;
	}

	private class Client extends Thread {
		private Socket client;

		public Client(Socket client) {
			super("Client");
			this.client = client;
		}

		@Override
		public void run() {
			while (!done) {
				try {
					int got = client.getInputStream().read();
					if (got == -1 || client == null)
						throw new IOException();
					if (Protocol.opcode(got) == Protocol.SET)
						set(client, Protocol.x(got), Protocol.y(got));
				} catch (IOException e) {
					killclient(client1);
					killclient(client2);
					break;
				}
			}
		}
	}

}

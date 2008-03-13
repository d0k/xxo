import java.io.*;
import java.net.*;

public class Server extends Thread {
	private ServerSocket server;
	private Socket client1 = null, client2 = null, last = null;
	boolean done = false;

	public enum States {
		X, O, NONE
	}

	private States[][] grid = new States[3][3];

	public Server() throws IOException {
		this(12345);
	}

	public Server(int port) throws IOException {
		for(int i = 0; i < grid.length; i++) {
			for(int j = 0; j < grid[i].length; j++) {
				grid[i][j] = States.NONE;
			}
		}
		server = new ServerSocket(port);
	}

	private synchronized void set(Socket client, int x, int y) {
		if (grid[x][y] == States.NONE && x < grid.length && y < grid[0].length && client != last) {
			byte b = 0;
			if (client == client1) {
				grid[x][y] = States.X;
				b = 1 << 4;
			} else if (client == client2) {
				grid[x][y] = States.O;
				b = 2 << 4;
			}
			b |= (x&3) << 2;
			b |= (y&3);

			try {
				client1.getOutputStream().write((int)b);
				client2.getOutputStream().write((int)b);
			} catch (IOException e) {}
			// check for tie
			boolean tie = true;
			for (int i = 0; i < grid.length; i++) {
				for (int j = 0; j < grid[0].length; j++) {
					if (grid[i][j] == States.NONE)
						tie = false;
				}
			}
			if (tie) {
				done = true;
				try {
					final byte tiebyte = 5 << 4;
					client1.getOutputStream().write((int)tiebyte);
					client2.getOutputStream().write((int)tiebyte);
				} catch (IOException e) {}
			}

			// check for win
			for (int i = 0; i < grid.length; i++) {
				if (grid[i][0] != States.NONE && grid[i][0] == grid[i][1] && grid[i][0] == grid[i][2] // waagerecht
				                                                                                   || grid[0][i] != States.NONE && grid[0][i] == grid[1][i] && grid[0][i] == grid[2][i]) // senkrecht
					sendDone(client);
			}
			if (grid[1][1] != States.NONE && (grid[0][0] == grid[1][1] && grid[0][0] == grid[2][2]
			                                                                                    || grid[2][0] == grid[1][1] && grid[2][0] == grid[0][2]))
				sendDone(client);
			last = client;			
		}
	}

	private void sendDone(Socket client) {
		final byte win = 3 << 4;
		final byte loose = 4 << 4;
		if (client == client1) {
			try {
				client1.getOutputStream().write((int)win);
				client2.getOutputStream().write((int)loose);
			} catch (IOException e) {}
		} else {
			try {
				client2.getOutputStream().write((int)win);
				client1.getOutputStream().write((int)loose);
			} catch (IOException e) {}
		}
		done = true;
	}

	@Override
	public void run() {
		while (client2 == null && !done) { 
			Socket client = null;
			try { 
				client = server.accept(); 
				if (client1 == null)
					client1 = client;
				else
					client2 = client;
			} catch (IOException e) { 
				e.printStackTrace(); 
			}
		}
		// remove data which was already sent
		try {
			client1.getInputStream().skip(client1.getInputStream().available());
			client2.getInputStream().skip(client2.getInputStream().available());
		} catch (IOException e) {}
		
		new Client(client1).start();
		new Client(client2).start();
	}

	@Override
	public void interrupt() {
		super.interrupt();
		done = true;
		try {
			server.close();
		} catch (IOException e) {}
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
					if (Protocol.opcode(got) == 0) {
						set(client, Protocol.x(got), Protocol.y(got));
					}
				} catch (IOException e) {}
			}
		}
	}

}

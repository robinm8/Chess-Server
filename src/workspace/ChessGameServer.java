package workspace;

import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;

import javax.swing.SwingWorker;

/**
 * Chess Game Server
 * 
 * @author Mark Robinson
 * 
 */
public class ChessGameServer {

	static ArrayList<String> playingColors = new ArrayList<String>();

	static String userPlayingAsWhite = "";
	static String userPlayingAsBlack = "";
	static String whoseTurn = "";

	public static void main(String[] args) throws Exception {
		final HashMap<String, Socket> users = new HashMap<String, Socket>();

		@SuppressWarnings("resource")
		ServerSocket server = new ServerSocket(5679);
		System.out.println("Bound to port " + server.getLocalPort());
		System.out.println("CHESS GAME SERVER READY");

		playingColors.add("White");
		playingColors.add("Black");

		while (true) {
			final Socket client = server.accept();
			System.out.println("New client connect accepted");
			new SwingWorker() {
				@Override
				protected Object doInBackground() throws Exception {
					ChessGameServer ser = new ChessGameServer();
					ClientFromThread cft = ser.new ClientFromThread(client);
					System.out.println("New client connected");
					String name = cft.getUserName();
					if (!name.equals("E404") && !name.equals("EXIT")
							&& !name.equals("REQSERVDATA")) {
						System.out.println("Chess server connected to "
								+ client.getInetAddress() + "  USER: " + name);
						users.put(name, client);
						cft.giveUsersMap(users);
						cft.giveOnlineUsers();
						cft.joinNotify(name);

					} else if (name.equals("REQSERVDATA")) {
						System.out.println("Chess server connected to "
								+ client.getInetAddress() + "  USER: " + name);
						System.out
								.println("Temp connection set. Waiting for command.");
					}
					return null;
				}
			}.execute();

		}
	}

	class ClientFromThread extends Thread {
		private Socket client;
		private Scanner fromClient;
		private PrintWriter toClient;
		private String userName;
		HashMap<String, Socket> users;

		public ClientFromThread(Socket c) throws Exception {
			client = c;
			fromClient = new Scanner(client.getInputStream());
			toClient = new PrintWriter(client.getOutputStream(), true);

			System.out
					.println("Client chat and data ready. Waiting for userName or command");

			userName = getUser();

			while (userName.equals("REQSERVDATA") && client.isConnected()) {
				System.out.println("Command received. Waiting for userName.");
				userName = getUser();
			}

			System.out.println("userName received. Starting chat service.");
			start();
		}

		public void giveUsersMap(HashMap<String, Socket> users) {
			this.users = users;
		}

		public void joinNotify(String s) {
			Collection b = users.keySet();
			for (Object ura : b) {
				if (!ura.equals(s)) {
					System.out.println("Sending to " + ura);

					Socket client = users.get(ura);
					try {
						ClientToThread ctt = new ClientToThread(client);
						ctt.sendMesg(s + " has connected to the server.",
								"SERVER");
						ctt.start();

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}

		public void giveOnlineUsers() {
			toClient.println("SERVER: These users are currently online:");

			int itusers = 0;
			for (Object ur : users.keySet()) {
				itusers++;
				toClient.println(itusers + ". " + ur);
			}
		}

		public String getUserName() {
			return userName;
		}

		private String getUser() {
			String s = "";

			while (s.length() < 1 || s == null) {
				s = fromClient.nextLine().trim();
				System.out.println(s);
			}

			if (s.equalsIgnoreCase("reqservdata")) {
				playingColors.removeAll(Collections.singleton(null));

				toClient.println("Available Playing Color(s): "
						+ Arrays.toString(playingColors.toArray()));
			} else {
				try {
					toClient.println("SERVER: Welcome " + s + ".");
				} catch (Exception e) {
					System.out.println("Anon User connected.");
				}
			}

			return s.toUpperCase();
		}

		public void run() {
			String s = null;
			String toUser;
			String mesg;

			System.out.println("running chat listener");

			try {

				while (client.isConnected() && fromClient.hasNext()) {

					boolean cont = true;

					s = fromClient.nextLine().trim();

					if (s.equalsIgnoreCase("exit")
							|| s.equalsIgnoreCase("e404")) {
						cont = false;
						break;
					}

					if (users != null) {
						for (Object ura : users.keySet()) {
							if (s.equalsIgnoreCase(ura + "-exit")) {

								System.out.println(ura + " has disconnected.");

								if (userPlayingAsBlack.equals(ura)) {
									userPlayingAsBlack = "";
									playingColors.add("Black");
								} else if (userPlayingAsWhite.equals(ura)) {
									userPlayingAsWhite = "";
									playingColors.add("White");
								}

								if (playingColors.size() >= 2) {
									whoseTurn = "";
									System.out.println("Set whoseTurn = empty");
								}

								cont = false;

								for (Object uro : users.keySet()) {
									if (ura != uro && !ura.equals(uro)) {
										Socket client = users.get(uro);
										try {
											ClientToThread ctt = new ClientToThread(
													client);
											if (!s.equalsIgnoreCase("exit")
													&& !s.equalsIgnoreCase("e404")) {
												ctt.sendMesg(
														ura
																+ " has disconnected from the server.",
														"SERVER");
												ctt.start();
											}

										} catch (Exception e) {
										}
									}
								}

								users.remove(ura);
								fromClient.close();
								break;
							} else if (s.equalsIgnoreCase(ura + "-play white")) {
								for (int n = 0; n < playingColors.size(); n++) {
									if (playingColors.get(n) == "White") {
										System.out.println(ura
												+ " is playing as White");
										toClient.println("You are playing as white.");
										playingColors.set(n, null);
										userPlayingAsWhite = (String) ura;
										cont = false;
									}
								}
								cont = false;
							} else if (s.equalsIgnoreCase(ura + "-play black")) {
								for (int n = 0; n < playingColors.size(); n++) {
									if (playingColors.get(n) == "Black") {
										System.out.println(ura
												+ " is playing as Black");
										toClient.println("You are playing as black.");
										playingColors.set(n, null);
										userPlayingAsBlack = (String) ura;
									}
								}
								cont = false;
							}
						}

						for (Object ura : users.keySet()) {
							try {
								Socket client = users.get(ura);
								ClientToThread ctt = new ClientToThread(client);

								System.out.println("userPlayingAsBlack "
										+ userPlayingAsBlack);
								System.out.println("userPlayingAsWhite "
										+ userPlayingAsWhite);
								System.out.println("whoseTurn " + whoseTurn);

								if (!userPlayingAsBlack.isEmpty()
										&& !userPlayingAsWhite.isEmpty()
										&& whoseTurn.isEmpty()) {
									ctt.sendMesg("It is now white's turn.",
											"SERVER");
									ctt.start();
								} else if (userPlayingAsBlack.isEmpty()
										|| userPlayingAsWhite.isEmpty()) {
									ctt.sendMesg("Waiting for another player.",
											"SERVER");
									ctt.start();
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						if (!userPlayingAsBlack.isEmpty()
								&& !userPlayingAsWhite.isEmpty()
								&& whoseTurn.isEmpty()) {
							whoseTurn = "w";
						}
					}

					if (cont) {
						for (int i = 0; i < s.length(); i++) {
							if (s.charAt(i) == '-') {
								toUser = s.substring(0, i).trim().toUpperCase();
								mesg = s.substring(i + 1).trim();

								System.out.println(toUser + " sent " + mesg
										+ " to SERVER.");

								if (users != null) {
									for (Object ura : users.keySet()) {
										System.out
												.println("SERVER relaying message to "
														+ ura);
										Socket client = users.get(ura);
										try {
											ClientToThread ctt = new ClientToThread(
													client);
											ctt.sendMesg(mesg, toUser);
											ctt.start();
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								}
							}
						}
					}

				}
			} catch (IllegalStateException e) {
			}
			System.out.println("Connection Closed");
			try {
				fromClient.close();
				toClient.close();
				client.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	class ClientToThread extends Thread {
		private Socket client;
		private PrintWriter toClient;
		private String mesg;

		public ClientToThread(Socket c) throws Exception {
			client = c;
			toClient = new PrintWriter(client.getOutputStream(), true);
		}

		public void sendMesg(String mesg, String userName) {
			this.mesg = userName + ": " + mesg;
		}

		public void run() {
			toClient.println(mesg);

		}

	}
}

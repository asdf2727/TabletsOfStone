package TabletsOfStone;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

public class CliTabletsOfStone {
    //private static final String _SERVER_ADDRESS = "192.168.5.229";
	private static final String _SERVER_ADDRESS = "20.3.85.95";
    private static final int _SERVER_PORT = 5025;

	public static MsgTabletsOfStone Message(MsgTabletsOfStone send) throws IOException, ClassNotFoundException {
        Socket socket = new Socket(_SERVER_ADDRESS, _SERVER_PORT);

    	OutputStream outStr = socket.getOutputStream();
    	ObjectOutputStream objOutStr = new ObjectOutputStream(outStr);
        objOutStr.writeObject(send);

    	InputStream inStr = socket.getInputStream();
		ObjectInputStream objInStr = new ObjectInputStream(inStr);
		MsgTabletsOfStone message = (MsgTabletsOfStone)objInStr.readObject();
		
		socket.close();
		return message;
	}

	private static void SendOld(String myName, Scanner in) throws IOException, ClassNotFoundException {
		MsgTabletsOfStone message;
		String to_send, arrive_at;
		do {
			System.out.print("Enter message: ");
			to_send = in.nextLine();
			if (to_send.length() != 6) {
				System.out.println("The message must have 6 characters!");
			}
		}
		while (to_send.length() != 6);
		do {
			System.out.print("Enter target username: ");
			arrive_at = in.nextLine();
			message = Message(new MsgTabletsOfStone(myName, arrive_at, to_send));
			System.out.println(message.getStatus());
		}
		while (message.getStatus().equals("[Err] Unknown recipient!"));
	}
	private static void Send(String myName, Scanner in) throws IOException, ClassNotFoundException {
		MsgTabletsOfStone message;
		System.out.print("Enter message: ");
		String to_send = in.nextLine(), arrive_at;
		while (to_send.length() % 6 != 0) {
			to_send += ' ';
		}
		int len = to_send.length() / 6;
		String header = Integer.toString(len);
		while (header.length() < 6) {
			header = '0' + header;
		}
		do {
			System.out.print("Enter target username: ");
			arrive_at = in.nextLine();
			message = Message(new MsgTabletsOfStone(myName, arrive_at, header));
			System.out.println(message.getStatus());
		}
		while (message.getStatus().equals("[Err] Unknown recipient!"));
		for (int i = 0; i < len; i++) {
			String send_now  = "";
			for(int j = 0; j < 6; j++) {
				send_now += to_send.charAt(i * 6 + j);
			}
			Message(new MsgTabletsOfStone(myName, arrive_at, send_now));
		}
	}
	private static void Recive(String myName) throws IOException, ClassNotFoundException {
		MsgTabletsOfStone message;

		while (true) {
			message = Message(new MsgTabletsOfStone(3, myName));
			if (message.getStatus() == null) {
				System.out.print("New message from ");
				System.out.print(message.getFrom());
				System.out.print(": ");
				try {
					int len = Integer.parseInt(message.getData());
					for (int i = 0; i < len; i++) {
						message = Message(new MsgTabletsOfStone(3, myName));
						System.out.print(message.getData());
					}
					System.out.println();
				}
				catch (Exception ignore) {
					String init_send = message.getFrom();
					boolean old = true;
					while (message.getFrom().equals(init_send)) {
						if (message.getData().endsWith("\r")) {
							System.out.print(message.getData().split("\r")[0]);
							old = false;
							break;
						}
						System.out.print(message.getData());
						message = Message(new MsgTabletsOfStone(3, myName));
					}
					if (old) {
						System.out.print(" - old format");
					}
					else {
						System.out.print(" - foreign format");
					}
					System.out.println();
				}
			}
			else {
				break;
			}
		}
	}

    public static void main(String[] args) throws IOException, ClassNotFoundException {
		Scanner in = new Scanner(System.in);
		MsgTabletsOfStone message;
		String myName;

        System.out.print("Username: ");
		myName = in.nextLine();
		message = Message(new MsgTabletsOfStone(1, myName));

		if (message.getStatus().equals("[Success] OK!")) {
			System.out.println("Login OK!");
			while (true) {
				System.out.print("> ");
				String cmd = in.nextLine();
				if (cmd.equals("logout") || cmd.equals("exit")) {
					break;
				}
				switch(cmd) {
				case "send":
					Send(myName, in);
					break;
				case "send_old":
					SendOld(myName, in);
					break;
				case "recive":
					Recive(myName);
					break;
				case "status":
					message = Message(new MsgTabletsOfStone(0, "status-get"));
					System.out.println(message.getStatus());
					break;
				default:
					System.out.println("Invalid command!");
				}
			}
			message = Message(new MsgTabletsOfStone(2, myName));
		}
		else {
			System.out.println("Login failed! Cancelling...");
		}
		in.close();
    }
}

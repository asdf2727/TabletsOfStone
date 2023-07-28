package TabletsOfStone;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class SrvTabletsOfStone {
    // The port bound by the server for listening
    private static final int _PORT = 5025;
    private static final int _TIMEOUT = 4000; // 4 seconds
    
    // Hash map associating the name of a client with a queue of messages sent to that client.
    private static Map<String, Queue<MsgTabletsOfStone>> _msgQueues =
        new HashMap<String, Queue<MsgTabletsOfStone>>();
    
    /**
     * Main entry point in the program. The server loops continuously, waiting for a message from any client.
     * When message is received, it is deserialized, processed, and a response message is sent back.
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        InetAddress inetAddr = InetAddress.getLocalHost();
        ServerSocket server = new ServerSocket(_PORT);
        System.out.printf("[%s @ %d] Server ready!\n", inetAddr.getHostAddress(), _PORT);
        
        while(true) {
            Socket socket = null;
            
        try {
                // Wait for the socket connecting to a client
                socket = server.accept();
                System.out.printf(">---- %s : ", socket.getInetAddress().getHostAddress());
                // Set a timeout such that the server does not freeze
                // if the client never sends a message.
                socket.setSoTimeout(_TIMEOUT);
    
                // Use the input stream of the socket to get the message from the client
                InputStream inStream = socket.getInputStream();
                ObjectInputStream objInStream = new ObjectInputStream(inStream);
                MsgTabletsOfStone inMessage = (MsgTabletsOfStone)objInStream.readObject();
                System.out.println(inMessage);
                
                // Process the incoming message and get the response message in return
                MsgTabletsOfStone outMessage = processMessage(inMessage);

                // Use the output stream of the socket to respond to the client with a status message
                OutputStream outStream = socket.getOutputStream();
                ObjectOutputStream objOutStream = new ObjectOutputStream(outStream);
                objOutStream.writeObject(outMessage);
                
                // Print the updated state of the server
                System.out.printf("----> OK\n");
            } catch (Exception e) {
                System.out.printf("----> %s\n", e.getMessage());
            }
            
            if (socket != null) {
                socket.close();
            }
        }
        
        //server.close();
    }

    /**
     * Processes a given input message returning an output (response) message.
     * @param inMessage - message to be processed.
     * @return response message.
     */
    public static MsgTabletsOfStone processMessage(MsgTabletsOfStone inMessage) {
        MsgTabletsOfStone outMessage;

        switch(inMessage.getType()) {
        case 1: //Login:
            outMessage = processMessageLogin(inMessage.getName());
            break;
        case 2: //Logout:
            outMessage = processMessageLogout(inMessage.getName());
            break;
        case 3: //Receive:
            outMessage = processMessageReceive(inMessage.getName());
            break;
        case 4: //Send:
            outMessage = processMessageSend(inMessage.getFrom(), inMessage.getTo(), inMessage);
            break;
        case 0: //Status:
            outMessage = processMessageStatus(inMessage.getStatus());
            break;
        default:
            outMessage = new MsgTabletsOfStone(0, "[Err] Unsupported message!");
        }
        
        return outMessage;
    }
 
    /**
     * Processes a Login message for the given client name. If the client is already logged
     * in the operation fails and a failing Status message is returned to the caller.
     * @param name - The name of the client logging in.
     * @return Status message indicating success or failure.
     */
    public static MsgTabletsOfStone processMessageLogin(String name) {
        _msgQueues.put(name, new LinkedList<MsgTabletsOfStone>());
        return new MsgTabletsOfStone(0, "[Success] OK!");
    }
    
    /**
     * Processes a Logout message for the given client name. If the client is not already
     * logged in the operation fails and a failing Status message is returned to the caller.
     * @param name - The name of the client logging out.
     * @return Status message indicating success or failure.
     */
    public static MsgTabletsOfStone processMessageLogout(String name) {
        _msgQueues.remove(name);
        return new MsgTabletsOfStone(0, "[Success] OK!");
    }

    /**
     * Processes a Send message for a given sender, targetting the given recepient and
     * containing the given message as payload. If either the send or the recepient
     * are not logged in, the operation fails and a failing Status message is returned to the caller.
     * @param from - the name of the client sending the message.
     * @param to - the name of the client to receive the message.
     * @param message - the message to be relayed.
     * @return Status message indicating success or failure.
     */
    public static MsgTabletsOfStone processMessageSend(String from, String to, MsgTabletsOfStone message) {
        if (!_msgQueues.containsKey(from)) {
            return new MsgTabletsOfStone(0, "[Err] Unknown sender!");
        }
        if (!_msgQueues.containsKey(to)) 
        {
            return new MsgTabletsOfStone(0, "[Err] Unknown recipient!");
        }
        
        Queue<MsgTabletsOfStone> msgQueue = _msgQueues.get(to);
        msgQueue.add(message);
        return new MsgTabletsOfStone(0, "[Success] OK!");
    }
    
    /**
     * Processes a Receive message for a given client. The method takes as parameter the client name
     * expecting to receive the message. If such a message is available, the method returns it as
     * a Send message, encapsulating the Sender's name, IP address and data playload. If no such
     * message exists, the method returns a failing Status message.
     * @param name - the name of the client requesting a message.
     * @return either a Send message or a Status indicating success or failure.
     */
    public static MsgTabletsOfStone processMessageReceive(String name) {
        Queue<MsgTabletsOfStone> msgQueue = _msgQueues.get(name);
        if (msgQueue == null || msgQueue.size() == 0) {
            return new MsgTabletsOfStone(0, "[Err] No message!");
        }
        return msgQueue.remove();
    }
    
    /**
     * Processes a Status message from a registered client. The message contains an "operation" request
     * in its status string, which can be either an "inquire" for server internal status or a 
     * "reset" directive to forcefully logout all clients and purge all message queues.
     * @param status - the status string
     * @return a Status message indicating success or failure.
     */
    private static MsgTabletsOfStone processMessageStatus(String status) {
        return new MsgTabletsOfStone(0, serverStatusString());
    }
    
    /**
     * Generates a string capturing the internal state of the server:
     * Who is logged in, whether there's a secrete associated with the login
     * and how many messages are in that login's queue.
     * @return the server status string.
     */
    public static String serverStatusString() {
        String info = String.format("----[Logged in: %d users]----\n", _msgQueues.size());
        for(Map.Entry<String, Queue<MsgTabletsOfStone>> kvp: _msgQueues.entrySet()) {
            info += String.format("%-25s : ", kvp.getKey().toString());
            info += String.format("%d messages\n", kvp.getValue().size());
        }
        return info;
    }
}

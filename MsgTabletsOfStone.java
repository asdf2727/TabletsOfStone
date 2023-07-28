package TabletsOfStone;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class MsgTabletsOfStone implements Serializable {
    private static final long serialVersionUID = 1L;

    // Message type code for this message
    private int _msgType;
    
    // Fields for MsgCode.Login,Logout,Receive
    private String _name;
    
    // Fields for MsgCode.Send
    private String _from; // name of the sender
    private String _to;   // name of the recepient
    private char[] _data; // data payload
    
    // Fields for MsgCode.Status
    private String _status;

    /**
     * Constructs a message of either Status(0), Login(1), Logout(2) or Receive(3) type.
     * @param msgType - the code for the message type.
     * @param content - the content for either the status or the name, depending on type.
     */
    public MsgTabletsOfStone(int msgType, String content) {
        _msgType = msgType;

        if (_msgType == 0) { // Status
            _status = content;
        } else if (_msgType == 1 || _msgType == 2 || _msgType == 3) { // Login, Logout, Receive
            _name = content;
        } else {
            throw new RuntimeException("Invalid message type");
        }
    }

    /**
     * Constructs a message of Send(4) type.
     * @param from - the sender of the message.
     * @param to - the recipient of the message.
     * @param data - the message content (should be 6 characters long)
     */
    public MsgTabletsOfStone(String from, String to, String data) {
        if (data.length() > 6) {
            throw new IllegalArgumentException("Invalid message length!");
        }
        _msgType = 4;
        _from = from;
        _to = to;
        _data = data.toCharArray();
    }
    
    /**
     * Gives the type of this message (used for all message types).
     * @return the MsgType code of this message.
     */
    public int getType() {
        return _msgType;
    }
    
    /**
     * Gives the client name (used for MsgType.Login,Logout,Receive).
     * @return the client name.
     */
    public String getName() {
        return _name;
    }
    
    /**
     * Gives the sender's name (used for MsgType.Send)
     * @return the sender's name.
     */
    public String getFrom() {
        return _from;
    }
    
    /**
     * Gives the recepient's name (used for MsgType.Receive)
     * @return the recepient's name.
     */
    public String getTo() {
        return _to;
    }
    
    /**
     * Gives the data paylod in this message (used for MsgType.Send).
     * @return the data payload.
     */
    public String getData() {
        return new String(_data);
    }
    
    /**
     * Gives the status string in this message (used for MsgType.Status).
     * @return the status string.
     */
    public String getStatus() {
        return _status;
    }
    
    /**
     * Serializes this object to the given output stream. The method writes the fields common across all
     * message types (the MsgType code) followed only by the fields applicable to this specific type.
     * @param out - output stream to write the object to.
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(_msgType);
        switch(_msgType) {
        case 0: //Status:
            out.writeObject(_status);
            break;
        case 1: //Login:
        case 2: //Logout:
        case 3: //Receive:
            out.writeObject(_name);
            break;
        case 4: //Send:
            out.writeObject(_from);
            out.writeObject(_to);
            out.writeObject(_data);
            break;
        }
    }

    /**
     * Deserializes this object from the given input stream. The method reads the message type first
     * then the reads only the fields applicable to this specific type.
     * @param in - input stream to read the object from.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        _msgType = (int)in.readObject();
        switch(_msgType) {
        case 0: //Status:
            _status = (String)in.readObject();
            break;
        case 1: //Login:
        case 2: //Logout:
        case 3: //Receive:
            _name = (String)in.readObject();
            break;
        case 4:
            _from = (String)in.readObject();
            _to = (String)in.readObject();
            _data = (char[])in.readObject();
            break;
        }
    }
    
    /**
     * Returns a string representation of this object. It formats first the common field
     * (the message type) followed only by the fields applicable to the specific message type.
     * @return the string representation of the object.
     */
    @Override
    public String toString() {
        String output = String.format("[%d]: ", _msgType);
        switch(_msgType) {
        case 0: //Status:
            output += String.format("Status: %s", _status);
            break;
        case 1: //Login:
        case 2: //Logout:
        case 3: //Receive:
            output += String.format("Name: %s", _name);
            break;
        case 4: //Send:
            output += String.format("\nFrom: %s", _from);
            output += String.format("\nTo:   %s", _to);
            output += String.format("\nData: %s",  new String(_data));
            break;
        default:
            output += "Undefined!";
        }

        return output;
    }
}

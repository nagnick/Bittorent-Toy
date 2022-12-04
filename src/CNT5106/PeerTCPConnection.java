package CNT5106;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Comparator;
import java.util.concurrent.LinkedBlockingQueue;

public class PeerTCPConnection extends Thread { // spinning thread waiting for peer messages
    LinkedBlockingQueue<Message> inbox;
    Socket connection;
    DataInputStream in;
    DataOutputStream out;
    int peerID;
    int totalInMessages = 0;
    int totalOptimisticPeriods = 0;
    int totalPreferredPeriods = 0;
    double downloadRate = 0;
    boolean interested = false;
    boolean choked = true; // my view of this peer whether I have choked it or not
    boolean iamChoked = true; // this peers view of me
    boolean haveFile = false; // used to decide when to terminate program

    public PeerTCPConnection(LinkedBlockingQueue<Message> inbox, Socket connection){ // pass in peer info to form tcp connection
        this.inbox = inbox;
        this.connection = connection;
        try {
            out = new DataOutputStream(connection.getOutputStream());
            in = new DataInputStream(connection.getInputStream());
        }
        catch(Exception e){
            System.out.println("Error creating TCP connection ");
        }
    }
    public Message getHandShake(){ // use before starting thread and only once will fail if anyother message
        byte[] messageBytes = new byte[32]; // puts a zero int in message if bellow fails
        System.out.println(" got handShake");
        try {
            messageBytes = in.readNBytes(32);
        }
        catch (Exception e) {
            System.out.println("Error reading TCPIn handshake" + e.toString());
        }
        Message toReturn = new Message(messageBytes, true,peerID);
        System.out.println("returned handshake" + toReturn.toString());
        return toReturn; // peerID not used for handshake peerId in message is read instead
    }
    public void run(){
        try {
            // tcp network stuff
            while(!connection.isClosed()) { // testing required
                // read bytes and create message to put in message queue inbox
                ByteBuffer lengthBuff = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
                int messageLength = lengthBuff.put(in.readNBytes(4)).getInt(0);
                ByteBuffer message = ByteBuffer.allocate(5+messageLength).order(ByteOrder.BIG_ENDIAN);
                message.putInt(0,messageLength);
                message.put(4,in.readNBytes(1));
                if(messageLength != 0)
                    message.put(5,in.readNBytes(messageLength));
                Message toPutInInbox = new Message(message.array(),false,peerID);
                inbox.put(toPutInInbox);
                if(toPutInInbox.type == Message.MessageTypes.piece)
                    incrementTotalInMessages(); // used to track download rate for choking/unchoking
            }
        }
        catch (Exception e) {
            System.out.println("Error running TCPIn thread");
        }
    }
    public boolean send(Message message){
        System.out.println("Sending message: " + message.toString() + " length in bytes:" + message.toBytes().length);
        try {
            out.write(message.toBytes());
            out.flush();
            System.out.println("Message writen out");
            return true;
        }
        catch (Exception e){
            System.out.println("Error writing bytes in send method"+ e);
        }
        return false;
    }
    public void close(){
        try {
            connection.close();
            in.close();
            out.flush();
            out.close();
        }
        catch (Exception e){
            System.out.println("Error closing TCP connection" + e);
        }
    }
    public void setPeerId(int ID){
        peerID = ID;
    }
    public void incrementTotalInMessages(){
        totalInMessages++;
    }
}

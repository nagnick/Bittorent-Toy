package CNT5106;

import java.io.ObjectOutputStream;
import java.net.Socket;

public class TCPOut{ // connections ready to send messages
    Socket connection;
    ObjectOutputStream out;
    public TCPOut(Socket connection){ // pass in peer info to connect to
        // initialize tcp connection to peer
        this.connection = connection;
        try {
            out = new ObjectOutputStream(connection.getOutputStream());
        }
        catch(Exception e){
            System.out.println("Error creating TCPIN ");
        }
    }
    public boolean send(Message message){
        System.out.println("Sending message: " + String.valueOf(message));
        try {
            out.write(message.toBytes());
        }
        catch (Exception e){
            System.out.println("Error writing bytes in send method");
        }
        return false;
    }
}

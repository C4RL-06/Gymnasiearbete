import com.fazecast.jSerialComm.SerialPort;


import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.Scanner;

public class backEnd {
    int[] data = {0,0,0};
    Timestamp[] time = {null,null,null};

    public void startBackEnd()  {
        System.out.println("Back end started");
        collisionDetected(1, 2);

        Scanner sc = new Scanner(System.in);
        System.out.println("1. Communication Over UDP(WiFi) \n2. Communication over COM(USB Cable)");
        int userInput = sc.nextInt();

        if (userInput == 1){
            communicationOverUDP();
        } else if (userInput == 2){
            communicationOverCOM();
        } else {
            System.out.println("Error: Invalid input");
            startBackEnd();
        }


    }

    private void communicationOverCOM(){
        System.out.println("Communication Over COM Started");
        SerialPort arduinoPort = SerialPort.getCommPort("COM3");

        for (int i = 2; i<=6; i++){
            arduinoPort = SerialPort.getCommPort("COM"+i);
            System.out.println("COM"+i);
            if (arduinoPort.openPort()){
                System.out.println("Open port found (COM"+i+")");
                break;
            }
        }



        arduinoPort.setComPortParameters(9600,8,1,0);
        arduinoPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING,10240,0);

        if (!arduinoPort.openPort()){
            System.out.println("ERROR: COM port not available");
        }

        try {
            InputStream in = arduinoPort.getInputStream();
            StringBuilder messageBuffer = new StringBuilder();
            byte[] buffer = new byte[1024]; // max packet size of 1024
            int len;

            while ((len = in.read(buffer)) > 0) {
                String receivedData = new String(buffer, 0, len);
                messageBuffer.append(receivedData);  // Append the received data to buffer

                if (receivedData.contains("\n")) {  // \n is the break which shows the code the string is finished
                    //System.out.println("Received: " + messageBuffer.toString().trim());
                    String inputStream = messageBuffer.toString().trim();
                    //System.out.println(inputStream);

                    String[] splitInputStream = inputStream.split(":"); // splitting the inputStream at :

                    //System.out.println("Test Split: "+splitInputStream[0]+" "+splitInputStream[1]); //Testing the splitting of the inputStream

                    int parseSplitInputStream0 = Integer.parseInt(splitInputStream[0].trim());

                    data[parseSplitInputStream0] = Integer.parseInt(splitInputStream[1].trim());

                    messageBuffer.setLength(0);  // Clear the buffer

                    System.out.println("Data 1: "+data[0]+", Data 2:"+data[1]);
                }
            }



        } catch (Exception e) {
            System.out.println("Error: " + e);
        } finally {
            arduinoPort.closePort();
        }
    }

    private void communicationOverUDP(){
        System.out.println("Communication Over UDP Started");
        //You will need to allow the port 2390 through your firewall

        try{
            DatagramSocket serverSocket = new DatagramSocket(Integer.parseInt("2390")); //The Integer is the port that we are listening at
            System.out.println("Server Started. Listening for Clients on port 2390");

            byte[] receiveData = new byte[1024]; // Max packet size of 1024
            DatagramPacket receivePacket;
            int id = 2390;
            int previousID = 2390;
            long startTime = 0;
            long endTime = 0;

            while (true) {

                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);

                String incomingPacket = new String(receivePacket.getData(),0,receivePacket.getLength());



                // Process the received data
                if (incomingPacket.endsWith("\n")) {
                    endTime = startTime;
                    startTime = System.currentTimeMillis();
                    incomingPacket = incomingPacket.trim(); // Remove whitespace/newline characters

                    String[] splitInputStream = incomingPacket.split(":");

                    previousID = id;
                    id = Integer.parseInt(splitInputStream[0].trim());
                    int value = Integer.parseInt(splitInputStream[1].trim());

                    // Gets the client's IP address and port
                    InetAddress IPAddress = receivePacket.getAddress();
                    int port = receivePacket.getPort();

                    Timestamp timestamp = new Timestamp(startTime);

                    data[id] = value;
                    time[id] = timestamp;
                    System.out.println("[" + timestamp + " ,IP: " + IPAddress + " ,Port: " + port +"]  "+"Data ID: " + id + ", Value: " + value);

                    long timeDifference = startTime-endTime;
                    System.out.println(timeDifference);

                    if (previousID != 2390 && id != previousID){
                        //Need to check timestamp difference.
                        if (timeDifference <= 5000 && timeDifference >= 0){
                            if (data[id] != 0 && data[previousID] != 0){
                                System.out.println("Collision detected between: "+id+" and "+previousID);


                                collisionDetected(id,previousID);

                                data[id] = 0;
                                data[previousID] = 0;
                            }
                        }


                    }



                } else {
                    System.out.println("Error: Incoming packet does not end with newline.");
                }



            }
        } catch (Exception e){
            System.out.println("Error: "+e);
        }

    }
    //Code to notify frontEnd when a collision happens between 2 sensors.
    //When a collision is confirmed between 2 sensors, call "collisionDetected" function ting
    public interface CollisionListener {
        void onCollisionDetected(int sensor1, int sensor2);
    }
    private CollisionListener collisionListener;
    public void setCollisionListener(CollisionListener listener) {
        this.collisionListener = listener;
    }
    public void collisionDetected(int sensor1, int sensor2) {
        System.out.println("Collision detected between sensors: " + sensor1 + " and " + sensor2);

        // Notify listener if it's set
        if (collisionListener != null) {
            collisionListener.onCollisionDetected(sensor1, sensor2);
        }
    }
}

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class backEnd {
    int[] data = {0,0,0};
    Timestamp[] time = {null,null,null};
    ArrayList<Integer> physicalIDs = new ArrayList<>();

    public void startBackEnd()  {
        System.out.println("Back end started");
        collisionDetected(1, 2);
        createIDArrayList();


        communicationOverUDP();

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
                    addDevice(IPAddress);

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

    private int generateNewID(File devicesFile) {
        int maxID = -1;

        // Read the file and find the max ID number
        try (BufferedReader reader = new BufferedReader(new FileReader(devicesFile))) {
            String line;
            while ((line = reader.readLine()) != null) {

                Pattern pattern = Pattern.compile("ID: (\\d+),");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    int id = Integer.parseInt(matcher.group(1));
                    maxID = Math.max(maxID, id);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Return the next available ID
        return maxID + 1;
    }

    void addDevice(InetAddress IP){
        File devicesFile = new File("./Vibrationssensor/device_registry/devices.txt");

        if (isDeviceAlreadyRegistered(devicesFile,IP)){
            System.out.println("Sensor already exists");
            return;
        }


        try (BufferedWriter writer = new BufferedWriter(new FileWriter(devicesFile, true))) {
            int id = generateNewID(devicesFile);
            InetAddress ip = InetAddress.getByName(IP.getHostAddress());
            writer.write("ID: " + id + ", IP: " + ip);
            writer.newLine();
            System.out.println("Device added successfully.");
            if (physicalIDs.isEmpty()){
                createIDArrayList();
            } else{
                physicalIDs.add(id);
            }
        } catch (IOException e) {

            e.printStackTrace();
            System.out.println("Error adding device.");
        }


    }

    private boolean isDeviceAlreadyRegistered(File devicesFile, InetAddress ip) {
        try (BufferedReader reader = new BufferedReader(new FileReader(devicesFile))) {
            String line;
            String ipString = "/" + ip.getHostAddress();  // Format to match '/<ip_address>'

            while ((line = reader.readLine()) != null) {
                // Use the formatted IP string (with leading "/") in the regular expression
                Pattern pattern = Pattern.compile("IP: " + Pattern.quote(ipString));
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    return true; // IP address already exists in the file
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false; // IP address not found, it's safe to add
    }

    void createIDArrayList(){
        try (BufferedReader reader = new BufferedReader(new FileReader( new File("./Vibrationssensor/device_registry/devices.txt")))) {
            String line;
            while ((line = reader.readLine()) != null) {

                Pattern pattern = Pattern.compile("ID: (\\d+),");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    int id = Integer.parseInt(matcher.group(1));
                    physicalIDs.add(id);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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

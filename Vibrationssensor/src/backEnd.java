import java.awt.*;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class backEnd {
    int[] data = {0,0,0};
    Timestamp[] time = {null,null,null};
    public ArrayList<Integer> physicalIDs = new ArrayList<>();
    public ArrayList<Integer> singleCurrentDetections = new ArrayList<>();
    public ArrayList<Point> dataBuffer = new ArrayList<>();

    public void startBackEnd()  {
        System.out.println("Back end started");
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

                //todo
                // rework id detection system
                // rework general id system within this method.

                // Process the received data
                if (incomingPacket.endsWith("\n")) {
                    endTime = startTime;
                    startTime = System.currentTimeMillis();



                    InetAddress IPAddress = receivePacket.getAddress();
                    int port = receivePacket.getPort();

                    previousID = id;
                    id = ipToID(IPAddress);

                    data[id] = 1;
                    // Gets the client's IP address and port

                    //addDevice(IPAddress);

                    Timestamp timestamp = new Timestamp(startTime);


                    time[id] = timestamp;
                    long timeDifference = startTime-endTime;
                    System.out.println("[ \033[0;34m" + timestamp + "\033[0m, IP: \u001B[31m" + IPAddress + "\u001B[0m, Port: \u001B[0;35m" + port +"\u001B[0m ]  "+"Data ID: \033[1;92m" + id+"\033[0m, TD: \u001B[0;33m"+timeDifference+"\033[0mms");
                    dataBuffer.add(new Point(id, Integer.parseInt(String.valueOf(timeDifference))));

                    if (previousID != 2390 && id != previousID){
                        //Need to check timestamp difference.
                        if (timeDifference <= 500 && timeDifference >= 0){
                            if (data[id] != 0 && data[previousID] != 0){
                                System.out.println("Collision detected between: "+id+" and "+previousID);

                                collisionDetected(id, previousID,2);

                                data[id] = 0;
                                data[previousID] = 0;
                            }
                        }
                    } else{
                        int finalId = id;
                        Thread thread = new Thread(() -> {
                            if (!singleCurrentDetections.contains(finalId)) {
                                singleCurrentDetections.add(finalId);
                                collisionDetected(finalId,finalId,1);
                                try {
                                    Thread.sleep(11000); // Pause for the specified delay
                                } catch (InterruptedException e) {
                                    System.out.println("Thread interrupted: " + e.getMessage());
                                }
                                singleCurrentDetections.remove(Integer.valueOf(finalId));
                            }
                        });
                        thread.start();
                    }
                } else {
                    System.out.println("Error: Incoming packet does not end with newline.");
                }
            }
        } catch (Exception e){
            System.out.println("Error: "+e);
        }

    }

    private int ipToID(InetAddress ip){
        File devicesFile = new File("./Vibrationssensor/device_registry/devices.txt");
        if (isDeviceAlreadyRegistered(devicesFile,ip)){
            return getID(devicesFile,ip);
        }else {
            return addDevice(ip);
        }
    }

    private int getID(File devicesFile, InetAddress ip) {
        try (BufferedReader reader = new BufferedReader(new FileReader(devicesFile))) {
            String line;
            String ipString = "/" + ip.getHostAddress();  // Format to match '/<ip_address>'

            while ((line = reader.readLine()) != null) {
                // Use the formatted IP string (with leading "/") in the regular expression
                Pattern pattern = Pattern.compile("IP: " + Pattern.quote(ipString));
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    // Extract the ID from the line
                    Pattern idPattern = Pattern.compile("ID: (\\d+),");
                    Matcher idMatcher = idPattern.matcher(line);
                    if (idMatcher.find()) {
                        return Integer.parseInt(idMatcher.group(1));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error reading the devices file.");
        }
        return physicalIDs.size(); // Return -1 if IP not found
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

    private int addDevice(InetAddress IP){
        File devicesFile = new File("./Vibrationssensor/device_registry/devices.txt");

        if (isDeviceAlreadyRegistered(devicesFile,IP)){
            System.out.println("Sensor already exists");
            return 2390;
        }


        try (BufferedWriter writer = new BufferedWriter(new FileWriter(devicesFile, true))) {
            int id = generateNewID(devicesFile);
            InetAddress ip = InetAddress.getByName(IP.getHostAddress());
            writer.write("ID: " + id + ", IP: " + ip);
            writer.newLine();
            System.out.println("Device added successfully.");
            if (physicalIDs.isEmpty()){
                createIDArrayList();
                return getID(devicesFile, ip);
            } else{
                physicalIDs.add(id);
                onDevicesChanged();
                return id;
            }
        } catch (IOException e) {

            e.printStackTrace();
            System.out.println("Error adding device.");
        }
        return -1;

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
            System.out.println("Actually develops paranoid schizophrenia");
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
            onDevicesChanged();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //Code to notify frontEnd when a collision happens between 2 sensors.
    //When a collision is confirmed between 2 sensors, call "collisionDetected" function ting
    public interface CollisionListener {
        void onCollisionDetected(int sensor1, int sensor2, int singleOrDoubleDetection);
    }
    private CollisionListener collisionListener;
    public void setCollisionListener(CollisionListener listener) {
        this.collisionListener = listener;
    }
    public void collisionDetected(int sensor1, int sensor2, int singleOrDoubleDetection) {
        //System.out.println("Collision detected between sensors: " + sensor1 + " and " + sensor2);

        // Notify listener if it's set
        if (collisionListener != null) {
            collisionListener.onCollisionDetected(sensor1, sensor2, singleOrDoubleDetection);
        }
    }

    public interface DeviceListener {
        void onDevicesChanged(ArrayList<Integer>deviceList);
    }
    private DeviceListener deviceListener;
    public void setDeviceListener(DeviceListener listener) {
        this.deviceListener = listener;
    }
    public void onDevicesChanged() {


        // Notify listener if it's set
        if (deviceListener != null) {
            deviceListener.onDevicesChanged(physicalIDs);
        }
    }
}

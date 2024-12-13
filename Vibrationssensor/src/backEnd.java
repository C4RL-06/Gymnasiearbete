import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class backEnd {
    File devicesFile = new File("./Vibrationssensor/device_registry/devices.txt");
    public ArrayList<Integer> physicalIDs = new ArrayList<>();
    public ArrayList<Integer> singleCurrentDetections = new ArrayList<>();
    private final BlockingQueue<dataOBJ> dataBuffer = new LinkedBlockingQueue<>();
    private final Map<InetAddress, Integer> IDandIPMap = new HashMap<>();

    public void startBackEnd()  {
        System.out.println("Back end started");
        createIDArrayList();
        processData(); //this runs on a seperate thread which hopefully wont interrupt the indata.
        communicationOverUDP();
    }

    protected class dataOBJ {
        public DatagramPacket recievedPacket;
        public long timeOfRecieve;
        public dataOBJ(DatagramPacket recievedPacket, long timeOfRecieve) {
            this.recievedPacket = recievedPacket;
            this.timeOfRecieve = timeOfRecieve;
        }
    }

    protected class IDandIP {
        public InetAddress IPAdress;
        public int correspondingID;
        public IDandIP(InetAddress IPAdress, int correspondingID){
            this.IPAdress = IPAdress;
            this.correspondingID = correspondingID;
        }
    }

    private void processData(){

        Thread processDataThread = new Thread(() -> {
            try {
                while (true) {
                    // Take the next packet, blocking until one is available
                    dataOBJ packet1 = dataBuffer.take();
                    dataOBJ packet2 = dataBuffer.take();

                    String incomingPacket1 = new String(packet1.recievedPacket.getData(), 0, packet1.recievedPacket.getLength());
                    String incomingPacket2 = new String(packet2.recievedPacket.getData(), 0, packet2.recievedPacket.getLength());

                    if (incomingPacket1.endsWith("\n") && incomingPacket2.endsWith("\n")) {
                        long timeDiff = (packet2.timeOfRecieve - packet1.timeOfRecieve) / 1_000_000; // Convert from ns to ms

                        InetAddress packet1IP = packet1.recievedPacket.getAddress();
                        InetAddress packet2IP = packet2.recievedPacket.getAddress();

                        System.out.printf("[ IP: %s, Port: %d ] Data ID: %d, TD: %dms%n",
                                // Convert to ms
                                packet1IP, packet1.recievedPacket.getPort(),
                                ipToID(packet1IP), timeDiff);

                        if (!packet1IP.equals(packet2IP) && timeDiff <= 500) {
                            collisionDetected(ipToID(packet1IP), ipToID(packet2IP), 2);
                        } else {
                            int packet1ID = ipToID(packet1IP);
                            Thread thread = new Thread(() -> {
                                if (!singleCurrentDetections.contains(packet1ID)) {
                                    singleCurrentDetections.add(packet1ID);
                                    collisionDetected(packet1ID, packet1ID, 1);
                                    try {
                                        Thread.sleep(11000); // Pause for the specified delay
                                    } catch (InterruptedException e) {
                                        System.out.println("Thread interrupted: " + e.getMessage());
                                    }
                                    singleCurrentDetections.remove(Integer.valueOf(packet1ID));
                                }
                            });
                            thread.start();
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error in processData: " + e.getMessage());
            }
        });
        processDataThread.start();
    }
    private void communicationOverUDP(){
        System.out.println("Communication Over UDP Started");
        //You will need to allow the port 2390 through your firewall

        try {
            DatagramSocket serverSocket = new DatagramSocket(Integer.parseInt("2390")); //The Integer is the port that we are listening at
            System.out.println("Server Started. Listening for Clients on port 2390");

            while (true) {
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                serverSocket.receive(receivePacket); // Blocking call
                long timestamp = System.nanoTime(); // High precision timestamp
                synchronized (dataBuffer) {
                    dataBuffer.add(new dataOBJ(receivePacket, timestamp));
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private int ipToID(InetAddress ip) {
        return IDandIPMap.computeIfAbsent(ip, this::addDevice);
    }

    private int addDevice(InetAddress ip) {
        int id = IDandIPMap.size();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(devicesFile, true))) {
            writer.write("ID: " + id + ", IP: " + ip);
            writer.newLine();
            writer.flush();
            System.out.println("Device added (" + ip + ")");
            createIDArrayList();
            onDevicesChanged();
        } catch (IOException e) {
            System.out.println("Error adding device: " + e.getMessage());
        }
        return id;
    }
    void createIDArrayList() {
        // Clear the ID mapping and physicalIDs list
        IDandIPMap.clear();
        physicalIDs.clear();

        try (BufferedReader reader = new BufferedReader(new FileReader(devicesFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Extract the IP address and ID from the file
                Pattern ipPattern = Pattern.compile("/(\\d+\\.\\d+\\.\\d+\\.\\d+)");
                Pattern idPattern = Pattern.compile("ID: (\\d+)");
                Matcher ipMatcher = ipPattern.matcher(line);
                Matcher idMatcher = idPattern.matcher(line);

                if (ipMatcher.find() && idMatcher.find()) {
                    String ipString = ipMatcher.group(1);
                    int id = Integer.parseInt(idMatcher.group(1));
                    InetAddress ipAddress = InetAddress.getByName(ipString);

                    // Populate the map with existing entries
                    IDandIPMap.put(ipAddress, id);
                    physicalIDs.add(id); // Maintain physical ID list if needed
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

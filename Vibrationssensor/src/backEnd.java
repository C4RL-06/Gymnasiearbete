import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class backEnd {
    File devicesFile = new File("./Vibrationssensor/device_registry/devices.txt");
    public ArrayList<Integer> physicalIDs = new ArrayList<>();
    public ArrayList<Integer> singleCurrentDetections = new ArrayList<>();
    private final Queue<dataOBJ> dataBuffer = new ConcurrentLinkedQueue<>();
    private final Map<InetAddress, Integer> IDandIPMap = new ConcurrentHashMap<>();

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

    private void processData() {
        Thread processDataThread = new Thread(() -> {
            try {
                while (true) {
                    if (dataBuffer.size() >= 2) {
                        synchronized (dataBuffer) {
                            // Retrieve packets and sort by timeOfRecieve
                            List<dataOBJ> sortedPackets = new ArrayList<>(dataBuffer);
                            sortedPackets.sort(Comparator.comparingLong(p -> p.timeOfRecieve));
                            dataBuffer.clear();
                            dataBuffer.addAll(sortedPackets);

                            dataOBJ packet1 = dataBuffer.poll();
                            dataOBJ packet2 = dataBuffer.poll();

                            // Check packet pairing
                            if (!packet1.recievedPacket.getAddress().equals(packet2.recievedPacket.getAddress())) {
                                System.out.println("Mismatched packet sources, skipping.");
                                continue;
                            }

                            // Calculate time difference
                            long timeDiff = packet2.timeOfRecieve - packet1.timeOfRecieve;

                            if (timeDiff == 0) {
                                System.out.println("Duplicate or simultaneous packets, skipping.");
                                continue;
                            }

                            // Debugging info
                            System.out.printf("[ IP: %s, Port: %d ] Data ID: %d, TD: %dms%n",
                                    packet1.recievedPacket.getAddress(), packet1.recievedPacket.getPort(),
                                    ipToID(packet1.recievedPacket.getAddress()), timeDiff);

                            // Proceed with processing as before
                            if (timeDiff <= 500) {
                                collisionDetected(ipToID(packet1.recievedPacket.getAddress()),
                                        ipToID(packet2.recievedPacket.getAddress()), 2);
                            } else {
                                handleSingleDetection(packet1);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error in processData: " + e.getMessage());
                e.printStackTrace();
            }
        });
        processDataThread.start();
    }

    private void handleSingleDetection(dataOBJ packet) {
        int packetID = ipToID(packet.recievedPacket.getAddress());
        Thread thread = new Thread(() -> {
            if (!singleCurrentDetections.contains(packetID)) {
                singleCurrentDetections.add(packetID);
                collisionDetected(packetID, packetID, 1);
                try {
                    Thread.sleep(11000); // Pause for the specified delay
                } catch (InterruptedException e) {
                    System.out.println("Thread interrupted: " + e.getMessage());
                }
                singleCurrentDetections.remove(Integer.valueOf(packetID));
            }
        });
        thread.start();
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
                synchronized (dataBuffer) {
                    dataBuffer.add(new dataOBJ(receivePacket, System.currentTimeMillis()));
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private synchronized int ipToID(InetAddress ip) {
        return IDandIPMap.computeIfAbsent(ip, this::addDevice);
    }

    private final AtomicInteger idCounter = new AtomicInteger();

    private synchronized int addDevice(InetAddress IP) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(devicesFile, true))) {
            int id = idCounter.getAndIncrement(); // Generate a unique ID
            writer.write("ID: " + id + ", IP: " + IP);
            writer.newLine();
            writer.flush();
            System.out.println("Device added (" + IP + ")");
            physicalIDs.add(id);
            onDevicesChanged();
            return id;
        } catch (IOException e) {
            System.out.println("Error adding device: " + e.getMessage());
            return -1; // Return a default value if there's an error
        }
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
                    idCounter.incrementAndGet();
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

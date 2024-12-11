import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class backEnd {
    File devicesFile = new File("./Vibrationssensor/device_registry/devices.txt");
    int[] data = {0,0,0};
    Timestamp[] time = {null,null,null};
    public ArrayList<Integer> physicalIDs = new ArrayList<>();
    public ArrayList<Integer> singleCurrentDetections = new ArrayList<>();
    public ArrayList<dataOBJ> dataBuffer = new ArrayList<>();
    public ArrayList<IDandIP> IDandIPList = new ArrayList<>();

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
        Thread processDataThread = new Thread(()-> {
            try {
                //DatagramSocket serverSocket = new DatagramSocket(Integer.parseInt("2390")); //The Integer is the port that we are listening at
                //System.out.println("Server Started. Listening for Clients on port 2390");

                InetAddress packet1IP, packet2IP;

                while (true) {
                    if (dataBuffer.size() >= 2) {
                        //serverSocket.receive(dataBuffer.get(0).recievedPacket);
                        //serverSocket.receive(dataBuffer.get(1).recievedPacket);

                        String incomingPacket = new String(dataBuffer.get(0).recievedPacket.getData(), 0, dataBuffer.get(0).recievedPacket.getLength());
                        String incomingPacket2 = new String(dataBuffer.get(1).recievedPacket.getData(), 0, dataBuffer.get(1).recievedPacket.getLength());
                        if (incomingPacket.endsWith("\n") && incomingPacket2.endsWith("\n")) {
                            packet1IP = dataBuffer.get(0).recievedPacket.getAddress();
                            packet2IP = dataBuffer.get(1).recievedPacket.getAddress();
                            System.out.println("[ \033[0;34m" + new Timestamp(dataBuffer.get(0).timeOfRecieve) + "\033[0m, IP: \u001B[31m" + packet1IP + "\u001B[0m, Port: \u001B[0;35m" + dataBuffer.get(0).recievedPacket.getPort() +"\u001B[0m ]  "+"Data ID: \033[1;92m" + ipToID(packet1IP) +"\033[0m, TD: \u001B[0;33m"+ (dataBuffer.get(1).timeOfRecieve - dataBuffer.get(0).timeOfRecieve) + "\033[0mms");
                            if (!String.valueOf(packet1IP).equals(String.valueOf(packet2IP)) && dataBuffer.get(1).timeOfRecieve - dataBuffer.get(0).timeOfRecieve <= 500) {
                                //collision detected between 2 sensors, get ID here and pass on to collisiondetected
                                collisionDetected(ipToID(packet1IP), ipToID(packet2IP), 2);
                            } else {
                                //Single collision detected here
                                int ID = ipToID(packet1IP);
                                Thread thread = new Thread(() -> {
                                    if (!singleCurrentDetections.contains(ID)) {
                                        singleCurrentDetections.add(ID);
                                        collisionDetected(ID,ID,1);
                                        try {
                                            Thread.sleep(11000); // Pause for the specified delay
                                        } catch (InterruptedException e) {
                                            System.out.println("Thread interrupted: " + e.getMessage());
                                        }
                                        singleCurrentDetections.remove(Integer.valueOf(ID));
                                    }
                                });
                                thread.start();
                            }
                        }
                        dataBuffer.remove(0);
                    } else {
                        Thread.sleep(250); //to not spam
                    }
                }
            } catch (Exception e){
                System.out.println("Error: "+e);
            }
        });
        processDataThread.start();
    }
    protected int id = 2390;
    protected int previousID = 2390;
    protected byte[] receiveData = new byte[1024]; // Max packet size of 1024
    private void communicationOverUDP(){
        System.out.println("Communication Over UDP Started");
        //You will need to allow the port 2390 through your firewall

        try {
            DatagramSocket serverSocket = new DatagramSocket(Integer.parseInt("2390")); //The Integer is the port that we are listening at
            System.out.println("Server Started. Listening for Clients on port 2390");
            DatagramPacket receivePacket;

            while (true) {
                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                dataBuffer.add(new dataOBJ(receivePacket, System.currentTimeMillis()));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private int ipToID(InetAddress ip){
        if (isDeviceAlreadyRegistered(ip)){
            return getID(ip);
        } else {
            return addDevice(ip);
        }
    }

    private int getID(InetAddress ip) {
        if (IDandIPList.size() >= 1) {
            for (IDandIP iDandIP : IDandIPList) {
                if (String.valueOf(iDandIP.IPAdress).equals(String.valueOf(ip))) {
                    return iDandIP.correspondingID;
                }
            }
        }
        return -1;
    }

    private int addDevice(InetAddress IP){
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(devicesFile, true))) {
            int id = IDandIPList.size();
            writer.write("ID: " + id + ", IP: " + IP);
            writer.newLine();
            writer.flush();
            System.out.println("Device added (" + IP + ")");
            createIDArrayList();
            onDevicesChanged();
            return id;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error adding device.");
        }
        return -1;
    }

    private boolean isDeviceAlreadyRegistered(InetAddress ip) {
        if (!IDandIPList.isEmpty()) {
            for (IDandIP iDandIP : IDandIPList) {
                if (String.valueOf(iDandIP.IPAdress).equals(String.valueOf(ip))) {
                    return true;
                }
            }
        }
        return false; // IP address not found, it's safe to add
    }

    void createIDArrayList(){
        IDandIPList.clear();
        physicalIDs.clear();

        try (BufferedReader reader = new BufferedReader(new FileReader(devicesFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Pattern IPAdressPattern = Pattern.compile("/(\\d+\\.\\d+\\.\\d+\\.\\d+)");
                Matcher IPAdressMatcher = IPAdressPattern.matcher(line);

                if (IPAdressMatcher.find()) {
                    String IPAdressUnformated = IPAdressMatcher.group(1);
                    InetAddress IPAddress = InetAddress.getByName(IPAdressUnformated);
                    physicalIDs.add(IDandIPList.size());
                    IDandIPList.add(new IDandIP(IPAddress, IDandIPList.size()));
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

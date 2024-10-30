import com.fazecast.jSerialComm.SerialPort;
import java.io.InputStream;

public class backEnd {
    int[] id = {0,0,0};
    int[] data = {0,0,0};

    public void startBackEnd()  {
        System.out.println("Back end started");
        //Maybe wrong com port


        SerialPort arduinoPort = SerialPort.getCommPort("COM3");

        for (int i = 1; i<=6; i++){
            arduinoPort = SerialPort.getCommPort("COM"+i);
            System.out.println("COM"+i);
            if (arduinoPort.openPort()){
                System.out.println("Open port found (COM"+i+")");
                break;
            }
        }



        arduinoPort.setComPortParameters(9600,8,1,0);
        arduinoPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING,10024,0);

        if (!arduinoPort.openPort()){
            System.out.println("ERROR: COM port not available");
        }

        try {
            InputStream in = arduinoPort.getInputStream();
            StringBuilder messageBuffer = new StringBuilder();
            byte[] buffer = new byte[1024];
            int len;

            while ((len = in.read(buffer)) > 0) {
                String receivedData = new String(buffer, 0, len);
                messageBuffer.append(receivedData);  // Append the received data to buffer

                if (receivedData.contains("\n")) {  // \n is the break which shows the code the string is finished
                    //System.out.println("Received: " + messageBuffer.toString().trim());
                    String inputStream = messageBuffer.toString().trim();
                    //System.out.println(inputStream);

                    String[] splitInputStream = inputStream.split(":"); // spliting the inputStream at :

                    //System.out.println("Test Split: "+splitInputStream[0]+" "+splitInputStream[1]); //Testing the spliting of the inputStream

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
}

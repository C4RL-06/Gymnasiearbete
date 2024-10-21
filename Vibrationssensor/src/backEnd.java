import com.fazecast.jSerialComm.SerialPort;
import java.io.InputStream;

public class backEnd {
    public void startBackEnd()  {
        System.out.println("Back end started");
        //Maybe wrong com port


        SerialPort arduinoPort = SerialPort.getCommPort("COM3");

        for (int i = 1; i<=6; i++){
            arduinoPort = SerialPort.getCommPort("COM"+i);
            System.out.println("COM"+i);
            if (arduinoPort.openPort()){
                break;
            }
        }



        arduinoPort.setComPortParameters(9600,8,1,0);
        arduinoPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING,1024,0);

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
                    System.out.println("Received: " + messageBuffer.toString().trim());
                    messageBuffer.setLength(0);  // Clear the buffer
                }
            }

        } catch (Exception e) {
            System.out.println("Error: " + e);
        } finally {
            arduinoPort.closePort();
        }



    }
}

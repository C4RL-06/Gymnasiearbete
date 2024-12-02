#include <WiFiS3.h>

int status = WL_IDLE_STATUS;
#include "arduino_secrets.h" 
///////please enter your sensitive data in the Secret tab/arduino_secrets.h
char ssid[] = SECRET_SSID;        // your network SSID (name)
char pass[] = SECRET_PASS;    // your network password (use for WPA, or use as key for WEP)
int keyIndex = 0;            // your network key index number (needed only for WEP)

unsigned int localPort = 2390;      // local port to listen on
IPAddress server(10,151,160,167);     //Server ip

WiFiUDP Udp;
const int micPin = 7;

void setup() {
  //Initialize serial and wait for port to open:
  Serial.begin(9600);
  //while (!Serial) {;} FÖR USB DEBUG!!!

  // check for the WiFi module:
  if (WiFi.status() == WL_NO_MODULE) {
    Serial.println("Communication with WiFi module failed!");
    // don't continue
    while (true);
  }

  String fv = WiFi.firmwareVersion();
  if (fv < WIFI_FIRMWARE_LATEST_VERSION) {
    Serial.println("Please upgrade the firmware");
  }

  // attempt to connect to WiFi network:
  while (status != WL_CONNECTED) {
    Serial.print("Attempting to connect to SSID: ");
    Serial.println(ssid);
    
    status = WiFi.begin(ssid, pass);

    
    delay(5000);
    if (status == WL_CONNECTED) {
        Serial.println("Connected to WiFi");
    } else {
        Serial.print("Connection failed, status: ");
        Serial.println(status);
    }
  }
  
  printWifiStatus();

  Serial.println("\nStarting connection to server...");
  pinMode(micPin, INPUT);
  Udp.begin(localPort);
}

void loop() {
  
  if (digitalRead(micPin) == 1)
    sendData();
    //Serial.println(1);
  
   

}

void sendData(){
  int ranID = random(0,2);
  
  char data[] = "\n";

  Udp.beginPacket(server, 2390);
  Udp.print(ranID);
  Udp.print(":");
  Udp.print(1);
  Udp.print(data);
  Udp.endPacket();
}

void printWifiStatus() {
  // print the SSID of the network you're attached to:
  Serial.print("SSID: ");
  Serial.println(WiFi.SSID());

  // print your board's IP address:
  IPAddress ip = WiFi.localIP();
  Serial.print("IP Address: ");
  Serial.println(ip);

  // print the received signal strength:
  long rssi = WiFi.RSSI();
  Serial.print("signal strength (RSSI):");
  Serial.print(rssi);
  Serial.println(" dBm");
}

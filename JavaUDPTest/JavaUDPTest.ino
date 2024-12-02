#include <WiFi.h>

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
  //Serial.begin(9600);
  //while (!Serial) { ; } //FÖR USB DEBUG!!!

  WiFi.begin(ssid, pass);
  Serial.print("Connecting to WiFi");

  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    //Serial.print(".");
  }
  
  Serial.println("Connected to WiFi");
  Serial.print("IP Address: ");
  Serial.println(WiFi.localIP());
  
  
  printWifiStatus();

  Serial.println("\nStarting connection to server...");
  pinMode(micPin, INPUT);
  Udp.begin(localPort);
}

void loop() {

  if (digitalRead(micPin) == 1) 
    sendData();
  //Serial.println(digitalRead(micPin));
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
  delay(2000);
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

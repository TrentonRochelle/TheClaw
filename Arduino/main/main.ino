//  Sketc: basicSerialWithNL_001
// 
//  Uses hardware serial to talk to the host computer and software serial 
//  for communication with the Bluetooth module
//  Intended for Bluetooth devices that require line end characters "\r\n"
//
//  Pins
//  Arduino 5V out TO BT VCC
//  Arduino GND to BT GND
//  Arduino D9 to BT RX through a voltage divider
//  Arduino D8 BT TX (no need voltage divider)
//
//  When a command is entered in the serial monitor on the computer 
//  the Arduino will relay it to the bluetooth module and display the result.
//
 
#include <Servo.h>
#include <SoftwareSerial.h>
SoftwareSerial BTserial(8, 9); // RX | TX
//Servo servoWaist;  // create servo object to control a servo
//Servo servoShoulder;
//Servo servoElbow;
int num;
const long baudRate = 9600; 
char c=' ';
String input = "";
boolean NL = true;
void setup() 
{
//    servoElbow.attach(2);
//    servoShoulder.attach(3);
//    servoElbow.writeMicroseconds(1000);
//    servoShoulder.writeMicroseconds(1000);
//    randomSeed(analogRead(0));

    Serial.begin(baudRate);
    Serial.print("Sketch:   ");   Serial.println(__FILE__);
    Serial.print("Uploaded: ");   Serial.println(__DATE__);
    Serial.println(" ");
 
    BTserial.begin(baudRate);  
//    Serial.print("BTserial started at "); Serial.println(baudRate);
//    Serial.println(" ");
}

String getString_BT(){
    String in = "";
    while(!BTserial.available()){
      
    }
//    servoElbow.detach();
    c = BTserial.read();
    in = in + c;
    while(BTserial.available() | BTserial.available()){
      c = BTserial.read();
      in = in + c;
    }
//    Serial.println(in);
//    servoElbow.attach(11);

  return in;
}

void loop()
{
    // Read from the Bluetooth module and send to the Arduino Serial Monitor
    input = getString_BT();
    Serial.println(input);
//    char ch = input[0];
//    ch = ch & ~(0x20);
//    input.remove(0,1);
//    int n = input.toInt();  //convert readString into a number
////    Serial.println(n); //so you can see the integer
//    switch(ch){
////      case('W'):{
////        servoWaist.writeMicroseconds(n);
////        break;
////      }
//      case('S'):{
//        servoShoulder.writeMicroseconds(n);
//        break;
//      }
//      case('E'):{
//        servoElbow.writeMicroseconds(n);
//        break;
//      }
//      case(DEFAULT):{
//        break;
//      }
//
//      
//    }

 
 
//    // Read from the Serial Monitor and send to the Bluetooth module
//    if (Serial.available())
//    {
//        c = Serial.read();
//        BTserial.write(c);   
// 
//        // Echo the user input to the main window. The ">" character indicates the user entered text.
//        if (NL) { Serial.print(">");  NL = false; }
//        Serial.write(c);
//        if (c==10) { NL = true; }
//    }
 
}

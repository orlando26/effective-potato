#include <SoftwareSerial.h>
#include<Servo.h>
SoftwareSerial bt(10, 11); // RX | TX
Servo brazo;
char val;
void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
  bt.begin(9600);
  brazo.attach(3);
  brazo.write(0);
}

void loop() {
  // put your main code here, to run repeatedly:
  if(bt.available() > 0){
    val = bt.read();
    Serial.println((int)val);
    brazo.write((int)val);
    //printPose(pose);  
  }
}

void printPose(char poseChar){
  String pose;
  switch(poseChar){
    case 't':
      pose = "Double tap";
      break;
    case 'f':
      pose = "fist";
      break;
    case 'w':
      pose = "Wave in";
      break;
    case 'W':
      pose = "Wave out";
      break;
    case 's':
      pose = "Spread fingers";
      break;
    default:
      pose = "nada";
      break;
  }
  Serial.println(pose);
}

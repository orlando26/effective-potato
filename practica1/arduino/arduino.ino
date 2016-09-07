#include <SoftwareSerial.h>

SoftwareSerial bt(10, 11);
void setup() {
  // initialize serial communication at 9600 bits per second:
  Serial.begin(9600);
  bt.begin(9600);
}

// the loop routine runs over and over again forever:
void loop() {
  // read the input on analog pin 0:
  int sensorValue = analogRead(A0);
  // print out the value you read:
  if(sensorValue >= 50){
    Serial.println("1");
    bt.println("1");
    }else{
      Serial.println("0");
      bt.println("0");
}
  
} 

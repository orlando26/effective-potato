#include <SoftwareSerial.h>
int led = 13;
float tmp;
SoftwareSerial bt(10, 11);
void setup() {
  // initialize serial communication at 9600 bits per second:
  Serial.begin(9600);
  bt.begin(9600);
  pinMode(led, OUTPUT);
}

// the loop routine runs over and over again forever:
void loop() {
  // read the input on analog pin 0:
  int sensorValue = analogRead(A0);
  tmp = analogRead(A1);
  
  tmp = (5.0 * tmp * 100.0)/1024.0;
  // print out the value you read:
  if(sensorValue >= 20){
    Serial.print("1");
    bt.print("1");
    digitalWrite(led, HIGH);
    }else{
      Serial.print("0");
      bt.print("0");
    digitalWrite(led, LOW);
}
Serial.print("-");
bt.print("-");
Serial.println(tmp);
bt.println(tmp);
  
} 

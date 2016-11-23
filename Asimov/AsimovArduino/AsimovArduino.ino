#include <SoftwareSerial.h>
#include<Servo.h>
SoftwareSerial bt(10, 11); // RX | TX
Servo brazo;
Servo shield;
char val;
int motor1[2] = {5, 4};
int motor2[2] = {6, 7};
void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
  bt.begin(9600);
  brazo.attach(3);
  brazo.write(0);
  shield.attach(9);
  shield.write(48);
  pinMode(4, OUTPUT);
  pinMode(5, OUTPUT);
  pinMode(6, OUTPUT);
  pinMode(7, OUTPUT);
}

void loop() {
  // put your main code here, to run repeatedly:
  if(bt.available() > 0){
    val = bt.read();
    if((int)val <= 90){
      Serial.print("Angulo del brazo: ");
      Serial.println((int)val);
      brazo.write((int)val);  
    }else{
      String dato;
      switch(val){
        case (char)91:
          Serial.println("escudo");
          usarEscudo();
          break;
        case (char)92:
          Serial.println("avanzar");
          avanzar();
          break;
        case (char)93:
          Serial.println("retroceder");
          retroceder();
          break;
        case (char)94:
          Serial.println("izquierda");
          izquierda();
          break;
        case (char)95:
          Serial.println("derecha");
          derecha();
          break;
        case (char)96:
          Serial.println("detener");
          detener();
          break;
        default:
          Serial.print("caracter desconocido: ");
          Serial.println(val);
      }  
    }
    
  }
}

void avanzar(){
  digitalWrite(motor1[0], HIGH);
  digitalWrite(motor1[1], LOW);
  digitalWrite(motor2[0], HIGH);
  digitalWrite(motor2[1], LOW);
}
void retroceder(){
  digitalWrite(motor1[0], LOW);
  digitalWrite(motor1[1], HIGH);
  digitalWrite(motor2[0], LOW);
  digitalWrite(motor2[1], HIGH);
}
void detener(){
  digitalWrite(motor1[0], LOW);
  digitalWrite(motor1[1], LOW);
  digitalWrite(motor2[0], LOW);
  digitalWrite(motor2[1], LOW);
}
void derecha(){
  digitalWrite(motor1[0], LOW);
  digitalWrite(motor1[1], LOW);
  digitalWrite(motor2[0], LOW);
  digitalWrite(motor2[1], HIGH);
}

void izquierda(){
  digitalWrite(motor1[0], LOW);
  digitalWrite(motor1[1], HIGH);
  digitalWrite(motor2[0], LOW);
  digitalWrite(motor2[1], LOW);
}

void usarEscudo(){
  shield.write(140);
  delay(200);
  shield.write(48);  
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

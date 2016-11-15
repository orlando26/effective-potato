#include <Servo.h>

int LDR1 = 4;                     // Pin LDR Arriba (S11)
int LDR2 = 3;                     // Pin LDR Abajo (S12)
Servo Servo1;                     // create servo object to control a servo Servo1

int LDR3 = 5;                     // Pin LDR Izquierda (S3)
int LDR4 = 2;                     // Pin LDR Derecha (S4)
Servo Servo2;                     // create servo object to control a servo Servo2

#define inputYd1 1     // entrada analógica para potenciómetro 0 (Servo1)
#define inputYd2 0     // entrada analógica para potenciómetro 0 (Servo1)
#define learnButton 3 // digital input for pushButton #1 - learn pattern

//Inputs and Outputs
float X1[2] = {0.0, 0.0}; //system inputs
byte XN1[2] = {0, 0};
float yd1 = 0.0; //desired output
byte ydN1 = 0;
float yr1 = 0.0; //real output (NN response)
byte yrN1 = 0;

float X2[2] = {0.0, 0.0}; //system inputs
byte XN2[2] = {0, 0};
float yd2 = 0.0; //desired output
byte ydN2 = 0;
float yr2 = 0.0; //real output (NN response)
byte yrN2 = 0;


//Pointers in operations with arrays
int i = 0, j = 0;

//Constants and other variables for the system
int CN1;
boolean trainMode;
boolean MaxSens1;
int posMaxSens1;

int CN2;
boolean MaxSens2;
int posMaxSens2;

const int NTI = 2;
const int NTN = 32;
const float MS = 0.6;
const float lambda = 0.1;
const float FU = 0.001;

//Matrix and vectors
float W1[NTN][NTI];
float SN1[NTN];
float AC1[NTN];
float U1[NTN];

float W2[NTN][NTI];
float SN2[NTN];
float AC2[NTN];
float U2[NTN];


//Setup function
void setup() {
  Servo1.attach(9);                 //Pin para servo 1 (para LDR1 y LDR2)
  Servo2.attach(10);                //Pin para servo 2 (para LDR3 y LDR4)
  pinMode(learnButton, INPUT);

  Serial.begin(9600);

  attachInterrupt(digitalPinToInterrupt(3), changeNNMode, RISING);
  interrupts(); //enable all interrupts
  //System startup
  nnStartUp1();
  nnStartUp2();
}

//StartUp function-----------------------------------------------------------------------
void nnStartUp1() {
  CN1 = 1;
  MaxSens1 = false;
  posMaxSens1 = -1;
  trainMode = false;
  //setMode(trainMode);
  for (i = 0; i < NTN; i++) {
    AC1[i] = 0.0;
    SN1[i] = 0.0;
    U1[i] = 1.0;
    for (j = 0; j < NTI; j++) {
      W1[i][j] = 0.0;
    }
  }
}

//StartUp function
void nnStartUp2() {
  CN2 = 1;
  MaxSens2 = false;
  posMaxSens2 = -1;
  trainMode = false;
  //setMode(trainMode);
  for (i = 0; i < NTN; i++) {
    AC2[i] = 0.0;
    SN2[i] = 0.0;
    U2[i] = 1.0;
    for (j = 0; j < NTI; j++) {
      W2[i][j] = 0.0;
    }
  }
}

//Main function----------------------------------------------------------------------------
void loop() {
  readData();
  if (trainMode == true) {
    learnPattern();
  }
  nnRun1();
  nnRun2();
  printData();
  delay(10);
}

//Activation function-----------------------------------------------------------------------------------
float activationFunction(float x, float l, float cm) {
  float y;
  if (l > 0) {
    y = exp( ( -1 * pow((x - cm), 2 ) ) / l);
  } else {
    y = 1;
  }
  y = constrain(y, 0, 1);
  return y;
}

//Update the U vector (the usage of each neuron is decreased through time)-------------------------------
void updateU() {
  for (i = 0; i < NTN; i++) {
    U1[i] = U1[i] - FU;
    U2[i] = U2[i] - FU;
  }
}

//Function to read the inputs and desired output, also converts each value into the range 0~1-----------------------------
void readData() {
  XN1[0] = analogRead(LDR1) >> 2;
  X1[0] = XN1[0] / 255.0;

  XN1[1] = analogRead(LDR2) >> 2;
  X1[1] = XN1[1] / 255.0;

  XN2[0] = analogRead(LDR3) >> 2;
  X2[0] = XN2[0] / 255.0;

  XN2[1] = analogRead(LDR4) >> 2;
  X2[1] = XN2[1] / 255.0;

  if (trainMode == true) {
    ydN1 = analogRead(inputYd1) >> 2;
    yd1 = ydN1 / 255.0;

    ydN2 = analogRead(inputYd2) >> 2;
    yd2 = ydN2 / 255.0;
  }
}

//Function to print the resulting data: inputs and outputs (through serial port and analog output)
void printData() {
  Serial.print("Entrenamiento: ");
  if (trainMode == true) {
    Serial.print("ON ");
  } else {
    Serial.print("OFF");
  }
  Serial.print(" S1: ");
  Serial.print(X1[0]);
  Serial.print("  S2: ");
  Serial.print(X1[1]);
  Serial.print("  yd1: ");
  Serial.print((ydN1 / 255.0) * 180.0);
  Serial.print("  yr1: ");
  Serial.print((yrN1 / 255.0) * 180.0);

  Serial.print("   S3: ");
  Serial.print(X2[0]);
  Serial.print("  S4: ");
  Serial.print(X2[1]);
  Serial.print("  yd2: ");
  Serial.print((ydN2 / 255.0) * 180.0);
  Serial.print("  yr2: ");
  Serial.println((yrN2 / 255.0) * 180.0);

  Servo1.write((yrN1 / 255.0) * 180.0);
  Servo2.write((yrN2 / 255.0) * 180.0);
}

void learnPattern() {
  nnRun1();
  nnRun2();
  updateU();
  nnLearn1();
  nnLearn2();
  updateU();
}

//Interruption for modeButton - change the mode when pressed
void changeNNMode() {
  noInterrupts();
  trainMode = !trainMode;
  interrupts();
}

//Function that computes the response of the neural network according to the inputs at the moment of the called----------------
void nnRun1() {
  //for each currently used neuron computes its response
  float ss1;
  for (i = 0; i < CN1; i++) {
    ss1 = 0;
    for (j = 0; j < NTI; j++) {
      ss1 = ss1 + pow(X1[j] - W1[i][j], 2);
    }
    SN1[i] = activationFunction(sqrt(ss1), lambda, 0);
  }
  //check wich neuron has the highest response
  int pos11 = 0;
  float val11 = SN1[0];
  for (i = 1; i < NTN; i++) {
    if (SN1[i] >= val11) {
      val11 = SN1[i];
      pos11 = i;
    }
  }

  //if that value reaches the MS then computes the output using only the neuron response multiplied by its AC  factor...
  if (val11 >= MS) {
    yr1 = AC1[pos11] * SN1[pos11];
    float temp = yr1 * 255;
    yrN1 = constrain(temp, 0, 255);
    U1[pos11] = 1; //... and reset to 1 the usage of this neuron
    MaxSens1 = true;
    posMaxSens1 = pos11;
  } else {
    //if that value does not reach the MS then approximate the output using a ponderation of all neurons    response
    float S11 = 0;
    float S12 = 0;
    for (i = 0; i < CN1; i++) {
      S11 = SN1[i] * AC1[i] + S11;
      S12 = SN1[i] + S12;
    }
    yr1 = S11 / S12;
    float temp = yr1 * 255;
    yrN1 = constrain(temp, 0, 255);
    MaxSens1 = false;
    posMaxSens1 = -1;
  }
}

void nnRun2() {
  //for each currently used neuron computes its response
  float ss2;
  for (i = 0; i < CN2; i++) {
    ss2 = 0;
    for (j = 0; j < NTI; j++) {
      ss2 = ss2 + pow(X2[j] - W2[i][j], 2);
    }
    SN2[i] = activationFunction(sqrt(ss2), lambda, 0);
  }

  //check wich neuron has the highest response
  int pos21 = 0;
  float val21 = SN2[0];
  for (i = 1; i < NTN; i++) {
    if (SN2[i] >= val21) {
      val21 = SN2[i];
      pos21 = i;
    }
  }

  //if that value reaches the MS then computes the output using only the neuron response multiplied by its AC  factor...
  if (val21 >= MS) {
    yr2 = AC2[pos21] * SN2[pos21];
    float temp = yr2 * 255;
    yrN2 = constrain(temp, 0, 255);
    U2[pos21] = 1; //... and reset to 1 the usage of this neuron
    MaxSens2 = true;
    posMaxSens2 = pos21;
  } else {
    //if that value does not reach the MS then approximate the output using a ponderation of all neurons    response
    float S21 = 0;
    float S22 = 0;
    for (i = 0; i < CN2; i++) {
      S21 = SN2[i] * AC2[i] + S21;
      S22 = SN2[i] + S22;
    }
    yr2 = S21 / S22;
    float temp = yr2 * 255;
    yrN2 = constrain(temp, 0, 255);
    MaxSens2 = false;
    posMaxSens2 = -1;
  }
}

//Function that adjust the W and AC when the learnButton is pressed------------------------------------------------------------
void nnLearn1() {
  if (MaxSens1 == true) {//maximum sensibility
    for (i = 0; i < NTI; i++)
    {
      W1[posMaxSens1][i] = (W1[posMaxSens1][i] + X1[i]) / 2;
    }
    AC1[posMaxSens1] = (AC1[posMaxSens1] + yd1) / 2;
  } else {
    float val12 = SN1[0];
    int pos12 = 0;
    //didn't occur maximum sensibility, so we must check if there'ss1 any avaliable neuron or it'ss1 going to be    necesary reuse one.
    if (CN1 + 1 > NTN) {
      for (i = 1; i < NTN; i++) { //reuse, because there'ss1 no one available
        if (SN1[i] <= val12) {
          val12 = SN1[i];
          pos12 = i;
        }
      }
    } else {
      CN1 = CN1 + 1; //create one, because there'ss1 one or more available
      pos12 = CN1;
    }
    for (i = 0; i < NTI; i++) {
      W1[pos12][i] = X1[i];
    }
    AC1[pos12] = yd1;
    U1[pos12] = 1;
  }
}

void nnLearn2() {
  if (MaxSens2 == true) {//maximum sensibility
    for (i = 0; i < NTI; i++)
    {
      W2[posMaxSens2][i] = (W2[posMaxSens2][i] + X2[i]) / 2;
    }
    AC2[posMaxSens2] = (AC2[posMaxSens2] + yd2) / 2;
  } else {
    float val22 = SN2[0];
    int pos22 = 0;
    //didn't occur maximum sensibility, so we must check if there'ss2 any avaliable neuron or it'ss2 going to be    necesary reuse one.
    if (CN2 + 1 > NTN) {
      for (i = 1; i < NTN; i++) { //reuse, because there'ss2 no one available
        if (SN2[i] <= val22) {
          val22 = SN2[i];
          pos22 = i;
        }
      }
    } else {
      CN2 = CN2 + 1; //create one, because there'ss2 one or more available
      pos22 = CN2;
    }
    for (i = 0; i < NTI; i++) {
      W2[pos22][i] = X2[i];
    }
    AC2[pos22] = yd2;
    U2[pos22] = 1;
  }
}

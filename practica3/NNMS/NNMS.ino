/*************************************************************
* NEURO-FUZZY SYSTEMS, Middle course project
* NNMS - Neural Network, Maximum Sensibility version 1.1.0
* MC Mecatrónica, FIME, UANL, MX
* Luis Lauro González Estrada
* March 2012
* All rights reserved
*************************************************************/
//Definitions of board pins
//Convention note: for analog inputs use pins: 0,1,... until last inpux X, then the desired output yd
//(example: for two inputs use pin 0 for x1, pin 1 for x2, and then pin 2 for the desired output yd)
#define inputYd 2
// analog input for potentiometer #3 - yd
#define outputYr 6
// analog output for real response of neural network
#define learnButton 3
// digital input for pushButton #1 - learn pattern
#define modeButton 2
// digital input for pushButton #2 - mode
#define ledLearning 9
// digital output for red led - learning pattern process
#define ledTraining 10
// digital output for yellow led - training mode
#define ledRunning 11
// digital output for green led - run mode
#define ledBuiltIn 13
// built-in led for indications of sytem mode (on = autonomous training, off = pc
training)
//Definitions for tags in serial communications
#define START 83
//ascii of 'S' will be used as START DATA tag
#define END 69
//ascii of 'E' will be used as END DATA tag
#define SEPARATOR 30
//ascii of RS (register separator) will be used as DATA BYTES SEPARATOR
#define RFPT 84
//ascii of 'T' will be used as ID TAG in the data reception from a computer
//Definitions of interrupts
#define modeRequest 0
#define learnRequest 1
//Global variables
//System operation
boolean pcMode;
char inputBuffer[9] = {' ',' ',' ',' ',' ',' ',' ',' ',' '};
byte inputData[5] = {0,0,0,0,0};
//Inputs and Outputs
float X[2] = {0.0,0.0}; //system inputs
byte XN[2] = {0,0};
float yd = 0.0; //desired output
byte ydN = 0;
float yr = 0.0; //real output (NN response)
byte yrN = 0;
//Pointers in operations with arrays
int i = 0, j = 0;
//Constants and other variables for the system
int CN;
boolean trainMode;
boolean MaxSens;
int posMaxSens;
const int NTI = 2;
const int NTN = 32;
const float MS = 0.6;
const float lambda = 0.1;
const float FU = 0.001;
//Matrix and vectors
float W[NTN][NTI];
float SN[NTN];
float AC[NTN];
float U[NTN];
//Setup function
void setup()
{
//Pin configurations for digital signals
pinMode(ledLearning,OUTPUT);
pinMode(ledTraining,OUTPUT);
pinMode(ledRunning,OUTPUT);
pinMode(outputYr,OUTPUT);
pinMode(learnButton,INPUT);
pinMode(modeButton,INPUT);
//Serial configurations
Serial.begin(9600);
//Interrupt configurations
attachInterrupt(learnRequest, learnPatternInterruptFunction, RISING);
attachInterrupt(modeRequest, changeNNModeInterruptFunction, RISING);
interrupts(); //enable all interrupts
//System startup
nnStartUp();
}
//StartUp function
void nnStartUp()
{
CN = 1;
MaxSens = false;
posMaxSens = -1;
trainMode = false;
setMode(trainMode);
for(i = 0; i < NTN; i++)
{
AC[i] = 0.0;
SN[i] = 0.0;
U[i] = 1.0;
for(j = 0; j < NTI; j++)
{
W[i][j] = 0.0;
}
}
}
//Main function
void loop()
{
if(!pcMode){
readData();
}
nnRun();
printData();
delay(10);
}
//Function to execute what's needed to change the mode (between Training and Running)
void setMode(boolean mode)
{
if(mode==true) //1=training, 0=running
{
digitalWrite(ledTraining,HIGH);
digitalWrite(ledRunning,LOW);
digitalWrite(ledLearning,LOW);
}
else
{
digitalWrite(ledTraining,LOW);
digitalWrite(ledRunning,HIGH);
digitalWrite(ledLearning,LOW);
}
}
//Function to execute what's needed to change the mode (between PcMode and ArduinoMode)
void setPcMode(boolean mode)
{
if(mode==true) //1=PC, 0=Arduino
{
digitalWrite(ledBuiltIn,HIGH);
pcMode = true;
}
else
{
digitalWrite(ledBuiltIn,LOW);
pcMode = false;
}
}
//Activation function
float activationFunction(float x, float l, float cm)
{
float y;
if(l > 0)
{
y = exp( ( -1 * pow((x-cm),2 ) ) / l);
}
else
{
y = 1;
}
y = constrain(y,0,1);
return y;
}
//Update the U vector (the usage of each neuron is decreased through time)
void updateU()
{
for(i = 0; i < NTN; i++)
{
U[i] = U[i] - FU;
}
}
//Blink led
void blinkLed(int led, int tHigh, int tLow, int cycles)
{
for(int cy = 1; cy <= cycles; cy++)
{
digitalWrite(led,HIGH);
delay(tHigh);
digitalWrite(led,LOW);
delay(tLow);
}
}
//Function to read the inputs and desired output, also converts each value into the range 0~1
void readData()
{
for(i = 0; i < NTI; i++)
{
XN[i] = analogRead(i) >> 2;
X[i] = XN[i]/255.0;
}
ydN = analogRead(inputYd) >> 2;
yd = ydN/255.0;
}
//Function to print the resulting data: inputs and outputs (through serial port and analog output)
void printData()
{
Serial.write(START);
Serial.write(SEPARATOR);
for(i = 0; i < NTI; i++)
{
Serial.write(XN[i]);
}
Serial.write(ydN);
Serial.write(yrN);
Serial.write(SEPARATOR);
Serial.write(END);
analogWrite(outputYr,yrN);
}
//Function that is used when the system is working with a computer (pcMode=true)
void serialEvent()
{
if(Serial.available()==9){
for(int i=8; i>=0; i--){
inputBuffer[i] = Serial.read();
}
if(pcMode==true
&
inputBuffer[0]==RFPT
&
inputBuffer[1]==RFPT
inputBuffer[8]==RFPT){
XN[0] = byte(inputBuffer[2]); //x1
XN[1] = byte(inputBuffer[3]); //x2
ydN = byte(inputBuffer[4]); //yd
X[0] = XN[0]/255.0;
X[1] = XN[1]/255.0;
yd = ydN/255.0;
inputData[3] = byte(inputBuffer[5]); //modeButton
inputData[4] = byte(inputBuffer[6]); //learnButton
if(inputData[3]==1){changeNNMode();}
if(inputData[4]==1){learnPattern();}
}
}
}
&
inputBuffer[7]==RFPT
//Interruption for learnButton - learn a pattern when pressed if the system is at Learning Mode
void learnPatternInterruptFunction()
{
if(!pcMode)
{
learnPattern();
}
else
{
setPcMode(false);
}
}
void learnPattern()
{
noInterrupts();
if(trainMode==true)
{
blinkLed(ledRunning, 25, 25, 2);
nnRun();
updateU();
blinkLed(ledRunning, 25, 25, 2);
blinkLed(ledLearning, 25, 25, 2);
nnLearn();
updateU();
blinkLed(ledLearning, 25, 25, 2);
}
else
{
setPcMode(true);
}
interrupts();
}
//Interruption for modeButton - change the mode when pressed
void changeNNModeInterruptFunction()
{
if(!pcMode)
{
changeNNMode();
}
else
{
setPcMode(false);
}
}
void changeNNMode()
{
noInterrupts();
trainMode = !trainMode;
setMode(trainMode);
interrupts();
}
//Function that computes the response of the neural network according to the inputs at the moment of the called
void nnRun()
{
//for each currently used neuron computes its response
float s;
for(i = 0; i < CN; i++)
{
s = 0;
for(j = 0; j < NTI; j++)
{
s = s + pow(X[j]-W[i][j],2);
}
SN[i] = activationFunction(sqrt(s),lambda,0);
}
//check wich neuron has the highest response
int pos = 0;
float val = SN[0];
for (i=1; i < NTN; i++)
{
if(SN[i] >= val)
{
val = SN[i];
pos = i;
}
}
//if that value reaches the MS then computes the output using only the neuron response multiplied by its AC
factor...
if(val >= MS)
{
yr = AC[pos]*SN[pos];
float temp = yr*255;
yrN = constrain(temp,0,255);
U[pos] = 1; //... and reset to 1 the usage of this neuron
MaxSens = true;
posMaxSens = pos;
}
else
{
//if that value does not reach the MS then approximate the output using a ponderation of all neurons
response
float S1 = 0;
float S2 = 0;
for(i = 0; i < CN; i++)
{
S1 = SN[i]*AC[i] + S1;
S2 = SN[i] + S2;
}
yr = S1 / S2;
float temp = yr*255;
yrN = constrain(temp,0,255);
MaxSens = false;
posMaxSens = -1;
}
}
//Function that adjust the W and AC when the learnButton is pressed
void nnLearn()
{
if(MaxSens == true) //maximum sensibility
{
for(i = 0; i < NTI; i++)
{
W[posMaxSens][i] = (W[posMaxSens][i] + X[i]) / 2;
}
AC[posMaxSens] = (AC[posMaxSens] + yd) / 2;
}
else
{
float val2 = SN[0];
int pos2 = 0;
//didn't occur maximum sensibility, so we must check if there's any avaliable neuron or it's going to be
necesary reuse one.
if(CN+1 > NTN)
{
for (i=1; i < NTN; i++) //reuse, because there's no one available
{
if(SN[i] <= val2)
{
val2 = SN[i];
pos2 = i;
}
}
}
else
{
CN = CN+1; //create one, because there's one or more available
pos2 = CN;
}
for(i = 0; i < NTI; i++)
{
W[pos2][i] = X[i];
}
AC[pos2] = yd;
U[pos2] = 1;
}
}
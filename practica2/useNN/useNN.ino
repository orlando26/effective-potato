float Wm[][5] = {   { 1.2763891,   1.3303445,  -2.5162961, 0.6200120,  -0.6779750},    
                    {- 1.2294553,  -1.1542496, 2.1271114,  -1.3273218, 0.8041494},  
                    {- 0.0899717 , 0.0872570,  -0.0109081, 0.3255030,  0.1057778}   };
                    
float Wo[5] = {1.742186, 1.7611341,  -3.8198274,  1.4109908,  -1.1521749};
int sensor1 = A0;
int sensor2 = A1;
float s1 = 0;
float s2 = 0;
float servo1 = 0;
int bias = 1;
int n1, n2, n3, n4, n5;
void setup(){
    Serial.begin(9600);
}

void loop(){
    s1 = 3.3884629783;//analogRead(sensor1);
    Serial.print(s1);
    Serial.print("\t");
    s1 = normalization(s1, 0.0573372, 4.4067539);
    Serial.print(s1);
    Serial.print("\t");
    s2 = 4.0788692019;//analogRead(sensor2);
    Serial.print(s2);
    Serial.print("\t");
    s2 = normalization(s2, 0.2836070, 4.4583616);//s2/1023;
    Serial.print(s2);
    Serial.print("\t");
    
    n1 = Wm[0][0]*s1 + Wm[1][0]*s2 + Wm[2][0];
    n1 = sigmoid(n1);
    
    n2 = Wm[0][1]*s1 + Wm[1][1]*s2 + Wm[2][1];
    n2 = sigmoid(n2);
    
    n3 = Wm[0][2]*s1 + Wm[1][2]*s2 + Wm[2][2];
    n3 = sigmoid(n3);
    
    n4 = Wm[0][3]*s1 + Wm[1][3]*s2 + Wm[2][3];
    n4 = sigmoid(n4);
    
    n5 = Wm[0][4]*s1 + Wm[1][4]*s2 + Wm[2][4];
    n5 = sigmoid(n5);
    
    servo1 = Wo[0]*n1 + Wo[1]*n2 + Wo[2]*n3 + Wo[3]*n4 + Wo[4]*n5;
    servo1 = sigmoid(servo1);
    Serial.print(servo1);
    Serial.print("\t");
    
    servo1 = desnormalization(servo1, -31.136366, 25.059602);//servo1*180;
    Serial.println(servo1);
    delay(10);
}

float sigmoid(float x){
    return 1/(1 + exp(-x));
}

float normalization(float Van, float LMAX, float LMIN){
  float a=(Van-LMIN)/((LMAX-LMIN)+0.000001);
  if(a > 1)a = 1;
  if(a < 0)a = 0;
  return a;
}

float desnormalization(float Vn, float LMIN, float LMAX){
  float a=Vn*LMAX+LMIN*(1-Vn);
  if(a > LMAX)a = LMAX;
  if(a < LMIN)a = LMIN;
  return a;
}


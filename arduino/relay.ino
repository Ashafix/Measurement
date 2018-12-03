#include <SoftwareSerial.h>

SoftwareSerial BTserial(10, 11); // RX | TX

//4 channel relay
#define relay1 4
#define relay2 5
#define relay3 6
#define relay4 7
#define relay true
//end of relay

//HC-SR04 distance measurement
#define echo 2
#define trigger 3
#define sensorDistance true

//TDS/salinity measurement
#define sensorTdsPin A1
#define sensorTds true

//length of the buffer storing commands
#define cmdLength 12

//every how many milliseconds the measurements are updated
#define updatePeriod 1000


boolean relayStatus[] = {false, false, false, false};
unsigned long time;

//CircularBuffer<char,cmdLength> cmdBuffer; 

void setRelays()
{
  int index = 0;
  for (int index = 0; index < sizeof(relayStatus); index ++)
  {
    setRelay(index);
  }
}

void setRelay(int index)
{
  if (relayStatus[index]) {
    digitalWrite(relay1 + index, HIGH);
  } else {
    digitalWrite(relay1 + index, LOW);
  }
}

void executeInput()
{
  char cmd[cmdLength];
  bool bt = false;
  char character;
    
  int index = 0;
  bool finished = false;
  while (index < cmdLength) {
    if ((index == 0 || bt == true) && BTserial.available()) {
      character = BTserial.read();
      bt = true;
    }
    else if (!bt && Serial.available())
    {
      character = Serial.read();
      bt == false;
    }
    else
    {
      finished = true;
    }
    if (finished)
    {
      character = 0;
    }
    else if (character == -1)
    {
      character = 0;
      finished = true;
    }
    cmd[index] = character;
    //cmdBuffer.push(character);
    index++;
  }
  String cmdStr = String(cmd);
  /*if (cmdBuffer.size() > 0)
  {
    //Serial.println(cmdBuffer);
  }
  for (int i = 0; i < cmdBuffer.size(); i++)
  {
    Serial.print(cmdBuffer[i]);
  }
  //Serial.println(cmdBuffer.size());
  */
  if (relay && cmdStr.substring(0, 8) == "button: ")
  {
    int index = cmdStr.substring(8, 9).toInt();
    if (index >= 0 && index < 4)
    {
      relayStatus[index] = !relayStatus[index];
      setRelay(index);  
    }  
  }
}

long measureDistance()
{
  digitalWrite(trigger, LOW);
  delay(5);
  noInterrupts();
  digitalWrite(trigger, HIGH);
  delay(10);
  digitalWrite(trigger, LOW);
  long t = pulseIn(echo, HIGH) / 2; // Echo-Zeit messen
  interrupts();
  long dist = t * 0.33356;
  if (dist > 0 && dist < 200)
  {
    return dist;
  }
  return 0;

  
}

String getMeasurements()
{
  String output = "\"millis\": " + String(millis());
  if (sensorDistance)
  {
    output += ",\n\"distance\": " + String(measureDistance());
  }
  if (sensorTds)
  {
    output += ",\n\"tds\": " + String(measureTds());
  }
  //Serial.println(output);
  return output;
    
}

int measureTds()
{
  return analogRead(sensorTdsPin);
}

void setup() {
  Serial.begin(9600);

  // 4 channel relay
  pinMode(relay1, OUTPUT);
  pinMode(relay2, OUTPUT);
  pinMode(relay3, OUTPUT);
  pinMode(relay4, OUTPUT);
  //end of relay

  pinMode(trigger, OUTPUT);
  pinMode(echo, INPUT);
  digitalWrite(trigger, HIGH);
  
  pinMode(13, OUTPUT);
  BTserial.begin(9600);
  setRelays();

  time = millis();
}

void loop() {
  executeInput();
  delay(1000);
  if (millis() - time > updatePeriod)
  {
    time = millis();
    BTserial.print("|^~\\{");
    BTserial.print(getMeasurements());
    BTserial.print("}|^~\\");    
  }
}

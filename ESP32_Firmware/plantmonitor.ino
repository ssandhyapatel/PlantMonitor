#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h>
#include <DHT.h>

// Sensor Definitions
#define DHTPIN 15          // GPIO15 connected to DHT22
#define DHTTYPE DHT22
#define VOC_PIN 34         // Analog pin for MP503 VOC sensor

// BLE Definitions
#define SERVICE_UUID        "0000ffe0-0000-1000-8000-00805f9b34fb"
#define CHARACTERISTIC_UUID "0000ffe1-0000-1000-8000-00805f9b34fb"

BLECharacteristic *pCharacteristic;
DHT dht(DHTPIN, DHTTYPE);

void setup() {
  Serial.begin(115200);
  dht.begin();

  // BLE Setup
  BLEDevice::init("PlantSensor_01"); 
  BLEServer *pServer = BLEDevice::createServer();
  BLEService *pService = pServer->createService(SERVICE_UUID);

  pCharacteristic = pService->createCharacteristic(
    CHARACTERISTIC_UUID,
    BLECharacteristic::PROPERTY_NOTIFY
  );

  pCharacteristic->addDescriptor(new BLE2902());
  pService->start();

  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);  
  pAdvertising->setMinPreferred(0x12);  
  BLEDevice::startAdvertising();

  Serial.println("BLE Sensor is advertising as PlantSensor_01...");
}

void loop() {
  // Read sensor data
  float temp = dht.readTemperature();
  float hum = dht.readHumidity();
  int vocRaw = analogRead(VOC_PIN);
  float vocVolt = vocRaw * (3.3 / 4095.0);

  // Debug Print
  Serial.println("----- Sensor Readings -----");
  Serial.print("Temp (°C): ");
  Serial.println(temp);
  Serial.print("Humidity (%): ");
  Serial.println(hum);
  Serial.print("VOC Value (raw): ");
  Serial.println(vocRaw);
  Serial.print("VOC Voltage (V): ");
  Serial.println(vocVolt);
  Serial.println("---------------------------");

  // Send to BLE
  if (pCharacteristic != nullptr && temp && hum) {
    String data = String(vocRaw) + "," + String(temp, 2) + "," + String(hum, 2);
    pCharacteristic->setValue(data.c_str());
    pCharacteristic->notify();  // Push to app
  }

  delay(3000);  // Send every 3 seconds
}



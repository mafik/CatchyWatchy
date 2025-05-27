#include "BLE.h"

#include <Arduino.h>
#include <BLE2902.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>

#define SERVICE_UUID "AAAF1338-D61A-452B-81ED-CA7C443A7C44"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

struct BLE::Impl : public BLEServerCallbacks {
  BLEServer *pServer = NULL;
  BLECharacteristic *pCharacteristic = NULL;
  BLEAdvertising *pAdvertising = NULL;
  bool pNotified = false;

  Impl() {
    BLEDevice::init("Catchy Watchy");

    pServer = BLEDevice::createServer();
    pServer->setCallbacks(this);

    // Create the BLE Service
    BLEService *pService = pServer->createService(SERVICE_UUID);

    // Create characteristic and add descriptor
    pCharacteristic = pService->createCharacteristic(
        CHARACTERISTIC_UUID, BLECharacteristic::PROPERTY_NOTIFY);
    pCharacteristic->addDescriptor(new BLE2902());

    // Start the service
    pService->start();

    // Start advertising
    pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->setMinPreferred(
        0x0); // set value to 0x00 to not advertise this parameter
    BLEDevice::startAdvertising();
    printf("Advertising\n");
  }

  ~Impl() { BLEDevice::deinit(); }

  void onConnect(BLEServer *pServer) {
    printf("Connected\n");
    //      esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_CONN_HDL0, ESP_PWR_LVL_N12);
    //      esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_CONN_HDL1, ESP_PWR_LVL_N12);
    //      esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_CONN_HDL2, ESP_PWR_LVL_N12);
    //      esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_CONN_HDL3, ESP_PWR_LVL_N12);
    //      esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_CONN_HDL4, ESP_PWR_LVL_N12);
    //      esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_CONN_HDL5, ESP_PWR_LVL_N12);
    //      esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_CONN_HDL6, ESP_PWR_LVL_N12);
    //      esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_CONN_HDL7, ESP_PWR_LVL_N12);
    //      esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_CONN_HDL8, ESP_PWR_LVL_N12);
    //      esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_ADV, ESP_PWR_LVL_N12);
    //      esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_SCAN, ESP_PWR_LVL_N12);
    //      esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_DEFAULT, ESP_PWR_LVL_N12);
    pAdvertising->stop();

    delay(500); // Wait for stable connection

    // Send 1 byte of data with value
    uint8_t value = 1;
    pCharacteristic->setValue((uint8_t *)&value, 1);
    pCharacteristic->notify();
    printf("Notification sent\n");
    pNotified = true;
  };

  void onDisconnect(BLEServer *pServer) {
    printf("Disconnected\n");
    BLEDevice::startAdvertising();
  }
};

BLE::BLE(void) : impl(new Impl()) {}

BLE::~BLE(void) {}

bool BLE::notified() { return impl->pNotified; }

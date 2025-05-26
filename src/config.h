#pragma once

#include <Arduino.h>

constexpr bool kDebug = true;
const char *kWiFiSSID PROGMEM = "YOUR WIFI PASSWORD HERE";
const char *kWiFiPass PROGMEM = "YOUR WIFI SSID HERE";
const char *kTZ PROGMEM = "CET-1CEST,M3.5.0,M10.5.0/3";
constexpr uint8_t kWiFiUpdateInterval = 30;
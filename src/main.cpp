// Based on STARRY HORIZON by Dan Delany (https://github.com/dandelany/watchy-faces/)

#include <Arduino.h>
#include <HTTPClient.h>
#include <DS3232RTC.h>
#include <GxEPD2_BW.h>
#include <Wire.h>
#include <time.h>
#include <esp_pthread.h>
#include <thread>
#include "MadeSunflower39pt7b.h"
#include "Pokemon_Classic4pt7b.h"
#include "starfield_bitmap.h"
#include "bluetooth_bitmap.h"
#include "timer_bitmap.h"

//pins
#define SDA 21
#define SCL 22
#define ADC_PIN 33
#define RTC_PIN GPIO_NUM_27
#define CS 5
#define DC 10
#define RESET 9
#define BUSY 19
#define VIB_MOTOR_PIN 13
#define MENU_BTN_PIN 26
#define BACK_BTN_PIN 25
#define UP_BTN_PIN 32
#define DOWN_BTN_PIN 4
#define MENU_BTN_MASK GPIO_SEL_26
#define BACK_BTN_MASK GPIO_SEL_25
#define UP_BTN_MASK GPIO_SEL_32
#define DOWN_BTN_MASK GPIO_SEL_4
#define ACC_INT_MASK GPIO_SEL_14
#define BTN_PIN_MASK MENU_BTN_MASK|BACK_BTN_MASK|UP_BTN_MASK|DOWN_BTN_MASK
//display
#define DISPLAY_WIDTH 200
#define DISPLAY_HEIGHT 200

const char* kWeekdayNames[] PROGMEM = {
  "niedziela",
  "poniedzialek",
  "wtorek",
  "sroda",
  "czwartek",
  "piatek",
  "sobota"
};

const char* kMonthNames[] PROGMEM = {
  "stycznia",
  "lutego",
  "marca",
  "kwietnia",
  "maja",
  "czerwca",
  "lipca",
  "sierpnia",
  "wrzesnia",
  "pazdziernika",
  "listopada",
  "grudnia"
};

constexpr bool kDebug = true;
const char* kWiFiSSID PROGMEM = "YOUR WIFI PASSWORD HERE";
const char* kWiFiPass PROGMEM = "YOUR WIFI SSID HERE";
const char* kTZ PROGMEM = "CET-1CEST,M3.5.0,M10.5.0/3";
constexpr uint8_t kWiFiUpdateInterval = 30;
DS3232RTC RTC(false);
GxEPD2_BW<GxEPD2_154_D67, GxEPD2_154_D67::HEIGHT> display(GxEPD2_154_D67(CS, DC, RESET, BUSY));

RTC_DATA_ATTR float temperature;
RTC_DATA_ATTR float rainfall;
RTC_DATA_ATTR uint8_t wifiUpdateInterval = kWiFiUpdateInterval;


bool parseImgwCsv(const String& csv) {
  int num_scanned = sscanf(csv.c_str(), "%*s %*d,%*[^,],%*[^,],%*d,%f,%*f,%*d,%*f,%f,%*f",
                           &temperature, &rainfall);
  return num_scanned == 2;
}

bool connectWiFi(){
  WiFi.begin(kWiFiSSID, kWiFiPass);
  return WiFi.waitForConnectResult() == WL_CONNECTED;  // attempt to connect for 10s
}

void doWiFiUpdate() {
  if (connectWiFi()) {
    configTzTime(kTZ, "pool.ntp.org", "time.nist.gov");
    if (kDebug) printf("Waiting for NTP time sync: ");
    time_t nowSecs = time(nullptr);
    while (nowSecs < 8 * 3600 * 2) {
      delay(500);
      if (kDebug) printf(".");
      yield();
      nowSecs = time(nullptr);
    }
    if (kDebug) {
      auto* timeinfo = localtime(&nowSecs);
      printf("\nLocal time: %s\n", asctime(timeinfo));
    }
    RTC.set(nowSecs);

    HTTPClient http;
    http.setConnectTimeout(3000);  // 3 second max timeout
    http.begin("https://danepubliczne.imgw.pl/api/data/synop/id/12465/format/csv");
    int httpResponseCode = http.GET();
    if (httpResponseCode == 200) {
        String payload = http.getString();
        if (!parseImgwCsv(payload)) {
          //printf("Couldn't scan: %s\n", payload.c_str());
        }
    } else {
        //http error
        String error = http.errorToString(httpResponseCode);
        String payload = http.getString();
        //printf("IMGW connection error [%s]: %s\n", error.c_str(), payload.c_str());
    }
    http.end();
  }
  WiFi.mode(WIFI_OFF);
  btStop();
  wifiUpdateInterval = 0;
}

float getRtcCelsius() {
  return RTC.temperature() / 4;
}

void maybeWiFiUpdate() {
  if (++wifiUpdateInterval >= kWiFiUpdateInterval) {
    doWiFiUpdate();
    wifiUpdateInterval = 0;
  }
}

void drawCenteredString(const String &str, int x, int y, bool drawBg) {
  int16_t x1, y1;
  uint16_t w, h;
  display.getTextBounds(str, x, y, &x1, &y1, &w, &h);
  display.setCursor(x - w / 2, y);
  if(drawBg) {
    int padY = 3;
    int padX = 10;
    display.fillRect(x - (w / 2 + padX), y - (h + padY), w + padX*2, h + padY*2, GxEPD_BLACK);
  }
  // display.drawRect(x - w / 2, y - h + 1, w, h, GxEPD_WHITE);
  display.print(str);
}

uint8_t getBatteryPercent(){
  //https://github.com/G6EJD/LiPo_Battery_Capacity_Estimator
  float voltage = analogRead(ADC_PIN) / 4096.0 * 7.23;
  if (voltage > 4.19) {
    return 100;
  } else if (voltage <= 3.50) {
    return 0;
  }
  return 2808.3808 * pow(voltage, 4) - 43560.9157 * pow(voltage, 3) + 252848.5888 * pow(voltage, 2) - 650767.4615 * voltage + 626532.5703;
}

void drawWatchFace(){
  display.fillScreen(GxEPD_BLACK);
  display.drawBitmap(0, 0, kStarfieldBitmap, 200, 200, GxEPD_WHITE);

  display.setTextColor(GxEPD_WHITE);
  display.setTextWrap(false);
  char buf[100];
  time_t t = DS3232RTC::get();
  if (kDebug) printf("RTC time: %d:%d:%d\n", hour(t), minute(t), second(t));
  struct tm* timeinfo = localtime(&t);

  // DRAW TIME
  display.setFont(&MADE_Sunflower_PERSONAL_USE39pt7b);
  snprintf(buf, sizeof(buf), "%d:%02d", timeinfo->tm_hour, timeinfo->tm_min);
  drawCenteredString(buf, 100, 115, false);
  
  // DRAW DATE
  display.setFont(&Pokemon_Classic4pt7b);
  snprintf(buf, sizeof(buf), "%s %d %s", kWeekdayNames[timeinfo->tm_wday], timeinfo->tm_mday, kMonthNames[timeinfo->tm_mon]);
  drawCenteredString(buf, 100, 138, true);

  snprintf(buf, sizeof(buf), "%.0f", temperature);
  display.setCursor(10, 8);
  display.print(buf);
  display.drawBitmap(display.getCursorX()-1, display.getCursorY()-6, kCelsiusBitmap, 11, 7, GxEPD_WHITE);

  snprintf(buf, sizeof(buf), "%.1fmm", rainfall);
  display.setCursor(10, 20);
  display.print(buf);

  snprintf(buf, sizeof(buf), "%d%%", (int)getBatteryPercent());
  display.setCursor(10, 32);
  display.print(buf);
}

void deepSleep() {
  esp_sleep_enable_ext0_wakeup(RTC_PIN, 0); //enable deep sleep wake on RTC interrupt
  esp_sleep_enable_ext1_wakeup(BTN_PIN_MASK, ESP_EXT1_WAKEUP_ANY_HIGH); //enable deep sleep wake on button press
  esp_deep_sleep_start();
}

void rtcConfig(){
  //https://github.com/JChristensen/DS3232RTC
  RTC.squareWave(SQWAVE_NONE); //disable square wave output
  RTC.setAlarm(ALM2_EVERY_MINUTE, 0, 0, 0, 0); //alarm wakes up Watchy every minute
  RTC.alarmInterrupt(ALARM_2, true); //enable alarm interrupt
}

float getBatteryVoltage(){
    return analogRead(ADC_PIN) / 4096.0 * 7.23;
}

void showWatchFace(bool partialRefresh){
  display.init(0, false); //_initial_refresh to false to prevent full update on init
  drawWatchFace();
  display.display(partialRefresh); //partial refresh
  display.hibernate();
}


void vibMotor(){
  if (kDebug) printf("VibMotor (core %d)\n", xPortGetCoreID());
  pinMode(VIB_MOTOR_PIN, OUTPUT);
  digitalWrite(VIB_MOTOR_PIN, true);
  delay(50);
  digitalWrite(VIB_MOTOR_PIN, false);
}

void bluetoothApp() {
  display.init(0, false);
  display.drawBitmap(0, 0, kBluetoothBitmap, 200, 200, GxEPD_WHITE);
  display.display(true);  // true means "partial refresh"
  display.hibernate();
  pinMode(MENU_BTN_PIN, INPUT);
  while (true) {
    if (digitalRead(MENU_BTN_PIN)) {
      break;
    }
    delay(10);
  }
  std::thread vib2(vibMotor);
  drawWatchFace();
  display.display(true);
  display.hibernate();
  vib2.join();
}

char* formatTime(int sec) {
  static char buf[60];
  if (sec < 60) {
    snprintf(buf, sizeof(buf), "%d", sec);
  } else {
    snprintf(buf, sizeof(buf), "%d:%02d", sec / 60, sec % 60);
  }
  return buf;
}

void timerApp() {
  long startMillis = millis();
  volatile long deadlineMillis = startMillis + 60 * 1000;
  volatile bool active = true;

  std::thread btns([&]() {
    pinMode(MENU_BTN_PIN, INPUT);
    pinMode(DOWN_BTN_PIN, INPUT);
    bool backDown = false;
    bool aDown = false;
    bool bDown = false;
    bool aLastDown = true;
    bool bLastDown = true;
    while (active) {
      backDown = digitalRead(MENU_BTN_PIN);
      aDown = digitalRead(DOWN_BTN_PIN);
      bDown = digitalRead(UP_BTN_PIN);
      if (backDown) {
        active = false;
        vibMotor();
        break;
      }
      if (aDown && !aLastDown) {
        deadlineMillis += 60 * 1000;
        vibMotor();
      }
      if (bDown && !bLastDown) {
        deadlineMillis += 5 * 60 * 1000;
        vibMotor();
      }
      aLastDown = aDown;
      bLastDown = bDown;
      delay(10);
    }
  });

  display.init(0, false);
  display.setFont(&MADE_Sunflower_PERSONAL_USE39pt7b);
  display.setTextWrap(false);

  int plane_x = -30;
  GFXcanvas1 canvas(200, 200);
  canvas.setFont(&MADE_Sunflower_PERSONAL_USE39pt7b);
  canvas.setTextWrap(false);

  int road = 0;

  while (active) {
    display.fillScreen(GxEPD_BLACK);
    display.drawBitmap(0, 0, kTimerBitmap, 200, 200, GxEPD_WHITE);
    if (++plane_x >= 200) {
      plane_x = -39;
    }
    display.drawBitmap(plane_x, 20, kPlaneBitmap, 40, 4, GxEPD_WHITE);
    long nowMillis = millis();
    long elapsedSec = (nowMillis - startMillis) / 1000;
    long totalSec = (deadlineMillis - startMillis) / 1000;
    long remainingSec = totalSec - elapsedSec;
    long nextUpdateMillis = startMillis + (elapsedSec + 1) * 1000;
    const char* timeStr = formatTime(remainingSec);
    
    // This section takes 180 ms
    int16_t x1, y1;
    uint16_t w, h;
    canvas.getTextBounds(timeStr, 0, 0, &x1, &y1, &w, &h);
    int x = 100 - w/2;
    int y = 120;
    canvas.setCursor(x, y);
    canvas.fillScreen(GxEPD_BLACK);
    canvas.setTextColor(GxEPD_WHITE);
    canvas.print(timeStr);
    for (int x = 0; x < 200; ++x) {
      for (int y = 0; y < 200; ++y) {
        if (canvas.getPixel(x, y)) {
          display.drawPixel(x, y, GxEPD_WHITE);
        } else if (canvas.getPixel(x, y+1) || canvas.getPixel(x+1, y) || canvas.getPixel(x-1, y)) {
          display.drawPixel(x, y, GxEPD_BLACK);
        } else if (canvas.getPixel(x, y-10) && !canvas.getPixel(x, y-9)) {
          display.drawPixel(x, y, GxEPD_BLACK);
        } else if (canvas.getPixel(x, y-9) || canvas.getPixel(x, y-8) || canvas.getPixel(x, y-7) || canvas.getPixel(x, y-6) || canvas.getPixel(x, y-5) || canvas.getPixel(x, y-4) || canvas.getPixel(x, y-3) || canvas.getPixel(x, y-2) || canvas.getPixel(x, y-1)) {
          display.drawPixel(x, y, (x % 2 == y % 2) ? GxEPD_BLACK : GxEPD_WHITE);
        }
      }
    }

    road = (road + 1) % 4;
    const unsigned char* roadPtr;
    switch (road) {
    case 0: roadPtr = kRoad1Bitmap; break;
    case 1: roadPtr = kRoad2Bitmap; break;
    case 2: roadPtr = kRoad3Bitmap; break;
    case 3: roadPtr = kRoad4Bitmap; break;
    }
    
    display.drawBitmap(91, 176, roadPtr, 17, 24, GxEPD_BLACK);

    display.display(true);

    if (nowMillis >= deadlineMillis) {
      pinMode(VIB_MOTOR_PIN, OUTPUT);
      for (int i = 0; i < 10; ++i) {
        digitalWrite(VIB_MOTOR_PIN, true);
        delay(100);
        digitalWrite(VIB_MOTOR_PIN, false);
        delay(100);
      }
      break;
    }

    nowMillis = millis();
    long remainingMillis = nextUpdateMillis - nowMillis;
    if (kDebug) printf("Remaining: %ld ms\n", remainingMillis);
    if (remainingMillis > 0) {
      delay(remainingMillis);
      nowMillis = millis();
    }
  }
  active = false;
  btns.join();
  drawWatchFace();
  display.display(true);
  display.hibernate();
  RTC.alarm(ALARM_2);
}

void handleButtonPress() {
  uint64_t wakeupBit = esp_sleep_get_ext1_wakeup_status();
  std::thread vib(vibMotor);
  if (wakeupBit & MENU_BTN_MASK) {
    bluetoothApp();
  } else if (wakeupBit & DOWN_BTN_MASK) {
    timerApp();
  }
  vib.join();
}

void setThreadCore(int core_id) {
  auto cfg = esp_pthread_get_default_config();
  cfg.pin_to_core = core_id;
  esp_pthread_set_cfg(&cfg);
}

void setup() {
  if (kDebug) Serial.begin(9600);

  esp_sleep_wakeup_cause_t wakeup_reason;
  wakeup_reason = esp_sleep_get_wakeup_cause(); //get wake up reason
  Wire.begin(SDA, SCL); //init i2c

  // Run extra threads on the other core
  setThreadCore(xPortGetCoreID() ? 0 : 1);

  setenv("TZ", kTZ, 1);
  tzset();

  switch (wakeup_reason) {
  case ESP_SLEEP_WAKEUP_EXT0: //RTC Alarm
    if (kDebug) printf("OnRTC (core %d)\n", xPortGetCoreID());
    RTC.alarm(ALARM_2); //resets the alarm flag in the RTC
    showWatchFace(true); //partial updates on tick
    maybeWiFiUpdate();
    break;
  case ESP_SLEEP_WAKEUP_EXT1: //button Press
    if (kDebug) printf("OnButtonPress (core %d)\n", xPortGetCoreID());
    handleButtonPress();
    break;
  default: //reset
    if (kDebug) printf("OnReset (core %d)\n", xPortGetCoreID());
    doWiFiUpdate();
    rtcConfig();
    showWatchFace(false); //full update on reset
    break;
  }

  deepSleep();
}

void loop() {
  // this should never run, Watchy deep sleeps after init();
}

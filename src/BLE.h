#pragma once

#include <memory>

class BLE {
public:
  BLE(void);
  ~BLE(void);

  bool notified();

private:
  class Impl;
  std::unique_ptr<Impl> impl;
};

# CatchyWatchy

CatchyWatchy is a ROM for [Watchy by SQFMI](https://watchy.sqfmi.com/).

![Main Screen](DSC00774.JPG "Main Screen")

![Timer](timer.gif "Timer")

![Bluetooth Settings](DSC00775.JPG "Bluetooth Settings")

## Status

CatchyWatchy was developed for personal use and some settings were hardcoded during its development. The UI language is set to Polish, the weather location is set to Łódź, timezone, WiFi SSID & password are provided as C constants. You will have to edit CatchyWatchy sources & rebuild it in order to personalize it. The recommended way to do this is to use clone this repo & open it with VSCode with PlatformIO extension installed.

The Bluetooth Settings Screen works in tandem with an Android companion app. When it's opened, it makes the companion app on the smartphone call a predefined Tasker task.

Working features:

* Current time & date
* Current battery charge percentage
* Current temperature & rainfall
* WiFi time & weather synchronization every 30 minutes
* Timer
* Bluetooth-triggered tasker task

The weather is fetched from [IMGW](https://www.imgw.pl/en) (Institute of Meteorology and Water Management in Poland) - if you live elsewhere you'll probably want to change it to a different weather provider.

Pull requests are welcome!

## Usage

Main Screen:

* Upper Left Button => Nothing
* Upper Right Button => Nothing
* Lower Left Button => Go to Bluetooth Settings Screen
* Lower Right Button => Go to Timer screen, start 1-minute countdown

Bluetooth Settings Screen:

* Upper Left Button => Nothing
* Upper Right Button => Nothing
* Lower Left Button => Cancel and go to Main Screen
* Lower Right Button => Nothing

Timer:

* Upper Left Button => Nothing
* Upper Right Button => Add 5 minutes to the countdown
* Lower Left Button => Go to Main Screen
* Lower Right Button => Add 1 minute to the countdown

## Art

The art used in this watch face is closely based on:

* Main Screen - StarryHorizon watch face on GitHub: https://github.com/dandelany/watchy-faces/
* Bluetooth Settings Screen - KadaburaDraws on Twitter: https://twitter.com/KadaburaDraws
* Timer - Hammerbeam Tea Corp. on Instagram: https://www.instagram.com/hammerbeam_tea_corp/
* Date Font - Pokemon Classic font by TheLouster115 on DaFont: https://www.dafont.com/pokemon-classic.font
* Time Font - MADE Sunflower font by MadeType on DaFont: https://www.dafont.com/made-sunflower.font

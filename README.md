# Data Points

An app that collects and records device sensor readings to CSV file on Android 
periodically in background.

For every day a new CSV file is recorded to:
```
/<internal_storage>/<csv_relative_path>/<date_iso_8601>_<device_name>.csv
```
* `<csv_relative_path>` is typically `DataPoints` folder - to change, see _Configure_ below
* `<device_name>` is the user-editable device name in Android settings

Example: `/sdcard/DataPoints/2023-12-25_MyPhone.csv`

### Data structure

| CSV field header  | Unit/Format              | Note                                                            |
|-------------------|--------------------------|-----------------------------------------------------------------|
| Date              | ISO 8601 date            | `yyyy-MM-dd` <br>Example: 2023-12-25                            |
| Time              | ISO 8601 time            | `HH:mm:ss` <br>Example: 17:03:57                                |
| Timezone          | ISO 8601 offset from UTC | `±HHmm` <br>Example: +0100                                      |
| ScreenState       | 0 or 1                   | 0: device screen is off <br>1: screen on                        |
| BatteryPercent    | Percentage               |                                                                 |
| UnlockState       | 0 or 1                   | 0: device is locked <br>1: unlocked                             |
| Latitude          | Degrees                  |                                                                 |
| Longitude         | Degrees                  |                                                                 |
| Pressure          | Hectopascals             |                                                                 |
| Bearing           | Degrees                  | Range: 0 until 360                                              |
| Pitch             | Degrees                  | 0: device flat <br>-90: pointing down <br>90: pointing up       |
| Tilt              | Degrees                  | 0: device flat <br>-90: facing left <br>90: facing right        |
| Acceleration      | m/s<sup>2</sup>          | Absolute value regardless of direction <br>0: device stationary |
| Temperature       | Degrees  °C              |                                                                 |
| Lux               | Lux                      |                                                                 |
| DataPointDuration | Milliseconds             | How long it took to collect all readings for current data point |

If a reading cannot be obtained, its recorded value will be 
its CSV field header followed by a suffix depending on the reason:
* `NoSensor`: app couldn't find the sensor to read the value from
* `Missing`: the value was not obtained due to denied permission
* `Timeout`: it took abnormally long to read the value: 25 seconds for location, 2 seconds for other readings

A line of sensor readings is added at every minute to the CSV file of the current day.
However, when device is in power saving mode, the app will respect that and skip data points.


## Build & install

Requirements:

* Java
* Android SDK

### Get the source code

```bash
git clone https://github.com/alex-vt/DataPoints
cd DataPoints
```

### Configure

The relative path of folder with CSV files in device internal storage is `DataPoints` by default.

To override it with a new path, create, or put line to `secrets.properties` in the project's root folder:

```
csvRelativePath=<new_path>
```

Example: `csvRelativePath=Documents/Data/CSV`

<details>
<summary>For debug build</summary>

The relative path of folder with CSV files in device internal storage is `DataPoints-debug` by default.

To override it with a new path, create, or put line to `secrets.properties` in the project's root folder:

```
csvRelativePathDebug=<new_path>
```
</details>

#### App signing setup

* Put your `keystore.jks` to the project's root folder for signing the app.
* Create, or put lines to `secrets.properties` in the project's root folder with `keystore.jks` credentials:

```
signingStoreLocation=../keystore.jks
signingStorePassword=<keystore.jks password>
signingKeyAlias=<keystore.jks alias>
signingKeyPassword=<keystore.jks key password>
```

<details>
<summary>For debug build</summary>
This step isn't required - debug keystore will be used instead automatically
</details>

### Install on ADB connected device:

```
./gradlew app:installRelease
```

<details>
<summary>For debug build</summary>

```
./gradlew app:installDebug
```
</details>

### Build installable APK:

```
./gradlew app:assembleRelease
```

Then install `app/build/outputs/apk/release/app-release.apk` on Android device.

<details>
<summary>For debug build</summary>

```
./gradlew app:assembleDebug
```

Then install `app/build/outputs/apk/release/app-debug.apk` on Android device.
</details>

### User manual

Once installed and launched, the app will request permissions:

| Permission             | If not granted                                                               |
|------------------------|------------------------------------------------------------------------------|
| Location, fine         | App will collect data points with missing location                           |
| Location, at all times | App will collect data points with missing location when its UI is not opened |
| Notifications          | App will collect data points without visual indication                       |
| Files access, all      | App will not collect or record data points                                   |

_Note: notifications will be shown for 5 seconds minimum in order to be seen adequately. 
Data point collection may complete quicker than that._

The app can be prevented from collecting and recording data points 
by setting the device to at least one of the following:
* Power saving mode is enabled
* Files access permission is not granted

Once the conditions above are lifted, normal operation resumes.

When launched, the app shows unprocessed CSV records of data points collected on the current day.


## License

[MIT](LICENSE) license.

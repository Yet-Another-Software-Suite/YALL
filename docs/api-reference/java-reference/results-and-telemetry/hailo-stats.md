# HailoStats

**Package:** `limelight.results`

Stats for the Hailo neural accelerator module (sold separately, Limelight 4 only), from the JSON `hailo` object nested in [`HardwareReport`](hardware-report.md).

***

## Fields

| Field      | Type     | Description                                        |
| ---------- | -------- | -------------------------------------------------- |
| `present`  | `int`    | `1` if a Hailo accelerator is detected, else `0`.  |
| `type`     | `String` | Hailo device type string.                          |
| `temp`     | `double` | Hailo temperature, in Celsius.                     |
| `power`    | `double` | Hailo power consumption, in watts.                 |
| `throttle` | `int`    | `1` if throttling capability is enabled, else `0`. |

***

## Methods

| Method         | Returns   | Description                  |
| -------------- | --------- | ---------------------------- |
| `getPower()`   | `Power`   | `power` as a WPILib `Power`. |
| `getEnabled()` | `boolean` | `true` if `throttle == 1`.   |

## See Also

* [HardwareReport](hardware-report.md)

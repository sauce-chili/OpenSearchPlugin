## Build & Runnig the project

To simplify the process of setting up and launching the project, docker-compose is used. The following steps should be performed to get started:

1. **Build the Docker image (if not already built):**
   ```bash
   docker-compose build
   ```
2. **Start the container:**
   ```bash
   docker-compose up
   ```
## About
The plugin is designed to calculate statistics for the specified index. 
The following functions are available for calculating statistics:
* _avg_ - calculates the arithmetic mean in the index by the field “ups_adv_battery_run_time_remaining”.
* _max_ - calculates the maximum value in the index by the field “ups_adv_output_voltage”.
* _values_ - collects all unique values ​​in an index by field “host”.

__IMPORTANT__: for the plugin to work, the index must contain fields: “ups_adv_battery_run_time_remaining”, “ups_adv_output_voltage”, “host”
The following types of mentioned fields are supported:
* ups_adv_battery_run_time_remaining/ups_adv_output_voltage : integer, long, byte,short, double, float, half_float, scaled_float.
* host : text, keyword, ip.

### How it use
The general format of the request is as follows:
```bash
curl -x GET "http://localhost:9200/<your index>/<one of the functions: avg, max, values>"
```
### Example requests:
avg:
```bash
curl -x GET "http://localhost:9200/test-idx/avg"
```
max:
```bash
curl -x GET "http://localhost:9200/test-idx/max"
```
values:
```bash
curl -x GET "http://localhost:9200/test-idx/values"
```

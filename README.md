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

### How it use
The general format of the request is as follows:
```bash
curl -X GET "http://localhost:9200/<your index>/<one of the functions: avg, max, values>"
```
### Example requests:
avg:
```bash
curl -X GET "http://localhost:9200/_custom-stats/test-idx/avg"
```
max:
```bash
curl -X GET "http://localhost:9200/_custom-stats/test-idx/max"
```
values:
```bash
curl -X GET "http://localhost:9200/_custom-stats/test-idx/values"
```
### Example response:
_success_:
```json
{
    "success": true,
    "isPartialResponse": false,
    "data": {
        "values": [
            "192.168.10.8",
            "192.168.11.9"
        ]
    }
}
```
_error_:
```json
{
   "errorStatus": "INDEX_NOT_FOUND",
   "success": false,
   "isPartialResponse": false,
   "error": {
      "code": 1806,
      "message": "Индекс test-non-existence не существует",
      "errors": []
   }
}
```

## Sample application

Created a sample test application (running a webservice based on springboot) that does the following in the request/response flow:
1. Increments the active request counter
2. Sleeps for x seconds (randomly between 0 to 15 seconds)
3. Decrements the active request counter
4. Returns back a response

Two key things to note in this program:
1. Return 429 error if the number of active requests is >= MAX_REQUEST (max allowed request) - https://github.com/hariohmprasath/load-test-app/blob/main/src/main/java/com/example/demoapp/TestController.java#L16
2. The program runs a background thread that pushes metrics to Cloudwatch every second on the total active requests served by the container - https://github.com/hariohmprasath/load-test-app/blob/main/src/main/java/com/example/demoapp/MetricEmitter.java

> Note: For the test I kept the `MAX_REQUESTS` to 15.

## Infrastructure for scaling
Deployed the application in ECS fargate as a service behind a ALB. Enabled the following integration with application auto-scaler:
1. Register with scalable target

```bash
aws application-autoscaling register-scalable-target \
--service-namespace ecs \
--scalable-dimension ecs:service:DesiredCount \
--resource-id service/Simple/Load-test-Service \
--min-capacity 1 --max-capacity 3
```

2. Scaling policy configuration

```json
{
    "TargetValue":15,
    "CustomizedMetricSpecification":{
       "MetricName":"CONCURRENCY",
       "Namespace":"APPULSE",
       "Dimensions":[
          {
             "Name":"QUALIFIER",
             "Value":"FUNCTION_ARN"
          }
       ],
       "Statistic":"Maximum",
       "Unit":"Percent"
    },
    "ScaleOutCooldown":45
 }
```

3. Register with scaling policy

```bash
aws application-autoscaling put-scaling-policy \
--policy-name ecs-request-based-scaling-policy \
--policy-type TargetTrackingScaling \
--service-namespace ecs \
--scalable-dimension ecs:service:DesiredCount \
--resource-id service/Simple/Load-test-Service \
--target-tracking-scaling-policy-configuration file://config.json
```

4. Manually updated the scale up alarm with the following configuration, so we can scale out faster.
    * Period: Changed from 60 seconds to 10 seconds
    * Evaluation window: Changed from "3 out of 3 datapoints" to "2 out of 6 datapoints"

## Load test
1. Used `hey` command to run the load test. The command is as follows:

```bash
date -u \
hey -z 3m -c 25 -n 15 http://load-test-load-balancer-1222163752.us-east-1.elb.amazonaws.com/ \
date -u
```

2. Ran the test for 3 minutes with 25 concurrent connections and 15 requests per connection and here are the results:

```bash
Test timing (UTC): Sun Aug 20 09:55:40 UTC 2023 - Sun Aug 20 09:58:52 UTC 2023
Status code distribution:
  [200]	523 responses
  [429]	6341 responses

CloudWatch scale out alarm triggered on 2023-08-20T09:56:28.050Z
ECS desired capacity updated at 2023-08-20 09:56:28
Task ready to serve traffic 2023-08-20 09:57:52
```
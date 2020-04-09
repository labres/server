# HMX LabRes

A little service to receive and store lab results.

## How to run locally with dynamo

1. Start a local instance of [Amazon Dynamo DB](https://aws.amazon.com/dynamodb/)
   
   For example, run `docker run --rm -p 8000:8000 amazon/dynamodb-local`
   
2. Make sure the `dynamo.local-endpoint` value in the `application.yaml`'s default
   profile matches the local Dynamo DB instance
   
   For example, `http://localhost:8000`
   
3. Start the server with `$ SPRING_PROFILES_ACTIVE=dynamo ./gradlew bootRun`
   
## How to run locally without dynamo

1. `$ ./gradlew bootRun`   
   
## Profiles

- `dynamo`: When enabled, uses Dynamo DB for storage instead
  of an in-memory HashMap

- `notify`: When enabled, POSTs a JSON object to the given endpoint with
  the external order number that was just updated
  
  Example payload: `{"externalOrderNumber":"1234567890"}`
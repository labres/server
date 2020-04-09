# HMX LabRes

A little service to receive and store lab results.

## How to run locally

1. Start a local instance of [Amazon Dynamo DB](https://aws.amazon.com/dynamodb/)
   
   For example, run `docker run --rm -p 8000:8000 amazon/dynamodb-local`
   
2. Make sure the `dynamo.local-endpoint` value in the `application.yaml`'s default
   profile matches the local Dynamo DB instance
   
   For example, `http://localhost:8000`
   
3. Start the server with `./gradlew bootRun`

   Make sure the `dynamo` profile is not set. If it is, the local endpoint
   will be ignored
   
## Profiles

- `dynamo`: When enabled, ignores the local dynamo endpoint. Since
  it will run on AWS, it will then find the real DynamoDB.

- `notify`: When enabled, POSTs a JSON object to the given endpoint with
  the external order number that was just updated
  
  Example payload: `{"externalOrderNumber":"1234567890"}`
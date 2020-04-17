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
  
## License

MIT License

Copyright (c) 2020 Healthmetrix GmbH

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

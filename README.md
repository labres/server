# LabRes Server

A service to connect users to their lab results, e.g. for COVID19 tests.

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

- `notify`: When enabled, sends out notifications to a target being set when registering the order on LabRes.
The notification is depending on the type of target either an empty HTTP POST request to the registered URL or a Google FCM 
notification to the registered token.

- `secrets`: When enabled, tries to fetch secrets from the AWS secret manager instead of accessing static mock values.

- `jsonlog`: When enabled, outputs log messages in json format

- `dev`: Sets sane defaults for a dev environment next to including the profiles `dynamo`, `notify`, `secrets` and `jsonlog`

- `prod`: Sets sane defaults for a dev environment next to including the profiles `dynamo`, `notify`, `secrets` and `jsonlog`

  
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

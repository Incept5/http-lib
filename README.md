# Http Lib

Base classes for implementing http gateway services

## Installation

### Gradle (Kotlin DSL)

Add the JitPack repository to your build file:

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}
```

Add the dependency:

```kotlin
dependencies {
    implementation("com.github.incept5:http-lib:1.0.0") // Replace with the latest version
}
```

### Gradle (Groovy DSL)

Add the JitPack repository to your build file:

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
```

Add the dependency:

```groovy
dependencies {
    implementation 'com.github.incept5:http-lib:1.0.0' // Replace with the latest version
}
```

### Maven

Add the JitPack repository to your pom.xml:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Add the dependency:

```xml
<dependency>
    <groupId>com.github.incept5</groupId>
    <artifactId>http-lib</artifactId>
    <version>1.0.0</version> <!-- Replace with the latest version -->
</dependency>
```

## Usage

### Basic Gateway functionality

To create a basic gateway you just need to subclass HttpClient like this:

    open class ExampleHttpGateway (baseUri: String) : HttpClient(baseUri) {

        /**
         * Get something by id and marshal the response into an ExamplePayload
         */
        fun getSomethingById (id: UUID) : ExamplePayload {
            return super.get("/something/$id", null)
        }
    
        fun getSomethingByQuery (query: String) : ExamplePayload {
            return super.get("/something", query)
        }
    
        fun postSomething (payload: ExamplePayload) {
            super.postJson<ExamplePayload>("/something", payload)
        }
    
        fun putSomething (id: UUID, payload: ExamplePayload) {
            super.putJson<ExamplePayload>("/something/$id", payload)
        }
    
        fun deleteSomething (id: UUID) {
            super.delete("/something/$id")
        }
    
        fun patchSomething (id: UUID, payload: ExamplePayload) {
            super.patchJson<ExamplePayload>("/something/$id", payload)
        }
    }

This gives you Json friendly marshalling and unmarshalling of payloads for all the http methods.

### Doing Something special

If you need to set specific headers or handle a response in a special way you can call the execute method and access the lower level okhttp3 classes like:

    open class ExampleHttpGateway (baseUri: String) : HttpClient(baseUri) {

        fun doSomethingSpecial (payload: MyPayload) : ExamplePayload {
            val url = url("/my-special-thing", null)
            val body = Json.toJson(payload).toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .header("Special-Header", "special-value")
                .build()
            return execute(request) { response ->
                // do something special with the response etc
            }
        }
    }    

### Authentication

If you are hitting an API that is authenticated then most likely you will just need to provide
an implementation of AuthTokenFetcher that is responsible for fetching a new token whenever needed.

The library includes built-in support for OIDC Client Credentials grant through `ClientCredentialsTokenFetcher`:

#### Basic Client Credentials Authentication

```kotlin
// Create configuration
val config = StdClientCredentialsConfig(
    tokenEndpoint = "https://auth.example.com/oauth/token",
    clientId = "your-client-id",
    clientSecret = "your-client-secret"
)

// Create token fetcher
val tokenFetcher = ClientCredentialsTokenFetcher(config)

// Create gateway with authentication
class ExampleGatewayWithAuth(
    baseUri: String, 
    tokenFetcher: AuthTokenFetcher
) : HttpClient(baseUri, tokenFetcher = tokenFetcher) {

    fun getSomethingById(id: UUID): ExamplePayload {
        return super.get("/something/$id", null)
    }
}
```

#### Client Credentials with Scopes

You can specify OAuth scopes when requesting access tokens:

```kotlin
val config = StdClientCredentialsConfig(
    tokenEndpoint = "https://auth.example.com/oauth/token",
    clientId = "your-client-id",
    clientSecret = "your-client-secret",
    scope = "payment:create payment:read webhook:read webhook:write"
)

val tokenFetcher = ClientCredentialsTokenFetcher(config)
val gateway = ExampleGatewayWithAuth(baseUri, tokenFetcher)
```

#### Form-Based Client Credentials

If your OAuth provider requires credentials in the request body instead of Basic authentication:

```kotlin
val config = StdClientCredentialsConfig(
    tokenEndpoint = "https://auth.example.com/oauth/token",
    clientId = "your-client-id",
    clientSecret = "your-client-secret",
    scope = "read write" // optional
)

val tokenFetcher = FormBasedClientCredentialsTokenFetcher(config)
val gateway = ExampleGatewayWithAuth(baseUri, tokenFetcher)
```

#### Custom Authentication

For custom authentication flows, implement the `AuthTokenFetcher` interface:

```kotlin
class CustomTokenFetcher : AuthTokenFetcher {
    override fun fetchNewToken(): TokenResponse {
        // Your custom token fetching logic
        return TokenResponse(access_token = "your-token")
    }
}
```

### Error Handling

By default the gateway will use the default failure handler which will map http status codes to ErrorCategories
and will throw an HttpRequestFailedException with the appropriate category and message.
409 and 5XX errors are marked as retryable.

    class DefaultHttpFailureHandler : HttpFailureHandler {
        override fun handleFailedResponse(response: Response) {
            if ( !response.isSuccessful ) {
                logger.warn { "http request returned code: ${response.code} with body: ${response.body?.string()}" }
                val category = when (response.code) {
                    401 -> ErrorCategory.AUTHENTICATION
                    403 -> ErrorCategory.AUTHORIZATION
                    404 -> ErrorCategory.NOT_FOUND
                    409 -> ErrorCategory.CONFLICT
                    else -> ErrorCategory.UNEXPECTED
                }
                val message = "http request failed with code: ${response.code}"
                // retry on 409 and 5XX
                val retryable = response.code == 409 || response.code >= 500
                throw HttpRequestFailedException(category, message, retryable)
            }
        }
    }
    
You can configure an alternate failure handler via the constructor of HttpClient:

    class ExampleGatewayWithCustomFailureHandler (baseUri: String) : HttpClient(baseUri, failureHandler = CustomHttpFailureHandler()) {
    }

### Retries

The library includes a built-in retry mechanism through the `RetryInterceptor` that automatically handles transient failures in HTTP requests. By default, this interceptor will:

- Retry requests that return HTTP 409 (Conflict) or any 5XX (Server Error) status codes
- Perform up to 3 retry attempts
- Wait 500ms between retry attempts

#### Basic Usage

The `RetryInterceptor` is automatically included with the default `HttpClient` configuration, so you don't need to do anything special to enable basic retry functionality.

#### Customizing Retry Behavior

You can customize the retry behavior by providing your own `RetryInterceptor` instance when creating your gateway:

```kotlin
class ExampleGatewayWithRetries(baseUri: String) : HttpClient(
    baseUri = baseUri,
    retryInterceptor = RetryInterceptor(
        maxRetries = 5,                     // Increase max retries to 5
        pauseBetweenRetriesMs = 1000,       // Wait 1 second between retries
        retryPolicy = MyCustomRetryPolicy() // Use custom retry logic
    )
) {
    // Gateway methods
}
```

#### Custom Retry Policies

You can implement your own retry policy by implementing the `RetryPolicy` interface:

```kotlin
class MyCustomRetryPolicy : RetryPolicy {
    override fun shouldRetry(response: Response): Boolean {
        // Custom logic to determine if a retry should be attempted
        // For example, retry on 429 (Too Many Requests) in addition to default cases
        return response.code == 409 || response.code == 429 || response.code in 500..599
    }
}
```

#### Retry Logging

The `RetryInterceptor` logs retry attempts at the INFO level. Each retry will log:
- The URL being retried
- The response code that triggered the retry
- The current retry count
- The maximum number of retries configured

#### When to Use Custom Retry Configuration

Consider customizing the retry configuration when:

1. **Working with rate-limited APIs**: Increase pause time between retries and add 429 status code handling
2. **Critical operations**: Increase the number of retry attempts for important operations
3. **Specific error handling**: Create custom policies to handle specific API response patterns
4. **Performance optimization**: Adjust pause times based on operation importance and expected recovery time

#### Disabling Retries

If you need to disable the retry mechanism for a specific gateway, you can pass `null` for the `retryInterceptor` parameter:

```kotlin
class GatewayWithoutRetries(baseUri: String) : HttpClient(
    baseUri = baseUri,
    retryInterceptor = null
) {
    // Gateway methods
}
```
    
### Logging

By default, we will use our own JsonLoggingInterceptor to log logbook style JSON for requests and response.

To see this in your logs you will need to turn on trace logging for the org.incept5.http.interceptors package.

    <logger name="org.incept5.http.interceptors" level="TRACE"/>

or in your application.yaml:

    quarkus:
      log:
        category:
          "org.incept5.http.interceptors":
            level: TRACE

It will automatically redact the Authorization header if it is present in the request.

If you need to redact things from your request or response then you can customise as follows:

    class MyGateway(baseUri: String) : HttpClient(
        baseUri = baseUri,
        loggingInterceptor = JsonLoggingInterceptor(
            RedactConfig(
                requestBodyElements = listOf("name", "secret"),
                responseBodyElements = listOf("name", "secret"),
                headers = listOf("My-Secret-Header"),
                queryParameters = listOf("myspecialqueryparam")
            )
        )
    )

And the output will be like:

    2024-04-09T19:38:05.586 TRACE c.v.h.i.JsonLoggingInterceptor - {"id":"a43d269362db","type":"request","method":"POST","url":"http://localhost:51460/something","headers":{"X-Correlation-ID":"c41e98e8-2164-4a42-881b-a32cf171da35"},"body":{"id":"9ab2d459-5236-47f4-8c61-1ee866f6f9fc","name":"xxxx","age":42,"components":[{"secret":"xxxx"}]}}
    2024-04-09T19:38:05.588 TRACE c.v.h.i.JsonLoggingInterceptor - {"id":"a43d269362db","type":"response","status":201,"headers":{"Content-Type":"application/json","Content-Length":"103"},"body":{"id":"9ab2d459-5236-47f4-8c61-1ee866f6f9fc","name":"xxxx","age":42,"components":[{"secret":"xxxx"}]}}


#### Unredacted Logging in Tests

If you want to turn off the redacting you can switch the logger to this one:

    quarkus:
      log:
        category:
          "org.incept5.http.interceptors-without-redaction":
            level: TRACE

### Unit Testing
If you want to unit test your gateway class you can use Mockwebserver by adding the following to your toml:
    
    [versions]
    okhttp3 = "4.12.0"

    [libraries]
    mockwebserver = { module = "com.squareup.okhttp3:mockwebserver", version.ref = "okhttp3" }

And then add a dependency to your build file:

    // test web interactions
    testImplementation(libs.mockwebserver)

And then use it like:

    should("get something by id") {
        val server = MockWebServer()
        val gateway = ExampleHttpGateway(server.url("/").toString())
        val id = UUID.randomUUID()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(Json.toJson(ExamplePayload(id, "get_by_id", 42)))
        )
        val result = gateway.getSomethingById(id)
        result.id shouldBe id
        val request = server.takeRequest()
        request.method shouldBe "GET"
        request.path shouldBe "/something/$id"
        server.shutdown()
    }

For more info on how to use Mockwebserver see: https://github.com/square/okhttp/tree/master/mockwebserver

### Integration Testing with Wiremock

For integration testing your gateway when running inside a Quarkus application you should use Wiremock by adding this to your toml:

    [versions]
    quarkus-wiremock = "1.1.1"

    [libraries]
    quarkus-wiremock = { module = "io.quarkiverse.wiremock:quarkus-wiremock", version.ref = "quarkus-wiremock" }
    quarkus-wiremock-test = { module = "io.quarkiverse.wiremock:quarkus-wiremock-test", version.ref = "quarkus-wiremock" }

And then add 2 dependencies to your build file:

    testImplementation(libs.quarkus.wiremock)
    testImplementation(libs.quarkus.wiremock.test)

Then configure your gateway yaml config properties in the test application.yaml like:

    # Example of how to use the WireMock DevServices to mock an external HTTP service
    example:
        http:
            base-url: http://localhost:${quarkus.wiremock.devservices.port}
            token-endpoint: http://localhost:${quarkus.wiremock.devservices.port}/token
            client-id: test-client
            client-secret: test-secret

And then use it like:

    @QuarkusTest
    @ConnectWireMock
    class ExampleGatewayTest {
    
        val KNOWN_ID = UUID.fromString("08cb06af-bbbf-42c1-af48-5411b1deb388")
    
        @Inject
        lateinit var exampleGateway: ExampleGatewayWithAuth
        lateinit var wireMock: WireMock

        /**
         * The mapping files are located in src/test/resources/mappings
         */
        @Test
        fun `example gateway test using wiremock mappings file`() {
            val payload = exampleGateway.getSomethingById(KNOWN_ID)
            assert(payload.id == KNOWN_ID)
            assert(payload.name == "Joe Bloggs")
            assert(payload.age == 23)
        }
    
        @Test
        fun `example gateway test using wiremock stubs`() {
    
            val id = UUID.randomUUID()
    
            wireMock.register(
                WireMock.get(WireMock.urlEqualTo("/something/$id"))
                    .willReturn(
                        WireMock.aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(
                                """
                                {
                                    "id": "$id",
                                    "name": "Mary Shelley",
                                    "age": 42
                                }
                                """.trimIndent()
                            )
                    )
            )
    
            val payload = exampleGateway.getSomethingById(id)
            assert(payload.id == id)
            assert(payload.name == "Mary Shelley")
            assert(payload.age == 42)
        }
    
    }

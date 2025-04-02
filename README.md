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

Here is an example implementation that supports the standard OIDC style Client Credentials grant:

    class ClientCredentialsTokenFetcher (val config: ClientCredentialsConfig): AuthTokenFetcher {

        private val encoder = Base64.getEncoder()
        private val client = OkHttpClient.Builder().addInterceptor(RetryInterceptor()).build()
    
        override fun fetchNewToken(): TokenResponse {
            val token = encoder.encode("${config.clientId()}:${config.clientSecret()}".toByteArray()).toString(Charsets.UTF_8)
            val body = "grant_type=client_credentials".toRequestBody(null)
            val request = Request.Builder()
                .url(config.tokenEndpoint())
                .header("Authorization", "Basic $token")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                val bodyAsString = response.body?.string()
                return Json.fromJson(bodyAsString!!)
            }
        }
    }

Then you can inject it into your gateway like this:

    class ExampleGatewayWithAuth (baseUri: String, tokenFetcher: AuthTokenFetcher) : HttpClient(baseUri, tokenFetcher = tokenFetcher) {

        fun getSomethingById (id: UUID) : ExamplePayload {
            return super.get("/something/$id", null)
        }
    }

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

One of the standard interceptors is the RetryInterceptor that will retry the request on 409 or 5XX upto 3 times.
You can customize the retry interceptor by providing a custom implementation of RetryInterceptor or by providing a different policy:


    class ExampleGatewayWithRetries (baseUri: String) : HttpClient(baseUri, retryInterceptor = RetryInterceptor(maxRetries = 5, retryPolicy=MyCustomRetryPolicy()) {
    }
    
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

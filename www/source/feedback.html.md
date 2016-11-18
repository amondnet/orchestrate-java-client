> Feedback is the breakfast of champions. <cite>Ken Blanchard</cite>

## <a name="feature-proposals"></a> [Feature Proposals](#feature-proposals)

We're constantly improving the Orchestrate.io service.

Adding new features, optimising the performance of the platform and refining our
 efforts on the goal of providing developers all the query functionality you
 need to build large scale applications like Foursquare, Twitter and Facebook
 with Orchestrate.

We welcome all feedback you have about the service, no matter how small. We're
 especially interested in limitations you encounter that delay or prevent you
 from develop your applications on the platform.

You can reach us within the [Dashboard](https://dashboard.orchestrate.io/) by
 clicking the support button on the right side menu (the icon that looks like a
 message bubble) and sending a message.

You can also request new features or vote for an existing one via our UserVoice:

[http://support.orchestrate.io/](http://support.orchestrate.io/)

## <a name="debugging-requests"></a> [Debugging Requests](#debugging-requests)

When you call any of the Resource methods that return an OrchestrateRequest, the library
 makes a HTTP request to the Orchestrate platform. The service responds to the request
 with a response containing an `X-Orchestrate-Req-Id` header. This helps us locate the HTTP
 request information internally when debugging a problem.

You can see this response information by enabling `INFO` level logging in the
 client and configuring an [SLF4J](http://www.slf4j.org/) logger. Some exception
 messages thrown by the client also contain this header information.

```bash
[OrchestrateClientPool(1)] INFO io.orchestrate.client.ClientFilter - Received content: HttpResponsePacket (
  status=200
  reason=OK
  protocol=HTTP/1.1
  content-length=101
  committed=false
  headers=[
      content-type=application/json
      date=Fri, 29 Nov 2013 15:36:59 GMT
      x-orchestrate-req-id=1622b5e0-590c-11e3-a6c4-12313d2f50f8
      content-length=101
      connection=keep-alive]
)
```

If you believe you've found a bug in the service, please open an issue (see below)
 and mention the `X-Orchestrate-Req-Id` in it if possible. Thanks. `:)`

## <a name="issues"></a> [Issues](#issues)

The codebase is managed on [GitHub](https://github.com/orchestrate-io/orchestrate-java-client),
 we also use their issue tracker to manage bug reporting. If you find a bug
 please report it:

[https://github.com/orchestrate-io/orchestrate-java-client/issues](https://github.com/orchestrate-io/orchestrate-java-client/issues)

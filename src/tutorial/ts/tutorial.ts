/// <reference path="contract"/>

module tutorial {

  function log(...args: any[]): void {
    console.log.apply(console, args);
  }

  log("running tutorial ...");

  function logger(type: string): yass.Interceptor {
    return (style, method, parameters, proceed) => {
      function doLog(kind: string, data: any): void {
        log("logger:", type, kind, yass.InvokeStyle[style], method, data);
      }
      doLog("entry", parameters);
      try {
        var result = proceed();
        doLog("exit", result);
        return result;
      } catch (e) {
        doLog("exception", e);
        throw e;
      }
    };
  }
  var clientLogger = logger("client");
  var serverLogger = logger("server");

  class PriceListenerImpl implements contract.PriceListener {
    newPrices(prices: contract.Price[]): void {
      log("newPrices:", prices);
    }
  }
  var PRICE_LISTENER = new PriceListenerImpl;

  class EchoServiceImpl implements contract.EchoService {
    echo(value: any): any {
      if (value === "throw") {
        var e = new contract.UnknownInstrumentsException;
        e.comment = "exception from echo";
        throw e;
      }
      return value;
    }
  }
  var ECHO_SERVICE = new EchoServiceImpl;
  log(ECHO_SERVICE.echo("echo called"));

  function simulateServerCallingClient(invokerFactory: yass.InvokerFactory): void {
    var priceListener = invokerFactory.invoker(contract.ClientServices.PriceListener)(clientLogger);
    var echoService = invokerFactory.invoker(contract.ClientServices.EchoService)(clientLogger);
    var price = new contract.Price;
    price.instrumentId = "123";
    price.type = contract.PriceType.ASK;
    price.value = 999;
    priceListener.newPrices([price, price]); // oneway method call
    // rpc style method calls
    var succeeded: yass.Succeeded<string> = result => {
      log("echo succeeded with", result);
    };
    var failed: yass.Failed = exception => {
      log("echo failed with", exception);
    };
    echoService.echo("hello").then(succeeded, failed);
    echoService.echo("throw").then(succeeded, failed);
  }

  simulateServerCallingClient(new yass.MockInvokerFactory(
    yass.server(
      new yass.Service(contract.ClientServices.PriceListener, PRICE_LISTENER, serverLogger),
      new yass.Service(contract.ClientServices.EchoService, ECHO_SERVICE, serverLogger)
    )
  ));

  function subscribePrices(invokerFactory: yass.InvokerFactory): void {
    // create invokers for ServerServices
    var instrumentServiceInvoker = invokerFactory.invoker(contract.ServerServices.InstrumentService);
    var priceEngineInvoker = invokerFactory.invoker(contract.ServerServices.PriceEngine);
    // create proxies; you can add 0..n interceptors to each proxy
    var instrumentService = instrumentServiceInvoker(clientLogger);
    var priceEngine = priceEngineInvoker(clientLogger);
    instrumentService.reload(true, 987654); // shows oneway method call
    function subscribe(instrumentIds: string[]): void {
      priceEngine.subscribe(instrumentIds).then( // shows rpc style method call
        result => { // called if succeeded
          log("subscribe succeeded"); // result not used here because it's void
        },
        exception => { // called if failed
          log("subscribe failed with", exception);
        }
      );
    }
    instrumentService.getInstruments().then(i => subscribe(i.map(i => i.id)), yass.RETHROW);
    subscribe(["unknownId"]); // shows exceptions
  }

  function sessionFactory(sessionInvokerFactory: yass.SessionInvokerFactory): yass.Session { // called on successful connect
    return {
      opened: function () { // called if session has been opened
        log("session opened");
        subscribePrices(sessionInvokerFactory);
        setTimeout(() => sessionInvokerFactory.close(), 5000); // closes the session
      },
      closed: function (exception) { // called if session has been closed; exception is null if regular close else reason for close
        log("session closed", exception);
      }
    };
  }

  yass.connect(
    "ws://localhost:9090/tutorial",
    contract.SERIALIZER,
    yass.server( // create server for ClientServices; you can add 0..n interceptors to each service
      new yass.Service(contract.ClientServices.PriceListener, PRICE_LISTENER, serverLogger),
      new yass.Service(contract.ClientServices.EchoService, ECHO_SERVICE, serverLogger)
    ),
    sessionFactory
  );

}

import yass = require("../../main/ts/yass");
import contract = require("../../tutorial/ts/contract");

function log(...args: any[]): void {
  console.log.apply(console, args);
}

function logger(type: string): yass.Interceptor {
  return function (method: string, parameters: any[], proceed: () => any): any {
    function doLog(kind: string, data: any): void {
      log("logger:", type, kind, method, data);
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
  echo(message: string): string {
    log("echo:", message);
    if (message === "throw") {
      var e = new contract.UnknownInstrumentsException;
      e.comment = "exception from echo";
      throw e;
    }
    return message;
  }
}
var PRICE_LISTENER = new PriceListenerImpl;

function simulateServerCallingClient(invokerFactory: yass.InvokerFactory): void {
  var priceListener = invokerFactory.invoker(contract.ClientServices.PriceListener)(clientLogger);
  var price = new contract.Price;
  price.instrumentId = "123";
  price.type = contract.PriceType.ASK;
  price.value = 999;
  priceListener.newPrices([price, price]); // oneway method call
  // rpc-style method calls
  var promiseValue = priceListener.echo("hello");
  var promiseException = priceListener.echo("throw");
  var callback = (result: any) => {
    try {
      log("promiseValue:", result()); // calling result() returns the function result or throws its exception
    } catch (e) {
      log("promiseException:", e);
    }
  };
  // executes callback on result received
  promiseValue.then(callback);
  promiseException.then(callback);
}

function subscribePrices(invokerFactory: yass.InvokerFactory): void {
  // create invokers for ServerServices
  var instrumentServiceInvoker = invokerFactory.invoker(contract.ServerServices.InstrumentService);
  var priceEngineInvoker = invokerFactory.invoker(contract.ServerServices.PriceEngine);
  // create proxies; you can add 0..n interceptors to each proxy
  var instrumentService = instrumentServiceInvoker(clientLogger);
  var priceEngine = priceEngineInvoker(clientLogger);
  instrumentService.reload(true, 987654); // shows oneway method call
  function subscribe(instrumentIds: string[]): void {
    priceEngine.subscribe(instrumentIds).then(result => {
      try {
        result();
        log("callback: subscribe");
      } catch (e) {
        log("callback: subscribe with exception", e)
      }
    });
  }
  instrumentService.getInstruments().then(result => {
    var instruments = result();
    subscribe(instruments.map(instrument => instrument.id));
  });
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

export function run() {
  log(PRICE_LISTENER.echo("hello"));
  simulateServerCallingClient(new yass.MockInvokerFactory(
    yass.server(
      new yass.Service(contract.ClientServices.PriceListener, PRICE_LISTENER, serverLogger)
    ),
    contract.SERIALIZER
  ));
  var host = location.host || "localhost:9090";
  yass.connect(
    "ws://" + host + "/tutorial",
    contract.SERIALIZER,
    yass.server( // create server for ClientServices; you can add 0..n interceptors to each service
      new yass.Service(contract.ClientServices.PriceListener, PRICE_LISTENER, serverLogger)
    ),
    sessionFactory
  );
}


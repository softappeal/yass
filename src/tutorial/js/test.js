'use strict';

function assert(value) {
  if (!value) {
    throw "error";
  }
}

//--------------------------------------------------------------------------------------------------------------------
// DTO's

var price = new contract.Price(); // class pattern
price.instrumentId = "IBM"; // string type
price.value = 123; // integer type
price.type = contract.PriceType.BID; // enum pattern

var stock = new contract.instrument.Stock(); // namespace pattern
stock.paysDividend = true; // boolean type
stock.name = "IBM";
assert(stock instanceof contract.Instrument); // inheritance pattern
assert(stock instanceof contract.instrument.Stock);
assert(!(stock instanceof contract.instrument.Bond));

// exception pattern
function throwException() {
  var exception = new contract.UnknownInstrumentsException();
  exception.instrumentIds = ["IBM", "Google"]; // list pattern
  throw exception;
}
try {
  throwException();
} catch (exception) {
  console.log(exception);
}

//--------------------------------------------------------------------------------------------------------------------
// logging interceptor

function logger(type) {
  return function (method, parameters, proceed) {
    function log(kind, data) {
      console.log("logger:", type, kind, method, data);
    }
    log("entry", parameters);
    try {
      var result = proceed();
      log("exit", result);
      return result;
    } catch (e) {
      log("exception", e);
      throw e;
    }
  };
}

//--------------------------------------------------------------------------------------------------------------------
// implement client services (note: rpc-style forbidden on client due to blocking [WebWorker?])

var priceListener = yass.create(contract.PriceListener, {
  newPrices: function (prices) { // oneway function
    console.log("newPrices:", prices);
  }
});
priceListener.newPrices([price, price]); // simulates server calling client

var server = yass.server([ // list all client services
  contract.ClientServices.PriceListener.service(priceListener, logger("server"))
  // other services
]);

//--------------------------------------------------------------------------------------------------------------------
// create/use proxies for server services

// $$$

// oneway server service invocation

// rpc-style server service invocation

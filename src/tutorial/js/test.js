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
// implement client services (note: rpc-style forbidden on client due to blocking [WebWorker?])

var priceListener = new contract.PriceListener();
priceListener.newPrices = function (prices) { // oneway function
  console.log("newPrices:", prices);
};
priceListener.newPrices([price, price]); // simulates server calling client

// list all client services
var server = [
  yass.service(contract.ClientServices.PriceListener, priceListener /* , interceptors...*/)
  // other services
];

//--------------------------------------------------------------------------------------------------------------------
// create/use proxies for server services

var session; // get session object from somewhere ...

var instrumentService = yass.proxy(session, contract.ServerServices.InstrumentService /* , interceptors...*/);

// server service implementation fake
instrumentService = new contract.InstrumentService();
instrumentService.getInstruments = function () {
  return [stock, stock];
};

// rpc-style server service invocation
yass.rpc(instrumentService.getInstruments(), function (result) {
  console.log(result);
});

// oneway server service invocation
// tradeService.sendTrade("bla"); // no such method in tutorial yet ...

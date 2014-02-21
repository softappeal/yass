// generated with yass null

'use strict';

var contract = {};

contract.ClientServices = {
  PriceListener: 0 /* contract.PriceListener */
};

contract.ServerServices = {
  PriceEngine: 0 /* contract.PriceEngine */,
  InstrumentService: 1 /* contract.InstrumentService */
};

contract.InstrumentService = function () {};
contract.InstrumentService.prototype.getInstruments = function () {};
contract.InstrumentService.prototype.reload = function () {};

contract.PriceEngine = function () {};
contract.PriceEngine.prototype.subscribe = function (param0) {};

contract.PriceListener = function () {};
contract.PriceListener.prototype.newPrices = function (param0) {};

contract.PriceType = {
  BID: 0,
  ASK: 1
};

contract.Price = function () {
  this.instrumentId = null;
  this.type = null;
  this.value = null;
};

contract.Trade = function () {
  this.amount = null;
  this.instrument = null;
};

contract.UnknownInstrumentsException = function () {
  this.instrumentIds = null;
};

contract.instrument = {};

contract.Instrument = function () {
  this.id = null;
  this.name = null;
};

contract.instrument.Stock = function () {
  contract.Instrument.call(this);
  this.paysDividend = null;
};
yass.inherits(contract.instrument.Stock, contract.Instrument);

contract.instrument.Bond = function () {
  contract.Instrument.call(this);
  this.coupon = null;
};
yass.inherits(contract.instrument.Bond, contract.Instrument);


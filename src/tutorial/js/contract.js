// generated with yass null

'use strict';

var contract = {};

contract.PriceType = yass.enumConstructor();
contract.PriceType.BID = new contract.PriceType(0, "BID");
contract.PriceType.ASK = new contract.PriceType(1, "ASK");
yass.enumDesc(6, contract.PriceType);

contract.Price = function () {
  this.instrumentId = null;
  this.type = null;
  this.value = null;
};
yass.inherits(contract.Price, yass.Class);
yass.classDesc(7, contract.Price);

contract.instrument = {};

contract.Instrument = function () {
  this.id = null;
  this.name = null;
};
yass.inherits(contract.Instrument, yass.Class);

contract.instrument.Stock = function () {
  contract.Instrument.call(this);
  this.paysDividend = null;
};
yass.inherits(contract.instrument.Stock, contract.Instrument);
yass.classDesc(8, contract.instrument.Stock);

contract.instrument.Bond = function () {
  contract.Instrument.call(this);
  this.coupon = null;
};
yass.inherits(contract.instrument.Bond, contract.Instrument);
yass.classDesc(9, contract.instrument.Bond);

contract.UnknownInstrumentsException = function () {
  this.comment = null;
  this.instrumentIds = null;
};
yass.inherits(contract.UnknownInstrumentsException, yass.Class);
yass.classDesc(10, contract.UnknownInstrumentsException);

contract.InstrumentService = {
  getInstruments: function () {},
  reload: function (param0, param1) {} // OneWay
};

contract.InstrumentService_MAPPER = yass.methodMapper(contract.InstrumentService, [
  yass.methodMapping("getInstruments", 0, false),
  yass.methodMapping("reload", 1, true)
]);

contract.PriceEngine = {
  subscribe: function (param0) {}
};

contract.PriceEngine_MAPPER = yass.methodMapper(contract.PriceEngine, [
  yass.methodMapping("subscribe", 0, false)
]);

contract.PriceListener = {
  echo: function (param0) {},
  newPrices: function (param0) {} // OneWay
};

contract.PriceListener_MAPPER = yass.methodMapper(contract.PriceListener, [
  yass.methodMapping("echo", 0, false),
  yass.methodMapping("newPrices", 1, true)
]);

contract.ClientServices = {
  PriceListener: yass.contractId(0, contract.PriceListener_MAPPER)
};

contract.ServerServices = {
  PriceEngine: yass.contractId(0, contract.PriceEngine_MAPPER),
  InstrumentService: yass.contractId(1, contract.InstrumentService_MAPPER)
};

yass.classField(contract.Price, 1, "instrumentId", yass.STRING);
yass.classField(contract.Price, 2, "type", contract.PriceType);
yass.classField(contract.Price, 3, "value", yass.INTEGER);

yass.classField(contract.instrument.Stock, 1, "id", yass.STRING);
yass.classField(contract.instrument.Stock, 2, "name", yass.STRING);
yass.classField(contract.instrument.Stock, 3, "paysDividend", yass.BOOLEAN);

yass.classField(contract.instrument.Bond, 1, "coupon", yass.INTEGER);
yass.classField(contract.instrument.Bond, 2, "id", yass.STRING);
yass.classField(contract.instrument.Bond, 3, "name", yass.STRING);

yass.classField(contract.UnknownInstrumentsException, 1, "comment", null);
yass.classField(contract.UnknownInstrumentsException, 2, "instrumentIds", yass.LIST);

contract.SERIALIZER = yass.serializer([
  contract.PriceType,
  contract.Price,
  contract.instrument.Stock,
  contract.instrument.Bond,
  contract.UnknownInstrumentsException
]);

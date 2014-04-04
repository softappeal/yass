// $todo: add properties constructor for generated classes ?

export class Writer {
  private capacity: number;
  private position = 0;
  private array: Uint8Array;
  constructor(initialCapacity: number) {
    this.capacity = initialCapacity;
    this.array = new Uint8Array(initialCapacity);
  }
  private needed(value: number): number {
    var oldPosition = this.position;
    this.position += value;
    if (this.position > this.capacity) {
      var oldArray = this.array;
      this.capacity = 2 * this.position;
      this.array = new Uint8Array(this.capacity);
      this.array.set(oldArray);
    }
    return oldPosition;
  }
  writeByte(value: number): void {
    var position = this.needed(1);
    this.array[position] = value;
  }
  writeInt(value: number): void {
    var position = this.needed(4);
    new DataView(this.array.buffer).setInt32(position, value);
  }
  writeVarInt(value: number): void {
    while (true) {
      if ((value & ~0x7F) === 0) {
        this.writeByte(value);
        return;
      }
      this.writeByte((value & 0x7F) | 0x80);
      value >>>= 7;
    }
  }
  writeZigZagInt(value: number): void {
    this.writeVarInt((value << 1) ^ (value >> 31));
  }
  writeUtf8(value: string): void { // $todo: is there a built-in way ?
    for (var c = 0; c < value.length; c++) {
      var code = value.charCodeAt(c);
      if (code < 0x80) { // 0xxx xxxx
        this.writeByte(code);
      } else if (code < 0x800) { // 110x xxxx  10xx xxxx
        this.writeByte(0xC0 | ((code >> 6) & 0x1F));
        this.writeByte(0x80 | (code & 0x3F));
      } else { // 1110 xxxx  10xx xxxx  10xx xxxx
        this.writeByte(0xE0 | ((code >> 12) & 0x0F));
        this.writeByte(0x80 | ((code >> 6) & 0x3F));
        this.writeByte(0x80 | (code & 0x3F));
      }
    }
  }
  getUint8Array(): Uint8Array {
    return this.array.subarray(0, this.position);
  }
  static calcUtf8bytes(value: string): number { // $todo: is there a built-in way ?
    var bytes = 0;
    for (var c = 0; c < value.length; c++) {
      var code = value.charCodeAt(c);
      if (code < 0x80) {
        bytes += 1;
      } else if (code < 0x800) {
        bytes += 2;
      } else {
        bytes += 3;
      }
    }
    return bytes;
  }
}

export class Reader {
  private array: Uint8Array;
  private length: number;
  private position = 0;
  constructor(arrayBuffer: ArrayBuffer) {
    this.array = new Uint8Array(arrayBuffer);
    this.length = arrayBuffer.byteLength;
  }
  private needed(value: number): number {
    var oldPosition = this.position;
    this.position += value;
    if (this.position > this.length) {
      throw new Error("reader buffer underflow");
    }
    return oldPosition;
  }
  isEmpty(): boolean {
    return this.position >= this.length;
  }
  readByte(): number {
    return this.array[this.needed(1)];
  }
  readInt(): number {
    return new DataView(this.array.buffer).getInt32(this.needed(4));
  }
  readVarInt(): number {
    var shift = 0;
    var value = 0;
    while (shift < 32) {
      var b = this.readByte();
      value |= (b & 0x7F) << shift;
      if ((b & 0x80) === 0) {
        return value;
      }
      shift += 7;
    }
    throw new Error("malformed VarInt input");
  }
  readZigZagInt(): number {
    var value = this.readVarInt();
    return (value >>> 1) ^ -(value & 1);
  }
  readUtf8(bytes: number): string { // $todo: is there a built-in way ?
    var result = "";
    while (bytes-- > 0) {
      var code: number;
      var b1 = this.readByte();
      if ((b1 & 0x80) === 0) { // 0xxx xxxx
        code = b1;
      } else if ((b1 & 0xE0) === 0xC0) { // 110x xxxx  10xx xxxx
        var b2 = this.readByte();
        if ((b2 & 0xC0) !== 0x80) {
          throw new Error("malformed String input (1)");
        }
        code = ((b1 & 0x1F) << 6) | (b2 & 0x3F);
        bytes--;
      } else if ((b1 & 0xF0) === 0xE0) { // 1110 xxxx  10xx xxxx  10xx xxxx
        var b2 = this.readByte();
        var b3 = this.readByte();
        if (((b2 & 0xC0) !== 0x80) || ((b3 & 0xC0) !== 0x80)) {
          throw new Error("malformed String input (2)");
        }
        code = ((b1 & 0x0F) << 12) | ((b2 & 0x3F) << 6) | (b3 & 0x3F);
        bytes -= 2;
      } else {
        throw new Error("malformed String input (3)");
      }
      result += String.fromCharCode(code);
    }
    return result;
  }
}

export class Type {
  // empty
}

export class Class extends Type {
  // empty
}

export class Enum extends Type {
  constructor(public value: number, public name: string) {
    super();
  }
}

export interface TypeHandler {
  write(value: any, output: Output): void;
  read (input: Input): any;
}

export class TypeDesc {
  constructor(public id: number, public handler: TypeHandler) {
    // empty
  }
  write(value: any, output: Output): void {
    output.writer.writeVarInt(this.id);
    this.handler.write(value, output);
  }
}

class NullTypeHandler implements TypeHandler {
  read(input: Input): any {
    return null;
  }
  write(value: any, output: Output): void {
    // empty
  }
}
var NULL = new TypeDesc(0, new NullTypeHandler());

class ListTypeHandler implements TypeHandler {
  read(input: Input): any[] {
    var list: any[] = [];
    for (var size = input.reader.readVarInt(); size > 0; size--) {
      list.push(input.read());
    }
    return list;
  }
  write(value: any[], output: Output): void {
    output.writer.writeVarInt(value.length);
    value.forEach((element: any) => output.write(element));
  }
}
export var LIST = new TypeDesc(2, new ListTypeHandler());

class BooleanTypeHandler implements TypeHandler {
  read(input: Input): boolean {
    return input.reader.readByte() !== 0;
  }
  write(value: boolean, output: Output): void {
    output.writer.writeByte(value ? 1 : 0);
  }
}
export var BOOLEAN = new TypeDesc(3, new BooleanTypeHandler());

class IntegerTypeHandler implements TypeHandler {
  read(input: Input): number {
    return input.reader.readZigZagInt();
  }
  write(value: number, output: Output): void {
    output.writer.writeZigZagInt(value);
  }
}
export var INTEGER = new TypeDesc(4, new IntegerTypeHandler());

class StringTypeHandler implements TypeHandler {
  read(input: Input): string {
    return input.reader.readUtf8(input.reader.readVarInt());
  }
  write(value: string, output: Output): void {
    output.writer.writeVarInt(Writer.calcUtf8bytes(value));
    output.writer.writeUtf8(value);
  }
}
export var STRING = new TypeDesc(5, new StringTypeHandler());

class EnumTypeHandler implements TypeHandler {
  constructor(private values: Enum[]) {
    // empty
  }
  read(input: Input): Enum {
    return this.values[input.reader.readVarInt()];
  }
  write(value: Enum, output: Output): void {
    output.writer.writeVarInt(value.value);
  }
}

export class Input {
  constructor(public reader: Reader, private id2typeHandler: TypeHandler[]) {
    // empty
  }
  read(): any {
    return this.id2typeHandler[this.reader.readVarInt()].read(this);
  }
}

export class Output {
  constructor(public writer: Writer) {
    // empty
  }
  write(value: any): void {
    if (value === null) {
      NULL.write(null, this);
    } else if (typeof(value) === "boolean") {
      BOOLEAN.write(value, this);
    } else if (typeof(value) === "number") {
      INTEGER.write(value, this);
    } else if (typeof(value) === "string") {
      STRING.write(value, this);
    } else if (Array.isArray(value)) {
      LIST.write(value, this);
    } else if (value instanceof Type) {
      value.constructor.TYPE_DESC.write(value, this);
    } else {
      throw new Error("unexpected value'" + value + "'");
    }
  }
}

class FieldHandler {
  constructor(private field: string, private typeHandler: TypeHandler) {
    // empty
  }
  read(object: any, input: Input): any {
    object[this.field] = this.typeHandler ? this.typeHandler.read(input) : input.read();
  }
  write(id: number, object: any, output: Output) {
    var value = object[this.field];
    if (value) {
      output.writer.writeVarInt(id);
      if (this.typeHandler) {
        this.typeHandler.write(value, output);
      } else {
        output.write(value);
      }
    }
  }
}

class ClassTypeHandler implements TypeHandler {
  private fieldId2handler: FieldHandler[] = [];
  constructor(private Constructor: any) {
    // empty
  }
  addField(id: number, handler: FieldHandler): void {
    this.fieldId2handler[id] = handler;
  }
  read(input: Input): any {
    var object = new this.Constructor();
    while (true) {
      var id = input.reader.readVarInt();
      if (id === 0) {
        return object;
      }
      this.fieldId2handler[id].read(object, input);
    }
  }
  write(value: any, output: Output): void {
    this.fieldId2handler.forEach((handler: FieldHandler, id: number) => handler.write(id, value, output));
    output.writer.writeVarInt(0);
  }
}

export interface Serializer {
  read(reader: Reader): any;
  write(value: any, writer: Writer): void;
}

export class JsFastSerializer implements Serializer {
  private id2typeHandler: TypeHandler[] = [];
  constructor(...Types: any[]) {
    var add = (typeDesc: TypeDesc) => this.id2typeHandler[typeDesc.id] = typeDesc.handler;
    [NULL, LIST, BOOLEAN, INTEGER, STRING].forEach(add);
    Types.forEach((Type: any)=> add(Type.TYPE_DESC));
  }
  read(reader: Reader): any {
    return new Input(reader, this.id2typeHandler).read();
  }
  write(value: any, writer: Writer): void {
    new Output(writer).write(value);
  }
}

export class FieldDesc {
  constructor(public id: number, public name: string, public typeDesc: TypeDesc) {
    //empty
  }
}

export function classDesc(id: number, Type: any, ...fieldDescs: FieldDesc[]): TypeDesc {
  var handler = new ClassTypeHandler(Type);
  fieldDescs.forEach((fieldDesc: FieldDesc) => {
    var typeDesc = fieldDesc.typeDesc;
    handler.addField(fieldDesc.id, new FieldHandler(fieldDesc.name, typeDesc && typeDesc.handler));
  });
  return new TypeDesc(id, handler);
}

export function enumDesc(id: number, Type: any): TypeDesc {
  var values: Enum[] = [];
  Object.keys(Type).forEach((property: any) => {
    var constant = Type[property];
    if (constant instanceof Enum) {
      values[constant.value] = constant;
    }
  });
  return new TypeDesc(id, new EnumTypeHandler(values));
}

export interface Interceptor {
  (method: string, parameters: any[], proceed: () => any): any;
}

export var DIRECT: Interceptor = (method: string, parameters: any[], proceed: () => any): any => proceed();

export function composite(...interceptors: Interceptor[]): Interceptor {
  function composite2(interceptor1: Interceptor, interceptor2: Interceptor): Interceptor {
    return function (method: string, parameters: any[], proceed: () => any): any {
      return interceptor1(method, parameters, () => interceptor2(method, parameters, proceed));
    };
  }
  var i1 = DIRECT;
  for (var i = 0; i < interceptors.length; i++) {
    var i2 = interceptors[i];
    i1 = (i1 === DIRECT) ? i2 : ((i2 === DIRECT) ? i1 : composite2(i1, i2));
  }
  return i1;
}

export interface Message {
  // empty
}

export class Request implements Message {
  constructor(public serviceId: number, public methodId: number, public parameters: any[]) {
    // empty
  }
}

export interface Reply extends Message {
  process(): any
}

class ValueReply implements Reply {
  constructor(public value: any) {
    // empty
  }
  process(): any {
    return this.value;
  }
}

class ExceptionReply implements Reply {
  constructor(public exception: any) {
    // empty
  }
  process(): any {
    throw this.exception;
  }
}

class MessageSerializer implements Serializer {
  private static REQUEST = 0;
  private static VALUE_REPLY = 1;
  private static EXCEPTION_REPLY = 2;
  constructor(private serializer: Serializer) {
    // empty
  }
  read(reader: Reader): Message {
    var type = reader.readByte();
    if (type === MessageSerializer.REQUEST) {
      return new Request(this.serializer.read(reader), this.serializer.read(reader), this.serializer.read(reader));
    }
    if (type === MessageSerializer.VALUE_REPLY) {
      return new ValueReply(this.serializer.read(reader));
    }
    return new ExceptionReply(this.serializer.read(reader));
  }
  write(message: Message, writer: Writer): void {
    if (message instanceof Request) {
      writer.writeByte(MessageSerializer.REQUEST);
      this.serializer.write((<Request>message).serviceId, writer);
      this.serializer.write((<Request>message).methodId, writer);
      this.serializer.write((<Request>message).parameters, writer);
    } else if (message instanceof ValueReply) {
      writer.writeByte(MessageSerializer.VALUE_REPLY);
      this.serializer.write((<ValueReply>message).value, writer);
    } else { // ExceptionReply
      writer.writeByte(MessageSerializer.EXCEPTION_REPLY);
      this.serializer.write((<ExceptionReply>message).exception, writer);
    }
  }
}

export class MethodMapping {
  constructor(public method: string, public id: number, public oneWay: boolean) {
    // empty
  }
}

export class MethodMapper<C> {
  private id2mapping: MethodMapping[] = [];
  private name2Mapping: any = {};
  constructor(...mappings: MethodMapping[]) {
    mappings.forEach(mapping => {
      this.id2mapping[mapping.id] = mapping;
      this.name2Mapping[mapping.method] = mapping;
    });
  }
  mapId(id: number): MethodMapping {
    return this.id2mapping[id];
  }
  mapMethod(method: string): MethodMapping {
    return this.name2Mapping[method];
  }
  proxy(interceptor: (method: string, parameters: any[]) => any): C { // $todo: is there something like a dynamic proxy ?
    var stub: any = {};
    var delegate = (method: string): void => {
      stub[method] = (...parameters: any[]) => interceptor(method, parameters);
    };
    Object.keys(this.name2Mapping).forEach(method => delegate(method));
    return stub;
  }
}

export class Promise<R> { // $todo: replace with ES6 Promise ?
  private callback: (result: () => R) => void = null;
  private result: () => R = null;
  then(callback: (result: () => R) => void): void {
    if (this.callback) {
      throw new Error("method 'then' already called");
    }
    if (typeof(callback) !== "function") {
      throw new Error("parameter 'callback' is not a function");
    }
    this.callback = callback;
    if (this.result) {
      callback(this.result);
    }
  }
  settle(result: () => R): void {
    if (this.callback) {
      this.callback(result);
    } else {
      this.result = result;
    }
  }
}

export class ContractId<C, PC> {
  constructor(public id: number, public methodMapper: MethodMapper<C>) {
    // empty
  }
}

export class Service<C> {
  interceptor: Interceptor;
  constructor(public contractId: ContractId<C, any>, public implementation: C, ...interceptors: Interceptor[]) {
    this.interceptor = composite.apply(null, interceptors);
  }
}

export class ServerInvoker {
  methodMapper: MethodMapper<any>;
  private interceptor: Interceptor;
  private implementation: any;
  constructor(service: Service<any>) {
    this.methodMapper = service.contractId.methodMapper;
    this.interceptor = service.interceptor;
    this.implementation = service.implementation;
  }
  invoke(method: string, parameters: any[]): Reply {
    var proceed = () => {
      var result = this.implementation[method].apply(this.implementation, parameters);
      return result ? result : null;
    };
    var value: Reply;
    try {
      value = this.interceptor(method, parameters, proceed);
    } catch (exception) {
      return new ExceptionReply(exception);
    }
    return new ValueReply(value);
  }
}

export class ServerInvocation {
  oneWay: boolean;
  private method: string;
  constructor(private serverInvoker: ServerInvoker, private request: Request) {
    var methodMapping = serverInvoker.methodMapper.mapId(request.methodId);
    this.oneWay = methodMapping.oneWay;
    this.method = methodMapping.method;
  }
  invoke(): Reply {
    return this.serverInvoker.invoke(this.method, this.request.parameters);
  }
}

export interface Server {
  (request: Request): ServerInvocation;
}

export function server(...services: Service<any>[]): Server {
  var serviceId2invoker: ServerInvoker[] = [];
  services.forEach(service => {
    var id = service.contractId.id;
    if (serviceId2invoker[id]) {
      throw new Error("serviceId '" + id + "' already added");
    }
    serviceId2invoker[id] = new ServerInvoker(service);
  });
  return function (request: Request): ServerInvocation {
    var invoker = serviceId2invoker[request.serviceId];
    if (!invoker) {
      throw new Error("no serviceId '" + request.serviceId + "' found (methodId '" + request.methodId + "')");
    }
    return new ServerInvocation(invoker, request);
  };
}

export interface Invoker<C> {
  (...interceptors: Interceptor[]): C;
}

export interface InvokerFactory {
  invoker<PC>(contractId: ContractId<any, PC>): Invoker<PC>;
}

export interface Tunnel {
  (request: Request, promise: Promise<any>): void;
}

export interface ClientInvoker {
  (invocation: (tunnel: Tunnel) => any): any
}

export class Client implements InvokerFactory {
  constructor(private clientInvoker: ClientInvoker) {
    // empty
  }
  invoker<C, PC>(contractId: ContractId<C, PC>): Invoker<PC> {
    return (...interceptors: Interceptor[]): PC => {
      var interceptor = composite.apply(null, interceptors);
      return <any>contractId.methodMapper.proxy((method: string, parameters: any[]): any => {
        var methodMapping = contractId.methodMapper.mapMethod(method);
        return this.clientInvoker(function (tunnel: Tunnel): Promise<any> {
          var promise = methodMapping.oneWay ? null : new Promise<any>();
          interceptor(
            methodMapping.method,
            parameters,
            () => tunnel(new Request(contractId.id, methodMapping.id, parameters), promise)
          );
          return promise;
        });
      });
    };
  }
}

export class MockInvokerFactory extends Client {
  constructor(server: Server, serializer: Serializer) {
    serializer = new MessageSerializer(serializer);
    function copy(value: any): any {
      var writer = new Writer(1024);
      serializer.write(value, writer);
      var reader = new Reader(writer.getUint8Array());
      value = serializer.read(reader);
      if (!reader.isEmpty()) {
        throw new Error("reader is not empty");
      }
      return value;
    }
    super(function (invocation: (tunnel: Tunnel) => any): any {
      return invocation(function (request: Request, promise: Promise<any>): any {
        var reply = copy(server(copy(request)).invoke());
        if (promise) {
          promise.settle(() => reply.process());
        }
        return null;
      });
    });
  }
}

class Packet {
  static END_REQUESTNUMBER = 0;
  static END = new Packet(Packet.END_REQUESTNUMBER, null);
  constructor(public requestNumber: number, public message: Message) {
    // empty
  }
  isEnd(): boolean {
    return this.requestNumber === Packet.END_REQUESTNUMBER;
  }
}

class PacketSerializer implements Serializer {
  constructor(private messageSerializer: Serializer) {
    // empty
  }
  read(reader: Reader): Packet {
    var requestNumber = reader.readInt();
    return (requestNumber === Packet.END_REQUESTNUMBER) ? Packet.END : new Packet(requestNumber, this.messageSerializer.read(reader));
  }
  write(packet: Packet, writer: Writer): void {
    if (packet.isEnd()) {
      writer.writeInt(Packet.END_REQUESTNUMBER);
    } else {
      writer.writeInt(packet.requestNumber);
      this.messageSerializer.write(packet.message, writer);
    }
  }
}

export interface SessionInvokerFactory extends InvokerFactory {
  close(): void;
}

export interface Session {
  opened(): void;
  closed(exception: any): void;
}

export interface SessionFactory {
  (sessionInvokerFactory: SessionInvokerFactory): Session;
}

interface Connection {
  write(packet: Packet): void;
  closed(): void;
}

class SessionClient extends Client implements SessionInvokerFactory {
  private closed = false;
  private nextRequestNumber = Packet.END_REQUESTNUMBER;
  private requestNumber2promise: Promise<any>[] = [];
  private session: Session = null;
  constructor(private server: Server, sessionFactory: SessionFactory, private connection: Connection) {
    super(function (invocation: (tunnel: Tunnel) => any): any {
      return invocation((request: Request, promise: Promise<any>): any => {
        if (!this.session) {
          throw new Error("session is not yet opened");
        }
        var requestNumber = ++this.nextRequestNumber; // $todo: implement 32bit signed int behaviour and skip END_REQUESTNUMBER
        this.write(requestNumber, request);
        if (promise) {
          if (this.requestNumber2promise[requestNumber]) {
            throw new Error("already waiting for requestNumber " + requestNumber);
          }
          this.requestNumber2promise[requestNumber] = promise;
        }
        return null;
      });
    });
    this.session = sessionFactory(this);
    this.session.opened();
  }
  doClose(exception: any): void {
    this.doCloseSend(false, exception);
  }
  private doCloseSend(sendEnd: boolean, exception: any): void {
    if (this.closed) {
      return;
    }
    this.closed = true;
    try {
      this.session.closed(exception);
      if (sendEnd) {
        this.connection.write(Packet.END);
      }
    } finally {
      this.connection.closed();
    }
  }
  private write(requestNumber: number, message: Message): void {
    if (this.closed) {
      throw new Error("session is already closed");
    }
    try {
      this.connection.write(new Packet(requestNumber, message));
    } catch (exception) {
      this.doClose(exception);
    }
  }
  close(): void {
    this.doCloseSend(true, null);
  }
  received(packet: Packet): void {
    try {
      if (packet.isEnd()) {
        this.doClose(null);
        return;
      }
      if (packet.message instanceof Request) {
        var invocation = this.server(<Request>packet.message);
        var reply = invocation.invoke();
        if (!invocation.oneWay) {
          this.write(packet.requestNumber, reply);
        }
      } else { // Reply
        var promise = this.requestNumber2promise[packet.requestNumber];
        delete this.requestNumber2promise[packet.requestNumber];
        promise.settle(() => (<Reply>packet.message).process());
      }
    } catch (exception) {
      this.doClose(exception);
    }
  }
}

export function connect(url: string, serializer: Serializer, server: Server, sessionFactory: SessionFactory): void {
  serializer = new PacketSerializer(new MessageSerializer(serializer))
  var ws = new WebSocket(url);
  ws.binaryType = "arraybuffer";
  ws.onopen = function () {
    var sessionClient = new SessionClient(server, sessionFactory, {
      write: function (packet) {
        var writer = new Writer(1024);
        serializer.write(packet, writer);
        ws.send(writer.getUint8Array()); // $todo: implement batching ?
      },
      closed: function () {
        ws.close();
      }
    });
    ws.onmessage = function (evt) {
      var reader = new Reader(evt.data);
      sessionClient.received(serializer.read(reader));
      if (!reader.isEmpty()) { // $todo: implement batching ?
        throw new Error("reader is not empty");
      }
    };
    ws.onerror = function (evt) {
      sessionClient.doClose(new Error("onerror"));
    };
    ws.onclose = function () {
      sessionClient.doClose(new Error("onclose"));
    };
  };
}

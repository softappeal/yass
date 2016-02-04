export class Writer {
    private capacity: number;
    private position = 0;
    array: Uint8Array;
    constructor(initialCapacity: number) {
        this.capacity = initialCapacity;
        this.array = new Uint8Array(initialCapacity);
    }
    needed(value: number): number {
        const oldPosition = this.position;
        this.position += value;
        if (this.position > this.capacity) {
            const oldArray = this.array;
            this.capacity = 2 * this.position;
            this.array = new Uint8Array(this.capacity);
            this.array.set(oldArray);
        }
        return oldPosition;
    }
    writeByte(value: number): void {
        const position = this.needed(1);
        this.array[position] = value;
    }
    writeInt(value: number): void {
        const position = this.needed(4);
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
    static calcUtf8bytes(value: string): number {
        let bytes = 0;
        for (let c = 0; c < value.length; c++) {
            const code = value.charCodeAt(c);
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
    writeUtf8(value: string): void {
        for (let c = 0; c < value.length; c++) {
            const code = value.charCodeAt(c);
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
    getArray(): Uint8Array {
        return this.array.subarray(0, this.position);
    }
}

export class Reader {
    array: Uint8Array;
    private length: number;
    private position = 0;
    constructor(arrayBuffer: ArrayBuffer) {
        this.array = new Uint8Array(arrayBuffer);
        this.length = arrayBuffer.byteLength;
    }
    needed(value: number): number {
        const oldPosition = this.position;
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
        let shift = 0;
        let value = 0;
        while (shift < 32) {
            const b = this.readByte();
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) === 0) {
                return value;
            }
            shift += 7;
        }
        throw new Error("malformed VarInt input");
    }
    readZigZagInt(): number {
        const value = this.readVarInt();
        return (value >>> 1) ^ -(value & 1);
    }
    readUtf8(bytes: number): string {
        let result = "";
        while (bytes-- > 0) {
            let code: number;
            const b1 = this.readByte();
            if ((b1 & 0x80) === 0) { // 0xxx xxxx
                code = b1;
            } else if ((b1 & 0xE0) === 0xC0) { // 110x xxxx  10xx xxxx
                const b2 = this.readByte();
                if ((b2 & 0xC0) !== 0x80) {
                    throw new Error("malformed String input (1)");
                }
                code = ((b1 & 0x1F) << 6) | (b2 & 0x3F);
                bytes--;
            } else if ((b1 & 0xF0) === 0xE0) { // 1110 xxxx  10xx xxxx  10xx xxxx
                const b2 = this.readByte();
                const b3 = this.readByte();
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

export interface TypeHandler<T> {
    read (reader: Reader, id2typeHandler?: TypeHandler<any>[]): T;
    write(value: T, writer: Writer): void;
}

export class TypeDesc {
    constructor(public id: number, public handler: TypeHandler<any>) {
        // empty
    }
    write(value: any, writer: Writer): void {
        writer.writeVarInt(this.id);
        this.handler.write(value, writer);
    }
}

class NullTypeHandler implements TypeHandler<any> {
    read(reader: Reader): any {
        return null;
    }
    write(value: any, writer: Writer): void {
        // empty
    }
}
const NULL_DESC = new TypeDesc(0, new NullTypeHandler);

class ListTypeHandler implements TypeHandler<any[]> {
    read(reader: Reader, id2typeHandler: TypeHandler<any>[]): any[] {
        const list: any[] = [];
        for (let size = reader.readVarInt(); size > 0; size--) {
            list.push(read(reader, id2typeHandler));
        }
        return list;
    }
    write(value: any[], writer: Writer): void {
        writer.writeVarInt(value.length);
        value.forEach(element => write(element, writer));
    }
}
export const LIST_DESC = new TypeDesc(2, new ListTypeHandler);

class BooleanTypeHandler implements TypeHandler<boolean> {
    read(reader: Reader): boolean {
        return reader.readByte() !== 0;
    }
    write(value: boolean, writer: Writer): void {
        writer.writeByte(value ? 1 : 0);
    }
}
export const BOOLEAN_DESC = new TypeDesc(3, new BooleanTypeHandler);

class NumberTypeHandler implements TypeHandler<number> {
    read(reader: Reader): number {
        return new DataView(reader.array.buffer).getFloat64(reader.needed(8));
    }
    write(value: number, writer: Writer): void {
        const position = writer.needed(8);
        new DataView(writer.array.buffer).setFloat64(position, value);
    }
}
export const NUMBER_DESC = new TypeDesc(4, new NumberTypeHandler);

class StringTypeHandler implements TypeHandler<string> {
    read(reader: Reader): string {
        return reader.readUtf8(reader.readVarInt());
    }
    write(value: string, writer: Writer): void {
        writer.writeVarInt(Writer.calcUtf8bytes(value));
        writer.writeUtf8(value);
    }
}
export const STRING_DESC = new TypeDesc(5, new StringTypeHandler);

class BytesTypeHandler implements TypeHandler<Uint8Array> {
    read(reader: Reader): Uint8Array {
        const length = reader.readVarInt();
        return new Uint8Array(reader.array.buffer, reader.needed(length), length);
    }
    write(value: Uint8Array, writer: Writer): void {
        writer.writeVarInt(value.length);
        const position = writer.needed(value.length);
        writer.array.set(value, position);
    }
}
export const BYTES_DESC = new TypeDesc(6, new BytesTypeHandler);

export const FIRST_ID = 7;

function read(reader: Reader, id2typeHandler: TypeHandler<any>[]): any {
    return id2typeHandler[reader.readVarInt()].read(reader, id2typeHandler);
}

function write(value: any, writer: Writer): void {
    if ((value === null) || (value === undefined)) {
        NULL_DESC.write(null, writer);
    } else if (typeof(value) === "number") {
        NUMBER_DESC.write(value, writer);
    } else if (typeof(value) === "string") {
        STRING_DESC.write(value, writer);
    } else if (typeof(value) === "boolean") {
        BOOLEAN_DESC.write(value, writer);
    } else if (Array.isArray(value)) {
        LIST_DESC.write(value, writer);
    } else if (value instanceof Uint8Array) {
        BYTES_DESC.write(value, writer);
    } else {
        const typeDesc = value.constructor.TYPE_DESC;
        if (typeDesc) {
            typeDesc.write(value, writer);
        } else {
            throw new Error("unexpected value '" + value + "'");
        }
    }
}

export class Enum {
    constructor(public number: number, public name: string) {
        // empty
    }
    toString(): string {
        return this.name;
    }
    static VALUES: Enum[];
}

class EnumTypeHandler implements TypeHandler<Enum> {
    constructor(private values: Enum[]) {
        // empty
    }
    read(reader: Reader): Enum {
        return this.values[reader.readVarInt()];
    }
    write(value: Enum, writer: Writer): void {
        writer.writeVarInt(value.number);
    }
}

class FieldHandler {
    constructor(private field: string, private typeHandler: TypeHandler<any>) {
        // empty
    }
    read(object: any, reader: Reader, id2typeHandler: TypeHandler<any>[]): any {
        object[this.field] = this.typeHandler ? this.typeHandler.read(reader, id2typeHandler) : read(reader, id2typeHandler);
    }
    write(id: number, object: any, writer: Writer) {
        const value = object[this.field];
        if ((value !== null) && (value !== undefined)) {
            writer.writeVarInt(id);
            if (this.typeHandler) {
                this.typeHandler.write(value, writer);
            } else {
                write(value, writer);
            }
        }
    }
}

class ClassTypeHandler implements TypeHandler<any> {
    private fieldId2handler: FieldHandler[] = [];
    constructor(private Type: any) {
        // empty
    }
    addField(id: number, handler: FieldHandler): void {
        this.fieldId2handler[id] = handler;
    }
    read(reader: Reader, id2typeHandler: TypeHandler<any>[]): any {
        const object = new this.Type;
        while (true) {
            const id = reader.readVarInt();
            if (id === 0) {
                return object;
            }
            this.fieldId2handler[id].read(object, reader, id2typeHandler);
        }
    }
    write(value: any, writer: Writer): void {
        this.fieldId2handler.forEach((handler, id) => handler.write(id, value, writer));
        writer.writeVarInt(0);
    }
}

export interface Serializer {
    read(reader: Reader): any;
    write(value: any, writer: Writer): void;
}

export class JsFastSerializer implements Serializer {
    private id2typeHandler: TypeHandler<any>[] = [];
    constructor(...typeDescHolders: any[]) {
        const add = (typeDesc: TypeDesc) => {
            if (this.id2typeHandler[typeDesc.id]) {
                throw new Error("TypeDesc with id " + typeDesc.id + " already added");
            }
            this.id2typeHandler[typeDesc.id] = typeDesc.handler;
        };
        [NULL_DESC, LIST_DESC, BOOLEAN_DESC, NUMBER_DESC, STRING_DESC, BYTES_DESC].forEach(add);
        typeDescHolders.forEach(typeDescHolder => add(typeDescHolder.TYPE_DESC));
    }
    read(reader: Reader): any {
        return read(reader, this.id2typeHandler);
    }
    write(value: any, writer: Writer): void {
        write(value, writer);
    }
}

export class FieldDesc {
    constructor(public id: number, public name: string, public typeDesc: TypeDesc) {
        // empty
    }
}

export function classDesc(id: number, Type: any, ...fieldDescs: FieldDesc[]): TypeDesc {
    const handler = new ClassTypeHandler(Type);
    fieldDescs.forEach(fieldDesc => {
        const typeDesc = fieldDesc.typeDesc;
        handler.addField(fieldDesc.id, new FieldHandler(fieldDesc.name, typeDesc && typeDesc.handler));
    });
    return new TypeDesc(id, handler);
}

export function enumDesc(id: number, Type: any): TypeDesc {
    const values: Enum[] = [];
    Object.keys(Type).forEach(property => {
        const value = Type[property];
        if (value instanceof Enum) {
            values[value.number] = value;
        }
    });
    Type.VALUES = values;
    return new TypeDesc(id, new EnumTypeHandler(values));
}

export abstract class Message {
    // empty
}

export class Request extends Message {
    constructor(public serviceId: number, public methodId: number, public parameters: any[]) {
        super();
    }
}

export abstract class Reply extends Message {
    abstract process(): any;
}

export class ValueReply extends Reply {
    constructor(public value: any) {
        super();
    }
    process(): any {
        return this.value;
    }
}

export class ExceptionReply extends Reply {
    constructor(public exception: any) {
        super();
    }
    process(): any {
        throw this.exception;
    }
}

export class MessageSerializer implements Serializer {
    private static REQUEST = 0;
    private static VALUE_REPLY = 1;
    private static EXCEPTION_REPLY = 2;
    constructor(private contractSerializer: Serializer) {
        // empty
    }
    read(reader: Reader): Message {
        const type = reader.readByte();
        if (type === MessageSerializer.REQUEST) {
            return new Request(
                reader.readZigZagInt(),
                reader.readZigZagInt(),
                this.contractSerializer.read(reader)
            );
        } else if (type === MessageSerializer.VALUE_REPLY) {
            return new ValueReply(
                this.contractSerializer.read(reader)
            );
        } else {
            return new ExceptionReply(
                this.contractSerializer.read(reader)
            );
        }
    }
    write(message: Message, writer: Writer): void {
        if (message instanceof Request) {
            writer.writeByte(MessageSerializer.REQUEST);
            writer.writeZigZagInt(message.serviceId);
            writer.writeZigZagInt(message.methodId);
            this.contractSerializer.write(message.parameters, writer);
        } else if (message instanceof ValueReply) {
            writer.writeByte(MessageSerializer.VALUE_REPLY);
            this.contractSerializer.write(message.value, writer);
        } else {
            writer.writeByte(MessageSerializer.EXCEPTION_REPLY);
            this.contractSerializer.write((<ExceptionReply>message).exception, writer);
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
    private name2Mapping: {[methodName: string]: MethodMapping} = {};
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
    proxy(interceptor: (method: string, parameters: any[]) => any): C {
        const stub: any = {};
        Object.keys(this.name2Mapping).forEach(method => stub[method] = (...parameters: any[]) => interceptor(method, parameters));
        return stub;
    }
}

export interface Invocation {
    (): any;
}

/**
 * An invocation has a Promise if and only if it is an initiator rpc style invocation.
 * If an invocation has a Promise, the interceptor will be called twice (first with PromiseEntry and then with PromiseExit).
 * If an invocation doesn't have a Promise, the interceptor will be called once (with NoPromise).
 */
export enum InvokeStyle { NoPromise, PromiseEntry, PromiseExit }

export interface Interceptor {
    /**
     * @return invocation()
     */
    (style: InvokeStyle, method: string, parameters: any[], invocation: Invocation): any;
}

export const DIRECT: Interceptor = (style, method, parameters, invocation) => invocation();

export function composite(...interceptors: Interceptor[]): Interceptor {
    function composite2(interceptor1: Interceptor, interceptor2: Interceptor): Interceptor {
        return (style, method, parameters, invocation) => interceptor1(
            style, method, parameters, () => interceptor2(style, method, parameters, invocation)
        );
    }
    let i1 = DIRECT;
    interceptors.forEach(i2 => i1 = (i1 === DIRECT) ? i2 : ((i2 === DIRECT) ? i1 : composite2(i1, i2)));
    return i1;
}

export class ContractId<C, PC> {
    constructor(public id: number, public methodMapper: MethodMapper<C>) {
        // empty
    }
    service(implementation: C, ...interceptors: Interceptor[]): Service {
        return new Service(this, implementation, composite.apply(null, interceptors));
    }
}

export class Service {
    constructor(public contractId: ContractId<any, any>, private implementation: any, private interceptor: Interceptor) {
        // empty
    }
    invoke(method: string, parameters: any[]): Reply {
        try {
            return new ValueReply(this.interceptor(InvokeStyle.NoPromise, method, parameters, () => {
                const result = this.implementation[method].apply(this.implementation, parameters);
                return ((result !== null) && (result !== undefined)) ? result : null;
            }));
        } catch (exception) {
            return new ExceptionReply(exception);
        }
    }
}

export class ServerInvocation {
    methodMapping: MethodMapping;
    parameters: any[];
    constructor(public service: Service, request: Request) {
        this.methodMapping = service.contractId.methodMapper.mapId(request.methodId);
        this.parameters = request.parameters;
    }
    invoke(): Reply {
        return this.service.invoke(this.methodMapping.method, this.parameters);
    }
}

export class Server {
    private id2service: Service[] = [];
    constructor(...services: Service[]) {
        services.forEach(service => {
            const id = service.contractId.id;
            if (this.id2service[id]) {
                throw new Error("serviceId " + id + " already added");
            }
            this.id2service[id] = service;
        });
    }
    invocation(request: Request): ServerInvocation {
        const service = this.id2service[request.serviceId];
        if (!service) {
            throw new Error("no serviceId " + request.serviceId + " found (methodId " + request.methodId + ")");
        }
        return new ServerInvocation(service, request);
    }
    static EMPTY = new Server;
}

export class Rpc {
    promise: Promise<any>;
    settle: (reply: Reply) => void;
    constructor(interceptor: Interceptor, method: string, parameters: any[]) {
        this.promise = new Promise<any>((resolve, reject) => {
            this.settle = reply => {
                try {
                    interceptor(InvokeStyle.PromiseExit, method, parameters, () => reply.process());
                } catch (ignore) {
                    // empty
                }
                try {
                    resolve(reply.process());
                } catch (exception) {
                    reject(exception);
                }
            };
        });
    }
}

export interface Tunnel {
    (request: Request, rpc: Rpc): void;
}

export class ClientInvocation {
    constructor(private interceptor: Interceptor, private serviceId: number, private methodMapping: MethodMapping, private parameters: any[]) {
        // empty
    }
    invoke(tunnel: Tunnel): Promise<any> {
        const rpc = this.methodMapping.oneWay ? null : new Rpc(this.interceptor, this.methodMapping.method, this.parameters);
        this.interceptor(
            this.methodMapping.oneWay ? InvokeStyle.NoPromise : InvokeStyle.PromiseEntry,
            this.methodMapping.method,
            this.parameters,
            (): any => {
                tunnel(new Request(this.serviceId, this.methodMapping.id, this.parameters), rpc);
                return null;
            }
        );
        return rpc ? rpc.promise : null;
    }
}

export abstract class Client {
    proxy<PC>(contractId: ContractId<any, PC>, ...interceptors: Interceptor[]): PC {
        const interceptor = composite.apply(null, interceptors);
        return <any>contractId.methodMapper.proxy((method, parameters) => this.invoke(
            new ClientInvocation(interceptor, contractId.id, contractId.methodMapper.mapMethod(method), parameters)
        ));
    }
    /**
     * @return ClientInvocation.invoke(Interceptor, Tunnel)
     */
    protected abstract invoke(invocation: ClientInvocation): Promise<any>;
}

export class Packet {
    static END_REQUESTNUMBER = 0;
    static END = new Packet(Packet.END_REQUESTNUMBER, null);
    constructor(public requestNumber: number, public message: Message) {
        // empty
    }
    isEnd(): boolean {
        return this.requestNumber === Packet.END_REQUESTNUMBER;
    }
}

export class PacketSerializer implements Serializer {
    constructor(private messageSerializer: Serializer) {
        // empty
    }
    read(reader: Reader): Packet {
        const requestNumber = reader.readInt();
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

export interface Connection {
    write(packet: Packet): void;
    closed(): void;
}

export interface SessionFactory {
    /**
     * It's allowed to call Client.proxy(ContractId, Interceptor...) during this method,
     * but the proxies can be used not before Session.opened() is called.
     * If this method throws an exception, the connection is rejected and Session.closed(Exception) won't be called.
     */
    (connection: Connection): Session;
}

export abstract class Session extends Client {
    constructor(private connection: Connection) {
        super();
    }
    private serverProp: Server;
    /**
     * Gets the server of this session. Called only once after creation of session.
     * This implementation returns Server.EMPTY.
     */
    protected server(): Server {
        return Server.EMPTY;
    }
    /**
     * Called when the session has been opened.
     * This implementation does nothing.
     */
    protected opened(): void {
        // empty
    }
    /**
     * Called once when the session has been closed.
     * This implementation does nothing.
     * @param exception if (exception == null) regular close else reason for close
     * @throws Exception note: will be ignored
     */
    protected closed(exception: any): void {
        // empty
    }
    private closedProp = true;
    isClosed(): boolean {
        return this.closedProp;
    }
    private doCloseSend(sendEnd: boolean, exception: any): void {
        if (this.closedProp) {
            return;
        }
        this.closedProp = true;
        try {
            try {
                this.closed(exception);
                if (sendEnd) {
                    this.connection.write(Packet.END);
                }
            } finally {
                this.connection.closed();
            }
        } catch (ignore) {
            // empty
        }
    }
    static doClose(session: Session, exception: any): void {
        session.doCloseSend(false, exception);
    }
    /**
     * Closes the session.
     * This method is idempotent.
     */
    close(): void {
        this.doCloseSend(true, null);
    }
    private serverInvoke(requestNumber: number, request: Request): void {
        const invocation = this.serverProp.invocation(request);
        const reply = invocation.invoke();
        if (!invocation.methodMapping.oneWay) {
            this.connection.write(new Packet(requestNumber, reply));
        }
    }
    private requestNumber2rpc: Rpc[] = [];
    private received(packet: Packet): void {
        try {
            if (packet.isEnd()) {
                this.doCloseSend(false, null);
                return;
            }
            const message = packet.message;
            if (message instanceof Request) {
                this.serverInvoke(packet.requestNumber, message);
            } else { // Reply
                const rpc = this.requestNumber2rpc[packet.requestNumber];
                delete this.requestNumber2rpc[packet.requestNumber];
                rpc.settle(<Reply>message);
            }
        } catch (exception) {
            Session.doClose(this, exception);
            throw exception;
        }
    }
    static received(session: Session, packet: Packet) {
        session.received(packet);
    }
    private requestNumber = Packet.END_REQUESTNUMBER;
    protected invoke(invocation: ClientInvocation): Promise<any> {
        if (this.isClosed()) {
            throw new Error("session is already closed or not yet opened");
        }
        return invocation.invoke((request, rpc) => {
            try {
                if (this.requestNumber === 2147483647) {
                    this.requestNumber = Packet.END_REQUESTNUMBER;
                }
                this.requestNumber++;
                if (rpc) {
                    if (this.requestNumber2rpc[this.requestNumber]) {
                        throw new Error("already waiting for requestNumber " + this.requestNumber);
                    }
                    this.requestNumber2rpc[this.requestNumber] = rpc;
                }
                this.connection.write(new Packet(this.requestNumber, request));
            } catch (exception) {
                Session.doClose(this, exception);
                throw exception;
            }
        });
    }
    private created(): void {
        this.closedProp = false;
        try {
            this.serverProp = this.server();
            this.opened();
        } catch (ignore) {
            Session.doClose(this, ignore);
        }
    }
    static create(sessionFactory: SessionFactory, connection: Connection): Session {
        const session = sessionFactory(connection);
        session.created();
        return session;
    }
}

export function writeTo(serializer: Serializer, value: any): Uint8Array {
    const writer = new Writer(128);
    serializer.write(value, writer);
    return writer.getArray();
}

export function readFrom(serializer: Serializer, arrayBuffer: ArrayBuffer): any {
    const reader = new Reader(arrayBuffer);
    const value = serializer.read(reader);
    if (!reader.isEmpty()) {
        throw new Error("reader is not empty");
    }
    return value;
}

export function connect(url: string, serializer: Serializer, sessionFactory: SessionFactory): void {
    serializer = new PacketSerializer(new MessageSerializer(serializer));
    const ws = new WebSocket(url);
    ws.binaryType = "arraybuffer";
    ws.onerror = () => {
        throw new Error("WebSocket.onerror");
    };
    ws.onclose = () => {
        throw new Error("WebSocket.onclose");
    };
    ws.onopen = () => {
        try {
            const session = Session.create(sessionFactory, {
                write: packet => ws.send(writeTo(serializer, packet)),
                closed: () => ws.close()
            });
            ws.onmessage = event => {
                try {
                    Session.received(session, readFrom(serializer, event.data));
                } catch (ignore) {
                    Session.doClose(session, ignore);
                }
            };
            ws.onerror = () => Session.doClose(session, new Error("WebSocket.onerror"));
            ws.onclose = event => {
                if (event.wasClean) {
                    session.close();
                } else {
                    Session.doClose(session, new Error("WebSocket.onclose"));
                }
            }
        } catch (exception) {
            ws.close();
            throw exception;
        }

    };
}

class XhrClient extends Client {
    private serializer: Serializer;
    constructor(private url: string, serializer: Serializer) {
        super();
        this.serializer = new MessageSerializer(serializer);
    }
    protected invoke(invocation: ClientInvocation): Promise<any> {
        return invocation.invoke((request, rpc) => {
            if (!rpc) {
                throw new Error("xhr not allowed for oneWay method (serviceId " + request.serviceId + ", methodId " + request.methodId + ")");
            }
            const xhr = new XMLHttpRequest();
            xhr.open("POST", this.url);
            xhr.responseType = "arraybuffer";
            xhr.onerror = () => rpc.settle(new ExceptionReply(new Error("XMLHttpRequest.onerror")));
            xhr.onload = () => {
                try {
                    rpc.settle(readFrom(this.serializer, xhr.response));
                } catch (exception) {
                    rpc.settle(new ExceptionReply(exception));
                }
            };
            xhr.send(writeTo(this.serializer, request));
        });
    }
}

export function xhr(url: string, serializer: Serializer): Client {
    return new XhrClient(url, serializer);
}

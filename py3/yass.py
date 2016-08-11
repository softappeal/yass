import inspect
from collections import OrderedDict
from struct import Struct
from typing import cast, Any, Dict, List, TypeVar, Generic, Optional, Callable


def abstract(abstractClass):
    def newDecorator(abstractNew):
        def decoratedNew(subClass, *args, **kwargs):
            if abstractClass is subClass:
                raise RuntimeError("can't instantiate abstract %s" % abstractClass)
            return abstractNew(subClass) if inspect.isbuiltin(abstractNew) else abstractNew(subClass, *args, **kwargs)

        return decoratedNew

    abstractClass.__new__ = newDecorator(abstractClass.__new__)
    return abstractClass


DOUBLE_STRUCT = Struct('>d')
INT_STRUCT = Struct('>i')


class Writer:
    def __init__(self, writeBytes: Callable[[bytes], None]) -> None:
        self.writeBytes = writeBytes

    def writeByte(self, value: int) -> None:
        self.writeBytes(bytes([value]))

    def writeVarInt(self, value: int) -> None:
        while True:
            if (value & 0xffffff80) == 0:
                self.writeByte(value)
                return
            self.writeByte((value & 0x7f) | 0x80)
            value = (value if value >= 0 else (value + 0x100000000)) >> 7

    def writeZigZagInt(self, value: int) -> None:
        self.writeVarInt((value << 1) ^ (value >> 31))

    def writeInt(self, value: int) -> None:
        self.writeBytes(INT_STRUCT.pack(value))

    def writeDouble(self, value: float) -> None:
        self.writeBytes(DOUBLE_STRUCT.pack(value))


class Reader:
    def __init__(self, readBytes: Callable[[int], bytes]) -> None:
        self.readBytes = readBytes

    def readByte(self) -> int:
        return self.readBytes(1)[0]

    def readVarInt(self) -> int:
        shift = 0
        value = 0
        while shift < 32:
            b = self.readByte()
            value |= ((b & 0x7f) << shift)
            if (b & 0x80) == 0:
                return value if value <= 0x7fffffff else value - 0x100000000
            shift += 7
        raise RuntimeError("malformed VarInt input")

    def readZigZagInt(self) -> int:
        value = self.readVarInt()
        return ((value if value >= 0 else (value + 0x100000000)) >> 1) ^ -(value & 1)

    def readInt(self) -> int:
        return INT_STRUCT.unpack(self.readBytes(4))[0]

    def readDouble(self) -> float:
        return DOUBLE_STRUCT.unpack(self.readBytes(8))[0]


@abstract
class TypeHandler:
    def read(self, input: 'Input') -> Optional[Any]:
        raise NotImplementedError()

    def write(self, value: Optional[Any], output: 'Output') -> None:
        raise NotImplementedError()

    def writeWithId(self, id: int, value: Optional[Any], output: 'Output') -> None:
        output.writer.writeVarInt(id)
        self.write(value, output)


class TypeDesc:
    def __init__(self, id: int, handler: TypeHandler) -> None:
        self.id = id
        self.handler = handler

    def write(self, value: Optional[Any], output: 'Output') -> None:
        self.handler.writeWithId(self.id, value, output)


TYPE_DESC = "TYPE_DESC"


def typeDesc(value: Any) -> TypeDesc:
    return getattr(value, TYPE_DESC)


class Input:
    def __init__(self, reader: Reader, id2typeHandler: Dict[int, TypeHandler]) -> None:
        self.reader = reader
        self.id2typeHandler = id2typeHandler
        self.referenceableObjects = None  # type: Optional[List[Any]]

    def read(self) -> Optional[Any]:
        return self.id2typeHandler[self.reader.readVarInt()].read(self)


class Output:
    def __init__(self, writer: Writer) -> None:
        self.writer = writer
        self.object2reference = None  # type: Optional[Dict[Any, int]]

    def write(self, value: Optional[Any]) -> None:
        if value is None:
            NULL_DESC.write(None, self)
        elif isinstance(value, list):
            LIST_DESC.write(value, self)
        elif isinstance(value, bool):
            BOOLEAN_DESC.write(value, self)
        elif isinstance(value, float):
            DOUBLE_DESC.write(value, self)
        elif isinstance(value, str):
            STRING_DESC.write(value, self)
        elif isinstance(value, bytes):
            BYTES_DESC.write(value, self)
        else:
            try:
                td = typeDesc(value)
            except:
                raise RuntimeError("missing type %s in serializer" % value.__class__)
            td.write(value, self)


class NullTypeHandler(TypeHandler):
    def read(self, input: Input) -> None:
        return None

    def write(self, value: None, output: Output) -> None:
        pass


class ReferenceTypeHandler(TypeHandler):
    def read(self, input: Input) -> Any:
        return input.referenceableObjects[input.reader.readVarInt()]

    def write(self, value: int, output: Output) -> None:
        output.writer.writeVarInt(value)


class ListTypeHandler(TypeHandler):
    def read(self, input: Input) -> List[Any]:
        return [input.read() for dummy in range(input.reader.readVarInt())]

    def write(self, value: List[Any], output: Output) -> None:
        output.writer.writeVarInt(len(value))
        for e in value:
            output.write(e)


@abstract
class BaseTypeHandler(TypeHandler):
    def readBase(self, reader: Reader) -> Any:
        raise NotImplementedError()

    def read(self, input: Input) -> Any:
        return self.readBase(input.reader)

    def writeBase(self, value: Any, writer: Writer) -> None:
        raise NotImplementedError()

    def write(self, value: Any, output: Output) -> None:
        self.writeBase(value, output.writer)


class BooleanTypeHandler(BaseTypeHandler):
    def readBase(self, reader: Reader) -> bool:
        return reader.readByte() != 0

    def writeBase(self, value: bool, writer: Writer) -> None:
        writer.writeByte(1 if value else 0)


class DoubleTypeHandler(BaseTypeHandler):
    def readBase(self, reader: Reader) -> float:
        return reader.readDouble()

    def writeBase(self, value: float, writer: Writer) -> None:
        writer.writeDouble(value)


class StringTypeHandler(BaseTypeHandler):
    def readBase(self, reader: Reader) -> str:
        return reader.readBytes(reader.readVarInt()).decode()

    def writeBase(self, value: str, writer: Writer) -> None:
        b = value.encode()
        writer.writeVarInt(len(b))
        writer.writeBytes(b)


class BytesTypeHandler(BaseTypeHandler):
    def readBase(self, reader: Reader) -> bytes:
        return reader.readBytes(reader.readVarInt())

    def writeBase(self, value: bytes, writer: Writer) -> None:
        writer.writeVarInt(len(value))
        writer.writeBytes(value)


NULL_DESC = TypeDesc(0, NullTypeHandler())
REFERENCE_DESC = TypeDesc(1, ReferenceTypeHandler())
LIST_DESC = TypeDesc(2, ListTypeHandler())
BOOLEAN_DESC = TypeDesc(3, BooleanTypeHandler())
DOUBLE_DESC = TypeDesc(4, DoubleTypeHandler())
STRING_DESC = TypeDesc(5, StringTypeHandler())
BYTES_DESC = TypeDesc(6, BytesTypeHandler())

FIRST_DESC_ID = 7


class EnumTypeHandler(BaseTypeHandler):
    def __init__(self, type: Any) -> None:
        self.type = type

    def readBase(self, reader: Reader) -> Any:
        return self.type(reader.readVarInt())

    def writeBase(self, value: Any, writer: Writer) -> None:
        writer.writeVarInt(value.value)


def enumDesc(id: int, type: Any) -> None:
    setattr(type, TYPE_DESC, TypeDesc(id, EnumTypeHandler(type)))


class FieldHandler:
    def __init__(self, field: str, typeHandler: Optional[TypeHandler]) -> None:
        self.field = field
        self.typeHandler = typeHandler

    def read(self, object: Any, input: Input) -> None:
        setattr(object, self.field, input.read() if self.typeHandler is None else self.typeHandler.read(input))

    def write(self, id: int, object: Any, output: Output):
        value = getattr(object, self.field)
        if value is not None:
            output.writer.writeVarInt(id)
            if self.typeHandler is None:
                output.write(value)
            else:
                self.typeHandler.write(value, output)


class FieldDesc:
    def __init__(self, id: int, field: str, typeInfo: Optional[Any]) -> None:
        self.id = id
        if typeInfo is None:
            self.handler = FieldHandler(field, None)
        elif isinstance(typeInfo, TypeDesc):
            self.handler = FieldHandler(field, typeInfo.handler)
        else:
            self.handler = FieldHandler(field, typeDesc(typeInfo).handler)


class ClassTypeHandler(TypeHandler):
    FIELD_END = 0

    def __init__(self, type: Any, referenceable: bool) -> None:
        self.type = type
        self.referenceable = referenceable
        self.id2fieldHandler = OrderedDict()  # type: Dict[int, FieldHandler]

    def addField(self, id: int, handler: FieldHandler) -> None:
        self.id2fieldHandler[id] = handler

    def read(self, input: Input) -> None:
        object = self.type()
        if self.referenceable:
            if input.referenceableObjects is None:
                input.referenceableObjects = []
            input.referenceableObjects.append(object)
        while True:
            id = input.reader.readVarInt()
            if id == ClassTypeHandler.FIELD_END:
                return object
            self.id2fieldHandler[id].read(object, input)

    def write(self, value: Any, output: Output) -> None:
        for id, handler in self.id2fieldHandler.items():
            handler.write(id, value, output)
        output.writer.writeVarInt(ClassTypeHandler.FIELD_END)

    def writeWithId(self, id: int, value: Any, output: Output) -> None:
        if self.referenceable:
            if output.object2reference is None:
                output.object2reference = {}
            object2reference = output.object2reference
            reference = object2reference.get(value)
            if reference is not None:
                REFERENCE_DESC.write(reference, output)
                return
            object2reference[value] = len(object2reference)
        super().writeWithId(id, value, output)


def fieldDescs(type: Any, fieldDescs: List[FieldDesc]) -> None:
    handler = cast(ClassTypeHandler, typeDesc(type).handler)
    for fieldDesc in fieldDescs:
        handler.addField(fieldDesc.id, fieldDesc.handler)


def classDesc(id: int, type: Any, referenceable: bool) -> None:
    setattr(type, TYPE_DESC, TypeDesc(id, ClassTypeHandler(type, referenceable)))


@abstract
class Serializer:
    def read(self, reader: Reader) -> Optional[Any]:
        raise NotImplementedError()

    def write(self, value: Optional[Any], writer: Writer) -> None:
        raise NotImplementedError()


class FastSerializer(Serializer):
    def __init__(self, typeInfos: List[Any]) -> None:
        self.id2typeHandler = {}  # type: Dict[int, TypeHandler]
        for typeInfo in [NULL_DESC, REFERENCE_DESC, LIST_DESC, BOOLEAN_DESC, DOUBLE_DESC, STRING_DESC, BYTES_DESC] + typeInfos:
            td = typeInfo if isinstance(typeInfo, TypeDesc) else typeDesc(typeInfo)
            self.id2typeHandler[td.id] = td.handler

    def read(self, reader: Reader) -> Optional[Any]:
        return Input(reader, self.id2typeHandler).read()

    def write(self, value: Optional[Any], writer: Writer) -> None:
        Output(writer).write(value)


@abstract
class Message:
    pass


class Request(Message):
    def __init__(self, serviceId: int, methodId: int, arguments: List[Optional[Any]]) -> None:
        self.serviceId = serviceId
        self.methodId = methodId
        self.arguments = arguments


@abstract
class Reply(Message):
    def process(self) -> Optional[Any]:
        raise NotImplementedError()


class ValueReply(Reply):
    def __init__(self, value: Optional[Any]) -> None:
        super().__init__()
        self.value = value

    def process(self) -> Optional[Any]:
        return self.value


class ExceptionReply(Reply):
    def __init__(self, exception: Exception) -> None:
        super().__init__()
        self.exception = exception

    def process(self) -> None:
        raise self.exception


class MethodMapping:
    def __init__(self, id: int, method: str, oneWay: bool) -> None:
        self.id = id
        self.method = method
        self.oneWay = oneWay


class MethodMapper:
    def __init__(self, mappings: List[MethodMapping]) -> None:
        self.id2mapping = {}  # type: Dict[int, MethodMapping]
        self.method2Mapping = {}  # type: Dict[str, MethodMapping]
        for mapping in mappings:
            self.id2mapping[mapping.id] = mapping
            self.method2Mapping[mapping.method] = mapping

    def mapId(self, id: int) -> Optional[MethodMapping]:
        return self.id2mapping.get(id)

    def mapMethod(self, method: str) -> MethodMapping:
        return self.method2Mapping.get(method)


Invocation = Callable[[], Optional[Any]]
Interceptor = Callable[
    [MethodMapping, List[Optional[Any]], Invocation],  # mapping, arguments, invocation
    Optional[Any]
]


def directInterceptor(mapping: MethodMapping, arguments: List[Optional[Any]], invocation: Invocation) -> Optional[Any]:
    return invocation()


class Service:
    def __init__(self, contractId: 'ContractId[Any]', implementation: Any, interceptor: Interceptor) -> None:
        self.id = contractId.id
        self.implementation = implementation
        self.interceptor = interceptor
        self.mapper = contractId.mapper

    def invoke(self, request: Request) -> Reply:
        mapping = self.mapper.mapId(request.methodId)
        if mapping is None:
            raise RuntimeError("no methodId %s found for serviceId %s" % (request.methodId, request.serviceId))
        try:
            return ValueReply(self.interceptor(
                mapping,
                request.arguments,
                lambda: getattr(self.implementation, mapping.method)(*request.arguments)
            ))
        except Exception as e:
            return ExceptionReply(e)


class Server:
    def __init__(self, services: List[Service]) -> None:
        self.id2service = {}  # type: Dict[int, Service]
        for service in services:
            id = service.id
            if id in self.id2service:
                raise RuntimeError("serviceId %s already added" % id)
            self.id2service[id] = service

    def invoke(self, request: Request) -> Reply:
        service = self.id2service.get(request.serviceId)
        if service is None:
            raise RuntimeError("no serviceId %s found" % request.serviceId)
        return service.invoke(request)


C = TypeVar('C')


class ContractId(Generic[C]):
    def __init__(self, contract: Any, id: int) -> None:
        self.mapper = getattr(contract, "MAPPER")  # type: MethodMapper
        self.id = id

    def service(self, implementation: C, interceptor: Interceptor = directInterceptor) -> Service:
        return Service(self, implementation, interceptor)


@abstract
class Client:
    def proxy(self, contractId: ContractId[C], interceptor: Interceptor = directInterceptor) -> C:
        client = self

        class Proxy:
            def __getattr__(self, method: str):
                mapping = contractId.mapper.mapMethod(method)
                if mapping is None:
                    raise RuntimeError("no method '%s' found for serviceId %s" % (method, contractId.id))

                class Method:
                    def __call__(self, *arguments):
                        args = list(arguments)
                        return interceptor(
                            mapping,
                            args,
                            lambda: client.invoke(Request(contractId.id, mapping.id, args)).process()
                        )

                return Method()

        return cast(C, Proxy())

    def invoke(self, request: Request) -> Reply:
        raise NotImplementedError()


class MessageSerializer(Serializer):
    REQUEST = 0
    VALUE_REPLY = 1
    EXCEPTION_REPLY = 2

    def __init__(self, contractSerializer: Serializer) -> None:
        self.contractSerializer = contractSerializer

    def read(self, reader: Reader) -> Message:
        type = reader.readByte()
        if type == MessageSerializer.REQUEST:
            return Request(reader.readZigZagInt(), reader.readZigZagInt(), self.contractSerializer.read(reader))
        elif type == MessageSerializer.VALUE_REPLY:
            return ValueReply(self.contractSerializer.read(reader))
        else:
            return ExceptionReply(self.contractSerializer.read(reader))

    def write(self, message: Message, writer: Writer) -> None:
        if isinstance(message, Request):
            writer.writeByte(MessageSerializer.REQUEST)
            writer.writeZigZagInt(message.serviceId)
            writer.writeZigZagInt(message.methodId)
            self.contractSerializer.write(message.arguments, writer)
        elif isinstance(message, ValueReply):
            writer.writeByte(MessageSerializer.VALUE_REPLY)
            self.contractSerializer.write(message.value, writer)
        else:
            writer.writeByte(MessageSerializer.EXCEPTION_REPLY)
            self.contractSerializer.write(cast(ExceptionReply, message).exception, writer)


class SimpleTransportSetup:
    def __init__(self, messageSerializer: Serializer, server: Server) -> None:
        self.messageSerializer = messageSerializer
        self.server = server


class SimplePathResolver:
    def __init__(self, pathMappings: Dict[Any, SimpleTransportSetup]) -> None:
        self.pathMappings = pathMappings

    def resolvePath(self, path: Any) -> SimpleTransportSetup:
        setup = self.pathMappings.get(path)
        if setup is None:
            raise RuntimeError("no mapping for path '%s'" % path)
        return setup


class PathSerializer(Serializer):
    DEFAULT = 0

    def read(self, reader: Reader) -> int:
        return reader.readInt()

    def write(self, value: int, writer: Writer) -> None:
        writer.writeInt(value)


@abstract
class Stream:
    def writeBytes(self, value: bytes) -> None:
        raise NotImplementedError()

    def writeDone(self) -> None:
        pass

    def readBytes(self, length: int) -> bytes:
        raise NotImplementedError()

    def readDone(self) -> None:
        pass


class ServerTransport:
    def __init__(self, pathSerializer: Serializer, pathResolver: SimplePathResolver) -> None:
        self.pathSerializer = pathSerializer
        self.pathResolver = pathResolver

    def invoke(self, stream: Stream) -> None:
        reader = Reader(stream.readBytes)
        setup = self.pathResolver.resolvePath(self.pathSerializer.read(reader))
        request = setup.messageSerializer.read(reader)
        stream.readDone()
        setup.messageSerializer.write(setup.server.invoke(request), Writer(stream.writeBytes))
        stream.writeDone()


def defaultServerTransport(contractSerializer: Serializer, server: Server) -> ServerTransport:
    return ServerTransport(
        PathSerializer(),
        SimplePathResolver({
            PathSerializer.DEFAULT: SimpleTransportSetup(MessageSerializer(contractSerializer), server)
        })
    )


class ClientTransport:
    def __init__(self, pathSerializer: Serializer, path: Any, messageSerializer: Serializer) -> None:
        self.pathSerializer = pathSerializer
        self.path = path
        self.messageSerializer = messageSerializer

    def invoke(self, request: Request, stream: Stream) -> Reply:
        writer = Writer(stream.writeBytes)
        self.pathSerializer.write(self.path, writer)
        self.messageSerializer.write(request, writer)
        stream.writeDone()
        reply = self.messageSerializer.read(Reader(stream.readBytes))
        stream.readDone()
        return reply


def defaultClientTransport(contractSerializer: Serializer) -> ClientTransport:
    return ClientTransport(PathSerializer(), PathSerializer.DEFAULT, MessageSerializer(contractSerializer))

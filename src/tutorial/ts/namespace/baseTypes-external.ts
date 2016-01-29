// shows how to use contract external base types

/// <reference path="../../../main/ts/namespace/yass"/>

interface Integer {
    get(): number;
}

class IntegerHandler implements yass.TypeHandler<Integer> {
    read(reader: yass.Reader): Integer {
        return new IntegerImpl(reader.readZigZagInt());
    }
    write(value: Integer, writer: yass.Writer): void {
        writer.writeZigZagInt(value.get());
    }
    static TYPE_DESC = new yass.TypeDesc(yass.FIRST_ID, new IntegerHandler);
}

class IntegerImpl implements Integer {
    constructor(private value: number) {
        // note: check if value is really a Java Integer should be implemented here
    }
    get(): number {
        return this.value;
    }
    toString(): string {
        return "IntegerImpl(" + this.get() + ")";
    }
}

(<any>IntegerImpl).TYPE_DESC = IntegerHandler.TYPE_DESC;

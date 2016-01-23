/// <reference path="../../main/ts/yass"/>


// shows how to use a contract external base type

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


// shows how to use a contract internal base type

namespace contract {

    class ExpirationHandler implements yass.TypeHandler<Expiration> {
        read(reader: yass.Reader): Expiration {
            return new Expiration(
                reader.readZigZagInt(),
                reader.readZigZagInt(),
                reader.readZigZagInt()
            );
        }
        write(value: Expiration, writer: yass.Writer): void {
            writer.writeZigZagInt(value.year);
            writer.writeZigZagInt(value.month);
            writer.writeZigZagInt(value.day);
        }
    }

    export class Expiration {
        constructor(public year: number, public month: number, public day: number) {
            // empty
        }
        static TYPE_DESC = new yass.TypeDesc(yass.FIRST_ID + 1, new ExpirationHandler);
    }

}

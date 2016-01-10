// shows how to use new base types

/// <reference path="../../main/ts/yass"/>

class Integer_HANDLER implements yass.TypeHandler<Integer> {
    read(reader: yass.Reader): Integer {
        return new Integer(reader.readZigZagInt());
    }
    write(value: Integer, writer: yass.Writer): void {
        writer.writeZigZagInt(value.value);
    }
}
class Integer extends yass.Type {
    constructor(private v: number) {
        super(); // note: check if v is really a Java Integer should be implemented here
    }
    get value(): number {
        return this.v;
    }
    static TYPE_DESC = new yass.TypeDesc(yass.FIRST_ID, new Integer_HANDLER);
}

namespace contract {
    class Expiration_HANDLER implements yass.TypeHandler<Expiration> {
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
    export class Expiration extends yass.Type {
        constructor(public year: number, public month: number, public day: number) {
            super();
        }
        static TYPE_DESC = new yass.TypeDesc(yass.FIRST_ID + 1, new Expiration_HANDLER);
    }
}

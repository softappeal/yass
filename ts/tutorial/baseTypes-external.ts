// shows how to use contract external base types

import * as yass from "softappeal-yass";

export interface Integer {
    get(): number;
}

export class IntegerHandler implements yass.TypeHandler<Integer> {
    read(reader: yass.Reader): Integer {
        return new IntegerImpl(reader.readZigZagInt());
    }
    write(value: Integer, writer: yass.Writer): void {
        writer.writeZigZagInt(value.get());
    }
    static TYPE_DESC = new yass.TypeDesc(yass.FIRST_DESC_ID, new IntegerHandler);
}

export class IntegerImpl implements Integer {
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

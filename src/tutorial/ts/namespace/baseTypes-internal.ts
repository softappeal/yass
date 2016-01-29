// shows how to use contract internal base types

/// <reference path="../../../main/ts/namespace/yass"/>

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
        static TYPE_DESC = new yass.TypeDesc(yass.FIRST_ID + 1, new ExpirationHandler);
    }

    export class Expiration {
        constructor(public year: number, public month: number, public day: number) {
            // empty
        }
        static TYPE_DESC = ExpirationHandler.TYPE_DESC;
    }

}

import * as yass from "yass";
import * as contract from "./generated/contract";
import {IntegerImpl} from "./baseTypes-external";

import PriceListener = contract.impl.PriceListener; // shows how to work with namespace alias

function log(...args: any[]): void {
    console.log.apply(console, args);
}

class Logger implements yass.Interceptor<yass.SimpleInterceptorContext> {
    constructor(private readonly side: string) {
        // empty
    }
    private doLog(context: yass.SimpleInterceptorContext, kind: string, data: any): void {
        log("logger:", this.side, kind, context.id, context.methodMapping.method, data);
    }
    entry(methodMapping: yass.MethodMapping, parameters: any[]): yass.SimpleInterceptorContext {
        const context = new yass.SimpleInterceptorContext(methodMapping, parameters);
        this.doLog(context, "entry", parameters);
        return context;
    }
    exit(context: yass.SimpleInterceptorContext, result: any): void {
        this.doLog(context, "exit", result);
    }
    exception(context: yass.SimpleInterceptorContext, exception: any): void {
        this.doLog(context, "exception", exception);
    }
    resolved(context: yass.SimpleInterceptorContext): void {
        this.doLog(context, "resolved", "");
    }
}

const clientLogger = new Logger("client");
const serverLogger = new Logger("server");

class TableRow {
    bidElement: HTMLElement;
    askElement: HTMLElement;
    constructor(public readonly instrument: contract.Instrument) {
        // empty
    }
}

const tableModel: TableRow[] = [];

function createTable(): void {
    let html = "<table border='1'><thead><tr>";
    ["Id", "Name", "Bid", "Ask"].forEach(title => html += "<th>" + title + "</th>");
    html += "</tr></thead><tbody>";
    tableModel.forEach(row => {
        html += "<tr>";
        const instrument = row.instrument;
        [instrument.id.get(), instrument.name].forEach(value => html += "<td>" + value + "</td>");
        ["bid", "ask"].forEach(kind => html += "<td id='" + instrument.id.get() + ":" + kind + "'></td>");
        html += "</tr>";
    });
    document.getElementById("table")!.innerHTML = html + "</tbody></table>";
    tableModel.forEach(row => {
        const find = (kind: string) => document.getElementById(row.instrument.id.get() + ":" + kind)!;
        row.bidElement = find("bid");
        row.askElement = find("ask");
    });
}

class PriceListenerImpl implements PriceListener {
    newPrices(prices: contract.Price[]): void {
        prices.forEach(price => {
            const tableRow = tableModel[price.instrumentId.get()];
            if (price.kind === contract.PriceKind.BID) {
                tableRow.bidElement.innerHTML = price.value.get().toString();
            } else {
                tableRow.askElement.innerHTML = price.value.get().toString();
            }
        });
    }
}

class EchoServiceImpl implements contract.impl.EchoService {
    echo(value: any): any {
        return value;
    }
}

async function subscribePrices(client: yass.Client) {
    // create proxies; you can add 0..n interceptors to a proxy
    const instrumentService = client.proxy(contract.acceptor.instrumentService, clientLogger);
    const priceEngine = client.proxy(contract.acceptor.priceEngine, clientLogger);
    instrumentService.showOneWay(true, new IntegerImpl(987654));
    try {
        const instruments = await instrumentService.getInstruments();
        instruments.forEach(instrument => tableModel[instrument.id.get()] = new TableRow(instrument));
        createTable();
        await priceEngine.subscribe(instruments.map(instrument => instrument.id));
        await priceEngine.subscribe([new IntegerImpl(987654321)]);
    } catch (e) {
        log("exception caught", e)
    }
}

class Session extends yass.Session {
    constructor(connection: yass.Connection) {
        super(connection);
    }
    protected server() {
        return new yass.Server( // you can add 0..n interceptors to a service
            contract.initiator.priceListener.service(new PriceListenerImpl, serverLogger),
            contract.initiator.echoService.service(new EchoServiceImpl, serverLogger)
        );
    }
    protected opened(): void {
        log("session opened");
        subscribePrices(this);
    }
    protected closed(exception: any): void {
        log("session closed", exception);
    }
}

const hostname = location.hostname;

yass.connect(
    "ws://" + hostname + ":9090/ws",
    contract.SERIALIZER,
    connection => new Session(connection)
);

const client = yass.xhr("http://" + hostname + ":9090/xhr", contract.SERIALIZER);
const echoService = client.proxy(contract.acceptor.echoService, clientLogger);
export function echoClick() {
    echoService.echo((<any>document.getElementById("echoInput")).value).then(
        result => document.getElementById("echoOutput")!.innerHTML = result,
        error => log("echo failed:", error)
    );
}

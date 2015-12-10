/// <reference path="contract"/>

namespace tutorial {

    import Integer = contract.instrument.stock.Integer;

    function log(...args: any[]): void {
        console.log.apply(console, args);
    }

    function logger(side: string): yass.Interceptor {
        return (style, method, parameters, invocation) => {
            function doLog(kind: string, data: any): void {
                log("logger:", side, kind, yass.InvokeStyle[style], method, data);
            }
            doLog("entry", parameters);
            try {
                const result = invocation();
                doLog("exit", result);
                return result;
            } catch (e) {
                doLog("exception", e);
                throw e;
            }
        };
    }
    const clientLogger = logger("client");
    const serverLogger = logger("server");

    class TableRow {
        bidElement: HTMLElement;
        askElement: HTMLElement;
        constructor(public instrument: contract.Instrument) {
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
            [instrument.id.value, instrument.name].forEach(value => html += "<td>" + value + "</td>");
            ["bid", "ask"].forEach(kind => html += "<td id='" + instrument.id.value + ":" + kind + "'></td>");
            html += "</tr>";
        });
        document.getElementById("table").innerHTML = html + "</tbody></table>";
        tableModel.forEach(row => {
            const find = (kind: string) => document.getElementById(row.instrument.id.value + ":" + kind);
            row.bidElement = find("bid");
            row.askElement = find("ask");
        });
    }

    class PriceListenerImpl implements contract.PriceListener {
        newPrices(prices: contract.Price[]): void {
            prices.forEach(price => {
                const tableRow = tableModel[price.instrumentId.value];
                if (price.kind === contract.PriceKind.BID) {
                    tableRow.bidElement.innerHTML = price.value.value.toString();
                } else {
                    tableRow.askElement.innerHTML = price.value.value.toString();
                }
            });
        }
    }

    class EchoServiceImpl implements contract.EchoService {
        echo(value: any): any {
            return value;
        }
    }

    function subscribePrices(proxyFactory: yass.ProxyFactory): void {
        // create proxies; you can add 0..n interceptors to a proxy
        const instrumentService = proxyFactory.proxy(contract.acceptor.instrumentService, clientLogger);
        const priceEngine = proxyFactory.proxy(contract.acceptor.priceEngine, clientLogger);
        instrumentService.reload(true, new Integer(987654)); // oneWay method call
        instrumentService.getInstruments().then(instruments => {
            instruments.forEach(instrument => tableModel[instrument.id.value] = new TableRow(instrument));
            createTable();
            return priceEngine.subscribe(instruments.map(instrument => instrument.id));
        }).then(
            () => log("subscribe succeeded")
        );
        priceEngine.subscribe([new Integer(987654321)]).catch(exception => log("subscribe failed with", exception));
    }

    class Session extends yass.Session {
        private static ID = 1;
        id = Session.ID++;
        constructor(connection: yass.Connection) {
            super(connection);
        }
        protected server() {
            return yass.server( // you can add 0..n interceptors to a service
                contract.initiator.priceListener.service(new PriceListenerImpl, serverLogger),
                contract.initiator.echoService.service(new EchoServiceImpl, serverLogger)
            );
        }
        protected opened(): void {
            log("session opened", this.id);
            subscribePrices(this);
        }
        protected closed(exception: any): void {
            log("session closed", this.id, exception);
        }
    }

    const hostname = location.hostname;

    yass.connect(
        "ws://" + hostname + ":9090/tutorial",
        contract.SERIALIZER,
        connection => new Session(connection),
        () => log("connect failed")
    );

    const proxyFactory = yass.xhr("http://" + hostname + ":9090/xhr", contract.SERIALIZER);
    const echoService = proxyFactory.proxy(contract.acceptor.echoService, clientLogger);
    export function echoClick() {
        echoService.echo((<any>document.getElementById("echoInput")).value).then(
            result => document.getElementById("echoOutput").innerHTML = result,
            error => log("echo failed:", error)
        );
    }

}

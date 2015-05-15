/// <reference path="contract"/>

module tutorial {

    import Integer = contract.instrument.stock.Integer;

    function log(...args: any[]): void {
        console.log.apply(console, args);
    }

    function logger(side: string): yass.Interceptor {
        return (style, method, parameters, invocation) => {
            function doLog(kind: string, data: any): void {
                log("logger:", side, yass.SESSION ? (<Session>yass.SESSION).id : null, kind, yass.InvokeStyle[style], method, data);
            }
            doLog("entry", parameters);
            try {
                var result = invocation();
                doLog("exit", result);
                return result;
            } catch (e) {
                doLog("exception", e);
                throw e;
            }
        };
    }
    var clientLogger = logger("client");
    var serverLogger = logger("server");

    class TableRow {
        bidElement: HTMLElement;
        askElement: HTMLElement;
        constructor(public instrument: contract.Instrument) {
            // empty
        }
    }

    var tableModel: TableRow[] = [];

    function createTable(): void {
        var html = "<table border='1'><thead><tr>";
        ["Id", "Name", "Bid", "Ask"].forEach(title => html += "<th>" + title + "</th>");
        html += "</tr></thead><tbody>";
        tableModel.forEach(row => {
            html += "<tr>";
            var instrument = row.instrument;
            [instrument.id.value, instrument.name].forEach(value => html += "<td>" + value + "</td>");
            ["bid", "ask"].forEach(kind => html += "<td id='" + instrument.id.value + ":" + kind + "'></td>");
            html += "</tr>";
        });
        document.getElementById("table").innerHTML = html + "</tbody></table>";
        tableModel.forEach(row => {
            var find = (kind: string) => document.getElementById(row.instrument.id.value + ":" + kind);
            row.bidElement = find("bid");
            row.askElement = find("ask");
        });
    }

    class PriceListenerImpl implements contract.PriceListener {
        newPrices(prices: contract.Price[]): void {
            prices.forEach(price => {
                var tableRow = tableModel[price.instrumentId.value];
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
        var instrumentService: contract.instrument.InstrumentService_PROXY = proxyFactory.proxy(contract.ServerServices.InstrumentService, clientLogger);
        var priceEngine = proxyFactory.proxy(contract.ServerServices.PriceEngine, clientLogger);
        instrumentService.reload(true, new Integer(987654)); // oneway method call
        instrumentService.getInstruments().then(instruments => {
            instruments.forEach(instrument => tableModel[instrument.id.value] = new TableRow(instrument));
            createTable();
            return priceEngine.subscribe(instruments.map(instrument => instrument.id));
        }).then(
            () => log("subscribe succeeded")
        );
        priceEngine.subscribe([new Integer(987654321)]).catch(exception => log("subscribe failed with", exception));
    }

    class Session implements yass.Session {
        private static ID = 1;
        id = Session.ID++;
        constructor(private sessionClient: yass.SessionClient) {
            // empty
        }
        opened(): void {
            log("session opened", this.id);
            subscribePrices(this.sessionClient);
        }
        closed(exception: any): void {
            log("session closed", this.id, exception);
        }
    }

    yass.connect(
        "ws://localhost:9090/tutorial",
        contract.SERIALIZER,
        yass.server( // you can add 0..n interceptors to a service
            new yass.Service(contract.ClientServices.PriceListener, new PriceListenerImpl, serverLogger),
            new yass.Service(contract.ClientServices.EchoService, new EchoServiceImpl, serverLogger)
        ),
        sessionClient => new Session(sessionClient),
        () => log("connect failed")
    );

    var proxyFactory = yass.xhr("http://localhost:9090/xhr", contract.SERIALIZER);
    var echoService = proxyFactory.proxy(contract.ServerServices.EchoService, clientLogger);
    export function echoClick() {
        echoService.echo((<any>document.getElementById("echoInput")).value).then(
            result => document.getElementById("echoOutput").innerHTML = result,
            error => log("echo failed:", error)
        );
    }

}

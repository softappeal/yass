module ch.softappeal.yass.tutorial { // $todo
    requires ch.softappeal.yass.generate;
    requires ch.softappeal.yass.unsupported;
    opens ch.softappeal.yass.tutorial.contract to ch.softappeal.yass.base;
    opens ch.softappeal.yass.tutorial.contract.generic to ch.softappeal.yass.base;
    opens ch.softappeal.yass.tutorial.contract.instrument to ch.softappeal.yass.base;
    opens ch.softappeal.yass.tutorial.contract.instrument.stock to ch.softappeal.yass.base;
    opens ch.softappeal.yass.tutorial.contract.instrument.stock.python to ch.softappeal.yass.base;
}

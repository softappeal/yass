declare let global: any;
declare function require(s: string): any;
global.WebSocket = require("ws");
global.window = false;

import "./test";

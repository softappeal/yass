declare let global: any;
declare function require(s: string): any;

global.WebSocket = require("ws");
global.XMLHttpRequest = require("xhr2");

import "./test";

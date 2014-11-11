var fs = require('fs');
var https = require('https');
var httpSignature = require('http-signature');

var options = {
    key: fs.readFileSync('/Users/amin/project/manta-clone/http-signature-auth/ssl/server.key'),
    cert: fs.readFileSync('/Users/amin/project/manta-clone/http-signature-auth/ssl/server.crt')
};

https.createServer(options, function (req, res) {
    for(var item in req.headers) {
        console.log(item + ": " + req.headers[item]);
    }

    var rc = 202;
    var parsed = httpSignature.parseRequest(req);
    var pub = fs.readFileSync(parsed.keyId, 'ascii');
    if (!httpSignature.verifySignature(parsed, pub))
        rc = 401;

    res.writeHead(rc);
    res.end();
}).listen(8443);
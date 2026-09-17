#!/bin/bash

PORT=9000

echo "Starting callback server on port $PORT..."

python3 - <<EOF
from http.server import BaseHTTPRequestHandler, HTTPServer

PORT = $PORT

class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        content_length = int(self.headers.get('Content-Length', 0))
        body = self.rfile.read(content_length)

        print("\\n=== CALLBACK RECEIVED ===")
        print("Path:", self.path)
        print("Headers:\\n", self.headers)
        print("Body:\\n", body.decode())

        self.send_response(200)
        self.end_headers()

    def log_message(self, format, *args):
        return  # silence default logs

print(f"Server running on 0.0.0.0:{PORT}")
HTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
EOF
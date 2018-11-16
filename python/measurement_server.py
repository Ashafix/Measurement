import sys
import os
import serial
import json
import http.server
import threading
import time
import argparse


FILENAME = 'response.json'

class UsbThread:
    def __init__(self, log=sys.stdout, usb='/dev/ttyUSB0', baud=115200):
        self.thread = threading.Thread(target=self.run, args=(log, usb, baud))
        self.thread.daemon = True
        self.thread.start()

    def run(self, log, usb, baud):
        log.write('connecting\n')
        try:
            ser = serial.Serial(usb, baud)
        except IOError as e:
            log.write(str(e))
            return
        log.write('connected\n')
        while True:
            resp = json.loads(ser.readline().decode('ascii'))
            resp['timestamp'] = time.time()
            with open(FILENAME, 'w') as f:
                json.dump(resp, f)

class MeasurementServer(http.server.BaseHTTPRequestHandler):
    def do_GET(self):

        if os.path.isfile(FILENAME):
            self.send_response(200)
            self.send_header('Content-type', 'text/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()

            with open(FILENAME, 'r') as f:
                self.wfile.write(bytes(f.read(), 'utf8'))
        else:
            self.send_response(404)


def parse_args(args):
    parser = argparse.ArgumentParser()
    parser.add_argument('-p', '--port', type=int, help='The port at which the server runs, default: 8080', default=8080)
    parser.add_argument('-b', '--baud', type=int, help='The baud rate for the bluetooth/USB connection, default: 115200', default=115200)
    parser.add_argument('-u', '--usb', type=str, help='The name of the USB connection', default='/dev/ttyUSB0')
    return parser.parse_args(args)


def main(args):
    args = parse_args(args)
    x = UsbThread(usb=args.usb, baud=args.baud)
    print('starting server')
    server_address = ('', args.port)
    httpd = http.server.HTTPServer(server_address, MeasurementServer)
    print('running server...')
    httpd.serve_forever()

if __name__ == '__main__':
    main(sys.argv[1:])
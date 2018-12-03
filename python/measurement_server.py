import sys
import os
import json
import http.server
import argparse
import urllib
import threading
from InputThreads import UsbThread, BluetoothThread, FILENAME

try:
    import bluetooth
except ImportError:
    sys.stderr.write('bluetooth module could not be imported\n')


comm_thread = None


content_type = {'html': 'text/html',
                'css': 'text/css',
                'js': 'application/javascript',
                'ico': 'image/x-icon'}

class MeasurementServer(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '' or self.path == '/':
            self.path = '/www/index.html'

        if (self.path.endswith(('.js', '.css', '.html')) and self.path.startswith('/www')) \
                or self.path == '/favicon.ico':
            filename = self.path
            if self.path == '/favicon.ico':
                filename = '/www' + filename
            if not os.path.isfile(filename):

                filename = os.path.join(os.getcwd(), '..', 'app/src/main/assets', filename[1:])
                if not os.path.isfile(filename):

                    self.send_response(404)
                    return

            with open(filename, 'rb') as f:
                self.send_response(200)
                self.send_header('Content-type', content_type.get(self.path.split('.')[-1]))
                self.send_header('Access-Control-Allow-Origin', '*')
                self.end_headers()
                self.wfile.write(f.read())

        elif self.path.startswith('/api/button/'):
            self.send_response(200)
            self.send_header('Content-type', 'text/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()

            output = 'button: {}\n'.format(self.path.split('/')[-1])
            comm_thread.sock.send(output)
            self.wfile.write(bytes(json.dumps({'button': True}), encoding='ascii'))

        elif self.path.startswith('/api/measurement') and os.path.isfile(FILENAME):
            self.send_response(200)
            self.set_header()
            self.end_headers()

            with open(FILENAME, 'r') as f:
                self.wfile.write(bytes(f.read(), 'utf8'))

        else:
            self.send_response(404)




    def set_header(self):
        self.send_header('Content-type', 'text/json')
        self.send_header('Access-Control-Allow-Origin', '*')


def parse_args(args):
    parser = argparse.ArgumentParser()
    parser.add_argument('-p', '--port', type=int, help='The port at which the server runs, default: 8080',
                        default=8080)
    parser.add_argument('-b', '--baud', type=int, help='The baud rate for the bluetooth/USB connection, default: 115200',
                        default=115200)
    parser.add_argument('-u', '--usb', type=str, help='The name of the USB connection',
                        default='/dev/ttyUSB0')
    parser.add_argument('-bt', '--bluetooth', type=str, help='The MAC address of the bluetooth module')

    return parser.parse_args(args)


def main(args):
    global comm_thread
    args = parse_args(args)
    if args.bluetooth is not None:
        comm_thread = BluetoothThread()
    else:
        comm_thread = UsbThread(usb=args.usb, baud=args.baud)
    comm_thread.start()
    print('starting server')
    server_address = ('', args.port)
    httpd = http.server.HTTPServer(server_address, MeasurementServer)
    print('running server...')
    httpd.serve_forever()


if __name__ == '__main__':
    main(sys.argv[1:])

import sys
import serial
import time
import threading
import random
import json

try:
    import bluetooth
except ImportError:
    sys.stderr.write('bluetooth module could not be imported\n')

FILENAME = 'response.json'


class UsbThread:
    def __init__(self, usb='/dev/ttyUSB0', baud=115200, log=sys.stderr):
        self.output = b''
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
                json.dump({'abc': random.random()}, f)
            if len(self.output) > 0:
                sys.stderr.write(str(self.sock.send(self.output)))
                sys.stderr.write(str(self.output))
                self.output = b''


class BluetoothThread(threading.Thread):

    DELIM = b'|^~\\'

    def __init__(self, log=sys.stderr, group=None, target=None, name=None,
                 args=(), kwargs=None, *, daemon=None):

        super().__init__(group=group, target=target, name=name,
                         daemon=daemon)
        self.args = args
        self.kwargs = kwargs
        self.mac_address = '98:D3:51:FD:96:8E'
        self.port = 1
        self.output = None
        self.log = log
        self.sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
        #self.sock.settimeout(5)
        #self.thread = threading.Thread(target=self.run, args=(log, mac_address, port))
        #self.thread.daemon = True
        #self.thread.start()

    def run(self):
        self.log.write('connecting\n')
        self.sock.connect((self.mac_address, self.port))
        self.log.write('connected\n')

        data = b''

        while True:
            data += self.sock.recv(8)
            if data.count(self.DELIM) >= 2:
                cells = data.split(self.DELIM)
                j = cells[1]
                if len(j) != 0:
                    resp = json.loads(j.decode('ascii'))
                    resp['timestamp'] = time.time()

                    with open(FILENAME, 'w') as f:
                        json.dump(resp, f)
                    data = self.DELIM.join(cells[2:])

            if self.output is not None:
                #for i in range(len(self.output)):
                #    self.sock.send(self.output[i:i + 1])
                    #time.sleep(0.05)
                self.sock.send('button: 2')
                with open('output.txt', 'w') as f:
                    f.write(self.output)
                self.output = None


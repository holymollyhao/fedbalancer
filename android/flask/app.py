import os
from datetime import datetime
from flask import Flask, request, send_from_directory
import firebase_admin
from firebase_admin import credentials
from firebase_admin import messaging
from werkzeug.utils import secure_filename

app = Flask(__name__)

cred = credentials.Certificate("realworldfl-firebase-adminsdk-lopnc-cf1f26d67f.json")
firebase_admin.initialize_app(cred)

@app.route("/")
def main_screen():
    return "<h1 style='color:red'>RealworldFL latency measurement server</h1>"


if __name__ == '__main__':
    # app.run(debug=True, host='0.0.0.0')

    # The topic name can be optionally prefixed with "/topics/".
    topic = 'latency_measurement'

    # See documentation on defining a message payload.
    message = messaging.Message(
        data={
            'msg': 'train',
        },
        topic=topic,
    )

    # Send a message to the devices subscribed to the provided topic.
    response = messaging.send(message)
    # Response is a message ID string.
    print('Successfully sent message:', response)


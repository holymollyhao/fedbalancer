import os
from datetime import datetime
from flask import Flask, request, send_from_directory
import firebase_admin
from firebase_admin import credentials
from firebase_admin import messaging
from werkzeug.utils import secure_filename

app = Flask(__name__)
app.config.from_pyfile('config.py')

cred = credentials.Certificate("realworldfl-firebase-adminsdk-lopnc-cf1f26d67f.json")
firebase_admin.initialize_app(cred)

@app.route("/")
def main_screen():
    return "<h1 style='color:red'>RealworldFL latency measurement server</h1>"

@app.route("/file/download/<path:filename>", methods=['GET', 'POST'])
def file_download_model(filename):
    print(datetime.now())
    return send_from_directory(app.config['DOWNLOAD_FOLDER'], filename) # current example: model.zip

@app.route("/file/upload/response", methods = ['POST'])
def file_upload_response():
    def allowed_file(filename):
        return '.' in filename and filename.rsplit('.', 1)[1].lower() in app.config['ALLOWED_EXTENSIONS']

    def rename_file_with_userinfo_and_timestamp(userid, username, filename):
        filename_split = filename.split('.')
        return f'{filename_split[0]}_{userid}_{username}_{datetime.now().isoformat()}.{filename_split[1]}'

    # print(request.form["file"])
    filepath = os.path.join(app.config["UPLOAD_FOLDER"], 'model.zip')
    # newFile = open(filepath, 'wb')
    # newFileByteArray = bytearray(request.form["file"], encoding='utf-8')
    # newFile.write(newFileByteArray)
    file = request.files["file"]
    file.save(filepath)
    print(datetime.now())
    print("TRAIN TIME", request.form["train_time"])

    return 'success'


if __name__ == '__main__':
    # app.run(debug=True, host='0.0.0.0')

    # The topic name can be optionally prefixed with "/topics/".

    # topic = 'latency_measurement'

    # # See documentation on defining a message payload.
    # message = messaging.Message(
    #     data={
    #         'msg': 'train',
    #     },
    #     android=messaging.AndroidConfig(
    #         priority= 'high'
    #     ),
    #     topic=topic,
    # )

    # # Send a message to the devices subscribed to the provided topic.
    # response = messaging.send(message)
    # # Response is a message ID string.
    # print('Successfully sent message:', response)

    # app.run(debug=True, host='0.0.0.0', port=45555)
    app.run(debug=True, host='0.0.0.0')


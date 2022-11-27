import os
from datetime import datetime
from sqlalchemy import *
from flask import Flask, request, send_from_directory
import firebase_admin
from firebase_admin import credentials
from firebase_admin import messaging
from werkzeug.utils import secure_filename

app = Flask(__name__)
app.config.from_pyfile('config.py')

# cred = credentials.Certificate("realworldfl-firebase-adminsdk-lopnc-cf1f26d67f.json")
# firebase_admin.initialize_app(cred)

# database = create_engine(app.config['DB_URL'], encoding = 'utf-8')
# app.database = database

# metadata = MetaData()
# User = Table('User',
#             metadata,
#             Column('userid', String(10), primary_key=True),
#             Column('username', String(50), primary_key=True),
#             Column('token', String(512), nullable=False),
#             Column('os_version', String(512), nullable=False),
#             Column('model', String(512), nullable=False),
#             Column('space', String(512), nullable=False)
#             )
# Heartbeat_Log = Table('HbLog',
#                 metadata,
#                 Column('log_idx', Integer, primary_key=True, autoincrement=True),
#                 Column('userid', String(10), nullable=False),
#                 Column('username', String(50), nullable=False),
#                 Column('filename', String(50), nullable=False),
#                 Column('size', String(50), nullable=False),
#                 Column('log_time', TIMESTAMP, nullable=False, server_default=text('CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'))
#                 )
# File = Table('File',
#             metadata,
#             Column('file_idx', Integer, primary_key=True, autoincrement=True),
#             Column('userid', String(10), nullable=False),
#             Column('username', String(50), nullable=False),
#             Column('filepath', String(512), nullable=False),
#             Column('size', String(50), nullable=False),
#             Column('log_time', TIMESTAMP, nullable=False, server_default=text('CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'))
#             )
# FileUpload_Log = Table('FileUploadLog',
#                 metadata,
#                 Column('log_idx', Integer, primary_key=True, autoincrement=True),
#                 Column('userid', String(10), nullable=False),
#                 Column('username', String(50), nullable=False),
#                 Column('filepath', String(512), nullable=False),
#                 Column('size', String(50), nullable=False),
#                 Column('log_time', TIMESTAMP, nullable=False, server_default=text('CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'))
#                 )

# metadata.create_all(database)



@app.route("/")
def main_screen():
    return "<h1 style='color:red'>RealworldFL latency measurement server</h1>"


@app.route("/file/upload/response", methods = ['POST'])
def file_upload_response():
    print(request.form)

    def allowed_file(filename):
        return '.' in filename and filename.rsplit('.', 1)[1].lower() in app.config['ALLOWED_EXTENSIONS']

    def rename_file_with_userinfo_and_timestamp(userid, username, filename):
        filename_split = filename.split('.')
        return f'{filename_split[0]}_{userid}_{username}_{datetime.now().isoformat()}.{filename_split[1]}'

    # print(request.form["file"])
    filepath = os.path.join(app.config["UPLOAD_FOLDER"], 'model.zip')
    newFile = open(filepath, 'wb')
    newFileByteArray = bytearray(request.form["file"], encoding='utf-8')
    newFile.write(newFileByteArray)

    return 'success'


if __name__ == '__main__':
    # app.run(debug=True, host='0.0.0.0')

    # The topic name can be optionally prefixed with "/topics/".
    topic = 'latency_measurement'

    # See documentation on defining a message payload.
    # for i in range(10):
    #     message = messaging.Message(
    #         data={
    #             'msg': 'train',
    #         },
    #         topic=topic,
    #     )
    #     response = messaging.send(message)
    #     # Response is a message ID string.
    #     print('Successfully sent message:', response)
    #     print('sent request!')
    #     from time import sleep
    #     sleep(30)
    app.run(debug=True, host='0.0.0.0')

    # Send a message to the devices subscribed to the provided topic.



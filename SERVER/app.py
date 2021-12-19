# -*- coding: utf-8 -*-
from flask import Flask, request,jsonify, json
from flask import redirect, url_for, send_from_directory, render_template
from waitress import serve
import json
import boto3
import base64
import time
import cv2
import math
from PIL import Image
from opencv import count_human_face_cv, save_face_mosaic_cv
from RetinaFace import count_human_face_retina, save_face_mosaic_retina
from werkzeug.exceptions import HTTPException
import pymysql
from dotenv import load_dotenv
import os

import sys
sys.path.append('/root/kickboard_helmet_project/SERVER/yolor')
sys.path.append('/root/kickboard_helmet_project/SERVER/train_model/RetinaFace-tf2')

from src.retinafacetf2.retinaface import RetinaFace

from pathlib import Path
import random
import torch
from models.experimental import attempt_load
from utils.torch_utils import select_device, time_synchronized
from utils.general import increment_path, set_logging, check_img_size, non_max_suppression, scale_coords
from utils.datasets import LoadImages
from utils.plots import plot_one_box


# from pytz import timezone
# KST = pytz.timezone('Asia/Seoul')
# print(datetime.datetime.now(KST))
load_dotenv()
app = Flask(__name__)

bucket_name = os.getenv('bucket_name')

model = attempt_load('/root/kickboard_helmet_project/SERVER/yolor/best.pt', map_location=select_device(''))
print('모델 로드 완료')
conn = pymysql.connect(
    host = os.getenv("host"),
    port = int(os.getenv("port")),
    user = os.getenv("user"),
    password = os.getenv("password"),
    db = os.getenv("database")
)
print('MySQL 연결')
# S3 호출

s3 = boto3.client('s3')
print('S3 연결')
detector = RetinaFace(False, 0.4)
face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')
print('얼굴 감지 모델 연결')

@app.route("/")
def hello():
    return "안녕하세요 !!"
  

# @app.route('/image', methods=['GET', 'POST'])
# def image(bucket = s3, bucket_name = bucket_name, face_cascade = face_cascade, detector=detector, model=model):
#     # num값 GET or POST 방식으로 받기
#     if request.method == 'GET':
#         print('GET으로 url이 왔어요!')
#         save_time = time.strftime('%y%m%d%H%M%S')
#         num = request.args["num"]
#     else:
#         print('POST로 url이 왔어요!')
#         save_time = time.strftime('%y%m%d%H%M%S')
#         num =  request.form['num']
#         rand_num = random.randrange(10000)
        
#     # 받은 문자열을 jpg 이미지로 저장
#     imgdata = base64.b64decode(num)
#     user_ip = request.environ.get('HTTP_X_REAL_IP', request.remote_addr)
#     filename = f"{user_ip}_{int(save_time)}.jpg"

#     with open("/root/kickboard_helmet_project/SERVER/static/" + filename, 'wb') as f:
#         f.write(imgdata)

#     # 탑승 인원 파악
#     # opencv version
#     # count = count_human_face(filename, face_cascade)

#     # retinaface version
#     count = count_human_face(filename, detector)

#     # 헬멧 탐지
#     img_dir = '/root/kickboard_helmet_project/SERVER/static/' + filename
#     print(img_dir)
#     detect_label = []
#     exec(open('/root/kickboard_helmet_project/SERVER/yolor/detect.py').read())
#     print('detect_label', detect_label)
#     #MySQL에 metadata저장
#     cursor = conn.cursor() 
#     cursor.execute("INSERT INTO flask2.Helmet_user (num, datetime) VALUES(%s, %s)",(rand_num,20210202))
#     result = cursor.fetchall()
#     conn.commit()
#     conn.close()

#     if count == 0:
#         return jsonify({"code" : 200,
#                 "description": '얼굴이 보이지 않아요. 카메라 각도를 조절해주세요!'
#                 })

#     else:
#         # S3에 얼굴 모자이크 처리 후 저장
#         # save_face_mosaic(filename, face_cascade, bucket, bucket_name) #opencv
#         save_face_mosaic(filename, detector, bucket, bucket_name) # retinaface

#         return jsonify({"code" : 200,
#                 "description": f"전달된 사진의 인원은 {count}명입니다. 캡처된 사진들은 s3에 모자이크 처리하여 저장되었습니다."
#                 }) 

    
@app.errorhandler(Exception)
def handle_exception(e):
    if isinstance(e, HTTPException):
        response = e.get_response()
        response.data = \
            {
            "code": e.code,
            "name": e.name,
            "description": e.description,
            "call_here" : "010-0000-0000"
        }
        response.content_type = "application/json"
        print(type(response))
        return jsonify(response)
    else:
        return "틀림"

@app.route('/register', methods=['POST'])
def register(bucket = s3, bucket_name = bucket_name, detector=detector):
    userid = request.form['userid']
    save_time = time.strftime('%Y-%m-%d %H:%M:%S')
    print(save_time)
    detect = request.form['detect']
    latitude = request.form['latitude']
    longitude = request.form['longitude']

    if int(detect) == 0:
        image = request.form['image']
        filename = f'{userid}_{save_time}.jpg'
        imgdata = base64.b64decode(image)
        img_save_path = "/root/kickboard_helmet_project/SERVER/static/" + filename
        with open(img_save_path, 'wb') as f:
            f.write(imgdata)

        save_face_mosaic_retina(filename, detector, bucket, bucket_name)
        response = '얼굴 모자이크 후 DB에 정보 저장'
    else:
        img_save_path = ''
        response = 'DB에 정보 저장'

    save_face_mosaic_retina(filename, detector, bucket, bucket_name)

    cursor = conn.cursor() 
    sql = 'INSERT INTO flask2.Helmet_user (userid, save_time, detect, latitude, longitude, img_save_path ) VALUES(%s, %s,%s, %s,%s, %s)'
    cursor.execute(sql, (userid, save_time, detect, latitude, longitude, img_save_path))
    conn.commit()
    # conn.close()
    return response


# @app.route('/register', methods=['POST'])
# def register(bucket = s3, bucket_name = bucket_name, face_cascade = face_cascade, detector=detector, model=model):
#     userid = request.form['userid']
#     save_time = time.strftime('%Y-%m-%d %H:%M:%S')
#     print(save_time)
#     detect = request.form['detect']
#     latitude = request.form['latitude']
#     longitude = request.form['longitude']

#     if int(detect) == 0:
#         image = request.form['image']
#         filename = f'{userid}_{save_time}.jpg'
#         imgdata = base64.b64decode(image)
#         img_save_path = "/root/kickboard_helmet_project/SERVER/static/" + filename
#         with open(img_save_path, 'wb') as f:
#             f.write(imgdata)
#         detect_label = []
#         exec(open('/root/kickboard_helmet_project/SERVER/yolor/detect.py').read())
#         print(detect_label)
#         if ('Helmet' in detect_label) | ('Bicycle helmet' in detect_label):
#             response = '서버에서 재 확인 결과 헬멧 감지 후 DB에 정보 저장'
#         else:
#             response = '서버에서 재 확인 결과 헬멧 감지 안됨 DB에 정보 저장'
#     else:
#         img_save_path = ''
#         response = 'DB에 정보 저장'
#     print(filename)
#     save_face_mosaic_retina(filename, detector, bucket, bucket_name)
#     # save_face_mosaic_cv(filename, face_cascade, bucket, bucket_name)
        
#     cursor = conn.cursor() 
#     sql = 'INSERT INTO flask2.Helmet_user (userid, save_time, detect, latitude, longitude, img_save_path ) VALUES(%s, %s,%s, %s,%s, %s)'
#     cursor.execute(sql, (userid, save_time, detect, latitude, longitude, img_save_path))
#     conn.commit()
#     # conn.close()
#     return response


@app.route('/checkdb', methods=['POST'])
def check():
    userid = request.form['userid']
    print(userid)
    sql = "select * from flask2.Helmet_user where userid = %s"
    # cursor = conn.cursor() 
    
    cursor = conn.cursor(pymysql.cursors.DictCursor)
    cursor.execute(sql, userid)
    user_infos = cursor.fetchall()
    conn.commit()
    # conn.close()

    distance = 0
    no_cnt = 0
    for info in user_infos:
        if int(info['detect']) == 0:
            no_cnt += 1
        try:
            distance += math.sqrt((before_lati - float(info['latitude']))**2 + (before_longi - float(info['longitude']))**2)
            before_lati, before_longi = float(info['latitude']), float(info['longitude'])
        except:
            before_lati, before_longi = float(info['latitude']), float(info['longitude'])

    print(distance)
    if (no_cnt / len(user_infos)) < 0.2:
        print(f'{(no_cnt / len(user_infos))}비율로 헬멧 착용함, 안전 운행을 위해 헬멧을 착용해주세요')
        return jsonify({"result" : 1,
                        'distance' : distance})
    else:
        print((no_cnt / len(user_infos)))
        return jsonify({'result' : 0,
                        'distance' : distance})

# @app.route('/his3')
# def dynamo_db(bucket = s3):
#     obj_list = bucket.list_objects(bucket_name)
#     print(obj_list['Contents'])


#     # now you're handling non-HTTP exceptions only
#     return render_template("500_generic.html", e=e), 500

# @app.errorhandler(HTTPException)
# def handle_exception(e):
#     """Return JSON instead of HTML for HTTP errors."""
#     # start with the correct headers and status code from the error
#     response = e.get_response()
#     # replace the body with JSON
#     response.data = json.dumps({
#         "code": e.code,
#         "description": e.description,
#     })
#     response.content_type = "application/json"
#     return responsep

serve(app, host='0.0.0.0', port=8080)
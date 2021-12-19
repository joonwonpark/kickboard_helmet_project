import sys
sys.path.append('/root/kickboard_helmet_project/SERVER/train_model/RetinaFace-tf2')
from src.retinafacetf2.retinaface import RetinaFace
import cv2
from PIL import Image
import matplotlib.pyplot as plt
import os

# bucket_name = os.getenv('bucket_name')

def count_human_face_retina(image_name, detector):
    # image
    image = cv2.imread(f'./static/{image_name}', cv2.IMREAD_COLOR)
    faces, landmarks = detector.detect(image, 0.8)

    if faces.shape[0] > 1:
        print(f'제발 그만해~~나~~~너무 무서워.....이러다그아 {faces.shape[0]}명 다아아 죽어어어~~')
        return faces.shape[0]

    elif faces.shape[0] == 1:
        print('안전 운행하세요!')
        return faces.shape[0]
            
    else:
        print('얼굴이 보이지 않아요. 카메라 각도를 조절해주세요!') 
        return 0

def save_face_mosaic_retina(image_name, detector, bucket, bucket_name):
    # image read
    image = cv2.imread(f'/root/kickboard_helmet_project/SERVER/static/{image_name}', cv2.IMREAD_COLOR)

    # face_detecting
    faces, landmarks = detector.detect(image, 0.8)

    for idx in range(faces.shape[0]):
        # print(faces[idx][:4])
        x ,y , xw, yh = map(int, faces[idx][:4])
        w = xw - x
        h = yh - y
        # print(x ,y , xw, yh)
        face_img = image[y:y + h, x:x + w]
        # 얼굴 모자이크
        small = cv2.resize(face_img, None, fx=0.3, fy=0.3, interpolation=cv2.INTER_NEAREST)

        # 얼굴 박스
        # cv2.rectangle(img, (x, y), (x + w, y + h), (255, 0, 0), 2)
        
        image[y: y + h, x: x + w] = cv2.resize(small, (w, h), interpolation=cv2.INTER_NEAREST)

    cv2.imwrite(f'/root/kickboard_helmet_project/SERVER/static/{image_name}', image)
    bucket.upload_file(f'/root/kickboard_helmet_project/SERVER/static/{image_name}', bucket_name, image_name)
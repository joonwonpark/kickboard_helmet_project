import cv2
from PIL import Image
import matplotlib.pyplot as plt
import os

# bucket_name = os.getenv('bucket_name')

def count_human_face(image_name, face_cascade):
    # image
    image = cv2.imread(f'./static/{image_name}')
    # gray scaling
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    try:
        faces = face_cascade.detectMultiScale(gray)
        if faces.shape[0] > 1:
            print(f'제발 그만해~~나~~~너무 무서워.....이러다그아 {faces.shape[0]}명 다아아 죽어어어~~')
            return faces.shape[0]

        else:
            print('안전 운행하세요!')
            return faces.shape[0]
            
    except:
        print('얼굴이 보이지 않아요. 카메라 각도를 조절해주세요!') 
        return 0

def save_face_mosaic(image_name, face_cascade, bucket, bucket_name):
    # image read
    image = cv2.imread(f'./static/{image_name}', cv2.IMREAD_COLOR)
    # gray scaling
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    # face_detecting
    faces = face_cascade.detectMultiScale(gray)

    for (x, y, w, h) in faces:
        face_img = image[y:y + h, x:x + w]
        # 얼굴 모자이크
        small = cv2.resize(face_img, None, fx=0.3, fy=0.3, interpolation=cv2.INTER_NEAREST)

        # 얼굴 박스
        # cv2.rectangle(img, (x, y), (x + w, y + h), (255, 0, 0), 2)
        
        image[y: y + h, x: x + w] = cv2.resize(small, (w, h), interpolation=cv2.INTER_NEAREST)

    cv2.imwrite(f'./static/mosaic_{image_name}', image)
    bucket.upload_file(f'./static/mosaic_{image_name}', bucket_name, image_name)

# def cut_human_face(image_name, face_cascade):
#     # image read
#     image = cv2.imread(f'./static/{image_name}', cv2.IMREAD_COLOR)
#     # gray scaling
#     gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
#     # face_detecting
#     faces = face_cascade.detectMultiScale(gray)

#     for (x, y, w, h) in faces:
#         face_img = image[y:y + h, x:x + w]
#         face_img = cv2.cvtColor(face_img, cv2.COLOR_BGR2RGB)
#         cv2.imwrite(f'./train/{image_name}', face_img)
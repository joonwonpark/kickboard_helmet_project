from pathlib import Path
import random
import time
import cv2

import torch
from models.experimental import attempt_load
from utils.torch_utils import select_device, time_synchronized
from utils.general import increment_path, set_logging, check_img_size, non_max_suppression, scale_coords
from utils.datasets import LoadImages
from utils.plots import plot_one_box

model = attempt_load('/root/final_project/yolor/best.pt', map_location=select_device(''))

def open_detect(model=model):
    # model = attempt_load('/root/final_project/yolor/best.pt')
    # model = torch.load('/root/final_project/yolor/best.pt', map_location='')
    img_dir = '/root/final_project/static/182.227.190.57_211125055651.jpg'
    exec(open('/root/final_project/yolor/detect.py').read())


open_detect()
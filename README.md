# KAIST Attendance Tracker with Identity Examination - KATIE

## Introduction
- This is the final project which we did for Mobile Computing course (CS442) in KAIST.
- KATIE is the app which is used to check attendance by using Bluetooth Low Energy (BLE) to authenticate nearby students devices and applying VarGFaceNet to verify the identities of students

## Bluetooth Low Energy (BLE)

- Our idea is creating a mass BLE network in which there is a single central device (Prof's or TA's smartphones) and multiple peripheral devices (student phones). The central device will advertise its own BLE with a random UUID to identify it. The student's device then scan all the BLE signals around. If it found the BLE signal which has the same UUID with the one stored in database then it will automatically become a peripheral device that also advertise the BLE with the same UUID.

![](https://i.imgur.com/R4PgqWL.png)


## VarGFaceNet

- Four main stages of the pipeline are: 
    - **Face detection:** Detect the face bounding box and face rotation angle. Here, we reuse the Android Mobile Vision API of Google, from which we get the bounding box and the face angle.
    - **Face alignment:** Resize and re-align the face to a fixed 3x112x112 image
    - **Feature extraction:** Map from the aligned-face image to 512 floats
    - **Feature matching:** Compare between two face features).


![](https://i.imgur.com/ZMO2Ue3.png)


## Images from the application

<img width = "290" src="https://i.imgur.com/gCkSqR7.jpg"> <img width = "290" src="https://i.imgur.com/LTtQh7N.jpg"> <img width = "290" src="https://i.imgur.com/JHQt0Lw.jpg"> <img width = "290" src="https://i.imgur.com/XUZZN77.jpg"> <img width="290" src="https://i.imgur.com/ewXOUTq.png">




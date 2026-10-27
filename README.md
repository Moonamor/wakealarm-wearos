# WakeAlarm (Wear OS)
خادم HTTP على الساعة. أرسل: http://<IP-الساعة>:8080/alert?value=WAKE
فتهتز الساعة بلا توقف حتى تضغط زر "إيقاف".

التثبيت: فعّل خيارات المطوّر على الساعة ← ADB debugging + Debug over Wi-Fi، ثم:
adb connect <IP>:5555
adb install app-debug.apk

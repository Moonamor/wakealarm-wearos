# WakeAlarm (Wear OS) — v2 عبر FCM

تطبيق لساعة Galaxy Watch Ultra: أرسل طلباً من أي مكان في العالم فتهتز الساعة بقوة وتعرض رسالتك حتى تضغط «إلغاء التنبيه».

```
المتصفح / curl / أي سكربت  ──►  Netlify Function (/alert)  ──►  Firebase Cloud Messaging  ──►  الساعة
```

لا خادم على الساعة، لا Wi-Fi محلي، لا polling. يعمل في البيت والشغل والأماكن العامة ما دامت الساعة متصلة بالإنترنت (عبر الجوال بالبلوتوث، أو Wi-Fi، أو LTE).

## الاستخدام اليومي

```
GET  https://<site>.netlify.app/alert?key=<WAKE_KEY>&msg=اصحى الاجتماع بعد 10 دقائق
POST https://<site>.netlify.app/alert   (header: x-key)   body: {"msg":"..."}
```

أو افتح `https://<site>.netlify.app` — صفحة بسيطة فيها خانة الرسالة وزر إرسال.

## الإعداد (مرة واحدة)

### 1. Firebase
1. [console.firebase.google.com](https://console.firebase.google.com) ← مشروع جديد (أي اسم).
2. أضف تطبيق Android بالحزمة `com.example.wakealarm` ← حمّل **google-services.json**.
3. إعدادات المشروع ← Service accounts ← **Generate new private key** ← حمّل ملف JSON.

### 2. Netlify
1. New site from Git ← اختر هذا الـ Repo (الإعدادات تُقرأ من `netlify.toml`).
2. Site configuration ← Environment variables:
   - `WAKE_KEY` = مفتاح سري طويل من اختيارك (مثل 32 حرفاً عشوائياً).
   - `FIREBASE_SERVICE_ACCOUNT` = **محتوى** ملف Service Account JSON كاملاً.
3. انشر. عنوان الموقع هو `WAKE_BACKEND_URL`.

### 3. GitHub (لبناء الـ APK)
Settings ← Secrets and variables ← Actions:
- Secret `GOOGLE_SERVICES_JSON` = محتوى ملف google-services.json كاملاً.
- Secret `WAKE_KEY` = نفس المفتاح المستخدم في Netlify.
- Variable `WAKE_BACKEND_URL` = `https://<site>.netlify.app`.

ثم Actions ← Build APK ← Run workflow ← حمّل `WakeAlarm-apk` من Artifacts.

### 4. الساعة
```
adb connect <IP>:5555
adb install -r app-debug.apk
```
افتح التطبيق مرة واحدة؛ سيسجّل توكن FCM في الباك إند تلقائياً وتظهر «جاهز للاستقبال».

## ملفات المشروع
- `app/` — تطبيق Wear OS (Kotlin + Compose for Wear).
- `netlify/functions/alert.mjs` — يستقبل طلبك ويرسل Push.
- `netlify/functions/register.mjs` — تستدعيه الساعة لتسجيل توكنها (Netlify Blobs).
- `public/index.html` — صفحة الإرسال.

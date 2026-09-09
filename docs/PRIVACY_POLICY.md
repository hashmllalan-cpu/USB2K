# سياسة الخصوصية / Privacy Policy

> **آخر تحديث:** 9 سبتمبر 2026 / **Last updated:** September 9, 2026

---

## اللغة العربية (Arabic)

### 1. نظرة عامة
تطبيق **USB Media Explorer** مصمم ليعمل بشكل مستقل وخاص تمامًا دون اتصال بالإنترنت (**Offline-First**). نحن نحترم خصوصيتك لأقصى درجة، ولا نقوم بجمع أو تسجيل أو نقل أي بيانات شخصية أو معلومات تعريفية عن المستخدمين.

### 2. الصلاحيات واستخدام البيانات
* **الوصول إلى الوسائط والتخزين:**
  * يطلب التطبيق صلاحيات قراءة الوسائط المحددة (`READ_MEDIA_VIDEO`, `READ_MEDIA_IMAGES`, `READ_MEDIA_AUDIO`) على أندرويد 13+، أو `READ_EXTERNAL_STORAGE` على الإصدارات الأقدم، فقط لعرض ملفاتك وتشغيلها محليًا.
  * لوحدات USB الخارجية وبطاقات الذاكرة، يعتمد التطبيق حصريًا على إطار عمل وصول المستندات (**Storage Access Framework - SAF**) لتفويض الوصول إلى المجلدات المطلوبة صراحةً دون الحاجة لصلاحية إدارة جميع الملفات (`MANAGE_EXTERNAL_STORAGE`).
* **الإشعارات:** تُستخدم صلاحية `POST_NOTIFICATIONS` على أندرويد 13+ حصريًا لإظهار تقدم عمليات النسخ والنقل الطويلة في الخلفية مع أزرار التحكم (إيقاف مؤقت / إلغاء).

### 3. عدم وجود خدمات تتبع أو خوادم خلفية
* التطبيق **لا يحتوي على أي أكواد تتبع أو تحليلات (No Analytics / No Telemetry)**.
* التطبيق **لا يتصل بأي خوادم خارجية أو شبكات إعلانية (No Ads / No Cloud Sync)**.
* جميع العمليات وقراءة الوسائط واستخراج المعاينات وتوليد الصور المصغرة تتم محليًا 100% داخل المعالج والذاكرة الخاصة بجهازك.

### 4. حفظ البيانات المحلية
* تُحفظ تفضيلات العرض، قائمة المفضلة، سجل المجلدات الأخيرة، ومواضع استئناف تشغيل الفيديو محليًا على جهازك في مجلد التخزين الخاص بالتطبيق (`filesDir` / DataStore).
* النسخ الاحتياطي التلقائي للنظام معطل (`allowBackup=false`) لضمان عدم تسرب سجل المشاهدة والتصفح إلى السحابة.

### 5. تقارير الأعطال
* في حال حدوث عطل غير متوقع، تُنشأ شاشة تقرير عطل محليًا في عملية مستقلة. لا يتم إرسال هذا التقرير تلقائيًا لأي خادم، ويبقى للمستخدم وحده كامل الحرية في نسخه ومشاركته يدويًا لأغراض تصحيح الأخطاء إن رغب في ذلك.

---

## English

### 1. Overview
**USB Media Explorer** is designed as a strictly offline-first Android application. We respect your privacy: we do not collect, log, track, or transmit any personal information or telemetry.

### 2. Permissions and Data Usage
* **Media and Storage Access:**
  * The app requests scoped media permissions (`READ_MEDIA_VIDEO`, `READ_MEDIA_IMAGES`, `READ_MEDIA_AUDIO`) on Android 13+ or `READ_EXTERNAL_STORAGE` on older Android versions solely to display and play your media files locally.
  * For USB drives and removable storage, the app relies exclusively on Android's **Storage Access Framework (SAF)** to gain scoped access to selected folder trees. It does not declare or request `MANAGE_EXTERNAL_STORAGE`.
* **Notifications:** `POST_NOTIFICATIONS` is used exclusively on Android 13+ to display background transfer progress with Pause/Cancel controls.

### 3. Zero Telemetry & No Remote Servers
* **No Analytics or Trackers:** The app includes no third-party tracking libraries, advertising SDKs, or analytics engines.
* **No Network Traffic:** The app makes zero network connections. All media indexing, metadata parsing, and thumbnail generation are performed entirely on-device.

### 4. Local Storage & Backups
* User preferences, favorites, recent folders, and video resume timestamps are stored strictly in private local application storage (`DataStore` / private JSON stores).
* Cloud backup is explicitly disabled (`allowBackup=false`) to ensure private viewing history is never synchronized off-device.

### 5. Crash Reporting
* If an unexpected crash occurs, an offline diagnostic report is generated in a separate process. No crash data is ever transmitted automatically; you maintain full control to copy or share the report manually if you choose.

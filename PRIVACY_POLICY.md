# Privacy Policy - Ren Browser

Last Updated: October 2, 2026


### 0. Rule 0: Disclaimer on Modified Builds, External APKs & Unofficial Forks:

- **Original Source Integrity:** This privacy policy, safety guarantees, and security features apply strictly and exclusively to the official, unmodified application binaries and source code distributed directly through this official repository ([hamzabellouch/ren-browser](https://github.com/hamzabellouch/ren-browser)) and its verified official releases.
- **Zero Liability for Modified / External Builds:** We (the publisher and maintainer) assume **no responsibility or liability whatsoever** for any modified APKs, third-party forks, repackaged binaries, unofficial distribution channels, or altered projects not originating directly from this official repository. Any installation or use of modified, tampered, or third-party builds is entirely at your own risk, and the publisher assumes no responsibility for any consequences, damages, security compromises, or data breaches that may occur.


### 1. Executive Summary & Overview:

**Ren Browser** is an open-source, privacy-first Android web browser engineered to give users complete control, security, and digital autonomy over their web browsing experience.
#### Core Privacy Commitment:
Ren Browser operates on a strict **"Zero-Tracking, Privacy-by-Default"** philosophy. Everything is executed **100% locally on your device**. We do not collect, store, transmit, profile, share, or sell any personal data, browsing history, search queries, passwords, cookies, IP addresses, or device identifiers to external analytics servers or third parties.


### 2. Information Processed On-Device:
To deliver a secure and private web browsing experience, Ren Browser processes the following information `locally on your device`:

- **Browsing History & Bookmarks:** Saved exclusively in isolated on-device application storage (SQLite / Room). Never synced with third-party cloud servers.
- **Search Queries:** Sent directly and solely to your chosen search engine (e.g., DuckDuckGo, Brave, Startpage, Google) when you initiate a search.
- **Cookies & Web Storage:** Isolated and managed per domain or cleared upon session exit.
- **Downloads & Files:** Saved strictly to your device's designated local storage only upon your explicit action.
- **QR Code Scanning & Generation:** Images and camera inputs for QR code scanning/generation are decoded directly in volatile memory (RAM) and are never stored or transmitted.
- **Local Preferences:** Minimal app configuration (such as theme, search engine choice, AdBlock rules, and Tor toggles) is stored locally on the device using Android SharedPreferences.


### 3. Permissions Used & Their Purposes:
Ren Browser requests specific Android permissions strictly to deliver its core browsing and utility features:

**A. Internet & Network State** (`INTERNET` & `ACCESS_NETWORK_STATE`)
- **Purpose:** Enables loading web pages, downloading files, verifying connectivity, and establishing encrypted circuits when routing through the Tor network.
- **Scope:** Used exclusively for web browsing and network operations initiated by the user. No background telemetry or diagnostic profiling is transmitted.

**B. Post Notifications** (`POST_NOTIFICATIONS`)
- **Purpose:** Displays ongoing download progress and active Incognito session indicators with quick-close actions on Android 13+.
- **Scope:** Used strictly for system notification alerts and foreground service indicators.

**C. Camera Permission** (`android.permission.CAMERA`)
- **Purpose:** Allows real-time QR code scanning via Android CameraX and ZXing when you open the QR Scanner tool.
- **Scope:** Used exclusively within the active scanner view. Camera frames are processed in memory and discarded immediately.

**D. Storage Access** (`READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` for legacy Android)
- **Purpose:** Allows saving downloaded files (documents, media, images) to your device's storage.
- **Scope:** Used strictly when you explicitly choose to download a file.


### 4. Privacy & Security Protection Engines:
Ren Browser integrates multiple on-device protection layers designed to shield your online identity:

- **Built-in AdBlock & Tracker Blocker:** Blocks invasive ads, trackers, analytics beacons, and malicious scripts natively before network requests are dispatched.
- **Native Tor Network Routing:** Encrypts and bounces traffic across distributed onion nodes to conceal your true IP address and evade network surveillance and censorship.
- **Anti-Fingerprinting Defense:** Spoofs canvas, audio, and device parameters to prevent cross-site digital fingerprinting.
- **Site Sandbox & Storage Isolation:** Isolates cookies and web storage per domain to prevent cross-site tracking.
- **WebRTC Protection:** Prevents real IP address leaks through WebRTC peer connections.
- **Incognito Mode:** Ephemeral browsing sessions with volatile storage that is wiped completely upon session closure.


### 5. Data Sharing, Analytics & Advertising:

- **No Data Sharing:** No personal user data, browsing activity, or device identifiers ever leave your device.
- **No Third-Party Analytics / Telemetry:** Ren Browser contains zero tracking SDKs, no Google Analytics, no Firebase Telemetry, and no crash report harvesters.
- **No Advertisements:** Ren Browser is 100% free of third-party advertisements, sponsored links, and behavioral monetization.


### 6. Data Retention & Lifecycle:

- **Volatile Memory Wiping:** Transient network responses, camera frames, and Incognito session caches are purged from RAM immediately after use.
- **Instant Data Clear:** You can permanently purge history, cookies, caches, and web storage at any time via the "Clear Data" option in Settings.
- **Complete Data Removal:** Uninstalling Ren Browser immediately and permanently deletes all local databases, preferences, and cached assets stored on your device.


### 7. Managing Permissions & User Rights:
You maintain full control over all permissions granted to Ren Browser. You may review or revoke any permission at any time via Android Settings:

**A. Disable Notifications:** `Settings > Apps > Ren Browser > Notifications > Turn Off`

**B. Manage Camera / Storage Permissions:** `Settings > Apps > Ren Browser > Permissions > Don't Allow`


### 8. Contact & Support:
If you have any questions or feedback regarding this Privacy Policy or permission usage, please contact us at:
Email: hamzabellouchcontact@gmail.com


-------------------------------------------



# سياسة الخصوصية - Ren Browser
آخر تحديث: ٢ أكتوبر ٢٠٢٦

### ٠. القاعدة رقم ٠: إخلاء المسؤولية عن التطبيقات المعدلة وحزم APK الخارجية والمشاريع المشتقة:

* **سلامة المصدر الأصلي:** تنطبق سياسة الخصوصية، وضمانات الأمان، والميزات المذكورة في هذه الوثيقة حصرياً وصراحةً على الإصدارات الرسمية غير المعدلة من التطبيق والشفرة المصدرية المنشورة مباشرة عبر هذا المستودع الأصلي المعتمد ([hamzabellouch/ren-browser](https://github.com/hamzabellouch/ren-browser)) وصفحة إصداراته الرسمية فقط.
* **عدم تحمل المسؤولية عن النسخ المعدلة أو الخارجية:** نحن (الناشر والمطور) **لا نتحمل أي مسؤولية قانونية أو أمنية أو تقنية** عن تثبيت أو استخدام أي تطبيقات معدلة، أو حزم APK خارجية، أو نسخ مُعاد تجميعها، أو مشاريع مشتقة ومعدلة لا تنتمي لهذا المستودع الأصلي. كل ما ينتج عن استخدام أو تثبيت نسخ خارجية أو معدلة (من فقدان بيانات، أو ثغرات أمنية، أو برمجيات خبيثة، أو أي أضرار ناجمة) يقع بالكامل على عاتق المستخدم وحده دون أدنى مسؤولية على الناشر.


### ١. الملخص التنفيذي والنظرة العامة:

متصفح **Ren Browser** هو متصفح ويب مفتوح المصدر ومبني على مبدأ **الخصوصية أولاً** لنظام Android، صُمم لمنح المستخدمين تحكماً كاملاً وحرية رقمية مع أعلى مستويات الأمان أثناء تصفح الإنترنت.
#### الالتزام الأساسي بالخصوصية:
يعمل Ren Browser وفق نموذج صارم هو **"الخصوصية افتراضياً وانعدام التتبع"** بنسبة **١٠٠٪ محلياً على جهازك**. نحن لا نجمع، ولا نخزن، ولا ننقل، ولا نشارك، ولا نبيع أي بيانات شخصية، أو سجلات تصفح، أو استعلامات بحث، أو كلمات مرور، أو ملفات تعريف ارتباط (Cookies)، أو عناوين IP، أو معرّفات الجهاز إلى أي خوادم تحليلات خارجية أو أطراف ثالثة.


### ٢. المعلومات التي تتم معالجتها على الجهاز:
لتقديم تجربة تصفح آمنة وسريعة، يقوم Ren Browser بمعالجة المعلومات التالية "محليًا على جهازك":

* **سجل التصفح والإشارات المرجعية:** تُخزن محلياً داخل المساحة التخزينية المعزولة للتطبيق على جهازك (SQLite / Room)، ولا تتم مزامنتها مع أي خوادم سحابية خارجية.
* **استعلامات البحث:** تُرسل مباشرة وفقط إلى محرك البحث الذي تختاره (مثل DuckDuckGo أو Brave أو Startpage أو Google) عند قيامك بالبحث.
* **ملفات تعريف الارتباط والتخزين المؤقت:** تُعزل وتُدار لكل نطاق على حدة أو تُحذف فور إغلاق الجلسة.
* **التنزيلات والملفات:** تُحفظ حصرياً في وحدة التخزين المحلية لجهازك بناءً على اختيارك وموافقتك الصريحة.
* **مسح وتوليد رموز QR:** تتم معالجة مدخلات الكاميرا والصور لفك شفرة رموز QR وتوليدها داخل الذاكرة المؤقتة (RAM) مباشرة ولا يتم حفظها أو رفعها إطلاقاً.
* **التفضيلات والإعدادات المحلية:** يتم تخزين التفضيلات البسيطة (مثل المظهر، محرك البحث المفضل، خيارات مانع الإعلانات ووضع Tor) محلياً على الجهاز عبر Android SharedPreferences.


### ٣. الأذونات المستخدمة وأغراضها:
يطلب Ren Browser أذونات محددة فقط لتقديم وظائف التصفح والأدوات المساعدة:

**أ. إذن الإنترنت وحالة الشبكة (`INTERNET` & `ACCESS_NETWORK_STATE`)**
* **الغرض:** يسمح بتحميل صفحات الويب، وتنزيل الملفات، والتحقق من حالة الاتصال، وبناء المسارات المشفرة عبر شبكة Tor.
* **النطاق:** يُستخدم حصرياً للعمليات الشبكية التي يطلبها المستخدم. لا يتم إرسال أي بيانات تتبع أو تشخيص للأداء.

**ب. إذن إرسال الإشعارات (`POST_NOTIFICATIONS`)**
* **الغرض:** يعرض شريط تقدم التنزيلات، وتنبيه الجلسة المتخفية النشطة مع زر الإغلاق السريع على نظام Android 13 فما فوق.
* **النطاق:** يُستخدم بشكل صارم لتنبيهات النظام والخدمات في الواجهة.

**ج. إذن الكاميرا (`android.permission.CAMERA`)**
* **الغرض:** يسمح بفتح كاميرا الجهاز لقراءة ومسح رموز QR في الوقت الفعلي عبر مكتبة CameraX.
* **النطاق:** يُستخدم حصرياً داخل شاشة الماسح الضوئي، ويتم التخلص من بيانات إطارات الكاميرا في الذاكرة فوراً دون حفظها.

**د. إذن التخزين والملفات (`READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` للإصدارات السابقة من Android)**
* **الغرض:** يتيح حفظ الملفات والمستندات والوسائط التي يختار المستخدم تنزيلها إلى جهازه.
* **النطاق:** يُستخدم فقط عند طلب تنزيل ملف بشكل صريح.


### ٤. محركات الأمان وحماية الخصوصية:
يحتوي Ren Browser على طبقات دفاعية متعددة تعمل محلياً لحماية هويتك وأمانك:

* **مانع الإعلانات والتتبع المدمج:** يفحص الطلبات الشبكية محلياً لمنع تحميل نطاقات الإعلانات وسكربتات التتبع السلوكي والتحليلات المزعجة.
* **التوجيه عبر شبكة Tor:** تشفير وتوجيه حركة المرور عبر عقد Onion لإخفاء عنوان IP الحقيقي وتجاوز الحجب والرقابة.
* **مكافحة البصمة الرقمية (Anti-Fingerprinting):** تمويه معايير ومخرجات المتصفح (مثل Canvas و AudioContext) لمنع تحديد هوية جهازك.
* **عزل المواقع (Site Sandbox):** فصل ملفات الكوكيز والذاكرة المؤقتة لكل موقع على حدة لمنع التتبع المشترك عبر المواقع.
* **حماية WebRTC:** حظر ومنع تسريب عنوان IP الحقيقي عبر بروتوكولات اتصالات WebRTC.
* **الوضع المتخفي (Incognito Mode):** جلسات تصفح سريعة ومؤقتة تُمسح كافة آثارها من الذاكرة تماماً فور إغلاقها.


### ٥. مشاركة البيانات والتحليلات والإعلانات:

* **عدم مشاركة البيانات:** لا تغادر أي بيانات شخصية، أو سجلات تصفح، أو معرّفات الجهاز هاتفك إطلاقاً.
* **انعدام أدوات التتبع والتحليلات:** لا يدمج Ren Browser أي حزم SDK للتحليلات أو التتبع مثل Google Analytics أو Firebase أو برمجيات Telemetry.
* **خالٍ تماماً من الإعلانات:** لا يعرض المتصفح أي إعلانات موجهة أو تجارية أو روابط ممولة.


### ٦. الاحتفاظ بالبيانات ودورة حياتها:

* **مسح البيانات المؤقتة:** يتم تفريغ استجابات الشبكة المؤقتة وإطارات الكاميرا وبيانات الجلسات المتخفية من الذاكرة (RAM) فور انتهاء معالجتها.
* **المسح الفوري للبيانات:** يمكنك حذف سجل التصفح، والكوكيز، والذاكرة المؤقتة، والملفات المحفوظة بالكامل في أي وقت بضغطة واحدة من إعدادات التطبيق ("مسح البيانات").
* **الإزالة الكاملة عند إلغاء التثبيت:** يؤدي إلغاء تثبيت Ren Browser إلى حذف جميع قواعد البيانات المحلية والتفضيلات المخزنة على الجهاز فوراً وبشكل دائم.


### ٧. إدارة الأذونات وحقوق المستخدم:
تحتفظ بالتحكم الكامل في كافة الأذونات الممنوحة لـ Ren Browser. يمكنك تعديل أو إلغاء أي إذن في أي وقت عبر إعدادات Android:

**أ. إيقاف الإشعارات:** `الإعدادات > التطبيقات > Ren Browser > الإشعارات > إيقاف`

**ب. تعديل أذونات الكاميرا / التخزين:** `الإعدادات > التطبيقات > Ren Browser > الأذونات > عدم السماح`


٨. التواصل والدعم:
إذا كانت لديك أي أسئلة أو ملاحظات بشأن سياسة الخصوصية هذه أو استخدام الأذونات، فيرجى التواصل معنا على:
البريد الإلكتروني: hamzabellouchcontact@gmail.com

# KebuzLect Mobile — Project Plan

> Android-версия KebuzLect: тот же функционал, без USB/ADB.
> Автор: Kebuz. Референс-реализация: app/desktop-prototype (Python/PyQt6).

---

## Отличие от десктопной версии

Десктоп читает фото с телефона через ADB (чужое устройство, USB-кабель).
Мобильная версия работает с собственным хранилищем устройства напрямую -
никакого ADB-слоя, никакого мастера настройки подключения, никакого
статуса authorized/unauthorized.

Это убирает целый пласт сложности десктопной версии (DeviceBackend,
AdbBackend, ShellBackend, SetupWizard) - архитектурно мобильная версия
проще в этой части.

Взамен появляется то, чего не было на десктопе: версионные развилки
Android (API 24-28 vs API 29+ vs API 33+) для доступа к файлам,
разрешений и удаления.

---

## Стек

| Слой | Технология | Аналог в десктопе |
|---|---|---|
| Язык | Kotlin | Python |
| UI | Jetpack Compose (Material 3) | PyQt6 |
| Фото - чтение | MediaStore API, группировка по bucket_id | ADB + scanner.py |
| Фото - удаление | StorageCompat (версионная развилка) | backend.delete_file() |
| PDF | android.graphics.pdf.PdfDocument | ReportLab |
| Настройки | DataStore (Preferences) | TOML config.py |
| Структурированные данные | Room | TOML albums + converted hashes |
| Папка вывода | SAF persistent URI (API 29+) / File (API 24-28) | output_folder |
| Локализация | res/values/strings.xml (RU/EN) | ui/i18n.py |

---

## Поддержка версий Android

minSdk 24 (Android 7.0), targetSdk 36.

Версионные развилки изолированы в data/storage/StorageCompat.kt:

| Функция | API 24-28 | API 29 | API 30+ |
|---|---|---|---|
| Удаление фото | ContentResolver.delete() | MediaStore.delete() + RecoverableSecurityException | createDeleteRequest() |
| Разрешение на чтение | READ_EXTERNAL_STORAGE | READ_EXTERNAL_STORAGE | READ_MEDIA_IMAGES (с API 33) |
| Запись PDF | SAF tree (DocumentsContract) | SAF tree | SAF tree |

> Правка по факту реализации (Фаза 5): запись PDF делается **единообразно через SAF**
> document tree на всех версиях — `ACTION_OPEN_DOCUMENT_TREE`/`DocumentsContract` доступны
> с API 21, поэтому отдельный File-fallback для 24-28 не нужен. Папка вывода выбирается
> один раз, persistable-permission и tree-URI сохраняются в `AlbumEntity.outputUri`.
> Сквозная нумерация лекций (`{lection_number}`) пока не реализована — токен подставляет 0
> (как fallback в десктопе); полноценная нумерация ждёт вместе с реордером, после Фазы 5.

---

## Модели данных (перенос из models.py)

```kotlin
data class Photo(
    val mediaStoreId: Long,
    val uri: Uri,
    val filename: String,
    val dateTaken: Long,
    val size: Long,
    var isBlurry: Boolean = false,
    var isDuplicate: Boolean = false,
    var rotation: Int = 0,
    var isSelected: Boolean = true
)

data class LectureGroup(
    val date: String,
    val photos: List<Photo>,
    var isConverted: Boolean = false,
    var lectureNumber: Int? = null
)
```

Album как полноценная модель не нужна в runtime-памяти как в Python -
её роль выполняет AlbumEntity (Room), привязанная к bucketId вместо
phone_path.

**Ключ группировки — дата из имени файла, не dateTaken.** Группировка
строго по дате, извлечённой из имени файла (`parseDateFromFilename`,
первый валидный `YYYYMMDD`), как в `scanner.py`. Поле `dateTaken`
(MediaStore.DATE_TAKEN) используется только для сортировки фото внутри
группы и для отображения - не как ключ группировки. Иначе расходящийся
EXIF увёл бы фото в другую группу и изменил бы `groupHash` относительно
десктопа.

---

## Конфиг (перенос из config.py + Settings)

DataStore хранит то, что было в `[settings]`:

```
defaultJpegQuality: Int = 70
defaultPdfDpi: Int = 144
defaultPhotosPerPage: Int = 2
defaultOutputFormat: String = "{predmet}_{YYYYMMDD}"
blurThreshold: Float = 100f
duplicateThreshold: Int = 10
lectureNumberWidth: Int = 3
theme: String = "light"
language: String = "ru"
```

Room хранит то, что было `[albums.*]`:

```kotlin
@Entity
data class AlbumEntity(
    @PrimaryKey val bucketId: String,
    val displayName: String,
    val outputUri: String,
    val outputFormat: String,
    val jpegQuality: Int,
    val pdfDpi: Int,
    val photosPerPage: Int
)

@Entity
data class ConvertedHashEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bucketId: String,
    val groupHash: String
)
```

groupHash вычисляется так же, как compute_group_hash в scanner.py -
md5 от отсортированного списка filename в группе.

---

## Перенос бизнес-логики (что копируется по смыслу, не построчно)

| Десктоп файл | Android аналог | Что переносится |
|---|---|---|
| scanner.py | data/scanner/AlbumScanner.kt | Извлечение даты из имени файла, группировка по дате, compute_group_hash |
| converter.py | data/pdf/PdfConverter.kt | Шаблон имени файла с токенами, раскладка 1/2 фото на A4, EXIF+rotation |
| analyzer.py | data/analyzer/PhotoAnalyzer.kt | Лапласиан для blur, pHash для дублей |
| config.py | data/settings/AppSettings.kt + Room DAO | Persistence настроек и альбомов |
| models.py | data/models/*.kt | Структуры данных |
| device/ (ADB) | — не переносится | Заменяется прямым MediaStore-доступом |
| ui/i18n.py | res/values/strings.xml + values-en/ | RU/EN строки |

---

## Архитектура экранов (адаптация под мобильный UI, не калька десктопа)

```
KebuzLect Mobile
├── Главный экран
│   ├── Список альбомов (LazyColumn)
│   ├── Добавление альбома (свой список бакетов из MediaStore, не системный picker)
│   └── Настройки (отдельный экран, не диалог)
│
├── Экран альбома
│   ├── LazyColumn групп по датам
│   │   ├── Заголовок даты + чекбокс "выбрать все"
│   │   └── LazyVerticalGrid миниатюр с чекбоксами и рамками
│   ├── Панель настроек альбома (blur/duplicate slider, jpeg quality, dpi,
│   │   photos per page) - сессионная, как на десктопе
│   └── Кнопка "Конвертировать выбранные"
│
├── Полноэкранный просмотр (оверлей, не split-screen как на десктопе)
│   ├── HorizontalPager для свайпа между всеми фото всех дат
│   ├── Дата текущего фото в углу
│   └── Кнопки поворота/удаления
│
└── Настройки
    ├── Тема (светлая/тёмная)
    ├── Язык (RU/EN)
    └── Глобальные дефолты (jpeg quality, dpi, thresholds)
```

---

## Чек-лист разработки

### Фаза 1 — Ядро
- [ ] Gradle-зависимости через version catalog (Compose, Room, DataStore)
- [ ] Модели данных (Photo, LectureGroup)
- [ ] Room entities (AlbumEntity, ConvertedHashEntity) + DAO
- [ ] DataStore-обёртка для настроек
- [ ] StorageCompat - версионные развилки (заглушки на этом этапе)
- [ ] MainActivity + NavHost с заглушкой экрана альбомов
- [ ] AndroidManifest с разрешениями под все версии

### Фаза 2 — Сканирование
- [ ] AlbumScanner: чтение MediaStore по bucketId, группировка по дате из имени файла (parseDateFromFilename)
- [ ] compute_group_hash аналог
- [ ] Экран выбора альбома (свой список бакетов из MediaStore, не системный picker)
- [ ] Список альбомов на главном экране

> Запись PDF (вывод) идёт через SAF tree-URI; bucketId используется только
> для чтения фото. Это два разных механизма, не смешивать.

### Фаза 3 — Просмотр и анализ
- [ ] Экран альбома: список групп, миниатюры
- [ ] PhotoAnalyzer: blur (Laplacian), duplicates (pHash) — анализ в корутинах вне main, результаты кешируются по mediaStoreId (иначе лаги при скролле)
- [ ] Чекбоксы выбора фото/групп
- [ ] Панель настроек альбома (сессионные слайдеры)

### Фаза 4 — Полноэкранный просмотр
- [ ] HorizontalPager, свайп через все фото
- [ ] Поворот, удаление через StorageCompat
- [ ] Дата в углу, синхронизация с списком

### Фаза 5 — Конвертация
- [ ] PdfConverter: PdfDocument, шаблон имени, 1/2 фото на страницу
- [ ] Запись через SAF/MediaStore.Downloads
- [ ] Прогресс конвертации, пометка converted в Room

### Фаза 6 — Настройки и локализация
- [ ] Экран настроек
- [ ] strings.xml (ru/en), переключатель языка
- [ ] Тёмная/светлая тема

### Фаза 7 — Полировка
- [ ] Обработка ошибок (нет доступа, файл удалён извне)
- [ ] Иконка приложения
- [ ] Подпись APK, сборка release-версии

---

## Закрытые вопросы

| Вопрос | Решение |
|---|---|
| Изменение порядка фото | Важно, переносится. Drag-and-drop внутри группы и между группами реализуется в мобильной версии (Фаза 3-4, конкретный механизм под Compose определится при реализации экрана альбома) |
| Кеш полноразмерных фото в просмотре | Не кешировать - MediaStore локальный, фото уже на устройстве, повторный доступ быстрый без сетевой задержки которая была через ADB |
| Фоновая конвертация (WorkManager) | Важная фича, но отложена на post-v1 (после первого релиза). В v1 конвертация идёт на переднем плане, пока приложение открыто |
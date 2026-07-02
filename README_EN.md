[Русский](README.md) | [English](README_EN.md)

# KebuzLect Mobile

Turns folders of lecture photos into clean PDF files - directly on your phone, no cables or computer needed.

Mobile version of [KebuzLect Desktop](https://github.com/Kebuzya/KebuzLect).

## Screenshots

| Albums                         | Album                         | Album settings                         |
|--------------------------------|-------------------------------|----------------------------------------|
| ![](screenshots/01_albums.png) | ![](screenshots/03_album.png) | ![](screenshots/04_album_settings.png) |

| Settings                         | Folder picker                         |
|----------------------------------|---------------------------------------|
| ![](screenshots/02_settings.png) | ![](screenshots/05_folder_picker.png) |

## Features

**Album management**
- Reads photo folders directly from the phone gallery - one folder per subject
- Groups photos by date automatically, one day = one lecture
- Remembers converted lectures - never repeats already done work

**Viewing and editing**
- Full-screen photo viewer - swipe through all photos across all dates
- Rotate photos directly in the viewer (clockwise and counter-clockwise)
- Delete unwanted photos from the phone directly in the app
- Reorder photos within a lecture and between dates (drag-and-drop)
- Close viewer with a swipe down

**Photo selection**
- Auto-detection of blurry shots (Laplacian algorithm)
- Auto-detection of duplicates (perceptual hash)
- Adjustable thresholds directly in the album screen - results update instantly
- Checkboxes for each photo and each date - choose what goes into the PDF

**Conversion**
- Export to PDF: 1 or 2 photos per A4 page
- Configurable JPEG quality and DPI
- Custom PDF engine with no third-party libraries - JPEG embedded directly
- Filename template with tokens ({predmet}, {YYYYMMDD}, {lection_number})

**Interface**
- Light, dark and system theme
- Russian and English UI
- Android 7.0+ support

## Installation

Download the APK from the [Releases](https://github.com/Kebuzya/KebuzLectMobile/releases) section and install it on your phone.

On first install Android may ask to allow installation from unknown sources - this is normal for APKs outside the Play Store.

## How to use

1. Open the app and tap "New album"
2. Select a folder with lecture photos from the gallery
3. The app will automatically group photos by date
4. Uncheck any photos you do not need
5. Tap "Convert" - select a folder to save the PDF

## Filename template

Configured in Settings:

| Token              | Meaning                   |
|--------------------|---------------------------|
| `{predmet}`        | Album name                |
| `{YYYYMMDD}`       | Lecture date              |
| `{lection_number}` | Sequential lecture number |

Example: `{predmet}_{YYYYMMDD}` - `Electronics_20260224.pdf`

## Build from source
git clone https://github.com/Kebuzya/KebuzLectMobile.git

Open in Android Studio, connect your phone and press Run.

Requirements: Android Studio Hedgehog or newer, JDK 11+.

## Related projects

- [KebuzLect](https://github.com/Kebuzya/KebuzLect) - Desktop version for Windows, connects to phone via USB/ADB

## License

MIT - see [LICENSE](LICENSE).
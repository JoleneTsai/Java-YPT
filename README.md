# Java-YPT Version 13

Java-YPT 是一個以 JavaFX 製作的讀書與時間規劃 App。主要功能包含每日 Timeline、Calendar、To-Do、課表 TimeTable，以及本機 JSON 資料儲存。Version 13 以 version-11 的 UI 架構為主，整合 version-12 中較完整的資料與課表改善，並修正 Timeline 疊加、To-Do 顯示、Schedule / To-Do 編輯刪除，以及 TimeTable 改名更新等問題。

---

## 功能概述

- Timeline：顯示每日行程、課程與 To-Do。
- Calendar：以月曆方式查看日期，並可進入單日行程列表。
- TimeTable：建立課表、設定學期區間、加入課程。
- To-Do：新增、顯示、編輯與刪除待辦事項。
- JSON 儲存：Schedule、To-Do、TimeTable 的新增、修改與刪除會寫回本機資料檔。

---

## Version 13 更新重點

- 修正同時間多個 Schedule 疊在一起時，舊行程看起來消失的問題。
- Timeline 中同時間的 Schedule / class 會左右分欄顯示。
- To-Do 改為小型 checkbox + 文字顯示，避免在 Timeline 中變成大色塊。
- To-Do 預設顯示在最上層；點擊 Schedule 可暫時把 Schedule 移到上層，再次點擊可讓 To-Do 回到上層。
- Calendar 單日列表中的 Schedule 可右鍵編輯或刪除。
- Timeline 中的 Schedule / To-Do 可右鍵編輯或刪除。
- 編輯 Schedule / To-Do 時會開啟原本新增頁面，並帶入原本資料。
- TimeTable 改名後會即時更新列表與主畫面標題。
- 新增完整 `timetables` JSON 格式，可儲存一整份課表。
- `JsonScheduleRepository` 可讀取舊版 To-Do 欄位，也相容 version-12 的 To-Do 欄位。
- `target/` 與本機 `schedule-data.json` 已加入 `.gitignore`，避免把編譯產物與個人資料推上 GitHub。

---

## 專案架構

```text
Java-YPT-version-13/
├── pom.xml
├── README.md
└── src/main/
    ├── java/
    │   ├── module-info.java
    │   └── com/timeapp/
    │       ├── MainApp.java
    │       ├── controller/
    │       │   └── DashboardController.java
    │       ├── domain/
    │       │   ├── model/
    │       │   ├── repository/
    │       │   └── service/
    │       ├── ui/
    │       │   └── model/
    │       └── view/
    │           ├── calendar/
    │           └── timetable/
    └── resources/
        └── com/timeapp/
            ├── css/
            └── fonts/
```

---

## 主要資料夾說明

### `controller`

負責管理畫面狀態與使用者操作流程。

主要檔案：

- `DashboardController.java`

處理目前選取日期、目前頁面、TimeTable 狀態、Schedule / To-Do / class 的新增、編輯、刪除，以及呼叫 service 儲存資料。

### `domain/model`

資料層的核心 model，定義會被儲存或處理的資料物件。

- `ScheduleData.java`：整份 JSON 資料的根物件。
- `CalendarEvent.java`：一般 Schedule 行程。
- `ToDoTask.java`：待辦事項。
- `TimetableEntry.java`：單筆課程資料。
- `TimetableData.java`：完整課表物件，包含課表名稱、學期區間與課程清單。
- `ExpandedTimetableEvent.java`：由課表展開後，顯示在特定日期 Timeline 上的課程。
- `EventType.java`、`Schedulable.java`：Timeline 資料排序與顯示用的共用型別。

### `domain/repository`

資料儲存層，負責把 Java 物件與 JSON 檔案互相轉換。

- `ScheduleRepository.java`：儲存介面。
- `JsonScheduleRepository.java`：JSON 讀取與寫入實作。

### `domain/service`

連接 controller 與資料層的服務層。

- `ScheduleService.java`：提供新增、更新、刪除、儲存資料的方法。
- `TimetableExpander.java`：將每週課表依照日期展開成 Timeline 可顯示的課程。
- `TimelineBuilder.java`：整合 Schedule 與課表事件，並依時間排序。

### `ui/model`

畫面顯示用的 JavaFX model，包含 `Property`，方便 UI 綁定與即時更新。

- `TimeEntry.java`
- `CalendarEvent.java`
- `TodoEntry.java`
- `TimetableClass.java`
- `TimetableClassRecord.java`
- `TimeTable.java`

### `view`

JavaFX 畫面元件。

- `DashboardView.java`：主畫面與頁面切換。
- `TimelinePane.java`：每日 Timeline 顯示、重疊行程排版、右鍵選單。
- `AddSchedulePane.java`：新增 / 編輯 Schedule。
- `AddTodoPane.java`：新增 / 編輯 To-Do。
- `calendar/`：Calendar 月曆與單日列表。
- `timetable/`：TimeTable 主畫面、新增課程、課表列表、新增課表。

---

## 資料儲存流程

```text
UI / Controller
      ↓
ScheduleService
      ↓
ScheduleRepository
      ↓
JsonScheduleRepository
      ↓
schedule-data.json
```

說明：

- UI 操作後會由 `DashboardController` 處理。
- `DashboardController` 呼叫 `ScheduleService`。
- `ScheduleService` 再呼叫 repository 儲存。
- `JsonScheduleRepository` 負責把資料寫入 `schedule-data.json`。

---

## JSON 資料格式

Version 13 的資料根物件為 `ScheduleData`，包含四個主要區塊：

```jsonc
{
  "calendarEvents": [],
  "todoTasks": [],
  "timetableEntries": [],
  "timetables": []
}
```

### 新增重點：`timetables`

Version 13 新增 `timetables`，讓系統可以儲存一整份課表，而不是只儲存單筆課程。

```jsonc
"timetables": [
  {
    "id": "...",
    "title": "113-2",
    "startDate": "2026-02-23",
    "endDate": "2026-06-26",
    "classes": [
      {
        "title": "Algorithm",
        "dayOfWeek": "FRIDAY",
        "startTime": "13:00",
        "endTime": "15:00",
        "room": "Room 65304",
        "teacher": "Prof. Lee"
      }
    ]
  }
]
```

`timetables` 的用途：

- `id`：課表唯一識別碼。
- `title`：課表名稱。
- `startDate` / `endDate`：學期或課表有效區間。
- `classes`：這份課表中的所有課程。

---

## 執行方式

需求：

- Java 17+
- Maven 3.8+

執行 App：

```bash
mvn javafx:run
```

如果 `mvn javafx:run` 無法執行，可嘗試：

```bash
mvn compile exec:java
```

---

## 驗證方式

編譯：

```bash
mvn -q -DskipTests compile
```

測試：

```bash
mvn -q test
```

檢查 Git diff 是否有多餘空白或格式問題：

```bash
git diff --check
```

Version 13 已驗證：

- `mvn -q -DskipTests compile`
- `mvn -q test`
- `git diff --check`

---

## Git 注意事項

本版本已將以下內容加入 `.gitignore`：

```text
target/
schedule-data.json
```

原因：

- `target/` 是 Maven 編譯產物，不需要上傳。
- `schedule-data.json` 是本機執行資料，可能包含個人測試內容，不建議直接推到 GitHub。

---

## 待確認項目

- JavaFX UI 需要實際執行確認互動細節，例如右鍵選單、Timeline 點擊圖層切換、編輯頁資料帶入。
- 不同組員的 service / UI 整合後，仍需確認是否都有呼叫 `ScheduleService` 的儲存方法。
- 若未來要多人共用同一份資料格式，需要確認 `schedule-data.json` 的欄位命名是否完全一致。

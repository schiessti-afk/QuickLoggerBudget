# 💡 IDEA.md — QuickLogger

This file is the **product** source of truth (why, who, what). How it is built lives in [`docs/ARCHITECTURE.md`](ARCHITECTURE.md). How it looks lives in [`docs/DESIGN.md`](DESIGN.md). How implementation is sequenced lives in [`docs/Sprint.md`](Sprint.md). If the docs disagree on mechanics, architecture wins; if they disagree on visuals, design wins; if they disagree on product intent, this file wins and the others must be updated.

## 1. Executive Summary
**QuickLogger** is an ultra-minimalist, offline-first Android application designed for one primary objective: **reducing friction to zero when recording everyday cash purchases and physical receipts.**

Instead of navigating complex multi-tab personal finance suites, QuickLogger operates like a rapid-fire point-of-sale utility: open the app, enter the amount, select a category, optionally attach a receipt photo, and save. Logging stays a sub-two-second action. Sharing is an extra step through the system share sheet, not part of that two-second budget.

---

## 2. The Problem
Most personal finance and expense-tracking apps fail in high-friction real-world scenarios:
* **Over-engineered UX:** Logging a simple $4 coffee requires 5–8 taps, date pickers, account selectors, and subcategory navigations.
* **Network Latency & Privacy Concerns:** Apps reliant on cloud sync stall in low-connectivity areas (rural areas, basements, local markets) and collect unnecessary personal telemetry.
* **Friction with Receipts:** Receipts end up cluttering the user's primary photo gallery or getting lost before ever being digitized.
* **Siloed Data:** Sharing an expense immediately with a spouse, business partner, or client requires manually taking a screenshot and opening a messaging app.

---

## 3. The Solution & Core Value Proposition
QuickLogger turns expense logging into a reactive, single-action workflow:
* **Instant Input Focus:** Opening the app puts the user directly in the amount entry field with formatted currency masks.
* **Single-Tap Categorization:** Prominent, customizable chips eliminate nested dropdowns.
* **Isolated Receipt Storage:** Camera captures (`ActivityResultContracts.TakePicture`) and gallery picks (`PickVisualMedia`) are copied into private app storage (`context.filesDir`). Nothing is written to the device gallery or `MediaStore`.
* **WhatsApp-oriented sharing:** After save, a plain-text caption (WhatsApp `*bold*` markup allowed, not Markdown/HTML) goes out through the **system share sheet**. Copy may say “WhatsApp”; the OS chooses the destination. The app does not target `com.whatsapp`.
* **Budget without bookkeeping:** An optional monthly target — overall and per category — turns the amount field into a live "what's left" readout. Setting a target is two taps on the dashboard; not setting one changes nothing about the app.
* **Independent Distribution:** Built and published as a standalone, signed APK without platform store gatekeeping.

---

## 4. User Personas & Use Cases

### Persona A: Independent Contractor / Business Owner
* **Scenario:** Purchases small hardware components, parts, or fuel during errands.
* **Workflow:** Opens QuickLogger, logs R$ 45.00, taps *Supplies*, snaps the paper receipt, and hits *Save & Share*. The system share sheet opens with the caption (and receipt image if present) so they can send it to the company group chat.

### Persona B: Shared Household / Personal Budgeter
* **Scenario:** Manages daily cash expenses (bakeries, markets, street vendors).
* **Workflow:** Quickly captures micro-transactions on the go. Sets a monthly grocery ceiling once and sees the remaining balance while typing each amount. At the end of the week, exports a formatted WhatsApp summary or `.csv` to reconcile with household finances.

---

## 5. System Architecture & Technical Highlights

┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                      │
│   • Jetpack Compose & Material 3                            │
│   • Unidirectional Data Flow (StateFlow / Events)           │
│   • Amount field, category chips, share sheet, camera/picker│
└──────────────────────────────┬──────────────────────────────┘
                               │ depends on
┌──────────────────────────────▼──────────────────────────────┐
│                        Domain Layer                         │
│   • Pure Kotlin use cases (SaveExpense, BuildExpensesCsv, …)│
│   • Models and repository interfaces; no Android types      │
└──────────────────────────────┬──────────────────────────────┘
                               │ implemented by
┌──────────────────────────────▼──────────────────────────────┐
│                         Data Layer                          │
│   • Room Database (reactive Flow queries, SQLite)           │
│   • Private app storage (receipt files)                     │
│   • FileProvider paths for receipts and CSV exports         │
└─────────────────────────────────────────────────────────────┘


### Why This Stands Out in a Portfolio
* **Clean Architecture & MVVM:** Demonstrates proper layer separation, testability, and inversion of control.
* **Modern Android Stack:** Jetpack Compose, Coroutines/Flow, Room DB with KSP, and Material 3 design system.
* **Android OS Interoperability:** Implements `FileProvider`, `ActivityResultContracts`, and system `ACTION_SEND` intents properly according to current platform security standards.
* **Automated Release Pipeline:** GitHub Actions workflow compiling and signing APK artifacts automatically on Git tag creation.

---

## 6. MVP Scope vs. Future Expansion

### In-Scope (MVP)
* Full local CRUD for expenses and categories.
* Instant system-keyboard amount input with dynamic currency formatting.
* Camera receipt capture via `ActivityResultContracts.TakePicture()`.
* Gallery receipt import via the system photo picker (`PickVisualMedia`), copied into private storage.
* WhatsApp-oriented plain-text share (single log and day/week/month summaries) via the system share sheet.
* `.csv` export via Android `FileProvider`.
* Monthly spending targets: one overall and one per category, with remaining-budget feedback on the log screen and a dashboard that visualizes progress.
* Automated standalone signed APK release via GitHub Actions.

### Explicitly Out-of-Scope for MVP (Roadmap)
* Direct cloud database / REST API synchronization.
* Automated Google Sheets API integration via WorkManager.
* Biometric authentication / PIN protection.
* Optical Character Recognition (OCR) for automatic receipt parsing.
* In-app CameraX preview.
* Multi-currency wallets or accounts.
* Budget periods other than the calendar month (weekly, per-paycheck, rolling 30 days).
* Rollover of unspent budget into the next month.
* Recurring expenses.
# Telvique

A clean, full-featured Android Default Phone & Dialer app built with Kotlin, Jetpack Compose, and Android Telecom APIs.

## Features
- **Apple iOS 26 Liquid Glass UI**: Real-time backdrop blur & lens refraction (`fletchmckee.liquid`, `kyant0.backdrop`, `haze`), custom SF Symbols-inspired vector icons, floating draggable Liquid Glass tab bar, and Contact Poster atmospheric In-Call screen
- **Dual Layout Modes**: Switch seamlessly between **Unified** (`Calls`, `Contacts`, `Keypad` + floating `Search` pill) and **Classic** (`Favorites`, `Recents`, `Contacts`, `Keypad`, `Settings`)
- **Default Dialer Integration** (`RoleManager.ROLE_DIALER` & `InCallService`)
- **Dialer**: T9 smart contact search, DTMF keypad tones, haptic feedback, paste, dual-SIM support
- **Recents**: Call history with All / Missed / Incoming / Outgoing filters, search, copy, SMS, block, and delete
- **Contacts**: Favorites strip, A-Z contacts list, multi-number detail sheet, star/unstar, add/edit contact
- **In-Call Screen**: Answer, decline, reject with quick SMS, mute, speaker/Bluetooth/headset routing, DTMF keypad, hold/resume, add call, swap & merge conference calls, proximity screen-off lock, `CallStyle` ongoing & incoming notifications
- **Call Blocking**: Built-in `CallScreeningService` for blocked numbers
- **Settings**: Dialpad tones & vibration, vibrate on connect, preferred SIM, blocked numbers manager, quick reject message editor

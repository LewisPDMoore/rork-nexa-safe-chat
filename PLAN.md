# Add display name, profile photos, snap-style picture messages, and friend profiles

## Sign up & profile fields

- **Add a "Display name" field** at sign up, alongside username, email, and password — display name accepts any text including emoji.
- Display name is saved to your profile and shown across the app (chats, headers, friend profiles).
- Empty-field validation stays in place; display name is required.

## Edit account in Settings

- New **"Account" section** in Settings lets you change:
  - Display name (instant save)
  - Username (checks it's not already taken)
  - Email (sends a confirmation email via Supabase)
  - Password (requires current password, then new password twice)
- Each field opens a clean edit sheet with a save button and inline error messages.

## Profile pictures + avatar (both)

- **Profile screen now supports both**: upload real photos *and* keep the emoji + gradient avatar builder.
- Tap your avatar in Settings to open a picker with two tabs: **Photos** and **Vibe (emoji)**.
- Photos: upload up to 6 images from gallery; first photo is your main profile picture; tap any to set as main, long-press to delete.
- Vibe tab: existing emoji + gradient picker preserved as a fallback shown when no photos are uploaded.
- Photos are stored in a Supabase Storage bucket named **`profile-pics`** (public read).

## Chat list with real names + nicknames

- Each chat row now shows the friend's **display name as the title**, with **@username in smaller text below**.
- Long-press a chat row → **"Set nickname"** dialog. Set a private nickname only you see.
- When a nickname is set, it shows as the main name and the friend's real display name appears smaller below it.
- Nicknames sync across devices (saved to your profile in the backend).

## In-chat header & snap-style picture messages

- Chat header shows friend's profile photo (or avatar), display name, and @username underneath.
- Tap the **camera/attach button** in the message bar to send a picture:
  - Pick from gallery or take a photo.
  - Choose a **timer**: 3s, 5s, 10s, or ∞ (keep).
  - Optional caption.
- Timed pictures open full-screen on tap, count down, then disappear forever (no replay, no save).
- ∞ pictures behave like normal photo messages.
- Pictures are stored in a Supabase Storage bucket named **`chat-media`** (private; signed URLs).

## Friend profile page

- In a chat, tap the **three-dot menu → "Profile"** to open a full friend profile page showing:
  - Their photos (swipeable carousel) or avatar
  - Display name + @username
  - Their vibe emoji
  - "Set nickname" button
  - "Report user" and "Block" actions
- Back button returns to the chat.

## Storage buckets you need to create in Supabase

Please create these two buckets in the Supabase dashboard before testing:

1. **`profile-pics`** — Public bucket. Allows authenticated users to upload to a folder matching their user id.
2. **`chat-media`** — Private bucket. Allows authenticated users to upload; reading uses signed URLs.

I'll also add two small columns to the `profiles` table (`display_name` text, `photos` text[]) and a `nicknames` table (`owner_id`, `friend_id`, `nickname`) — I'll provide the SQL in the chat after the plan is approved so you can run it.

## Design

- Matches the existing violet/indigo gradient and Material 3 style.
- Smooth fade/slide animations between sheets.
- Snap viewer is full-screen black with a circular countdown timer ring around the image.
- Friend profile uses a tall hero image with name overlay, then a soft card list of actions below.

-- Run this once in your Supabase project's SQL editor.
-- Creates the profiles table linked to auth.users.
-- Passwords are stored only by Supabase Auth (bcrypt-hashed in auth.users).

create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    username text unique not null,
    email text not null,
    avatar_emoji text,
    avatar_gradient int,
    created_at timestamptz default now()
);

alter table public.profiles enable row level security;

drop policy if exists "profiles self read"   on public.profiles;
drop policy if exists "profiles self insert" on public.profiles;
drop policy if exists "profiles self update" on public.profiles;

create policy "profiles self read"
    on public.profiles for select
    using (auth.uid() = id);

create policy "profiles self insert"
    on public.profiles for insert
    with check (auth.uid() = id);

create policy "profiles self update"
    on public.profiles for update
    using (auth.uid() = id);

-- Stories: 24-hour photo posts uploaded to the `stories` storage bucket.
create table if not exists public.stories (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    media_url text not null,
    media_type text not null default 'image',
    created_at timestamptz not null default now(),
    expires_at timestamptz not null
);

create index if not exists stories_user_id_expires_at_idx
    on public.stories (user_id, expires_at desc);

alter table public.stories enable row level security;

drop policy if exists "stories read all"     on public.stories;
drop policy if exists "stories owner insert" on public.stories;
drop policy if exists "stories owner delete" on public.stories;

create policy "stories read all"
    on public.stories for select
    using (auth.role() = 'authenticated');

create policy "stories owner insert"
    on public.stories for insert
    with check (auth.uid() = user_id);

create policy "stories owner delete"
    on public.stories for delete
    using (auth.uid() = user_id);

-- Storage buckets (run in dashboard or with create_bucket helpers):
--   profile-pics  — public bucket, path:  {auth.uid()}/{uuid}.jpg
--   chat-media    — private bucket, path: {conversation_id}/{uuid}.jpg
--   stories       — public bucket, path:  {auth.uid()}/{uuid}.jpg
--
-- Suggested storage policies (per bucket):
--   insert: bucket_id = '<bucket>' and auth.role() = 'authenticated'
--           and (storage.foldername(name))[1] = auth.uid()::text   -- for profile-pics & stories
--   select: bucket_id = '<bucket>'                                 -- public buckets only

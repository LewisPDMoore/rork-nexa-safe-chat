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

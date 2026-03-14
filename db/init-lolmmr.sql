create table if not exists summoner_profile (
  puuid text primary key,
  game_name text not null,
  tag_line text not null,
  summoner_id text,
  profile_icon_id int,
  summoner_level int,
  last_profile_sync_at timestamptz not null default now(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_summoner_profile_riotid
  on summoner_profile (lower(game_name), lower(tag_line));

create table if not exists rank_snapshot (
  puuid text not null references summoner_profile(puuid) on delete cascade,
  queue_type text not null,
  tier text,
  rank text,
  league_points int,
  wins int,
  losses int,
  fetched_at timestamptz not null default now(),
  primary key (puuid, queue_type)
);

create table if not exists match_core (
  match_id text primary key,
  queue_id int not null,
  game_duration_seconds int,
  game_end_timestamp bigint,
  game_version text,
  fetched_at timestamptz not null default now()
);

create index if not exists idx_match_core_queue_end
  on match_core (queue_id, game_end_timestamp desc);

create table if not exists match_participant (
  match_id text not null references match_core(match_id) on delete cascade,
  puuid text not null,
  participant_id int,
  team_id int,
  team_position text,
  champion_name text,
  kills int,
  deaths int,
  assists int,
  total_cs int,
  gold_earned int,
  damage_to_champions int,
  vision_score int,
  wards_placed int,
  wards_killed int,
  control_wards_placed int,
  total_time_spent_dead int,
  is_win boolean,
  raw_json jsonb,
  fetched_at timestamptz not null default now(),
  primary key (match_id, puuid)
);

create index if not exists idx_match_participant_puuid
  on match_participant (puuid, fetched_at desc);

create table if not exists timeline_summary (
  match_id text not null references match_core(match_id) on delete cascade,
  puuid text not null,
  gold_diff_15 int,
  cs_diff_15 int,
  xp_diff_15 int,
  objective_participation_score int,
  throw_death_penalty int,
  timeline_fetched_at timestamptz not null default now(),
  primary key (match_id, puuid)
);

create table if not exists analysis_snapshot (
  match_id text not null,
  puuid text not null,
  bucket_minutes int not null,
  payload jsonb not null,
  computed_at timestamptz not null default now(),
  expires_at timestamptz,
  primary key (match_id, puuid, bucket_minutes)
);
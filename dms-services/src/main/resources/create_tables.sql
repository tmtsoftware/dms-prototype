drop index if exists idx_exposure_id;
drop index if exists idx_hdr_exposure_id;

drop table if exists event_snapshots;
drop table if exists keyword_values;

create table event_snapshots
(
    exposure_id    varchar(50) not null,
    obs_event_name varchar(50) not null,
    source         varchar(50) not null,
    eventname      varchar(50) not null,
    eventid        varchar(50) not null,
    eventtime      timestamp   not null,
    paramset       text
);

create index idx_exposure_id
    on event_snapshots (exposure_id);

create table keyword_values
(
    exposure_id varchar(50) not null,
    keyword     varchar(50) not null,
    value       varchar(50) not null
);

create index idx_hdr_exposure_id
    on keyword_values (exposure_id);
